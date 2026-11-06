package christianfrei.mpc3000.floppy.entity;

import java.util.Set;

/// An external whole disk as the operating system reports it, e.g. `disk4`
public record Disk(String identifier, long size) {

    /// 1.44 MB high density and 720 KB double density
    static final Set<Long> FLOPPY_SIZES = Set.of(1_474_560L, 737_280L);

    public boolean isFloppy() {
        return FLOPPY_SIZES.contains(size);
    }

    /// The block device diskutil works on
    public String device() {
        return "/dev/" + identifier;
    }

    /// The unbuffered character device, much faster than the block device
    public String rawDevice() {
        return "/dev/r" + identifier;
    }
}
