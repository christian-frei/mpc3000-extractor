package christianfrei.mpc3000.image.control;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

/// Little-endian random access reads from an image
interface Sectors {

    int LENGTH = 512;

    static ByteBuffer read(FileChannel channel, long position, int length) throws IOException {
        var buffer = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN);
        while (buffer.hasRemaining() && channel.read(buffer, position + buffer.position()) > 0) {}
        return buffer.flip();
    }
}
