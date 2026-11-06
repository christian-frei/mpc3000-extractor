package christianfrei.mpc3000.floppy.boundary;

import static christianfrei.mpc3000.floppy.Requirement.Rn.*;

import christianfrei.mpc3000.floppy.Requirement;
import christianfrei.mpc3000.floppy.control.Drives;
import christianfrei.mpc3000.floppy.control.ImageFiles;
import christianfrei.mpc3000.floppy.control.MacDrives;
import christianfrei.mpc3000.floppy.entity.CopyReport;
import christianfrei.mpc3000.floppy.entity.Disk;
import christianfrei.mpc3000.floppy.entity.FloppyImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/// Copies floppies in the USB drive to and from image files; reports progress to output
public interface Floppies {

    static void read(Optional<Path> image, Consumer<String> output) throws IOException {
        read(image, Path.of(""), new MacDrives(), output);
    }

    /// @throws IOException after the image is written when sectors could not be read
    @Requirement({R1_1, R1_2, R1_3, R1_4, R1_5, R1_6, R1_7, R1_8, R1_9, R1_10, R1_11, R1_12})
    static void read(Optional<Path> image, Path folder, Drives drives, Consumer<String> output) throws IOException {
        var disk = floppy(drives.externalDisks());
        var target = image.orElseGet(() -> ImageFiles.nextFree(folder));
        if (Files.exists(target))
            throw new IllegalArgumentException("already exists, not overwritten: " + target);
        if (!drives.canAskForPassword())
            throw new IllegalArgumentException("run floppy-read in a terminal: sudo asks for the administrator password there");
        output.accept("reading %s (%,d bytes) into %s — enter your administrator password if asked".formatted(disk.rawDevice(), disk.size(), target));
        String copyOutput;
        try {
            copyOutput = drives.copy(disk, target);
        } catch (CancellationException e) {
            Files.deleteIfExists(target);
            throw new IllegalArgumentException("password prompt cancelled or failed, no image written");
        } catch (IOException e) {
            Files.deleteIfExists(target);
            throw e;
        }
        var report = CopyReport.of(copyOutput, Files.size(target));
        output.accept("wrote %s, %,d bytes".formatted(target, report.bytes()));
        if (report.unreadableSectors() > 0)
            throw new IOException("%d sector(s) could not be read and were filled with zeros".formatted(report.unreadableSectors()));
        if (!report.complete(disk))
            throw new IOException("incomplete image: %,d of %,d bytes".formatted(report.bytes(), disk.size()));
    }

    static void write(Path image, Consumer<String> output) throws IOException {
        write(image, new MacDrives(), output);
    }

    /// @throws IOException when the disk could not be written or reads back differently
    @Requirement({R2_1, R2_2, R2_3, R2_4, R2_5, R2_6, R2_7, R2_8, R2_9, R2_10, R2_11})
    static void write(Path image, Drives drives, Consumer<String> output) throws IOException {
        if (!Files.isRegularFile(image))
            throw new IllegalArgumentException("image not found: " + image);
        var disk = floppy(drives.externalDisks());
        var floppyImage = new FloppyImage(Files.readAllBytes(image));
        if (!floppyImage.fits(disk))
            throw new IllegalArgumentException("%s has %,d bytes, the floppy in %s holds %,d".formatted(image, floppyImage.content().length, disk.identifier(), disk.size()));
        if (!floppyImage.startsWithFatBootSector())
            throw new IllegalArgumentException("not a floppy image, no FAT boot sector: " + image);
        if (drives.isWriteProtected(disk))
            throw new IllegalArgumentException("the floppy in %s is write-protected; close its write-protect hole".formatted(disk.identifier()));
        if (!drives.canAskForPassword())
            throw new IllegalArgumentException("run floppy-write in a terminal: it asks for confirmation and sudo for the administrator password there");
        if (!drives.confirm("overwrite everything on the floppy in %s (%,d bytes) with %s? [y/N] ".formatted(disk.identifier(), disk.size(), image)))
            throw new IllegalArgumentException("not confirmed, floppy left untouched");
        output.accept("writing %s onto %s — enter your administrator password if asked".formatted(image, disk.rawDevice()));
        var workspace = Files.createTempDirectory("mpc3000-");
        var readBack = workspace.resolve("read-back.img");
        try {
            drives.write(disk, image, readBack);
            var differing = floppyImage.differingSectors(Files.exists(readBack) ? Files.readAllBytes(readBack) : new byte[0]);
            if (differing > 0)
                throw new IOException("%d sector(s) read back differently; the floppy has been ejected, use another disk".formatted(differing));
        } catch (CancellationException e) {
            throw new IllegalArgumentException("password prompt cancelled or failed, floppy left untouched");
        } finally {
            Files.deleteIfExists(readBack);
            Files.delete(workspace);
        }
        output.accept("wrote and verified %s on %s, %,d bytes; the floppy has been ejected".formatted(image, disk.identifier(), disk.size()));
    }

    static Disk floppy(List<Disk> disks) {
        var floppies = disks.stream().filter(Disk::isFloppy).toList();
        if (floppies.isEmpty())
            throw new IllegalArgumentException("no floppy disk found; insert a disk into the USB floppy drive");
        if (floppies.size() > 1)
            throw new IllegalArgumentException("several floppy disks attached: %s; leave only one".formatted(
                    floppies.stream().map(Disk::identifier).collect(Collectors.joining(", "))));
        return floppies.getFirst();
    }
}
