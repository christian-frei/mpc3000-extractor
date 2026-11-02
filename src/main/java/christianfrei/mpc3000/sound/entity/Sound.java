package christianfrei.mpc3000.sound.entity;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/// An MPC3000 sound: a 38-byte header followed by 16-bit signed little-endian mono samples
public record Sound(String name, int level, int tune, long start, long loop, long end, ByteBuffer samples) {

    public static final int HEADER_LENGTH = 38;
    public static final int SAMPLE_RATE = 44_100;
    static final int MARKER = 0x01;
    static final int VERSION = 0x02;

    /// @throws IllegalArgumentException when the bytes are not a mono MPC3000 sound
    public static Sound parse(ByteBuffer bytes) {
        var buffer = bytes.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        if (buffer.limit() < HEADER_LENGTH || buffer.get(0) != MARKER || buffer.get(1) != VERSION)
            throw new IllegalArgumentException("not an MPC3000 sound");
        if (buffer.get(21) != 0)
            throw new IllegalArgumentException("stereo sounds are not supported");
        var start = Integer.toUnsignedLong(buffer.getInt(22));
        var loop = Integer.toUnsignedLong(buffer.getInt(26));
        var end = Integer.toUnsignedLong(buffer.getInt(30));
        var expected = HEADER_LENGTH + 2 * end;
        if (buffer.limit() != expected)
            throw new IllegalArgumentException("length %d does not match %d samples (expected %d bytes)".formatted(buffer.limit(), end, expected));
        var name = new byte[16];
        buffer.get(2, name);
        var samples = buffer.slice(HEADER_LENGTH, buffer.limit() - HEADER_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
        return new Sound(new String(name, StandardCharsets.US_ASCII).strip(),
                Byte.toUnsignedInt(buffer.get(19)), buffer.get(20), start, loop, end, samples);
    }

    public boolean loops() {
        return loop < end;
    }

    public double seconds() {
        return (double) end / SAMPLE_RATE;
    }
}
