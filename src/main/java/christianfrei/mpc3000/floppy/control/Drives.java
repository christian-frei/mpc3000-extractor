package christianfrei.mpc3000.floppy.control;

import christianfrei.mpc3000.floppy.entity.Disk;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/// Access to the operating system's disks and the user's terminal
public interface Drives {

    List<Disk> externalDisks() throws IOException;

    /// Whether the administrator password can be asked for
    boolean canAskForPassword();

    /// Copies the disk into image, a file that does not exist yet, and returns the copy tool's output
    /// @throws java.util.concurrent.CancellationException when the administrator password prompt is cancelled or fails
    String copy(Disk disk, Path image) throws IOException;

    boolean isWriteProtected(Disk disk) throws IOException;

    /// Asks question in the terminal; true only for the answer `y`
    boolean confirm(String question) throws IOException;

    /// Unmounts disk, writes image onto it, reads the disk back into readBack, a file that does not exist yet, and ejects the disk
    /// @throws IOException with the tool's reason when the disk cannot be unmounted or written
    /// @throws java.util.concurrent.CancellationException when the administrator password prompt is cancelled or fails
    void write(Disk disk, Path image, Path readBack) throws IOException;
}
