package christianfrei.mpc3000.image.entity;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/// A FAT12 allocation table
public record Fat(ByteBuffer table) {

    static final int END_OF_CHAIN = 0xFF8;
    /// The end-of-chain mark the MPC3000 writes
    public static final int LAST_CLUSTER = 0xFFF;

    /// FAT12 packs two 12-bit entries into three bytes
    public int next(int cluster) {
        var offset = cluster + cluster / 2;
        if (offset + 1 >= table.limit())
            return END_OF_CHAIN;
        var value = Short.toUnsignedInt(table.getShort(offset));
        return (cluster & 1) == 0 ? value & 0xFFF : value >> 4;
    }

    public void set(int cluster, int value) {
        var offset = cluster + cluster / 2;
        var current = Short.toUnsignedInt(table.getShort(offset));
        var updated = (cluster & 1) == 0 ? (current & 0xF000) | value : (current & 0x000F) | (value << 4);
        table.putShort(offset, (short) updated);
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
