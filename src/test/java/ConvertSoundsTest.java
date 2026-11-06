import javax.sound.sampled.AudioSystem;

import static christianfrei.mpc3000.sound.Requirement.Rn.*;

import christianfrei.mpc3000.sound.Requirement;
import christianfrei.mpc3000.sound.boundary.Sounds;

interface Check {
    void run() throws Exception;
}

record Case(Requirement.Rn req, Check check) {}

void main() throws Exception {
    var kick = SndFile.Spec.of("KICK", 0, 300, 300);
    var snare = SndFile.Spec.of("SNARE", 10, 100, 400);

    var cases = List.of(
            new Case(R1_1, () -> {
                var folder = Files.createTempDirectory("mpc3000-");
                var snd = SndFile.write(folder, "KICK.SND", kick.bytes());
                Sounds.convert(snd, Optional.empty(), _ -> {});
                assertWav(R1_1, folder.resolve("KICK.WAV"), kick);
            }),
            new Case(R1_2, () -> {
                var folder = Files.createTempDirectory("mpc3000-");
                SndFile.write(folder.resolve("A"), "KICK.SND", kick.bytes());
                SndFile.write(folder.resolve("A/B"), "snare.snd", snare.bytes());
                SndFile.write(folder, "notes.txt", "no sound".getBytes());
                Sounds.convert(folder, Optional.empty(), _ -> {});
                assertWav(R1_2, folder.resolve("A/KICK.WAV"), kick);
                assertWav(R1_2, folder.resolve("A/B/snare.wav"), snare);
                assertMissing(R1_2, folder.resolve("notes.wav"));
            }),
            new Case(R1_3, () -> {
                var folder = Files.createTempDirectory("mpc3000-");
                SndFile.write(folder.resolve("MPC-PART--A"), "KICK.SND", kick.bytes());
                Sounds.convert(folder, Optional.empty(), _ -> {});
                assertWav(R1_3, folder.resolve("MPC-PART--A/KICK.WAV"), kick);
            }),
            new Case(R1_4, () -> {
                var folder = Files.createTempDirectory("mpc3000-");
                SndFile.write(folder.resolve("MPC-PART--A"), "KICK.SND", kick.bytes());
                var target = Files.createTempDirectory("mpc3000-").resolve("wav");
                Sounds.convert(folder, Optional.of(target), _ -> {});
                assertWav(R1_4, target.resolve("MPC-PART--A/KICK.WAV"), kick);
                assertMissing(R1_4, folder.resolve("MPC-PART--A/KICK.WAV"));
            }),
            new Case(R1_5, () -> {
                var folder = Files.createTempDirectory("mpc3000-");
                var snd = SndFile.write(folder, "KICK.SND", kick.bytes());
                Files.writeString(folder.resolve("KICK.WAV"), "stale");
                Sounds.convert(snd, Optional.empty(), _ -> {});
                assertWav(R1_5, folder.resolve("KICK.WAV"), kick);
            }),
            new Case(R1_6, () -> {
                var message = rejection(IllegalArgumentException.class, () -> Sounds.convert(Path.of("missing.SND"), Optional.empty(), _ -> {}));
                assertContains(R1_6, message, "missing.SND");
            }),
            new Case(R1_7, () -> {
                var folder = Files.createTempDirectory("mpc3000-");
                var snd = SndFile.write(folder, "KICK.SND", kick.bytes());
                var file = SndFile.write(folder, "target.txt", new byte[1]);
                var message = rejection(IllegalArgumentException.class, () -> Sounds.convert(snd, Optional.of(file), _ -> {}));
                assertContains(R1_7, message, "not a folder");
            }),
            new Case(R1_8, () -> {
                var folder = Files.createTempDirectory("mpc3000-");
                var marker = kick.bytes();
                marker[1] = 0x04;
                SndFile.write(folder, "MARKER.SND", marker);
                SndFile.write(folder, "SHORT.SND", Arrays.copyOf(kick.bytes(), 100));
                SndFile.write(folder, "KICK.SND", kick.bytes());
                var output = new ArrayList<String>();
                rejection(IOException.class, () -> Sounds.convert(folder, Optional.empty(), output::add));
                var report = String.join("\n", output);
                assertContains(R1_8, report, "MARKER.SND");
                assertContains(R1_8, report, "not an MPC3000 sound");
                assertContains(R1_8, report, "SHORT.SND");
                assertContains(R1_8, report, "does not match");
                assertWav(R1_8, folder.resolve("KICK.WAV"), kick);
            }),
            new Case(R1_9, () -> {
                var folder = Files.createTempDirectory("mpc3000-");
                SndFile.write(folder, "STEREO.SND", new SndFile.Spec("STEREO", 0, 300, 300, true).bytes());
                SndFile.write(folder, "KICK.SND", kick.bytes());
                var output = new ArrayList<String>();
                rejection(IOException.class, () -> Sounds.convert(folder, Optional.empty(), output::add));
                assertContains(R1_9, String.join("\n", output), "STEREO.SND");
                assertContains(R1_9, String.join("\n", output), "not supported");
                assertMissing(R1_9, folder.resolve("STEREO.WAV"));
                assertWav(R1_9, folder.resolve("KICK.WAV"), kick);
            }),
            new Case(R1_10, () -> {
                var folder = Files.createTempDirectory("mpc3000-");
                SndFile.write(folder, "._KICK.SND", new byte[]{0, 5, 22});
                SndFile.write(folder, "KICK.SND", kick.bytes());
                Sounds.convert(folder, Optional.empty(), _ -> {});
                assertMissing(R1_10, folder.resolve("._KICK.WAV"));
                assertWav(R1_10, folder.resolve("KICK.WAV"), kick);
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

/// Reads the WAV back with the JDK's own audio reader
void assertWav(Requirement.Rn req, Path wav, SndFile.Spec spec) throws Exception {
    assert Files.exists(wav) : "%s — %s — missing %s".formatted(req, req.statement(), wav);
    try (var stream = AudioSystem.getAudioInputStream(wav.toFile())) {
        var format = stream.getFormat();
        var actual = "%s Hz, %d bit, %d channel(s), %d frames, %s".formatted(
                format.getSampleRate(), format.getSampleSizeInBits(), format.getChannels(), stream.getFrameLength(),
                format.isBigEndian() ? "big endian" : "little endian");
        var expected = "44100.0 Hz, 16 bit, 1 channel(s), %d frames, little endian".formatted(spec.end());
        assert actual.equals(expected) : "%s — %s — %s: expected %s but got %s".formatted(req, req.statement(), wav, expected, actual);
        assert Arrays.equals(stream.readAllBytes(), spec.samples()) : "%s — %s — %s: samples differ".formatted(req, req.statement(), wav);
    }
}

void assertMissing(Requirement.Rn req, Path file) {
    assert !Files.exists(file) : "%s — %s — unexpected %s".formatted(req, req.statement(), file);
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
