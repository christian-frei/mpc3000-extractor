package christianfrei.mpc3000.sound.boundary;

import static christianfrei.mpc3000.sound.Requirement.Rn.*;

import christianfrei.mpc3000.sound.Requirement;
import christianfrei.mpc3000.sound.control.Conversion;
import christianfrei.mpc3000.sound.entity.ConvertedSound;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

/// Converts MPC3000 sounds to WAV; reports one line per sound and a summary to output
public interface Sounds {

    /// @throws IOException after the summary when at least one sound could not be converted
    @Requirement({R1_1, R1_2, R1_3, R1_4, R1_5, R1_6, R1_7, R1_8, R1_9, R1_10, R2_1, R2_2, R2_3})
    static void convert(Path input, Optional<Path> target, Consumer<String> output) throws IOException {
        if (!Files.exists(input))
            throw new IllegalArgumentException("not found: " + input);
        if (target.filter(t -> Files.exists(t) && !Files.isDirectory(t)).isPresent())
            throw new IllegalArgumentException("not a folder: " + target.get());
        var root = (Files.isDirectory(input) ? input : input.toAbsolutePath().getParent()).toAbsolutePath().normalize();
        var converted = Conversion.convert(input, target, sound -> output.accept(describe(sound, root)));
        var failed = converted.stream().filter(sound -> !sound.succeeded()).count();
        var destination = target.map(t -> "into " + t.toAbsolutePath().normalize()).orElse("next to the sources");
        output.accept("converted %d of %d sound(s) %s".formatted(converted.size() - failed, converted.size(), destination));
        if (failed > 0)
            throw new IOException("%d sound(s) could not be converted".formatted(failed));
    }

    static String describe(ConvertedSound sound, Path root) {
        var source = root.relativize(sound.source());
        return sound.wav()
                .map(wav -> "  %-28s -> %s  %.2f s".formatted(source, wav.getFileName(), sound.seconds()))
                .orElse("  %-28s failed: %s".formatted(source, sound.failure()));
    }
}
