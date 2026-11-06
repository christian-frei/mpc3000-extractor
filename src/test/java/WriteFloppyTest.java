import static christianfrei.mpc3000.floppy.Requirement.Rn.*;

import christianfrei.mpc3000.floppy.Requirement;
import christianfrei.mpc3000.floppy.boundary.Floppies;
import christianfrei.mpc3000.floppy.control.Drives;
import christianfrei.mpc3000.floppy.control.MacDrives;
import christianfrei.mpc3000.floppy.entity.Disk;
import java.util.concurrent.CancellationException;

interface Check {
    void run() throws Exception;
}

record Case(Requirement.Rn req, Check check) {}

/// Stands in for the drive: holds the disk's bytes, "writes" the image onto them and reads them back
static class FakeDrives implements Drives {

    List<Disk> disks = List.of(new Disk("disk9", 1_474_560));
    boolean writeProtected;
    boolean terminal = true;
    String answer = "y";
    boolean cancel;
    IOException failure;
    /// Sectors that read back as zeros
    Set<Integer> badSectors = Set.of();
    String question;
    Disk written;
    byte[] onDisk;

    @Override
    public List<Disk> externalDisks() {
        return disks;
    }

    @Override
    public boolean canAskForPassword() {
        return terminal;
    }

    @Override
    public String copy(Disk disk, Path image) {
        throw new UnsupportedOperationException("writing never reads into an image");
    }

    @Override
    public boolean isWriteProtected(Disk disk) {
        return writeProtected;
    }

    @Override
    public boolean confirm(String question) {
        this.question = question;
        return answer.equals("y");
    }

    @Override
    public void write(Disk disk, Path image, Path readBack) throws IOException {
        if (cancel)
            throw new CancellationException("cancelled");
        if (failure != null)
            throw failure;
        written = disk;
        onDisk = Files.readAllBytes(image);
        var copy = onDisk.clone();
        for (var sector : badSectors)
            Arrays.fill(copy, sector * 512, sector * 512 + 512, (byte) 0);
        Files.write(readBack, copy);
    }
}

