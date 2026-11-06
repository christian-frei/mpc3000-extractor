package christianfrei.mpc3000.floppy.entity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// The outcome of copying a disk: bytes in the image and sectors that could not be read
public record CopyReport(long bytes, long unreadableSectors) {

    /// `N+M records in`: N full blocks read, M partial ones — with `conv=noerror,sync` each unreadable block is a partial one
    static final Pattern RECORDS_IN = Pattern.compile("^(\\d+)\\+(\\d+) records in$");

    /// Counts unreadable blocks from dd's last summary; BSD dd prints its error message twice per block,
    /// so the `dd: ` lines only serve as a fallback when no summary is present
    public static CopyReport of(String ddOutput, long bytes) {
        var summaries = ddOutput.lines()
                .map(String::strip)
                .map(RECORDS_IN::matcher)
                .filter(Matcher::matches)
                .map(m -> Long.parseLong(m.group(2)))
                .toList();
        var unreadable = summaries.isEmpty()
                ? ddOutput.lines().filter(line -> line.startsWith("dd: ")).count()
                : summaries.getLast();
        return new CopyReport(bytes, unreadable);
    }

    public boolean complete(Disk disk) {
        return unreadableSectors == 0 && bytes == disk.size();
    }
}
