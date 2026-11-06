import christianfrei.mpc3000.image.boundary.Images;

/// Round trip extract → restore → extract against a real MPC3000 floppy; the boot sector must match the MPC3000's
void main() throws Exception {
    var original = Path.of("src/test/resources/FLOPPY01.img");
    if (!Files.exists(original)) {
        IO.println("skipped: " + original + " not present");
        return;
    }
    var work = Files.createTempDirectory("mpc3000-");
    Images.extract(original, work);
    var folder = work.resolve("FLOPPY01");
    Images.restore(folder, Optional.empty());
    var restored = work.resolve("FLOPPY01.img");
    var a = Files.readAllBytes(original);
    var b = Files.readAllBytes(restored);
    assert a.length == b.length : "size %d vs %d".formatted(a.length, b.length);
    assert Arrays.equals(a, 0, 512, b, 0, 512) : "boot sector differs";
    assert Arrays.equals(a, 512, 515, b, 512, 515) : "FAT start differs";

    var again = Files.createTempDirectory("mpc3000-");
    Images.extract(restored, again);
    try (var files = Files.list(folder)) {
        var names = files.toList();
        assert names.size() == 15 : "expected 15 files but got " + names.size();
        for (var file : names) {
            var copy = again.resolve("FLOPPY01").resolve(file.getFileName().toString());
            assert Files.exists(copy) && Files.mismatch(file, copy) == -1 : "round trip differs: " + file.getFileName();
        }
    }
}
