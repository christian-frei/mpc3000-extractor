package christianfrei.mpc3000.sound;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Generated from the capability spec in [christianfrei.mpc3000.sound] — do not edit.
/// Marks the boundary method or test that realizes the given requirement statements.
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Requirement {

    /// One constant per statement id in the spec's `## Requirements`.
    enum Rn {
        /// When an SND file is converted, the BC shall write a 16-bit mono 44.1 kHz PCM WAV holding all of its samples unchanged, named after the SND file with the extension `WAV` in the same letter case.
        R1_1("R1.1", "When an SND file is converted, the BC shall write a 16-bit mono 44.1 kHz PCM WAV holding all of its samples unchanged, named after the SND file with the extension `WAV` in the same letter case."),
        /// When a folder is converted, the BC shall convert every file below it whose extension is `SND` in any letter case.
        R1_2("R1.2", "When a folder is converted, the BC shall convert every file below it whose extension is `SND` in any letter case."),
        /// When no target folder is given, the BC shall write each WAV next to its SND file.
        R1_3("R1.3", "When no target folder is given, the BC shall write each WAV next to its SND file."),
        /// When a target folder is given, the BC shall write the WAVs into it, mirroring the subfolders of the converted folder and creating missing folders.
        R1_4("R1.4", "When a target folder is given, the BC shall write the WAVs into it, mirroring the subfolders of the converted folder and creating missing folders."),
        /// While a WAV of the same name already exists, the BC shall replace it.
        R1_5("R1.5", "While a WAV of the same name already exists, the BC shall replace it."),
        /// If the input does not exist, then the BC shall reject the request naming it.
        R1_6("R1.6", "If the input does not exist, then the BC shall reject the request naming it."),
        /// If the target exists and is not a folder, then the BC shall reject the request.
        R1_7("R1.7", "If the target exists and is not a folder, then the BC shall reject the request."),
        /// If an SND file does not start with the MPC3000 marker or its length does not match its sample count, then the BC shall skip it, name it with the reason, convert the remaining files and report the conversion as failed.
        R1_8("R1.8", "If an SND file does not start with the MPC3000 marker or its length does not match its sample count, then the BC shall skip it, name it with the reason, convert the remaining files and report the conversion as failed."),
        /// If an SND file is flagged as stereo, then the BC shall skip it as unsupported, convert the remaining files and report the conversion as failed.
        R1_9("R1.9", "If an SND file is flagged as stereo, then the BC shall skip it as unsupported, convert the remaining files and report the conversion as failed."),
        /// The BC shall ignore hidden files, whose names start with `.`.
        R1_10("R1.10", "The BC shall ignore hidden files, whose names start with `.`."),
        /// When a sound is converted, the BC shall mark its start point as a cue point in the WAV.
        R2_1("R2.1", "When a sound is converted, the BC shall mark its start point as a cue point in the WAV."),
        /// While the loop point of a sound lies before its end, the BC shall write a forward sampler loop from the loop point to the last sample into the WAV.
        R2_2("R2.2", "While the loop point of a sound lies before its end, the BC shall write a forward sampler loop from the loop point to the last sample into the WAV."),
        /// While the loop point of a sound equals its end, the BC shall write no loop.
        R2_3("R2.3", "While the loop point of a sound equals its end, the BC shall write no loop.");

        private final String id;
        private final String statement;

        Rn(String id, String statement) {
            this.id = id;
            this.statement = statement;
        }

        public String statement() {
            return statement;
        }

        @Override
        public String toString() {
            return id;
        }
    }

    Rn[] value();
}
