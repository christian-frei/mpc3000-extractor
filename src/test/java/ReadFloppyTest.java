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

/// Stands in for the drive: "copies" fixed bytes and records which disk it read
static class FakeDrives implements Drives {

    final List<Disk> disks;
    final byte[] content;
    final String output;
    final boolean cancel;
    Disk copied;

    FakeDrives(List<Disk> disks, byte[] content, String output, boolean cancel) {
        this.disks = disks;
        this.content = content;
        this.output = output;
        this.cancel = cancel;
    }

    static FakeDrives floppy(byte[] content) {
        return new FakeDrives(List.of(new Disk("disk9", content.length)), content, "2880+0 records in\r2880+0 records out\r", false);
    }

    @Override
    public List<Disk> externalDisks() {
        return disks;
    }

    @Override
    public boolean canAskForPassword() {
        return true;
    }

    @Override
    public String copy(Disk disk, Path image) throws IOException {
        copied = disk;
        Files.write(image, content);
        if (cancel)
            throw new CancellationException("cancelled");
        return output;
    }
}

void main() throws Exception {
    var content = new byte[1_474_560];
    new Random(3000).nextBytes(content);
    var hardDisk = new Disk("disk2", 500_107_862_016L);

    var cases = List.of(
            new Case(R1_1, () -> {
                var folder = Files.createTempDirectory("mpc3000-");
                var drives = FakeDrives.floppy(content);
                var output = new ArrayList<String>();
                Floppies.read(Optional.empty(), folder, drives, output::add);
                assert Arrays.equals(Files.readAllBytes(folder.resolve("FLOPPY01.img")), content) : R1_1 + " — " + R1_1.statement() + " — image differs";
                assertContains(R1_1, String.join("\n", output), "1,474,560 bytes");
                var device = Files.write(folder.resolve("device's copy"), content);
                var image = folder.resolve("COPY 'A'.img");
                MacDrives.ddOutput(shell(MacDrives.shellCommand(device.toString(), image, user())), image);
                assert Arrays.equals(Files.readAllBytes(image), content) : R1_1 + " — " + R1_1.statement() + " — dd copy differs";
                assert Arrays.equals(Files.readAllBytes(device), content) : R1_1 + " — " + R1_1.statement() + " — source changed";
                var command = MacDrives.copyCommand(new Disk("disk9", 1_474_560), image, "duke").getLast();
                assertContains(R1_1, command, "if='/dev/rdisk9'");
                assert !command.contains("of='/dev") : R1_1 + " — " + R1_1.statement() + " — writes to a device: " + command;
            }),
            new Case(R1_2, () -> {
                var folder = Files.createTempDirectory("mpc3000-");
                Files.write(folder.resolve("FLOPPY01.img"), new byte[1]);
                Files.write(folder.resolve("FLOPPY03.img"), new byte[1]);
                Floppies.read(Optional.empty(), folder, FakeDrives.floppy(content), _ -> {});
                assert Files.size(folder.resolve("FLOPPY02.img")) == content.length : R1_2 + " — " + R1_2.statement() + " — FLOPPY02.img not written";
            }),
            new Case(R1_3, () -> {
                var folder = Files.createTempDirectory("mpc3000-");
                var image = folder.resolve("MY DISK.img");
                Floppies.read(Optional.of(image), folder, FakeDrives.floppy(content), _ -> {});
                assert Files.size(image) == content.length : R1_3 + " — " + R1_3.statement() + " — named image not written";
                assert !Files.exists(folder.resolve("FLOPPY01.img")) : R1_3 + " — " + R1_3.statement() + " — default name used";
            }),
            new Case(R1_4, () -> {
                var floppy = new Disk("disk9", content.length);
                var drives = new FakeDrives(List.of(hardDisk, floppy), content, "", false);
                Floppies.read(Optional.empty(), Files.createTempDirectory("mpc3000-"), drives, _ -> {});
                assert floppy.equals(drives.copied) : "%s — %s — read %s".formatted(R1_4, R1_4.statement(), drives.copied);
                var plist = """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
                        <plist version="1.0"><dict>
                        <key>AllDisksAndPartitions</key><array>
                        <dict><key>Content</key><string></string><key>DeviceIdentifier</key><string>disk4</string>
                        <key>Size</key><integer>1474560</integer></dict>
                        </array></dict></plist>""";
                var disks = MacDrives.disks(plist);
                assert disks.equals(List.of(new Disk("disk4", 1_474_560))) : "%s — %s — parsed %s".formatted(R1_4, R1_4.statement(), disks);
            }),
            new Case(R1_5, () -> {
                var drives = new FakeDrives(List.of(hardDisk), content, "", false);
                var message = rejection(IllegalArgumentException.class, () -> Floppies.read(Optional.empty(), Files.createTempDirectory("mpc3000-"), drives, _ -> {}));
                assertContains(R1_5, message, "no floppy disk found");
            }),
            new Case(R1_6, () -> {
                var drives = new FakeDrives(List.of(new Disk("disk8", 737_280), new Disk("disk9", 1_474_560)), content, "", false);
                var message = rejection(IllegalArgumentException.class, () -> Floppies.read(Optional.empty(), Files.createTempDirectory("mpc3000-"), drives, _ -> {}));
                assertContains(R1_6, message, "disk8, disk9");
            }),
            new Case(R1_7, () -> {
                var folder = Files.createTempDirectory("mpc3000-");
                var image = Files.writeString(folder.resolve("FLOPPY01.img"), "keep");
                var drives = FakeDrives.floppy(content);
                var message = rejection(IllegalArgumentException.class, () -> Floppies.read(Optional.of(image), folder, drives, _ -> {}));
                assertContains(R1_7, message, "already exists");
                assert Files.readString(image).equals("keep") && drives.copied == null : R1_7 + " — " + R1_7.statement() + " — existing image touched";
            }),
            new Case(R1_8, () -> {
                var folder = Files.createTempDirectory("mpc3000-");
                var output = "dd: /dev/rdisk9: Input/output error\rdd: /dev/rdisk9: Input/output error\r2878+2 records in\r2880+0 records out\r";
                var drives = new FakeDrives(List.of(new Disk("disk9", content.length)), content, output, false);
                var message = rejection(IOException.class, () -> Floppies.read(Optional.empty(), folder, drives, _ -> {}));
                assertContains(R1_8, message, "2 sector(s) could not be read");
                assert Files.exists(folder.resolve("FLOPPY01.img")) : R1_8 + " — " + R1_8.statement() + " — partial image not kept";
            }),
            new Case(R1_9, () -> {
                var folder = Files.createTempDirectory("mpc3000-");
                var drives = new FakeDrives(List.of(new Disk("disk9", content.length)), content, "", true);
                var message = rejection(IllegalArgumentException.class, () -> Floppies.read(Optional.empty(), folder, drives, _ -> {}));
                assertContains(R1_9, message, "cancelled");
                assert !Files.exists(folder.resolve("FLOPPY01.img")) : R1_9 + " — " + R1_9.statement() + " — image left behind";
            }),
            new Case(R1_10, () -> {
                var folder = Files.createTempDirectory("mpc3000-");
                var device = Files.write(folder.resolve("device"), content);
                var image = folder.resolve("FLOPPY01.img");
                MacDrives.ddOutput(shell(MacDrives.shellCommand(device.toString(), image, user())), image);
                var owner = Files.getOwner(image).getName();
                assert owner.equals(user()) : "%s — %s — owner %s".formatted(R1_10, R1_10.statement(), owner);
                assertContains(R1_10, MacDrives.copyCommand(new Disk("disk9", 1_474_560), image, "duke").getLast(), "/usr/sbin/chown 'duke'");
            }),
            new Case(R1_11, () -> {
                var folder = Files.createTempDirectory("mpc3000-");
                var image = folder.resolve("FLOPPY01.img");
                var message = rejection(IOException.class, () -> MacDrives.ddOutput(shell(MacDrives.shellCommand(folder.resolve("no-device").toString(), image, user())), image));
                assertContains(R1_11, message, "No such file or directory");
                var failing = new FakeDrives(List.of(new Disk("disk9", content.length)), content, "", false) {
                    @Override
                    public String copy(Disk disk, Path target) throws IOException {
                        Files.write(target, new byte[512]);
                        throw new IOException("copy failed: dd: /dev/rdisk9: Operation not permitted");
                    }
                };
                var reason = rejection(IOException.class, () -> Floppies.read(Optional.empty(), folder, failing, _ -> {}));
                assertContains(R1_11, reason, "Operation not permitted");
                assert !Files.exists(folder.resolve("FLOPPY01.img")) : R1_11 + " — " + R1_11.statement() + " — image left behind";
            }),
            new Case(R1_12, () -> {
                var folder = Files.createTempDirectory("mpc3000-");
                var noTerminal = new FakeDrives(List.of(new Disk("disk9", content.length)), content, "", false) {
                    @Override
                    public boolean canAskForPassword() {
                        return false;
                    }
                };
                var message = rejection(IllegalArgumentException.class, () -> Floppies.read(Optional.empty(), folder, noTerminal, _ -> {}));
                assertContains(R1_12, message, "in a terminal");
                assert noTerminal.copied == null : R1_12 + " — " + R1_12.statement() + " — copied without a terminal";
                assertContains(R1_12, String.join(" ", MacDrives.copyCommand(new Disk("disk9", 1_474_560), folder.resolve("X.img"), "duke")), "sudo -p ");
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

/// Runs a shell command unprivileged, the way the password dialog runs it elevated
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
