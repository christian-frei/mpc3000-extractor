package christianfrei.mpc3000.floppy;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Generated from the capability spec in [christianfrei.mpc3000.floppy] — do not edit.
/// Marks the boundary method or test that realizes the given requirement statements.
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Requirement {

    /// One constant per statement id in the spec's `## Requirements`.
    enum Rn {
        /// When a floppy is read, the BC shall copy every sector of the disk into the image file without writing to the disk, and report the number of bytes copied.
        R1_1("R1.1", "When a floppy is read, the BC shall copy every sector of the disk into the image file without writing to the disk, and report the number of bytes copied."),
        /// When no image name is given, the BC shall write the lowest-numbered free `FLOPPYnn.img` (`FLOPPY01.img`, `FLOPPY02.img`, …) in the current directory.
        R1_2("R1.2", "When no image name is given, the BC shall write the lowest-numbered free `FLOPPYnn.img` (`FLOPPY01.img`, `FLOPPY02.img`, …) in the current directory."),
        /// When an image name is given, the BC shall write the image to that file.
        R1_3("R1.3", "When an image name is given, the BC shall write the image to that file."),
        /// When exactly one external disk of floppy size is attached, the BC shall read from that disk.
        R1_4("R1.4", "When exactly one external disk of floppy size is attached, the BC shall read from that disk."),
        /// If no external disk of floppy size is attached, then the BC shall reject the request.
        R1_5("R1.5", "If no external disk of floppy size is attached, then the BC shall reject the request."),
        /// If several external disks of floppy size are attached, then the BC shall reject the request naming them.
        R1_6("R1.6", "If several external disks of floppy size are attached, then the BC shall reject the request naming them."),
        /// If the image file already exists, then the BC shall reject the request without touching the file.
        R1_7("R1.7", "If the image file already exists, then the BC shall reject the request without touching the file."),
        /// If sectors of the disk cannot be read, then the BC shall fill them with zeros, copy the remaining sectors, keep the image and report the read as failed with the number of unreadable sectors.
        R1_8("R1.8", "If sectors of the disk cannot be read, then the BC shall fill them with zeros, copy the remaining sectors, keep the image and report the read as failed with the number of unreadable sectors."),
        /// If the administrator password dialog is cancelled, then the BC shall reject the request and leave no image file behind.
        R1_9("R1.9", "If the administrator password dialog is cancelled, then the BC shall reject the request and leave no image file behind."),
        /// When the image is written, the BC shall make the current user its owner.
        R1_10("R1.10", "When the image is written, the BC shall make the current user its owner.");

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
