package christianfrei.mpc3000.sound.control;

import christianfrei.mpc3000.sound.entity.Sound;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/// Encodes a sound as RIFF WAVE: `fmt `, `data`, a `cue ` point for the start and a `smpl` loop
interface Wav {

    int CHANNELS = 1;
    int BYTES_PER_SAMPLE = 2;
    int FMT_LENGTH = 16;
    int CUE_LENGTH = 4 + 24;
    int SMPL_LENGTH = 36 + 24;
    int MIDDLE_C = 60;

    static ByteBuffer encode(Sound sound) {
        var data = sound.samples().remaining();
        var smpl = sound.loops() ? 8 + SMPL_LENGTH : 0;
        var riff = 4 + (8 + FMT_LENGTH) + (8 + data) + (8 + CUE_LENGTH) + smpl;
        var wav = ByteBuffer.allocate(8 + riff).order(ByteOrder.LITTLE_ENDIAN);
        chunk(wav, "RIFF", riff);
        ascii(wav, "WAVE");

        chunk(wav, "fmt ", FMT_LENGTH);
        wav.putShort((short) 1)
                .putShort((short) CHANNELS)
                .putInt(Sound.SAMPLE_RATE)
                .putInt(Sound.SAMPLE_RATE * CHANNELS * BYTES_PER_SAMPLE)
                .putShort((short) (CHANNELS * BYTES_PER_SAMPLE))
                .putShort((short) (8 * BYTES_PER_SAMPLE));

        chunk(wav, "data", data);
        wav.put(sound.samples().duplicate());

        chunk(wav, "cue ", CUE_LENGTH);
        wav.putInt(1);
        wav.putInt(1).putInt((int) sound.start());
        ascii(wav, "data");
        wav.putInt(0).putInt(0).putInt((int) sound.start());

        if (sound.loops()) {
            chunk(wav, "smpl", SMPL_LENGTH);
            wav.putInt(0).putInt(0)
                    .putInt(Math.round(1_000_000_000f / Sound.SAMPLE_RATE))
                    .putInt(MIDDLE_C).putInt(0)
                    .putInt(0).putInt(0)
                    .putInt(1).putInt(0);
            wav.putInt(0).putInt(0)
                    .putInt((int) sound.loop()).putInt((int) (sound.end() - 1))
                    .putInt(0).putInt(0);
        }
        return wav.flip();
    }

    static void chunk(ByteBuffer wav, String id, int length) {
        ascii(wav, id);
        wav.putInt(length);
    }

    static void ascii(ByteBuffer wav, String text) {
        wav.put(text.getBytes(StandardCharsets.US_ASCII));
    }
}
