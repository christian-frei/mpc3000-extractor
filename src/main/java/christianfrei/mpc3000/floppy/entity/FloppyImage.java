package christianfrei.mpc3000.floppy.entity;

import java.util.Arrays;

/// The content of a floppy image file
public record FloppyImage(byte[] content) {

    public static final int SECTOR = 512;

    /// A jump instruction, 512 bytes per sector and the boot signature
    public boolean startsWithFatBootSector() {
        if (content.length < SECTOR)
            return false;
        var jump = Byte.toUnsignedInt(content[0]);
        var bytesPerSector = Byte.toUnsignedInt(content[11]) | Byte.toUnsignedInt(content[12]) << 8;
        return (jump == 0xEB || jump == 0xE9) && bytesPerSector == SECTOR
                && content[510] == 0x55 && content[511] == (byte) 0xAA;
    }

    public boolean fits(Disk disk) {
        return content.length == disk.size();
    }

    /// Sectors whose bytes differ, counting sectors missing from readBack
    public int differingSectors(byte[] readBack) {
        var differing = 0;
        for (var offset = 0; offset < content.length; offset += SECTOR) {
            var end = Math.min(offset + SECTOR, content.length);
            if (end > readBack.length || !Arrays.equals(content, offset, end, readBack, offset, end))
                differing++;
        }
        return differing;
    }
}
