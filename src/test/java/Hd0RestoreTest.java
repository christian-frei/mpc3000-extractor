import christianfrei.mpc3000.image.boundary.Images;

/// Round trip extract → restore → extract against a real MPC3000 hard disk; the restored partition table,
/// boot sectors and empty partitions must match the MPC3000's bytes
void main() throws Exception {
    var original = Path.of("src/test/resources/HD0.iso");
    if (!Files.exists(original)) {
        IO.println("skipped: " + original + " not present");
        return;
    }
    var work = Files.createTempDirectory("mpc3000-");
    var folder = work.resolve("HD0");
    Images.extract(original, folder);
    Images.restore(folder, Optional.empty());
    var restored = work.resolve("HD0.iso");
    try (var a = FileChannel.open(original); var b = FileChannel.open(restored)) {
        assert a.size() == b.size() : "size %d vs %d".formatted(a.size(), b.size());
        assertSame(a, b, 0, 512, "MBR");
        for (var k = 0; k < 23; k++) {
            var ebr = k == 0 ? 63L : 64_260L * k;
            assertSame(a, b, ebr * 512, 63 * 512, "EBR and gap of partition " + k);
            // partitions G–W are empty: boot sector, FATs and root directory must match entirely
            assertSame(a, b, (ebr + 63) * 512, k < 6 ? 512 : 512 + 2 * 11 * 512 + 512 * 32, "metadata of partition " + k);
        }
    }
    var again = work.resolve("again");
    Images.extract(restored, again);
    assertSameTree(folder, again);
    Files.delete(restored);
}

void assertSame(FileChannel a, FileChannel b, long position, int length, String what) throws IOException {
    var x = ByteBuffer.allocate(length);
    var y = ByteBuffer.allocate(length);
    a.read(x, position);
    b.read(y, position);
    assert x.flip().equals(y.flip()) : what + " differs";
}

void assertSameTree(Path expected, Path actual) throws IOException {
    try (var files = Files.walk(expected)) {
        for (var file : files.filter(Files::isRegularFile).toList()) {
            var copy = actual.resolve(expected.relativize(file).toString());
            assert Files.exists(copy) && Files.mismatch(file, copy) == -1 : "round trip differs: " + expected.relativize(file);
        }
    }
    try (var a = Files.walk(expected); var b = Files.walk(actual)) {
        assert a.count() == b.count() : "round trip has a different number of files";
    }
}
