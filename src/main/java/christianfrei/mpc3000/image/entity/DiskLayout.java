package christianfrei.mpc3000.image.entity;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/// The MPC3000's 23-partition hard disk: an extended partition chaining one logical FAT12 partition per
/// 4-cylinder slot (255 heads × 63 sectors), the first slot starting at sector 63
public interface DiskLayout {

    int PARTITIONS = 23;
    long SLOT_SECTORS = 64_260;
    long EXTENDED_START = 63;
    int HEADS = 255;
    int SECTORS_PER_TRACK = 63;
    long IMAGE_BYTES = 779_091_968;
    int DISK_SIGNATURE = 0x0720FECA;
    int EXTENDED = 0x05;
    int FAT12 = 0x01;
    /// Every EBR repeats the partitions' geometry (16 sectors per track, 16 heads) and the start sectors of all partitions
    int GEOMETRY_OFFSET = 0x18;
    int PARTITION_STARTS_OFFSET = 0x80;

    static String label(int index) {
        return "MPC-PART--" + (char) ('A' + index);
    }

    /// The EBR of partition index; the first one is the extended partition's own first sector
    static long ebrSector(int index) {
        return index == 0 ? EXTENDED_START : index * SLOT_SECTORS;
    }

    static long partitionStart(int index) {
        return ebrSector(index) + SECTORS_PER_TRACK;
    }

    static long partitionSectors(int index) {
        return (index + 1) * SLOT_SECTORS - partitionStart(index);
    }

    static byte[] masterBootRecord() {
        var sector = table();
        entry(sector, 0, EXTENDED, EXTENDED_START, PARTITIONS * SLOT_SECTORS - EXTENDED_START);
        return sector.array();
    }

    /// Slot 1 holds the logical partition relative to this EBR, slot 2 the next EBR relative to the extended partition
    static byte[] extendedBootRecord(int index) {
        var sector = table();
        sector.putShort(GEOMETRY_OFFSET, (short) VolumeFormat.HARD_DISK_PARTITION.sectorsPerTrack())
                .putShort(GEOMETRY_OFFSET + 2, (short) VolumeFormat.HARD_DISK_PARTITION.heads());
        for (var i = 0; i < PARTITIONS; i++)
            sector.putInt(PARTITION_STARTS_OFFSET + 4 * i, (int) partitionStart(i));
        var ebr = ebrSector(index);
        entry(sector, 0, FAT12, partitionStart(index), partitionSectors(index), ebr);
        if (index + 1 < PARTITIONS)
            entry(sector, 1, EXTENDED, ebrSector(index + 1), SLOT_SECTORS, EXTENDED_START);
        return sector.array();
    }

    static ByteBuffer table() {
        var sector = ByteBuffer.allocate(VolumeFormat.SECTOR).order(ByteOrder.LITTLE_ENDIAN);
        sector.putInt(0x1B8, DISK_SIGNATURE);
        sector.putShort(510, (short) 0xAA55);
        return sector;
    }

    static void entry(ByteBuffer sector, int slot, int type, long first, long sectors) {
        entry(sector, slot, type, first, sectors, 0);
    }

    /// first is absolute; the stored start is relative to base
    static void entry(ByteBuffer sector, int slot, int type, long first, long sectors, long base) {
        var offset = PartitionEntry.TABLE_OFFSET + slot * PartitionEntry.SLOT_LENGTH;
        sector.put(offset + 1, chs(first));
        sector.put(offset + 4, (byte) type);
        sector.put(offset + 5, chs(first + sectors - 1));
        sector.putInt(offset + 8, (int) (first - base));
        sector.putInt(offset + 12, (int) sectors);
    }

    /// Head, sector with the cylinder's top two bits, cylinder's low eight bits
    static byte[] chs(long lba) {
        var cylinder = Math.min(lba / ((long) HEADS * SECTORS_PER_TRACK), 1023);
        var head = (lba / SECTORS_PER_TRACK) % HEADS;
        var sector = lba % SECTORS_PER_TRACK + 1;
        return new byte[]{(byte) head, (byte) (sector | ((cylinder >> 2) & 0xC0)), (byte) cylinder};
    }
}
