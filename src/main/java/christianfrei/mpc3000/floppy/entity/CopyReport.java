package christianfrei.mpc3000.floppy.entity;

/// The outcome of copying a disk: bytes in the image and sectors that could not be read
public record CopyReport(long bytes, long unreadableSectors) {

    /// dd with `conv=noerror` prints one `dd: <device>: <reason>` line per block it could not read
    public static CopyReport of(String ddOutput, long bytes) {
        var unreadable = ddOutput.lines()
                .filter(line -> line.startsWith("dd: "))
                .count();
        return new CopyReport(bytes, unreadable);
    }

    public boolean complete(Disk disk) {
        return unreadableSectors == 0 && bytes == disk.size();
    }
}
