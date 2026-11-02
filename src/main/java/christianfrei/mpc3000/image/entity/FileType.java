package christianfrei.mpc3000.image.entity;

import java.util.Arrays;

/// MPC3000 file types, identified by extension
public enum FileType {
    SND("sound"),
    PGM("program"),
    SEQ("sequence"),
    ALL("all sequences and songs"),
    APS("all programs and sounds"),
    UNKNOWN("unknown");

    final String description;

    FileType(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }

    public static FileType of(String extension) {
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(extension))
                .findFirst()
                .orElse(UNKNOWN);
    }
}
