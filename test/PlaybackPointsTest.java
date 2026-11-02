import static christianfrei.mpc3000.sound.Requirement.Rn.*;

import christianfrei.mpc3000.sound.Requirement;
import christianfrei.mpc3000.sound.boundary.Sounds;

interface Check {
    void run() throws Exception;
}

record Case(Requirement.Rn req, Check check) {}

void main() throws Exception {
    var looped = SndFile.Spec.of("LOOPED", 25, 120, 400);
    var oneShot = SndFile.Spec.of("ONESHOT", 25, 400, 400);

    var cases = List.of(
            new Case(R2_1, () -> {
                var cue = convert(looped).get("cue ");
                assert cue != null : R2_1 + " — " + R2_1.statement() + " — no cue chunk";
                var points = cue.getInt(0);
                var sampleOffset = cue.getInt(4 + 20);
                assert points == 1 && sampleOffset == looped.start()
                        : "%s — %s — expected 1 cue at %d but got %d at %d".formatted(R2_1, R2_1.statement(), looped.start(), points, sampleOffset);
            }),
            new Case(R2_2, () -> {
                var smpl = convert(looped).get("smpl");
                assert smpl != null : R2_2 + " — " + R2_2.statement() + " — no smpl chunk";
                var loops = smpl.getInt(28);
                var type = smpl.getInt(36 + 4);
                var start = smpl.getInt(36 + 8);
                var end = smpl.getInt(36 + 12);
                var expected = "1 loop(s), type 0, %d..%d".formatted(looped.loop(), looped.end() - 1);
                var actual = "%d loop(s), type %d, %d..%d".formatted(loops, type, start, end);
                assert actual.equals(expected) : "%s — %s — expected %s but got %s".formatted(R2_2, R2_2.statement(), expected, actual);
            }),
            new Case(R2_3, () -> {
                var chunks = convert(oneShot);
                assert !chunks.containsKey("smpl") : R2_3 + " — " + R2_3.statement() + " — unexpected smpl chunk";
            }));

    for (var c : cases) {
        try {
            c.check().run();
        } catch (Exception e) {
            throw new AssertionError("%s — %s".formatted(c.req(), c.req().statement()), e);
        }
    }
}

Map<String, ByteBuffer> convert(SndFile.Spec spec) throws Exception {
    var folder = Files.createTempDirectory("mpc3000-");
    var snd = SndFile.write(folder, spec.name() + ".SND", spec.bytes());
    Sounds.convert(snd, Optional.empty(), _ -> {});
    return SndFile.chunks(folder.resolve(spec.name() + ".WAV"));
}
