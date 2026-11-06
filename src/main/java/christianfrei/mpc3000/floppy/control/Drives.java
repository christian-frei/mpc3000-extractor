package christianfrei.mpc3000.floppy.control;

import christianfrei.mpc3000.floppy.entity.Disk;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/// Access to the operating system's disks
public interface Drives {

    List<Disk> externalDisks() throws IOException;

    /// Whether the administrator password can be asked for
    boolean canAskForPassword();

    /// Copies the disk into image, a file that does not exist yet, and returns the copy tool's output
    /// @throws java.util.concurrent.CancellationException when the administrator password prompt is cancelled or fails
    String copy(Disk disk, Path image) throws IOException;
}
