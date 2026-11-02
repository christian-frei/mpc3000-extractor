import javax.sound.sampled.AudioSystem;

import christianfrei.mpc3000.image.boundary.Images;
import christianfrei.mpc3000.sound.boundary.Sounds;

/// Regression against a real MPC3000 hard disk: every extracted sound converts and reads back with the JDK's audio reader
void main() throws Exception {
    var image = Path.of("src/test/resources/HD0.iso");
    if (!Files.exists(image)) {
        IO.println("skipped: " + image + " not present");
        return;
    }
    var extracted = Files.createTempDirectory("mpc3000-");
    Images.extract(image, extracted);
    var target = Files.createTempDirectory("mpc3000-");
    Sounds.convert(extracted, Optional.of(target), _ -> {});
    try (var wavs = Files.walk(target)) {
        var files = wavs.filter(f -> f.toString().endsWith(".WAV")).toList();
        assert files.size() == 165 : "expected 165 WAVs but got " + files.size();
        for (var wav : files) {
            var snd = extracted.resolve(target.relativize(wav).toString().replaceAll("WAV$", "SND"));
            var expectedFrames = (Files.size(snd) - 38) / 2;
            try (var stream = AudioSystem.getAudioInputStream(wav.toFile())) {
                assert stream.getFrameLength() == expectedFrames : "%s: expected %d frames but got %d".formatted(wav, expectedFrames, stream.getFrameLength());
            }
        }
    }
}
