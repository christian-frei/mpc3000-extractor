package christianfrei.mpc3000.floppy.boundary;

import static christianfrei.mpc3000.floppy.Requirement.Rn.*;

import christianfrei.mpc3000.floppy.Requirement;
import christianfrei.mpc3000.floppy.control.Drives;
import christianfrei.mpc3000.floppy.control.ImageFiles;
import christianfrei.mpc3000.floppy.control.MacDrives;
import christianfrei.mpc3000.floppy.entity.CopyReport;
import christianfrei.mpc3000.floppy.entity.Disk;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/// Copies the floppy in the USB drive into an image file; reports progress to output
public interface Floppies {

    static void read(Optional<Path> image, Consumer<String> output) throws IOException {
        read(image, Path.of(""), new MacDrives(), output);
    }

    /// @throws IOException after the image is written when sectors could not be read
    @Requirement({R1_1, R1_2, R1_3, R1_4, R1_5, R1_6, R1_7, R1_8, R1_9, R1_10})
    static void read(Optional<Path> image, Path folder, Drives drives, Consumer<String> output) throws IOException {
        var disk = floppy(drives.externalDisks());
        var target = image.orElseGet(() -> ImageFiles.nextFree(folder));
        if (Files.exists(target))
            throw new IllegalArgumentException("already exists, not overwritten: " + target);
        output.accept("reading %s (%,d bytes) into %s — confirm the administrator password dialog".formatted(disk.rawDevice(), disk.size(), target));
        String copyOutput;
        try {
            copyOutput = drives.copy(disk, target);
        } catch (CancellationException e) {
            Files.deleteIfExists(target);
            throw new IllegalArgumentException("cancelled, no image written");
        }
        var report = CopyReport.of(copyOutput, Files.size(target));
        output.accept("wrote %s, %,d bytes".formatted(target, report.bytes()));
        if (report.unreadableSectors() > 0)
            throw new IOException("%d sector(s) could not be read and were filled with zeros".formatted(report.unreadableSectors()));
        if (!report.complete(disk))
            throw new IOException("incomplete image: %,d of %,d bytes".formatted(report.bytes(), disk.size()));
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
