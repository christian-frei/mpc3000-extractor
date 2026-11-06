import christianfrei.mpc3000.image.boundary.Images;

/// Regression against a real MPC3000 floppy; checksums come from the macOS FAT driver reading the mounted disk
void main() throws Exception {
    var image = Path.of("src/test/resources/FLOPPY01.img");
    if (!Files.exists(image)) {
        IO.println("skipped: " + image + " not present");
        return;
    }
    var listing = Images.list(image);
    assert listing.contains("1 partition(s)") : listing;
    assert listing.contains("(no label)  1.4 MiB capacity, 15 file(s), 887.0 KiB used") : listing;
    assert listing.contains("PROGRAM_01.PGM") : listing;

    var target = Files.createTempDirectory("mpc3000-");
    Images.extract(image, target);
    var checksums = Map.ofEntries(
            Map.entry("1OUND067.SND", "f2b8a2f126e2dab2dbb6f39fe76ecdfd97ddd54409152ad2e902f75fb0bec792"),
            Map.entry("1OUND072.SND", "c04aab29c98c6dbdaaa1ebdb06aad1651fbe158d99c85102ae0a4d16e9dfd058"),
            Map.entry("1OUND073.SND", "2ffeb274fc3bdb58806efcc88aee9e39266c0c5988d8abc525a6699cb6529d09"),
            Map.entry("2OUND067.SND", "44608879e8801321c6097bf0b08c12209269d76dcb970a6f1c0274b29b45dd86"),
            Map.entry("3OUND067.SND", "a48ac4d2a7143a87d277c1cc72912453daad11e4a0813cd0a532edbe8e610285"),
            Map.entry("ABB_SEQS.ALL", "3916aa45b75548e5fa697f97ed9cb90664ae7ffc9d7376a4ea5fc4471ce3100b"),
            Map.entry("PROGRAM_01.PGM", "65b096a43db46eb73864bb9100b48f9f70956df0858db7d89e0b3cff1c5af4c9"),
            Map.entry("SOUND067.SND", "b54a1de2c15d85b6a844b437c8579cde25c179ffa6c32f1780e1168e38569440"),
            Map.entry("SOUND068.SND", "afad04f5b78954330ce41707ca7b15fb7505e4623731b7479183214ecc0bd048"),
            Map.entry("SOUND070.SND", "d780cdd417757dc73425453aba362cd88c677c57e79fd701d45fa772ab695190"),
            Map.entry("SOUND071.SND", "9d066b71cde797eae1eb3f66c9e7af738c3a3084c7e57e48a255be58c136bf8e"),
            Map.entry("SOUND072.SND", "0b74a3535f3f759d259cf48c247582eea0f12bea47fbda545908aea369ae100b"),
            Map.entry("SOUND073.SND", "7825a63fbd1924a527964a773f352711789b5df89326d7f3601eb78d09d1c8d6"),
            Map.entry("SOUND074.SND", "e6d0380fb8fe498e726db6547afc7d9f57795688cf0f40076eafac83bd26a202"),
            Map.entry("SOUND075.SND", "759b9ea174af4d8f262b12a3dac8af5b7106273cd54e62819015a614e0fcd0e9"));
    var folder = target.resolve("FLOPPY01");
    try (var files = Files.list(folder)) {
        var names = files.map(f -> f.getFileName().toString()).collect(Collectors.toSet());
        assert names.equals(checksums.keySet()) : "expected %s but got %s".formatted(checksums.keySet(), names);
    }
    for (var entry : checksums.entrySet()) {
        var actual = sha256(folder.resolve(entry.getKey()));
        assert actual.equals(entry.getValue()) : "%s: expected %s but got %s".formatted(entry.getKey(), entry.getValue(), actual);
    }
}

String sha256(Path file) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
}
