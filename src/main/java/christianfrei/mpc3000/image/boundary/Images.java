package christianfrei.mpc3000.image.boundary;

import static christianfrei.mpc3000.image.Requirement.Rn.*;

import christianfrei.mpc3000.image.Requirement;
import christianfrei.mpc3000.image.control.Extraction;
import christianfrei.mpc3000.image.control.Volumes;
import christianfrei.mpc3000.image.entity.DirectoryEntry;
import christianfrei.mpc3000.image.entity.ExtractedPartition;
import christianfrei.mpc3000.image.entity.Volume;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

/// Lists and extracts MPC3000 disk images; returns the report to print
public interface Images {

    @Requirement({R1_1, R1_2, R1_3, R1_4, R1_5, R1_6})
    static String list(Path image) throws IOException {
        var volumes = Volumes.read(existing(image));
        var header = "%s: %d partition(s), %s".formatted(image.getFileName(), volumes.size(), human(Files.size(image)));
        return volumes.stream()
                .map(Images::describe)
                .collect(Collectors.joining("\n", header + "\n", ""));
    }

    @Requirement({R2_1, R2_2, R2_3, R2_4, R2_5, R2_6, R2_7, R2_8, R2_9, R2_10})
    static String extract(Path image, Path target) throws IOException {
        existing(image);
        if (Files.exists(target) && !Files.isDirectory(target))
            throw new IllegalArgumentException("not a folder: " + target);
        var extracted = Extraction.extract(image, target);
        var files = extracted.stream().mapToInt(ExtractedPartition::files).sum();
        var footer = "extracted %d file(s) from %d partition(s) into %s".formatted(files, extracted.size(), target.toAbsolutePath().normalize());
        return extracted.stream()
                .map(e -> "  %-12s %4d file(s) %12s".formatted(e.folder().getFileName(), e.files(), human(e.bytes())))
                .collect(Collectors.joining("\n", "", extracted.isEmpty() ? footer : "\n" + footer));
    }

    /// Where extract writes without a target folder; the result is always a folder in the current directory named after the image
    static Path defaultTarget(Path image) throws IOException {
        return Extraction.defaultTarget(existing(image));
    }

    static Path existing(Path image) {
        if (!Files.isRegularFile(image))
            throw new IllegalArgumentException("image not found: " + image);
        return image;
    }

    static String describe(Volume volume) {
        var files = volume.entries();
        var header = "\n%s  %s capacity, %d file(s), %s used".formatted(
                volume.displayLabel(), human(volume.capacity()), files.size(), human(volume.used()));
        return files.stream()
                .map(Images::describe)
                .collect(Collectors.joining("", header, ""));
    }

    static String describe(DirectoryEntry file) {
        return "\n  %-20s %12s  %s".formatted(file.fileName(), "%,d".formatted(file.size()), file.type().description());
    }

    static String human(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        var units = "KMGT";
        var value = (double) bytes;
        var unit = -1;
        while (value >= 1024 && unit < units.length() - 1) {
            value /= 1024;
            unit++;
        }
        return "%.1f %siB".formatted(value, units.charAt(unit));
    }
}
