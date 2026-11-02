import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/// Builds small MPC3000-style FAT12 images in memory: one sector per cluster, 64 sectors per volume
class FatImage {

    static final int SECTOR = 512;
    static final int ROOT_ENTRIES = 16;
    static final int VOLUME_SECTORS = 64;
    static final int DATA_SECTOR = 4;

    /// A file stored in the given clusters; size beyond the clusters simulates a broken chain
    record Entry(String name, String extension, int size, int... clusters) {

        byte[] content() {
            var bytes = new byte[size];
            var seed = (name + extension).hashCode();
            for (var i = 0; i < size; i++)
                bytes[i] = (byte) (seed + i * 31);
            return bytes;
        }
    }

    record Partition(String label, List<Entry> entries) {

        static Partition of(String label, Entry... entries) {
            return new Partition(label, List.of(entries));
        }
    }

    static byte[] floppy(Partition partition) {
        var buffer = ByteBuffer.allocate(VOLUME_SECTORS * SECTOR).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(0, new byte[]{(byte) 0xEB, 0x3C, (byte) 0x90});
        ascii(buffer, 3, "MPC-3000", 8);
        buffer.putShort(11, (short) SECTOR);
        buffer.put(13, (byte) 1);
        buffer.putShort(14, (short) 1);
        buffer.put(16, (byte) 2);
        buffer.putShort(17, (short) ROOT_ENTRIES);
        buffer.putShort(19, (short) VOLUME_SECTORS);
        buffer.put(21, (byte) 0xF8);
        buffer.putShort(22, (short) 1);
        ascii(buffer, 43, partition.label(), 11);
        buffer.putShort(510, (short) 0xAA55);

        var fat = new byte[SECTOR];
        fat[0] = (byte) 0xF8;
        fat[1] = (byte) 0xFF;
        fat[2] = (byte) 0xFF;
        var root = 3 * SECTOR;
        ascii(buffer, root, partition.label(), 11);
        buffer.put(root + 11, (byte) 0x08);
        var slot = 1;
        for (var entry : partition.entries()) {
            var offset = root + slot++ * 32;
            var name = "%-16s".formatted(entry.name());
            ascii(buffer, offset, name.substring(0, 8), 8);
            ascii(buffer, offset + 8, entry.extension(), 3);
            if (!name.substring(8).isBlank())
                ascii(buffer, offset + 12, name.substring(8), 8);
            buffer.putShort(offset + 26, (short) entry.clusters()[0]);
            buffer.putInt(offset + 28, entry.size());
            var content = entry.content();
            var clusters = entry.clusters();
            for (var i = 0; i < clusters.length; i++) {
                var from = i * SECTOR;
                var length = Math.min(SECTOR, content.length - from);
                if (length > 0)
                    buffer.put((DATA_SECTOR + clusters[i] - 2) * SECTOR, content, from, length);
                setFat(fat, clusters[i], i + 1 < clusters.length ? clusters[i + 1] : 0xFFF);
            }
        }
        buffer.put(SECTOR, fat);
        buffer.put(2 * SECTOR, fat);
        return buffer.array();
    }

    /// An MBR whose extended partition chains one logical FAT12 partition per given partition
    static byte[] disk(Partition... partitions) {
        var stride = 1 + VOLUME_SECTORS;
        var buffer = ByteBuffer.allocate((1 + partitions.length * stride) * SECTOR).order(ByteOrder.LITTLE_ENDIAN);
        slot(buffer, 0, 0x05, 1, partitions.length * stride);
        for (var i = 0; i < partitions.length; i++) {
            var ebr = (1 + i * stride) * SECTOR;
            slot(buffer, ebr, 0x01, 1, VOLUME_SECTORS);
            if (i + 1 < partitions.length)
                slot(buffer, ebr + 16, 0x05, (i + 1) * stride, stride);
            buffer.putShort(ebr + 510, (short) 0xAA55);
            buffer.put(ebr + SECTOR, floppy(partitions[i]));
        }
        buffer.putShort(510, (short) 0xAA55);
        return buffer.array();
    }

    static Path write(byte[] image) {
        try {
            var file = Files.createTempFile("mpc3000-", ".img");
            file.toFile().deleteOnExit();
            return Files.write(file, image);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void slot(ByteBuffer buffer, int sectorOffset, int type, long firstSector, long sectors) {
        var offset = sectorOffset + 0x1BE;
        buffer.put(offset + 4, (byte) type);
        buffer.putInt(offset + 8, (int) firstSector);
        buffer.putInt(offset + 12, (int) sectors);
    }

    static void setFat(byte[] fat, int cluster, int value) {
        var offset = cluster + cluster / 2;
        if ((cluster & 1) == 0) {
            fat[offset] = (byte) value;
            fat[offset + 1] = (byte) ((fat[offset + 1] & 0xF0) | ((value >> 8) & 0x0F));
        } else {
            fat[offset] = (byte) ((fat[offset] & 0x0F) | ((value << 4) & 0xF0));
            fat[offset + 1] = (byte) (value >> 4);
        }
    }

    static void ascii(ByteBuffer buffer, int offset, String text, int length) {
        var bytes = Arrays.copyOf(("%-" + length + "s").formatted(text).getBytes(StandardCharsets.US_ASCII), length);
        buffer.put(offset, bytes);
    }
}
