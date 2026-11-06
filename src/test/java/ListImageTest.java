import static christianfrei.mpc3000.image.Requirement.Rn.*;

import christianfrei.mpc3000.image.Requirement;
import christianfrei.mpc3000.image.boundary.Images;

interface Check {
    void run() throws Exception;
}

record Case(Requirement.Rn req, Check check) {}

void main() throws Exception {
    var sounds = FatImage.Partition.of("MPC-PART--A", new FatImage.Entry("SOUND001", "SND", 700, 2, 3));
    var empty = FatImage.Partition.of("MPC-PART--B");
    var named = FatImage.Partition.of("MPC-PART--A",
            new FatImage.Entry("11042026B", "PGM", 300, 2),
            new FatImage.Entry("11042026C", "PGM", 300, 3));

    var cases = List.of(
            new Case(R1_1, () -> {
                var listing = Images.list(FatImage.write(FatImage.disk(sounds, empty)));
                assertContains(R1_1, listing, "2 partition(s)");
                assertContains(R1_1, listing, "MPC-PART--A  32.0 KiB capacity, 1 file(s), 1.0 KiB used");
                assertContains(R1_1, listing, "MPC-PART--B  32.0 KiB capacity, 0 file(s), 0 B used");
                assert listing.indexOf("MPC-PART--A") < listing.indexOf("MPC-PART--B") : R1_1 + " — partitions out of order:\n" + listing;
            }),
            new Case(R1_2, () -> {
                var listing = Images.list(FatImage.write(FatImage.floppy(sounds)));
                assertContains(R1_2, listing, "1 partition(s)");
                assertContains(R1_2, listing, "MPC-PART--A  32.0 KiB capacity, 1 file(s)");
            }),
            new Case(R1_3, () -> {
                var listing = Images.list(FatImage.write(FatImage.floppy(named)));
                assertContains(R1_3, listing, "11042026B.PGM                 300  program");
                assertContains(R1_3, listing, "11042026C.PGM                 300  program");
                assertContains(R1_3, Images.list(FatImage.write(FatImage.floppy(sounds))), "SOUND001.SND                  700  sound");
            }),
            new Case(R1_4, () -> {
                var message = rejection(() -> Images.list(Path.of("missing.iso")));
                assertContains(R1_4, message, "missing.iso");
            }),
            new Case(R1_5, () -> {
                var message = rejection(() -> Images.list(FatImage.write(new byte[4096])));
                assertContains(R1_5, message, "not an MPC3000 image");
            }),
            new Case(R1_6, () -> {
                var unlabeled = FatImage.Partition.of("", new FatImage.Entry("SOUND001", "SND", 700, 2, 3));
                var listing = Images.list(FatImage.write(FatImage.floppy(unlabeled)));
                assertContains(R1_6, listing, "\n(no label)  32.0 KiB capacity, 1 file(s)");
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
    assert actual.contains(expected) : "%s — %s\nexpected to contain: %s\nactual:\n%s".formatted(req, req.statement(), expected, actual);
}

String rejection(Check check) throws Exception {
    try {
        check.run();
    } catch (IllegalArgumentException expected) {
        return expected.getMessage();
    }
    throw new AssertionError("expected IllegalArgumentException was not thrown");
}
