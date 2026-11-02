package christianfrei.mpc3000.sound.entity;

import java.nio.file.Path;
import java.util.Optional;

/// The outcome of converting one SND file: the written WAV and its duration, or why it was skipped
public record ConvertedSound(Path source, Optional<Path> wav, double seconds, String failure) {

    public static ConvertedSound converted(Path source, Path wav, double seconds) {
        return new ConvertedSound(source, Optional.of(wav), seconds, "");
    }

    public static ConvertedSound failed(Path source, String reason) {
        return new ConvertedSound(source, Optional.empty(), 0, reason);
    }

    public boolean succeeded() {
        return wav.isPresent();
    }
}
