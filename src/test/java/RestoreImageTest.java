import static christianfrei.mpc3000.image.Requirement.Rn.*;

import christianfrei.mpc3000.image.Requirement;
import christianfrei.mpc3000.image.boundary.Images;
import christianfrei.mpc3000.image.control.Restoration;
import christianfrei.mpc3000.image.entity.RestoredFile;
import christianfrei.mpc3000.image.entity.RestoredVolume;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

interface Check {
    void run() throws Exception;
}

record Case(Requirement.Rn req, Check check) {}

static final long HARD_DISK_BYTES = 779_091_968;
static final long FLOPPY_BYTES = 1_474_560;
/// Partition A starts at sector 126; FAT 1 sector, 2 × 11 FAT sectors, then the root directory
static final long PARTITION_A = 126 * 512;
static final int HARD_DISK_ROOT = 512 + 2 * 11 * 512;
static final int FLOPPY_ROOT = 512 + 2 * 9 * 512;

void main() throws Exception {
    var cases = List.of(
            new Case(R3_1, () -> {
                var folder = folder(Map.of("MPC-PART--A/SOUND001.SND", bytes(700), "MPC-PART--C/PROG.PGM", bytes(300)));
                var image = restore(folder);
                assert Files.size(image) == HARD_DISK_BYTES : "%s — %s — size %d".formatted(R3_1, R3_1.statement(), Files.size(image));
                var listing = Images.list(image);
                assertContains(R3_1, listing, "23 partition(s)");
                assertContains(R3_1, listing, "MPC-PART--A  31.3 MiB capacity, 1 file(s)");
                assertContains(R3_1, listing, "MPC-PART--B  31.3 MiB capacity, 0 file(s)");
                assertContains(R3_1, listing, "MPC-PART--C  31.3 MiB capacity, 1 file(s)");
                assertContains(R3_1, listing, "MPC-PART--W  31.3 MiB capacity, 0 file(s)");
            }),
            new Case(R3_2, () -> {
                var image = restore(folder(Map.of("SOUND001.SND", bytes(700))));
                assert Files.size(image) == FLOPPY_BYTES : "%s — %s — size %d".formatted(R3_2, R3_2.statement(), Files.size(image));
                var listing = Images.list(image);
                assertContains(R3_2, listing, "1 partition(s)");
                assertContains(R3_2, listing, "(no label)  1.4 MiB capacity, 1 file(s)");
            }),
            new Case(R3_3, () -> {
                var disk = read(restore(folder(Map.of("MPC-PART--A/ABCDEFGHIJKLMNOP.SND", bytes(700)))));
                var boot = slice(disk, PARTITION_A, 512);
                assertAscii(R3_3, boot, 3, "MPC-3000");
                assertZero(R3_3, boot, 62, 510, "boot code");
                var label = slice(disk, PARTITION_A + HARD_DISK_ROOT, 32);
                assertAscii(R3_3, label, 0, "MPC-PART--A");
                assert label.get(11) == 0x08 : R3_3 + " — " + R3_3.statement() + " — label entry attribute " + label.get(11);
                var file = slice(disk, PARTITION_A + HARD_DISK_ROOT + 32, 32);
                assertAscii(R3_3, file, 0, "ABCDEFGHSND");
                assertAscii(R3_3, file, 12, "IJKLMNOP");
                assertZero(R3_3, file, 20, 26, "time and date");
                assert file.getShort(26) == 4 : R3_3 + " — " + R3_3.statement() + " — hard disk file starts at cluster " + file.getShort(26);

                var floppy = read(restore(folder(Map.of("SOUND1.SND", bytes(700)))));
                assertZero(R3_3, floppy, 3, 11, "OEM name");
                assertZero(R3_3, floppy, 36, 510, "extended boot record and boot code");
                var entry = slice(floppy, FLOPPY_ROOT, 32);
                assertAscii(R3_3, entry, 0, "SOUND1  SND");
                assertZero(R3_3, entry, 12, 26, "name tail, time and date");
                assert entry.getShort(26) == 2 : R3_3 + " — " + R3_3.statement() + " — floppy file starts at cluster " + entry.getShort(26);
            }),
            new Case(R3_4, () -> {
                var first = bytes(1100);
                var second = bytes(300);
                // host order puts B.SND before a.snd, name order puts A.SND first
                var folder = folder(Map.of("B.SND", second, "a.snd", first));
                var floppy = read(restore(folder));
                var a = slice(floppy, FLOPPY_ROOT, 32);
                var b = slice(floppy, FLOPPY_ROOT + 32, 32);
                assertAscii(R3_4, a, 0, "A       SND");
                assertAscii(R3_4, b, 0, "B       SND");
                assert a.getShort(26) == 2 && b.getShort(26) == 5 : "%s — %s — clusters %d and %d".formatted(R3_4, R3_4.statement(), a.getShort(26), b.getShort(26));
                var extracted = Files.createTempDirectory("mpc3000-");
                Images.extract(folder.resolveSibling(folder.getFileName() + ".img"), extracted);
                assertContent(R3_4, extracted.resolve(folder.getFileName() + "/A.SND"), first);
                assertContent(R3_4, extracted.resolve(folder.getFileName() + "/B.SND"), second);
            }),
            new Case(R3_5, () -> {
                var listing = Images.list(restore(folder(Map.of("kick 1.snd", bytes(10)))));
                assertContains(R3_5, listing, "KICK 1.SND");
            }),
            new Case(R3_6, () -> {
                var parent = Files.createTempDirectory("mpc3000-");
                folder(parent.resolve("KIT"), Map.of("SOUND1.SND", bytes(10)));
                folder(parent.resolve("DISK"), Map.of("MPC-PART--A/SOUND1.SND", bytes(10)));
                Images.restore(parent.resolve("KIT"), Optional.empty());
                Images.restore(parent.resolve("DISK"), Optional.empty());
                assert Files.size(parent.resolve("KIT.img")) == FLOPPY_BYTES : R3_6 + " — " + R3_6.statement() + " — KIT.img missing";
                assert Files.size(parent.resolve("DISK.iso")) == HARD_DISK_BYTES : R3_6 + " — " + R3_6.statement() + " — DISK.iso missing";
            }),
            new Case(R3_7, () -> {
                var folder = folder(Map.of("SOUND1.SND", bytes(10)));
                var image = Files.writeString(folder.resolveSibling(folder.getFileName() + ".img"), "mine");
                var message = rejection(() -> Images.restore(folder, Optional.empty()));
                assertContains(R3_7, message, "already exists");
                assert Files.readString(image).equals("mine") : R3_7 + " — " + R3_7.statement() + " — existing file was changed";
            }),
            new Case(R3_8, () -> {
                var folder = folder(Map.of("SOUND1.SND", bytes(10), "notes.txt", bytes(10)));
                var report = Images.restore(folder, Optional.empty());
                assertContains(R3_8, report, "left out");
                assertContains(R3_8, report, "notes.txt");
                var listing = Images.list(folder.resolveSibling(folder.getFileName() + ".img"));
                assert !listing.contains("NOTES") : R3_8 + " — " + R3_8.statement() + " — notes.txt in image";
                assertContains(R3_8, listing, "1 file(s)");
            }),
            new Case(R3_9, () -> {
                var folder = folder(Map.of("SOUND1.SND", bytes(10), ".DS_Store", bytes(10), "._SOUND1.SND", bytes(10), ".hidden/SOUND2.SND", bytes(10)));
                var report = Images.restore(folder, Optional.empty());
                assert !report.contains("left out") : R3_9 + " — " + R3_9.statement() + " — hidden files listed: " + report;
                var listing = Images.list(folder.resolveSibling(folder.getFileName() + ".img"));
                assertContains(R3_9, listing, "1 file(s)");
                assert !listing.contains("SOUND2") : R3_9 + " — " + R3_9.statement() + " — hidden folder restored";
            }),
            new Case(R3_10, () -> {
                assertContains(R3_10, rejection(() -> restore(folder(Map.of("ABCDEFGHIJKLMNOPQ.SND", bytes(10))))), "ABCDEFGHIJKLMNOPQ.SND");
                assertContains(R3_10, rejection(() -> restore(folder(Map.of("SÖUND.SND", bytes(10))))), "not printable ASCII");
            }),
            new Case(R3_11, () -> {
                // case-insensitive host file systems cannot hold both names, so the volume is built directly
                var volume = new RestoredVolume("", List.of(
                        new RestoredFile(Path.of("kick.snd"), "KICK", "SND", 10),
                        new RestoredFile(Path.of("KICK.SND"), "KICK", "SND", 10)));
                var message = rejection(() -> Restoration.rejectDuplicates(volume, Path.of("KIT")));
                assertContains(R3_11, message, "KICK.SND and kick.snd");
            }),
            new Case(R3_12, () -> {
                assertContains(R3_12, rejection(() -> restore(folder(Map.of("BIG.SND", bytes(1_500_000))))), "available");
                var many = new HashMap<String, byte[]>();
                for (var i = 0; i < 225; i++)
                    many.put("S%03d.SND".formatted(i), bytes(1));
                assertContains(R3_12, rejection(() -> restore(folder(many))), "225 files, 224 directory entries available");
            }),
            new Case(R3_13, () -> {
                assertContains(R3_13, rejection(() -> restore(folder(Map.of("SOUNDS/SOUND1.SND", bytes(10))))), "SOUNDS");
                var stray = folder(Map.of("MPC-PART--A/SOUND1.SND", bytes(10), "STRAY.SND", bytes(10)));
                assertContains(R3_13, rejection(() -> restore(stray)), "STRAY.SND");
            }),
            new Case(R3_14, () -> {
                var missing = Files.createTempDirectory("mpc3000-").resolve("missing");
                assertContains(R3_14, rejection(() -> restore(missing)), "folder not found");
                assertContains(R3_14, rejection(() -> restore(folder(Map.of("notes.txt", bytes(10))))), "no MPC files");
            }));

    for (var c : cases) {
        try {
            c.check().run();
        } catch (Exception | AssertionError e) {
            if (e instanceof AssertionError && String.valueOf(e.getMessage()).startsWith(c.req().toString()))
                throw e;
            throw new AssertionError("%s — %s".formatted(c.req(), c.req().statement()), e);
        }
    }
}

