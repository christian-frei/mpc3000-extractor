package christianfrei.mpc3000.image.entity;

import java.nio.file.Path;

/// The outcome of extracting one partition
public record ExtractedPartition(Path folder, int files, long bytes) {}
