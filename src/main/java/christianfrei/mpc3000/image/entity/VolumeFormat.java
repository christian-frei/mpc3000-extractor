package christianfrei.mpc3000.image.entity;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/// How the MPC3000 formats a FAT12 volume, read off a hard disk partition and a floppy it formatted
public record VolumeFormat(byte[] jump, String oemName, int sectorsPerCluster, int rootEntries, int sectorsPerFat,
                           int media, int sectorsPerTrack, int heads, byte[] fatStart, int firstCluster, boolean extendedBootRecord) {

    public static final int SECTOR = 512;
    static final int RESERVED_SECTORS = 1;
    static final int FATS = 2;

    /// Clusters 2 and 3 are reserved by the MPC3000 (FAT starts `F8 FF FF FF FF 00`), files start at cluster 4
    public static final VolumeFormat HARD_DISK_PARTITION = new VolumeFormat(
            new byte[]{(byte) 0xEB, 0x3C, (byte) 0x90}, "MPC-3000", 16, 512, 11, 0xF8, 16, 16,
            new byte[]{(byte) 0xF8, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x00}, 4, true);

    /// 1.44 MB: no OEM name, no label, no serial; files start at cluster 2
    public static final VolumeFormat FLOPPY = new VolumeFormat(
            new byte[]{(byte) 0xEB, 0x34, (byte) 0x90}, "", 1, 224, 9, 0xF0, 18, 2,
            new byte[]{(byte) 0xF0, (byte) 0xFF, (byte) 0xFF}, 2, false);

    public static final long FLOPPY_SECTORS = 2880;

    /// No boot code: everything beyond the BIOS parameter block stays zero
    public byte[] bootSector(long totalSectors, long hiddenSectors, int serialNumber, String label) {
        var sector = ByteBuffer.allocate(SECTOR).order(ByteOrder.LITTLE_ENDIAN);
        sector.put(0, jump);
        if (!oemName.isEmpty())
            sector.put(3, DirectoryEntry.padded(oemName, 8));
        sector.putShort(11, (short) SECTOR)
                .put(13, (byte) sectorsPerCluster)
                .putShort(14, (short) RESERVED_SECTORS)
                .put(16, (byte) FATS)
                .putShort(17, (short) rootEntries)
                .putShort(19, (short) totalSectors)
                .put(21, (byte) media)
                .putShort(22, (short) sectorsPerFat)
                .putShort(24, (short) sectorsPerTrack)
                .putShort(26, (short) heads)
                .putInt(28, (int) hiddenSectors);
        if (extendedBootRecord) {
            sector.put(36, (byte) 0x82)
                    .put(38, (byte) 0x29)
                    .putInt(39, 0xCAFE0000 | serialNumber)
                    .put(43, DirectoryEntry.padded(label, 11))
                    .put(54, DirectoryEntry.padded("FAT12", 8));
        }
        sector.putShort(510, (short) 0xAA55);
        return sector.array();
    }

    public long fatOffset() {
        return (long) RESERVED_SECTORS * SECTOR;
    }

    public int fatSize() {
        return sectorsPerFat * SECTOR;
    }

    public long rootDirectoryOffset() {
        return fatOffset() + (long) FATS * fatSize();
    }

    public long dataOffset() {
        return rootDirectoryOffset() + (long) rootEntries * DirectoryEntry.LENGTH;
    }

    public long clusterSize() {
        return (long) sectorsPerCluster * SECTOR;
    }

    public long clusterOffset(int cluster) {
        return dataOffset() + (cluster - 2L) * clusterSize();
    }

    /// Clusters available for files on a volume of the given size, bounded by the data region and by the FAT's entries
    public int freeClusters(long totalSectors) {
        var dataClusters = (int) ((totalSectors * SECTOR - dataOffset()) / clusterSize());
        var fatEntries = fatSize() * 2 / 3;
        return Math.min(dataClusters + 2, fatEntries) - firstCluster;
    }

    /// Root directory entries left for files; a labeled volume spends one on its label
    public int fileEntries() {
        return extendedBootRecord ? rootEntries - 1 : rootEntries;
    }

    public int clustersFor(long bytes) {
        return (int) ((bytes + clusterSize() - 1) / clusterSize());
    }
}