void main() throws Exception {
    var floppy = floppyImage(1_474_560);
    var image = Files.write(Files.createTempDirectory("mpc3000-").resolve("KIT.img"), floppy);
    var disk = new Disk("disk9", 1_474_560);

    var cases = List.of(
            new Case(R2_1, () -> {
                var drives = new FakeDrives();
                var output = new ArrayList<String>();
                Floppies.write(image, drives, output::add);
                assert Arrays.equals(drives.onDisk, floppy) : R2_1 + " — " + R2_1.statement() + " — disk differs";
                assertContains(R2_1, String.join("\n", output), "wrote and verified");
                var command = MacDrives.writeShellCommand(disk, image, Path.of("/tmp/rb.img"), "duke");
                var unmount = command.indexOf("diskutil unmountDisk '/dev/disk9'");
                var write = command.indexOf("of='/dev/rdisk9'");
                var readBack = command.indexOf("if='/dev/rdisk9' of='/tmp/rb.img'");
                assert unmount >= 0 && unmount < write && write < readBack : "%s — %s — expected unmount, write, read back: %s".formatted(R2_1, R2_1.statement(), command);
            }),
            new Case(R2_2, () -> {
                var command = MacDrives.writeShellCommand(disk, image, Path.of("/tmp/rb.img"), "duke");
                var eject = command.indexOf("diskutil eject '/dev/disk9'");
                assert eject > command.indexOf("if='/dev/rdisk9'") : "%s — %s — no eject after reading back: %s".formatted(R2_2, R2_2.statement(), command);
                assert !command.substring(eject).contains("&&") : "%s — %s — eject depends on the write succeeding".formatted(R2_2, R2_2.statement());
            }),
            new Case(R2_3, () -> {
                var drives = new FakeDrives();
                Floppies.write(image, drives, _ -> {});
                assertContains(R2_3, drives.question, "disk9");
                assertContains(R2_3, drives.question, "KIT.img");
                var declined = new FakeDrives();
                declined.answer = "n";
                var message = rejection(IllegalArgumentException.class, () -> Floppies.write(image, declined, _ -> {}));
                assertContains(R2_3, message, "untouched");
                assert declined.written == null : R2_3 + " — " + R2_3.statement() + " — written without confirmation";
            }),
            new Case(R2_4, () -> {
                var drives = new FakeDrives();
                drives.disks = List.of(new Disk("disk2", 500_107_862_016L), disk);
                Floppies.write(image, drives, _ -> {});
                assert disk.equals(drives.written) : "%s — %s — wrote %s".formatted(R2_4, R2_4.statement(), drives.written);
                var none = new FakeDrives();
                none.disks = List.of(new Disk("disk2", 500_107_862_016L));
                assertContains(R2_4, rejection(IllegalArgumentException.class, () -> Floppies.write(image, none, _ -> {})), "no floppy disk found");
                var several = new FakeDrives();
                several.disks = List.of(new Disk("disk8", 1_474_560), disk);
                assertContains(R2_4, rejection(IllegalArgumentException.class, () -> Floppies.write(image, several, _ -> {})), "disk8, disk9");
            }),
            new Case(R2_5, () -> {
                var missing = image.resolveSibling("MISSING.img");
                assertContains(R2_5, rejection(IllegalArgumentException.class, () -> Floppies.write(missing, new FakeDrives(), _ -> {})), "MISSING.img");
            }),
            new Case(R2_6, () -> {
                var small = new FakeDrives();
                small.disks = List.of(new Disk("disk9", 737_280));
                assertContains(R2_6, rejection(IllegalArgumentException.class, () -> Floppies.write(image, small, _ -> {})), "holds 737,280");
                var noFat = Files.write(image.resolveSibling("NOISE.img"), new byte[1_474_560]);
                var drives = new FakeDrives();
                assertContains(R2_6, rejection(IllegalArgumentException.class, () -> Floppies.write(noFat, drives, _ -> {})), "no FAT boot sector");
                assert drives.written == null && drives.question == null : R2_6 + " — " + R2_6.statement() + " — went on to write";
            }),
            new Case(R2_7, () -> {
                var drives = new FakeDrives();
                drives.writeProtected = true;
                assertContains(R2_7, rejection(IllegalArgumentException.class, () -> Floppies.write(image, drives, _ -> {})), "write-protected");
                assert drives.written == null : R2_7 + " — " + R2_7.statement() + " — written";
                assert MacDrives.isWriteProtected(info("false")) && !MacDrives.isWriteProtected(info("true")) : R2_7 + " — " + R2_7.statement() + " — WritableMedia misread";
            }),
            new Case(R2_8, () -> {
                var drives = new FakeDrives();
                drives.badSectors = Set.of(2879);
                var message = rejection(IOException.class, () -> Floppies.write(image, drives, _ -> {}));
                assert message.startsWith("1 sector(s) read back differently") : "%s — %s — got: %s".formatted(R2_8, R2_8.statement(), message);
                var unreadable = new FakeDrives() {
                    @Override
                    public void write(Disk disk, Path image, Path readBack) {}
                };
                var nothing = rejection(IOException.class, () -> Floppies.write(image, unreadable, _ -> {}));
                assert nothing.startsWith("2880 sector(s)") : "%s — %s — without read-back got: %s".formatted(R2_8, R2_8.statement(), nothing);
            }),
            new Case(R2_9, () -> {
                // unprivileged and for a disk that does not exist, diskutil refuses and nothing is written
                var readBack = Files.createTempDirectory("mpc3000-").resolve("rb.img");
                var output = shell(MacDrives.writeShellCommand(new Disk("disk999", 1_474_560), image, readBack, user()));
                var message = rejection(IOException.class, () -> MacDrives.written(output));
                assertContains(R2_9, message, "disk999");
                assert !Files.exists(readBack) : R2_9 + " — " + R2_9.statement() + " — read back after a failed write";
                var drives = new FakeDrives();
                drives.failure = new IOException("write failed (exit 1): dd: /dev/rdisk9: Input/output error");
                assertContains(R2_9, rejection(IOException.class, () -> Floppies.write(image, drives, _ -> {})), "Input/output error");
            }),
            new Case(R2_10, () -> {
                var drives = new FakeDrives();
                drives.cancel = true;
                assertContains(R2_10, rejection(IllegalArgumentException.class, () -> Floppies.write(image, drives, _ -> {})), "cancelled");
                assertContains(R2_10, String.join(" ", MacDrives.writeCommand(disk, image, Path.of("/tmp/rb.img"), "duke")), "sudo -p ");
            }),
            new Case(R2_11, () -> {
                var drives = new FakeDrives();
                drives.terminal = false;
                assertContains(R2_11, rejection(IllegalArgumentException.class, () -> Floppies.write(image, drives, _ -> {})), "in a terminal");
                assert drives.question == null && drives.written == null : R2_11 + " — " + R2_11.statement() + " — went on without a terminal";
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

/// Random sectors behind a floppy boot sector
byte[] floppyImage(int size) {
    var bytes = new byte[size];
    new Random(3000).nextBytes(bytes);
    var boot = new byte[512];
    boot[0] = (byte) 0xEB;
    boot[1] = 0x34;
    boot[2] = (byte) 0x90;
    boot[12] = 0x02;
    boot[510] = 0x55;
    boot[511] = (byte) 0xAA;
    System.arraycopy(boot, 0, bytes, 0, 512);
    return bytes;
}

String info(String writable) {
    return """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0"><dict>
            <key>Writable</key><true/>
            <key>WritableMedia</key><%s/>
            </dict></plist>""".formatted(writable);
}

/// Runs a shell command unprivileged
String shell(String command) throws Exception {
    var process = new ProcessBuilder("/bin/sh", "-c", command).redirectErrorStream(true).start();
    var output = new String(process.getInputStream().readAllBytes());
    assert process.waitFor() == 0 : "shell command failed: " + output;
    return output;
}

String user() {
    return System.getProperty("user.name");
}

void assertContains(Requirement.Rn req, String actual, String expected) {
    assert actual != null && actual.contains(expected) : "%s — %s\nexpected to contain: %s\nactual: %s".formatted(req, req.statement(), expected, actual);
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
