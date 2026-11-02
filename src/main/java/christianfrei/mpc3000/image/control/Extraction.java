package christianfrei.mpc3000.image.control;

import christianfrei.mpc3000.image.entity.DirectoryEntry;
import christianfrei.mpc3000.image.entity.ExtractedPartition;
import christianfrei.mpc3000.image.entity.Fat;
import christianfrei.mpc3000.image.entity.Volume;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/// Copies the files of an image's partitions into host folders
public interface Extraction {

    /// Writes all files of every partition containing sounds into a subfolder of target named after the partition label
    static List<ExtractedPartition> extract(Path image, Path target) throws IOException {
        try (var channel = FileChannel.open(image, StandardOpenOption.READ)) {
            var extracted = new ArrayList<ExtractedPartition>();
            var volumes = Volumes.read(channel);
            for (var i = 0; i < volumes.size(); i++) {
                var volume = volumes.get(i);
                if (!volume.hasSounds())
                    continue;
                var folder = Files.createDirectories(target.resolve(HostNames.safe(volume.folderName(i + 1))));
                var fat = new Fat(Sectors.read(channel, volume.offset() + volume.boot().fatOffset(), volume.boot().fatSize()));
                var taken = new HashSet<String>();
                for (var entry : volume.entries())
                    extractFile(channel, volume, fat, entry, folder.resolve(HostNames.unique(HostNames.safe(entry.fileName()), taken)));
                extracted.add(new ExtractedPartition(folder, volume.entries().size(), volume.fileBytes()));
            }
            return extracted;
        }
    }

    static void extractFile(FileChannel channel, Volume volume, Fat fat, DirectoryEntry entry, Path file) throws IOException {
        var boot = volume.boot();
        var needed = boot.clustersFor(entry.size());
        var clusters = fat.chain(entry.firstCluster(), boot.clusterCount(), needed);
        if (clusters.size() < needed)
            throw new IOException("broken cluster chain: " + volume.label() + "/" + entry.fileName());
        try (var out = FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            var remaining = entry.size();
            for (var cluster : clusters) {
                var length = Math.min(remaining, boot.clusterSize());
                var position = volume.offset() + boot.clusterOffset(cluster);
                var written = 0L;
                while (written < length)
                    written += channel.transferTo(position + written, length - written, out);
                remaining -= length;
            }
        }
    }
}
