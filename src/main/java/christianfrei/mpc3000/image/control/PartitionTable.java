package christianfrei.mpc3000.image.control;

import christianfrei.mpc3000.image.entity.PartitionEntry;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/// Finds the FAT partitions of an MBR disk, including logical ones in the extended partition
interface PartitionTable {

    static List<Long> fatPartitionStarts(FileChannel channel) throws IOException {
        var starts = new ArrayList<Long>();
        for (var entry : PartitionEntry.of(Sectors.read(channel, 0, Sectors.LENGTH))) {
            if (entry.isFat())
                starts.add(entry.firstSector());
            else if (entry.isExtended())
                starts.addAll(logicalStarts(channel, entry.firstSector()));
        }
        return starts;
    }

    /// Walks the EBR chain: the first entry is relative to its own EBR, the link to the next EBR relative to the extended partition
    static List<Long> logicalStarts(FileChannel channel, long extendedStart) throws IOException {
        var starts = new ArrayList<Long>();
        var visited = new HashSet<Long>();
        var ebr = extendedStart;
        while (visited.add(ebr)) {
            var entries = PartitionEntry.of(Sectors.read(channel, ebr * Sectors.LENGTH, Sectors.LENGTH));
            if (entries.isEmpty())
                return starts;
            if (entries.getFirst().isFat())
                starts.add(ebr + entries.getFirst().firstSector());
            if (entries.size() < 2 || !entries.get(1).isExtended())
                return starts;
            ebr = extendedStart + entries.get(1).firstSector();
        }
        return starts;
    }
}
