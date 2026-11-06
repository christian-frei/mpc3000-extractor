package christianfrei.mpc3000.image.entity;

import java.nio.file.Path;

/// A host file going into an image under its uppercase MPC name
public record RestoredFile(Path source, String name, String extension, long size) {

    public String fileName() {
        return name + "." + extension;
    }
}