/// Restores folder next to itself and returns the image
Path restore(Path folder) throws IOException {
    Images.restore(folder, Optional.empty());
    try (var siblings = Files.list(folder.toAbsolutePath().getParent())) {
        return siblings.filter(path -> path.getFileName().toString().startsWith(folder.getFileName() + "."))
                .findFirst()
                .orElseThrow();
    }
}

Path folder(Map<String, byte[]> files) throws IOException {
    return folder(Files.createTempDirectory("mpc3000-").resolve("FOLDER"), files);
}

Path folder(Path folder, Map<String, byte[]> files) throws IOException {
    Files.createDirectories(folder);
    for (var file : files.entrySet()) {
        var path = folder.resolve(file.getKey());
        Files.createDirectories(path.getParent());
        Files.write(path, file.getValue());
    }
    return folder;
}

byte[] bytes(int size) {
    var bytes = new byte[size];
    for (var i = 0; i < size; i++)
        bytes[i] = (byte) (i * 7 + size);
    return bytes;
}

ByteBuffer read(Path image) throws IOException {
    return ByteBuffer.wrap(Files.readAllBytes(image)).order(ByteOrder.LITTLE_ENDIAN);
}

ByteBuffer slice(ByteBuffer buffer, long offset, int length) {
    return buffer.slice((int) offset, length).order(ByteOrder.LITTLE_ENDIAN);
}

