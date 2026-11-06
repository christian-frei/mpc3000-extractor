package christianfrei.mpc3000.image.entity;

import java.nio.file.Path;
import java.util.List;

/// What a host folder restores into: a floppy or the partitions of a hard disk, and the files left out
public record RestoredImage(boolean floppy, List<RestoredVolume> volumes, List<Path> leftOut) {

    public int files() {
        return volumes.stream().mapToInt(volume -> volume.files().size()).sum();
    }

    public String extension() {
        return floppy ? ".img" : ".iso";
    }
}
