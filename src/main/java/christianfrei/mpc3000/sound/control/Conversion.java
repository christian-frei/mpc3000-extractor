package christianfrei.mpc3000.sound.control;

import christianfrei.mpc3000.sound.entity.ConvertedSound;
import christianfrei.mpc3000.sound.entity.Sound;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/// Converts SND files to WAV files next to them or mirrored into a target folder
public interface Conversion {

    /// Converts the SND file, or every SND file below the folder, and passes each outcome to the listener
    static List<ConvertedSound> convert(Path input, Optional<Path> target, Consumer<ConvertedSound> listener) throws IOException {
        var absolute = input.toAbsolutePath().normalize();
        var root = Files.isDirectory(absolute) ? absolute : absolute.getParent();
        var converted = new ArrayList<ConvertedSound>();
        for (var source : sounds(absolute)) {
            var outcome = convert(source, wavFor(source, root, target));
            listener.accept(outcome);
            converted.add(outcome);
        }
        return converted;
    }

    static List<Path> sounds(Path input) throws IOException {
        if (!Files.isDirectory(input))
            return List.of(input);
        try (var files = Files.walk(input)) {
            return files.filter(Files::isRegularFile)
                    .filter(Conversion::isSound)
                    .sorted(Comparator.naturalOrder())
                    .toList();
        }
    }

    static boolean isSound(Path file) {
        var name = file.getFileName().toString();
        return !name.startsWith(".") && name.toLowerCase().endsWith(".snd");
    }

    /// Same name with `WAV` in the extension's letter case, next to the source or at the same place relative to root below target
    static Path wavFor(Path source, Path root, Optional<Path> target) {
        var name = source.getFileName().toString();
        var dot = name.lastIndexOf('.');
        var extension = dot >= 0 && name.substring(dot + 1).equals(name.substring(dot + 1).toLowerCase()) ? "wav" : "WAV";
        var wavName = (dot >= 0 ? name.substring(0, dot) : name) + "." + extension;
        var folder = target
                .map(t -> t.resolve(root.relativize(source.getParent())))
                .orElse(source.getParent());
        return folder.resolve(wavName);
    }

    static ConvertedSound convert(Path source, Path wav) {
        try {
            var sound = Sound.parse(ByteBuffer.wrap(Files.readAllBytes(source)));
            Files.createDirectories(wav.toAbsolutePath().getParent());
            try (var out = FileChannel.open(wav, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                var bytes = Wav.encode(sound);
                while (bytes.hasRemaining())
                    out.write(bytes);
            }
            return ConvertedSound.converted(source, wav, sound.seconds());
        } catch (IllegalArgumentException | IOException e) {
            return ConvertedSound.failed(source, e.getMessage());
        }
    }
}
