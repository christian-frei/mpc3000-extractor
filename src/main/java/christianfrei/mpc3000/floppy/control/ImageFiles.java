package christianfrei.mpc3000.floppy.control;

import java.nio.file.Files;
import java.nio.file.Path;

/// Names for new floppy image files
public interface ImageFiles {

    /// The lowest-numbered `FLOPPYnn.img` that does not exist in folder
    static Path nextFree(Path folder) {
        for (var number = 1; ; number++) {
            var candidate = folder.resolve("FLOPPY%02d.img".formatted(number));
            if (!Files.exists(candidate))
                return candidate;
        }
    }
}
