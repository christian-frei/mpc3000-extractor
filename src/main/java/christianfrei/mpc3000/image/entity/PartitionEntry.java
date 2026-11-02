package christianfrei.mpc3000.image.entity;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/// One of the four slots of an MBR or EBR partition table
public record PartitionEntry(int type, long firstSector, long sectors) {

    static final int TABLE_OFFSET = 0x1BE;
    static final int SLOT_LENGTH = 16;

    public static List<PartitionEntry> of(ByteBuffer sector) {
        if (!BootSector.hasSignature(sector))
            return List.of();
        var entries = new ArrayList<PartitionEntry>();
        for (var i = 0; i < 4; i++) {
            var offset = TABLE_OFFSET + i * SLOT_LENGTH;
            var type = Byte.toUnsignedInt(sector.get(offset + 4));
            if (type != 0)
                entries.add(new PartitionEntry(type,
                        Integer.toUnsignedLong(sector.getInt(offset + 8)),
                        Integer.toUnsignedLong(sector.getInt(offset + 12))));
        }
        return entries;
    }

    public boolean isExtended() {
        return type == 0x05 || type == 0x0F || type == 0x85;
    }

    public boolean isFat() {
        return type == 0x01 || type == 0x04 || type == 0x06 || type == 0x0E;
    }
}
