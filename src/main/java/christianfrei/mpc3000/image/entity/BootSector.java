package christianfrei.mpc3000.image.entity;

import java.nio.ByteBuffer;

/// The BIOS parameter block of a FAT12 volume and the layout derived from it
public record BootSector(String label, int bytesPerSector, int sectorsPerCluster, int reservedSectors,
                         int fats, int rootEntries, long totalSectors, int sectorsPerFat) {

    public static final int LENGTH = 512;

    public static boolean hasSignature(ByteBuffer sector) {
        return Short.toUnsignedInt(sector.getShort(510)) == 0xAA55;
    }

    /// A FAT volume starts with a jump instruction and a sane BPB; an MBR does not
    public static boolean isFat(ByteBuffer sector) {
        var jump = Byte.toUnsignedInt(sector.get(0));
        var bytesPerSector = Short.toUnsignedInt(sector.getShort(11));
        return hasSignature(sector) && (jump == 0xEB || jump == 0xE9) && bytesPerSector == LENGTH;
    }

    public static BootSector parse(ByteBuffer sector) {
        var totalSectors = Short.toUnsignedInt(sector.getShort(19));
        return new BootSector(
                DirectoryEntry.text(sector, 43, 11),
                Short.toUnsignedInt(sector.getShort(11)),
                Byte.toUnsignedInt(sector.get(13)),
                Short.toUnsignedInt(sector.getShort(14)),
                Byte.toUnsignedInt(sector.get(16)),
                Short.toUnsignedInt(sector.getShort(17)),
                totalSectors != 0 ? totalSectors : Integer.toUnsignedLong(sector.getInt(32)),
                Short.toUnsignedInt(sector.getShort(22)));
    }

    public long fatOffset() {
        return (long) reservedSectors * bytesPerSector;
    }

    public int fatSize() {
        return sectorsPerFat * bytesPerSector;
    }

    public long rootDirectoryOffset() {
        return fatOffset() + (long) fats * fatSize();
    }

    public int rootDirectorySize() {
        return rootEntries * DirectoryEntry.LENGTH;
    }

    public long dataOffset() {
        var rootBytes = (long) rootDirectorySize();
        return rootDirectoryOffset() + (rootBytes + bytesPerSector - 1) / bytesPerSector * bytesPerSector;
    }

    /// Clusters are numbered from 2; the first one starts at the data region
    public long clusterOffset(int cluster) {
        return dataOffset() + (cluster - 2L) * clusterSize();
    }

    public int clusterCount() {
        return (int) ((capacity() - dataOffset()) / clusterSize());
    }

    public int clustersFor(long bytes) {
        return (int) ((bytes + clusterSize() - 1) / clusterSize());
    }

    public long capacity() {
        return totalSectors * bytesPerSector;
    }

    public long clusterSize() {
        return (long) sectorsPerCluster * bytesPerSector;
    }
}
