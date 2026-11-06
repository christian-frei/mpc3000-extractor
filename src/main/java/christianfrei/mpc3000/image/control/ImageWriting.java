package christianfrei.mpc3000.image.control;

import christianfrei.mpc3000.image.entity.DirectoryEntry;
import christianfrei.mpc3000.image.entity.DiskLayout;
import christianfrei.mpc3000.image.entity.Fat;
import christianfrei.mpc3000.image.entity.RestoredFile;
import christianfrei.mpc3000.image.entity.RestoredImage;
import christianfrei.mpc3000.image.entity.RestoredVolume;
import christianfrei.mpc3000.image.entity.VolumeFormat;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/// Writes a restored image the way the MPC3000 formats and fills a disk
public interface ImageWriting {

    /// Creates image, which must not exist; a failed write leaves no file behind
    static void write(RestoredImage restored, Path image) throws IOException {
        try (var out = FileChannel.open(image, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            if (restored.floppy())
                writeFloppy(out, restored.volumes().getFirst());
            else
                writeHardDisk(out, restored.volumes());
        } catch (IOException | RuntimeException e) {
            if (!(e instanceof FileAlreadyExistsException))
                Files.deleteIfExists(image);
            throw e;
        }
    }

    static void writeFloppy(FileChannel out, RestoredVolume volume) throws IOException {
        var sectors = VolumeFormat.FLOPPY_SECTORS;
        writeVolume(out, 0, VolumeFormat.FLOPPY, sectors, 0, 0, volume);
        extend(out, sectors * VolumeFormat.SECTOR);
    }

    /// Every partition is formatted; those without a folder stay empty
    static void writeHardDisk(FileChannel out, List<RestoredVolume> volumes) throws IOException {
        write(out, 0, DiskLayout.masterBootRecord());
        for (var index = 0; index < DiskLayout.PARTITIONS; index++) {
            var label = DiskLayout.label(index);
            var volume = volumes.stream()
                    .filter(v -> v.label().equals(label))
                    .findFirst()
                    .orElse(new RestoredVolume(label, List.of()));
            var start = DiskLayout.partitionStart(index);
            write(out, DiskLayout.ebrSector(index) * VolumeFormat.SECTOR, DiskLayout.extendedBootRecord(index));
            writeVolume(out, start * VolumeFormat.SECTOR, VolumeFormat.HARD_DISK_PARTITION,
                    DiskLayout.partitionSectors(index), start, index + 1, volume);
        }
        extend(out, DiskLayout.IMAGE_BYTES);
    }

    /// Files lie contiguously from the format's first cluster on, in the volume's order
    static void writeVolume(FileChannel out, long offset, VolumeFormat format, long sectors, long hiddenSectors,
                            int serialNumber, RestoredVolume volume) throws IOException {
        write(out, offset, format.bootSector(sectors, hiddenSectors, serialNumber, volume.label()));
        var table = ByteBuffer.allocate(format.fatSize()).order(ByteOrder.LITTLE_ENDIAN).put(0, format.fatStart());
        var fat = new Fat(table);
        var directory = ByteBuffer.allocate(format.rootEntries() * DirectoryEntry.LENGTH);
        var position = 0;
        if (!volume.label().isEmpty()) {
            directory.put(position, DirectoryEntry.label(volume.label()));
            position += DirectoryEntry.LENGTH;
        }
        var cluster = format.firstCluster();
        for (var file : volume.files()) {
            var clusters = format.clustersFor(file.size());
            for (var i = 0; i < clusters; i++)
                fat.set(cluster + i, i == clusters - 1 ? Fat.LAST_CLUSTER : cluster + i + 1);
            directory.put(position, DirectoryEntry.encode(file.name(), file.extension(), clusters == 0 ? 0 : cluster, file.size()));
            position += DirectoryEntry.LENGTH;
            copy(file, out, offset + format.clusterOffset(cluster));
            cluster += clusters;
        }
        write(out, offset + format.fatOffset(), table.array());
        write(out, offset + format.fatOffset() + format.fatSize(), table.array());
        write(out, offset + format.rootDirectoryOffset(), directory.array());
    }

    static void copy(RestoredFile file, FileChannel out, long position) throws IOException {
        try (var in = FileChannel.open(file.source(), StandardOpenOption.READ)) {
            var copied = 0L;
            while (copied < file.size()) {
                var transferred = out.transferFrom(in, position + copied, file.size() - copied);
                if (transferred == 0)
                    throw new IOException("file changed while restoring: " + file.source());
                copied += transferred;
            }
        }
    }

    static void write(FileChannel out, long position, byte[] bytes) throws IOException {
        var buffer = ByteBuffer.wrap(bytes);
        while (buffer.hasRemaining())
            out.write(buffer, position + buffer.position());
    }

    /// Unwritten space stays a hole in the file; it reads as zeros
    static void extend(FileChannel out, long size) throws IOException {
        if (out.size() < size)
            write(out, size - 1, new byte[1]);
    }
}
