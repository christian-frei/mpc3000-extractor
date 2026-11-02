package christianfrei.mpc3000.image.entity;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/// A FAT12 allocation table
public record Fat(ByteBuffer table) {

    static final int END_OF_CHAIN = 0xFF8;

    /// FAT12 packs two 12-bit entries into three bytes
    public int next(int cluster) {
        var offset = cluster + cluster / 2;
        if (offset + 1 >= table.limit())
            return END_OF_CHAIN;
        var value = Short.toUnsignedInt(table.getShort(offset));
        return (cluster & 1) == 0 ? value & 0xFFF : value >> 4;
    }

    /// Follows the chain from first, stopping at its end, at an invalid cluster or after limit clusters
    public List<Integer> chain(int first, int clusterCount, int limit) {
        var clusters = new ArrayList<Integer>();
        var cluster = first;
        while (clusters.size() < limit && cluster >= 2 && cluster < clusterCount + 2) {
            clusters.add(cluster);
            cluster = next(cluster);
        }
        return clusters;
    }
}
