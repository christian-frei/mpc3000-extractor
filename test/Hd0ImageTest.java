import christianfrei.mpc3000.image.boundary.Images;

/// Regression against a real MPC3000 hard disk; checksums come from an independent FAT12 reader
void main() throws Exception {
    var image = Path.of("test/HD0.iso");
    if (!Files.exists(image)) {
        IO.println("skipped: " + image + " not present");
        return;
    }
    var listing = Images.list(image);
    assert listing.contains("23 partition(s)") : listing;
    assert listing.contains("MPC-PART--A  31.3 MiB capacity, 66 file(s), 5.4 MiB used") : listing;
    assert listing.contains("MPC-PART--W  31.3 MiB capacity, 0 file(s), 0 B used") : listing;

    var target = Files.createTempDirectory("mpc3000-");
    Images.extract(image, target);
    var expected = Map.of(
            "MPC-PART--A", 66, "MPC-PART--B", 12, "MPC-PART--C", 26,
            "MPC-PART--D", 53, "MPC-PART--E", 35, "MPC-PART--F", 11);
    try (var folders = Files.list(target)) {
        var actual = folders.collect(Collectors.toMap(f -> f.getFileName().toString(), this::count));
        assert actual.equals(expected) : "expected %s but got %s".formatted(expected, actual);
    }
    var checksums = Map.of(
            "MPC-PART--A/SOUND001.SND", "8c92fea81fed91a101e34f3fe92de926ce075cdfc3b6be1d0691284dc18178a3",
            "MPC-PART--A/(OUND038.SND", "e360970ddf59997803812e12dad5d2285667da39e054e8989784c2ab60f2c939",
            "MPC-PART--A/STARTUP.ALL", "4bb3488f6a2cc753164a6adeb618e96616f16b7a511d7e715aa1d451513573eb",
            "MPC-PART--C/11042026B.PGM", "6b1b837eb606793eb68506f8bb44cdd356e7bba95de66a315a1c9d728efc4b4c",
            "MPC-PART--C/11042026C.PGM", "ecf276a7a3dbde5741b44f340e20ae4e0366d440547d3f2955cef2ff0f9602d6",
            "MPC-PART--D/20260725A.ALL", "b597ded71fb24be79978062f707e4e70e54678618666a9e1eb42e3a86e858558");
    for (var entry : checksums.entrySet()) {
        var actual = sha256(target.resolve(entry.getKey()));
        assert actual.equals(entry.getValue()) : "%s: expected %s but got %s".formatted(entry.getKey(), entry.getValue(), actual);
    }
}

int count(Path folder) {
    try (var files = Files.list(folder)) {
        return (int) files.count();
    } catch (IOException e) {
        throw new UncheckedIOException(e);
    }
}

String sha256(Path file) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
}
