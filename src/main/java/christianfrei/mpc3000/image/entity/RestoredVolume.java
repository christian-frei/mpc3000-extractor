package christianfrei.mpc3000.image.entity;

import java.util.List;

/// The files of one partition or floppy, in the order they are stored; a floppy has no label
public record RestoredVolume(String label, List<RestoredFile> files) {

    public long fileBytes() {
        return files.stream().mapToLong(RestoredFile::size).sum();
    }
}
