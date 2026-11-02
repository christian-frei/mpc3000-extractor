package christianfrei.mpc3000.image.control;

import christianfrei.mpc3000.image.entity.BootSector;
import christianfrei.mpc3000.image.entity.DirectoryEntry;
import christianfrei.mpc3000.image.entity.Volume;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/// Reads the FAT12 volumes of a hard disk image (partitioned) or a floppy image (a single volume)
public interface Volumes {

    static List<Volume> read(Path image) throws IOException {
        try (var channel = FileChannel.open(image, StandardOpenOption.READ)) {
            return read(channel);
        }
    }

    /// @throws IllegalArgumentException when the image holds no FAT volume
    static List<Volume> read(FileChannel channel) throws IOException {
        if (BootSector.isFat(Sectors.read(channel, 0, Sectors.LENGTH)))
            return List.of(volume(channel, 0));
        var volumes = new ArrayList<Volume>();
        for (var startSector : PartitionTable.fatPartitionStarts(channel))
            volumes.add(volume(channel, startSector * Sectors.LENGTH));
        if (volumes.isEmpty())
            throw new IllegalArgumentException("not an MPC3000 image: no FAT partition found");
        return volumes;
    }

    static Volume volume(FileChannel channel, long offset) throws IOException {
        var boot = BootSector.parse(Sectors.read(channel, offset, Sectors.LENGTH));
        var entries = DirectoryEntry.parse(Sectors.read(channel, offset + boot.rootDirectoryOffset(), boot.rootDirectorySize()));
        var label = entries.stream()
                .filter(DirectoryEntry::isVolumeLabel)
                .map(DirectoryEntry::fileName)
                .findFirst()
                .orElse(boot.label());
        var files = entries.stream().filter(DirectoryEntry::isFile).toList();
        return new Volume(label, offset, boot, files);
    }
}
