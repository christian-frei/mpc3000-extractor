package christianfrei.mpc3000.image.entity;

import java.util.List;

/// A FAT12 partition of an image: where it starts, its layout and the files in its root directory
public record Volume(String label, long offset, BootSector boot, List<DirectoryEntry> entries) {

    public long capacity() {
        return boot.capacity();
    }

    /// Space occupied on disk, rounded up to whole clusters
    public long used() {
        var cluster = boot.clusterSize();
        return entries.stream().mapToLong(e -> (e.size() + cluster - 1) / cluster * cluster).sum();
    }

    public boolean hasSounds() {
        return entries.stream().anyMatch(entry -> entry.type() == FileType.SND);
    }

    public long fileBytes() {
        return entries.stream().mapToLong(DirectoryEntry::size).sum();
    }

    /// Partition label as folder name, numbered when the label is missing
    public String folderName(int number) {
        return label.isBlank() ? "PARTITION-%02d".formatted(number) : label;
    }
}
