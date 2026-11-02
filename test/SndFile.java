import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/// Builds MPC3000 SND files and reads back the RIFF chunks of WAV files for tests
class SndFile {

    static final int HEADER = 38;

    record Spec(String name, int start, int loop, int end, boolean stereo) {

        static Spec of(String name, int start, int loop, int end) {
            return new Spec(name, start, loop, end, false);
        }

        byte[] samples() {
            var buffer = ByteBuffer.allocate(2 * end).order(ByteOrder.LITTLE_ENDIAN);
            for (var i = 0; i < end; i++)
                buffer.putShort((short) (Math.sin(i / 7.0) * 12_000 + name.hashCode() % 1000));
            return buffer.array();
        }

        byte[] bytes() {
            var buffer = ByteBuffer.allocate(HEADER + 2 * end).order(ByteOrder.LITTLE_ENDIAN);
            buffer.put((byte) 0x01).put((byte) 0x02);
            buffer.put("%-16s".formatted(name).getBytes(StandardCharsets.US_ASCII));
            buffer.put((byte) 0).put((byte) 100).put((byte) 0).put((byte) (stereo ? 1 : 0));
            buffer.putInt(start).putInt(loop).putInt(end).putInt(0);
            buffer.put(samples());
            return buffer.array();
        }
    }

    static Path write(Path folder, String fileName, byte[] bytes) {
        try {
            Files.createDirectories(folder);
            return Files.write(folder.resolve(fileName), bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /// Chunk id to payload of a RIFF WAVE file
    static Map<String, ByteBuffer> chunks(Path wav) throws IOException {
        var bytes = ByteBuffer.wrap(Files.readAllBytes(wav)).order(ByteOrder.LITTLE_ENDIAN);
        var chunks = new HashMap<String, ByteBuffer>();
        var position = 12;
        while (position + 8 <= bytes.limit()) {
            var id = new String(bytes.array(), position, 4, StandardCharsets.US_ASCII);
            var length = bytes.getInt(position + 4);
            chunks.put(id, bytes.slice(position + 8, length).order(ByteOrder.LITTLE_ENDIAN));
            position += 8 + length + (length & 1);
        }
        return chunks;
    }
}
