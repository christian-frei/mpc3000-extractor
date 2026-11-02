package christianfrei.mpc3000.image.entity;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/// A 32-byte FAT directory entry carrying a 16-character MPC name
public record DirectoryEntry(String name, String extension, int attributes, int firstCluster, long size) {

    public static final int LENGTH = 32;
    static final int VOLUME_LABEL = 0x08;
    static final int DIRECTORY = 0x10;
    static final int LONG_NAME = 0x0F;
    static final int DELETED = 0xE5;
    /// MPC names have 16 characters: 8 in the regular name field, 8 more in otherwise unused bytes 12–19
    static final int NAME_TAIL = 12;

    public static List<DirectoryEntry> parse(ByteBuffer directory) {
        var entries = new ArrayList<DirectoryEntry>();
        for (var offset = 0; offset + LENGTH <= directory.limit(); offset += LENGTH) {
            var marker = Byte.toUnsignedInt(directory.get(offset));
            if (marker == 0)
                break;
            if (marker == DELETED)
                continue;
            entries.add(new DirectoryEntry(
                    (raw(directory, offset, 8) + raw(directory, offset + NAME_TAIL, 8)).strip(),
                    text(directory, offset + 8, 3),
                    Byte.toUnsignedInt(directory.get(offset + 11)),
                    Short.toUnsignedInt(directory.getShort(offset + 26)),
                    Integer.toUnsignedLong(directory.getInt(offset + 28))));
        }
        return entries;
    }

    public boolean isVolumeLabel() {
        return attributes != LONG_NAME && (attributes & VOLUME_LABEL) != 0;
    }

    public boolean isFile() {
        return attributes != LONG_NAME && (attributes & (VOLUME_LABEL | DIRECTORY)) == 0;
    }

    public String fileName() {
        if (isVolumeLabel())
            return (name + extension).strip();
        return extension.isEmpty() ? name : name + "." + extension;
    }

    public FileType type() {
        return FileType.of(extension);
    }

    static String text(ByteBuffer buffer, int offset, int length) {
        return raw(buffer, offset, length).strip();
    }

    static String raw(ByteBuffer buffer, int offset, int length) {
        var bytes = new byte[length];
        buffer.get(offset, bytes);
        return new String(bytes, StandardCharsets.US_ASCII).replace('\0', ' ');
    }
}
