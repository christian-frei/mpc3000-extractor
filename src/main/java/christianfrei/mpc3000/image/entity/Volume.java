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

    /// A floppy is a single FAT volume at the very start of the image, without partition table
    public boolean isFloppy() {
        return offset == 0;
    }

    public boolean isLabeled() {
        return !label.isBlank();
    }

    public String displayLabel() {
        return isLabeled() ? label : "(no label)";
    }

    /// The label as folder name; an unlabeled floppy is named after its image, an unlabeled partition is numbered
    public String folderName(int number, String imageName) {
        if (isLabeled())
            return label;
        return isFloppy() ? imageName : "PARTITION-%02d".formatted(number);
    }
}
