import static christianfrei.mpc3000.image.Requirement.Rn.*;

import christianfrei.mpc3000.image.Requirement;
import christianfrei.mpc3000.image.boundary.Images;

interface Check {
    void run() throws Exception;
}

record Case(Requirement.Rn req, Check check) {}

void main() throws Exception {
    var sound = new FatImage.Entry("SOUND001", "SND", 700, 2, 3);
    var program = new FatImage.Entry("11042026B", "PGM", 300, 4);
    var withSounds = FatImage.Partition.of("MPC-PART--A", sound, program);
    var programsOnly = FatImage.Partition.of("MPC-PART--B", new FatImage.Entry("PROGRAM1", "PGM", 300, 2));
    var empty = FatImage.Partition.of("MPC-PART--C");
    var disk = FatImage.write(FatImage.disk(withSounds, programsOnly, empty));

    var cases = List.of(
            new Case(R2_1, () -> {
                var target = extract(disk);
                assertContent(R2_1, target.resolve("MPC-PART--A/SOUND001.SND"), sound);
                assertContent(R2_1, target.resolve("MPC-PART--A/11042026B.PGM"), program);
            }),
            new Case(R2_2, () -> {
                var target = extract(disk);
                assert !Files.exists(target.resolve("MPC-PART--B")) : R2_2 + " — " + R2_2.statement() + " — partition without sounds was extracted";
                assert !Files.exists(target.resolve("MPC-PART--C")) : R2_2 + " — " + R2_2.statement() + " — empty partition was extracted";
            }),
            new Case(R2_3, () -> {
                var target = Images.defaultTarget(Path.of("some", "folder", "HD0.iso"));
                assert target.equals(Path.of("HD0")) : "%s — %s — expected HD0 but got %s".formatted(R2_3, R2_3.statement(), target);
            }),
            new Case(R2_4, () -> {
                var target = Files.createTempDirectory("mpc3000-");
                var partition = Files.createDirectories(target.resolve("MPC-PART--A"));
                Files.writeString(partition.resolve("SOUND001.SND"), "stale");
                Files.writeString(partition.resolve("keep.txt"), "mine");
                Images.extract(disk, target);
                assertContent(R2_4, partition.resolve("SOUND001.SND"), sound);
                assert Files.readString(partition.resolve("keep.txt")).equals("mine") : R2_4 + " — " + R2_4.statement() + " — existing file was not kept";
            }),
            new Case(R2_5, () -> {
                var fragmented = new FatImage.Entry("SOUND002", "SND", 1300, 5, 2, 9);
                var target = extract(FatImage.write(FatImage.floppy(FatImage.Partition.of("MPC-PART--A", fragmented))));
                assertContent(R2_5, target.resolve("MPC-PART--A/SOUND002.SND"), fragmented);
            }),
            new Case(R2_6, () -> {
                var first = new FatImage.Entry("SOUND001", "SND", 100, 2);
                var second = new FatImage.Entry("SOUND001", "SND", 200, 3);
                var target = extract(FatImage.write(FatImage.floppy(FatImage.Partition.of("MPC-PART--A", first, second))));
                assertContent(R2_6, target.resolve("MPC-PART--A/SOUND001.SND"), first);
                assertContent(R2_6, target.resolve("MPC-PART--A/SOUND001~2.SND"), second);
            }),
            new Case(R2_7, () -> {
                var reserved = new FatImage.Entry("SOUND*01", "SND", 100, 2);
                var symbol = new FatImage.Entry("(OUND038", "SND", 100, 3);
                var target = extract(FatImage.write(FatImage.floppy(FatImage.Partition.of("MPC-PART--A", reserved, symbol))));
                assertContent(R2_7, target.resolve("MPC-PART--A/SOUND_01.SND"), reserved);
                assertContent(R2_7, target.resolve("MPC-PART--A/(OUND038.SND"), symbol);
            }),
            new Case(R2_8, () -> {
                var file = Files.createTempFile("mpc3000-", ".txt");
                var message = rejection(IllegalArgumentException.class, () -> Images.extract(disk, file));
                assertContains(R2_8, message, "not a folder");
            }),
            new Case(R2_9, () -> {
                var truncated = new FatImage.Entry("SOUND003", "SND", 1500, 2);
                var image = FatImage.write(FatImage.floppy(FatImage.Partition.of("MPC-PART--A", truncated)));
                var message = rejection(IOException.class, () -> extract(image));
                assertContains(R2_9, message, "SOUND003.SND");
            }));

    for (var c : cases) {
        try {
            c.check().run();
        } catch (Exception e) {
            throw new AssertionError("%s — %s".formatted(c.req(), c.req().statement()), e);
        }
    }
}

Path extract(Path image) throws IOException {
    var target = Files.createTempDirectory("mpc3000-");
    Images.extract(image, target);
    return target;
}

void assertContent(Requirement.Rn req, Path file, FatImage.Entry entry) throws IOException {
    assert Files.exists(file) : "%s — %s — missing %s".formatted(req, req.statement(), file);
    assert Arrays.equals(Files.readAllBytes(file), entry.content()) : "%s — %s — content differs: %s".formatted(req, req.statement(), file);
}

void assertContains(Requirement.Rn req, String actual, String expected) {
    assert actual.contains(expected) : "%s — %s\nexpected to contain: %s\nactual: %s".formatted(req, req.statement(), expected, actual);
}

String rejection(Class<? extends Exception> expected, Check check) throws Exception {
    try {
        check.run();
    } catch (Exception e) {
        if (expected.isInstance(e))
            return e.getMessage();
        throw e;
    }
    throw new AssertionError("expected %s was not thrown".formatted(expected.getSimpleName()));
}
