import static christianfrei.mpc3000.image.Requirement.Rn.*;

import christianfrei.mpc3000.image.Requirement;
import christianfrei.mpc3000.image.boundary.Images;

interface Check {
    void run() throws Exception;
}

record Case(Requirement.Rn req, Check check) {}

void main() throws Exception {
    var cases = List.of(
            new Case(R4_1, () -> {
                var folder = Files.createTempDirectory("mpc3000-");
                var image = folder.resolve("HD1.iso");
                Images.format(image, false);
                assert Files.size(image) == 779_091_968 : R4_1 + " — " + R4_1.statement() + " — size " + Files.size(image);
                var listing = Images.list(image);
                assertContains(R4_1, listing, "23 partition(s)");
                assert listing.lines().filter(line -> line.startsWith("MPC-PART--")).allMatch(line -> line.contains(" 0 file(s)"))
                        : R4_1 + " — " + R4_1.statement() + " — partitions not empty:\n" + listing;
                // a restore of only MPC-PART--W leaves A–V in the same empty state
                var source = Files.createDirectories(folder.resolve("DISK/MPC-PART--W"));
                Files.write(source.resolve("SOUND1.SND"), new byte[10]);
                Images.restore(folder.resolve("DISK"), Optional.empty());
                var formatted = Files.readAllBytes(image);
                var restored = Files.readAllBytes(folder.resolve("DISK.iso"));
                var end = 22 * 64_260 * 512;
                assert Arrays.equals(formatted, 0, end, restored, 0, end) : R4_1 + " — " + R4_1.statement() + " — differs from a restored image";
            }),
            new Case(R4_2, () -> {
                var image = Files.createTempDirectory("mpc3000-").resolve("EMPTY.img");
                Images.format(image, true);
                assert Files.size(image) == 1_474_560 : R4_2 + " — " + R4_2.statement() + " — size " + Files.size(image);
                assertContains(R4_2, Images.list(image), "(no label)  1.4 MiB capacity, 0 file(s)");
                var bytes = Files.readAllBytes(image);
                assert bytes[0] == (byte) 0xEB && bytes[1] == 0x34 && bytes[512] == (byte) 0xF0 : R4_2 + " — " + R4_2.statement() + " — not the MPC floppy layout";
            }),
            new Case(R4_3, () -> {
                var image = Files.writeString(Files.createTempDirectory("mpc3000-").resolve("HD1.iso"), "mine");
                var message = rejection(() -> Images.format(image, false));
                assertContains(R4_3, message, "already exists");
                assert Files.readString(image).equals("mine") : R4_3 + " — " + R4_3.statement() + " — existing file changed";
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