void assertAscii(Requirement.Rn req, ByteBuffer buffer, int offset, String expected) {
    var bytes = new byte[expected.length()];
    buffer.get(offset, bytes);
    var actual = new String(bytes, StandardCharsets.US_ASCII);
    assert actual.equals(expected) : "%s — %s — expected '%s' at %d but got '%s'".formatted(req, req.statement(), expected, offset, actual);
}

void assertZero(Requirement.Rn req, ByteBuffer buffer, int from, int to, String what) {
    for (var i = from; i < to; i++)
        assert buffer.get(i) == 0 : "%s — %s — %s not empty at byte %d".formatted(req, req.statement(), what, i);
}

void assertContent(Requirement.Rn req, Path file, byte[] expected) throws IOException {
    assert Files.exists(file) : "%s — %s — missing %s".formatted(req, req.statement(), file);
    assert Arrays.equals(Files.readAllBytes(file), expected) : "%s — %s — content differs: %s".formatted(req, req.statement(), file);
}

void assertContains(Requirement.Rn req, String actual, String expected) {
    assert actual.contains(expected) : "%s — %s\nexpected to contain: %s\nactual: %s".formatted(req, req.statement(), expected, actual);
}

String rejection(Check check) throws Exception {
    try {
        check.run();
    } catch (IllegalArgumentException e) {
        return e.getMessage();
    }
    throw new AssertionError("expected IllegalArgumentException was not thrown");
}
