package christianfrei.mpc3000.image.control;

import christianfrei.mpc3000.image.entity.DiskLayout;
import christianfrei.mpc3000.image.entity.FileType;
import christianfrei.mpc3000.image.entity.RestoredFile;
import christianfrei.mpc3000.image.entity.RestoredImage;
import christianfrei.mpc3000.image.entity.RestoredVolume;
import christianfrei.mpc3000.image.entity.VolumeFormat;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

/// Decides what a host folder restores into: subfolders make a hard disk, files alone a floppy
public interface Restoration {

    int NAME_LENGTH = 16;
    List<String> LABELS = IntStream.range(0, DiskLayout.PARTITIONS).mapToObj(DiskLayout::label).toList();

    static RestoredImage scan(Path folder) throws IOException {
        if (!Files.isDirectory(folder))
            throw new IllegalArgumentException("folder not found: " + folder);
        var entries = visible(folder);
        var subfolders = entries.stream().filter(Files::isDirectory).toList();
        var leftOut = new ArrayList<Path>();
        var image = subfolders.isEmpty()
                ? new RestoredImage(true, List.of(volume(folder, "", VolumeFormat.FLOPPY, VolumeFormat.FLOPPY_SECTORS, leftOut)), leftOut)
                : hardDisk(entries, subfolders, leftOut);
        if (image.files() == 0)
            throw new IllegalArgumentException("no MPC files in " + folder);
        return image;
    }

    static RestoredImage hardDisk(List<Path> entries, List<Path> subfolders, List<Path> leftOut) throws IOException {
        var foreign = subfolders.stream().filter(sub -> !LABELS.contains(name(sub))).map(Restoration::name).toList();
        if (!foreign.isEmpty())
            throw new IllegalArgumentException("not a partition folder (MPC-PART--A … MPC-PART--W): " + String.join(", ", foreign));
        var files = entries.stream().filter(Files::isRegularFile).toList();
        var stray = files.stream().filter(Restoration::isMpcFile).map(Restoration::name).toList();
        if (!stray.isEmpty())
            throw new IllegalArgumentException("MPC files next to partition folders: " + String.join(", ", stray));
        leftOut.addAll(files);
        var volumes = new ArrayList<RestoredVolume>();
        for (var sub : subfolders.stream().sorted(Comparator.comparing(Restoration::name)).toList()) {
            var label = name(sub);
            var sectors = DiskLayout.partitionSectors(LABELS.indexOf(label));
            volumes.add(volume(sub, label, VolumeFormat.HARD_DISK_PARTITION, sectors, leftOut));
        }
        return new RestoredImage(false, volumes, leftOut);
    }

    /// The MPC files of folder in name order; everything else goes to leftOut
    static RestoredVolume volume(Path folder, String label, VolumeFormat format, long sectors, List<Path> leftOut) throws IOException {
        var files = new ArrayList<RestoredFile>();
        for (var entry : visible(folder)) {
            if (Files.isRegularFile(entry) && isMpcFile(entry))
                files.add(restoredFile(entry));
            else
                leftOut.add(entry);
        }
        files.sort(Comparator.comparing(RestoredFile::fileName));
        var volume = new RestoredVolume(label, List.copyOf(files));
        rejectDuplicates(volume, folder);
        rejectOverflow(volume, folder, format, sectors);
        return volume;
    }

    static RestoredFile restoredFile(Path file) throws IOException {
        var fileName = name(file);
        var dot = fileName.lastIndexOf('.');
        var stem = fileName.substring(0, dot);
        if (stem.length() > NAME_LENGTH)
            throw new IllegalArgumentException("name longer than %d characters: %s".formatted(NAME_LENGTH, file));
        if (!stem.chars().allMatch(c -> c >= 0x20 && c < 0x7F))
            throw new IllegalArgumentException("name not printable ASCII: " + file);
        return new RestoredFile(file, stem.toUpperCase(Locale.ROOT), fileName.substring(dot + 1).toUpperCase(Locale.ROOT), Files.size(file));
    }

    static void rejectDuplicates(RestoredVolume volume, Path folder) {
        var byName = new HashMap<String, List<String>>();
        for (var file : volume.files())
            byName.computeIfAbsent(file.fileName(), _ -> new ArrayList<>()).add(name(file.source()));
        var clashes = byName.values().stream()
                .filter(sources -> sources.size() > 1)
                .map(sources -> String.join(" and ", sources.stream().sorted().toList()))
                .sorted()
                .toList();
        if (!clashes.isEmpty())
            throw new IllegalArgumentException("same uppercase name in %s: %s".formatted(folder, String.join(", ", clashes)));
    }

    static void rejectOverflow(RestoredVolume volume, Path folder, VolumeFormat format, long sectors) {
        var files = volume.files();
        if (files.size() > format.fileEntries())
            throw new IllegalArgumentException("%s: %d files, %d directory entries available".formatted(folder, files.size(), format.fileEntries()));
        var needed = files.stream().mapToLong(file -> format.clustersFor(file.size())).sum();
        var available = format.freeClusters(sectors);
        if (needed > available)
            throw new IllegalArgumentException("%s: needs %,d bytes (%d clusters), %,d bytes (%d clusters) available".formatted(
                    folder, needed * format.clusterSize(), needed, available * format.clusterSize(), available));
    }

    static boolean isMpcFile(Path file) {
        var fileName = name(file);
        var dot = fileName.lastIndexOf('.');
        return dot > 0 && FileType.of(fileName.substring(dot + 1)) != FileType.UNKNOWN;
    }

    /// Entries not starting with `.`, which macOS adds (`.DS_Store`, `._*`)
    static List<Path> visible(Path folder) throws IOException {
        try (var entries = Files.list(folder)) {
            return entries.filter(entry -> !name(entry).startsWith(".")).sorted().toList();
        }
    }

    static String name(Path path) {
        return path.getFileName().toString();
    }
}
