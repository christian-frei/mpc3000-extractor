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
        /// If the administrator password prompt is cancelled or fails, then the BC shall reject the request and leave no image file behind.
        R1_9("R1.9", "If the administrator password prompt is cancelled or fails, then the BC shall reject the request and leave no image file behind."),
        /// When the image is written, the BC shall make the current user its owner.
        R1_10("R1.10", "When the image is written, the BC shall make the current user its owner."),
        /// If the disk cannot be copied at all, then the BC shall report the reason given by the copy tool and leave no image file behind.
        R1_11("R1.11", "If the disk cannot be copied at all, then the BC shall report the reason given by the copy tool and leave no image file behind."),
        /// If no terminal is available to ask for the administrator password, then the BC shall reject the request before copying and ask for it to be run in a terminal.
        R1_12("R1.12", "If no terminal is available to ask for the administrator password, then the BC shall reject the request before copying and ask for it to be run in a terminal."),
        /// When an image is written to a floppy, the BC shall unmount the disk, copy every sector of the image onto it, read the disk back and report the write as verified when every sector matches the image.
        R2_1("R2.1", "When an image is written to a floppy, the BC shall unmount the disk, copy every sector of the image onto it, read the disk back and report the write as verified when every sector matches the image."),
        /// When the disk has been written, the BC shall eject it.
        R2_2("R2.2", "When the disk has been written, the BC shall eject it."),
        /// Before writing, the BC shall ask for confirmation naming the disk and the image, and shall leave the disk untouched unless the answer is `y`.
        R2_3("R2.3", "Before writing, the BC shall ask for confirmation naming the disk and the image, and shall leave the disk untouched unless the answer is `y`."),
        /// When exactly one external disk of floppy size is attached, the BC shall write to that disk, and reject the request otherwise as reading does.
        R2_4("R2.4", "When exactly one external disk of floppy size is attached, the BC shall write to that disk, and reject the request otherwise as reading does."),
        /// If the image file does not exist, then the BC shall reject the request naming the file.
        R2_5("R2.5", "If the image file does not exist, then the BC shall reject the request naming the file."),
        /// If the image's size differs from the disk's or the image does not start with a FAT boot sector, then the BC shall reject the request before writing.
        R2_6("R2.6", "If the image's size differs from the disk's or the image does not start with a FAT boot sector, then the BC shall reject the request before writing."),
        /// If the disk is write-protected, then the BC shall reject the request before writing and say so.
        R2_7("R2.7", "If the disk is write-protected, then the BC shall reject the request before writing and say so."),
        /// If sectors read back from the disk differ from the image, then the BC shall report the write as failed with the number of differing sectors.
        R2_8("R2.8", "If sectors read back from the disk differ from the image, then the BC shall report the write as failed with the number of differing sectors."),
        /// If the disk cannot be unmounted or written, then the BC shall report the reason given by the tool.
        R2_9("R2.9", "If the disk cannot be unmounted or written, then the BC shall report the reason given by the tool."),
        /// If the administrator password prompt is cancelled or fails, then the BC shall reject the request with the disk untouched.
        R2_10("R2.10", "If the administrator password prompt is cancelled or fails, then the BC shall reject the request with the disk untouched."),
        /// If no terminal is available to ask for confirmation and the administrator password, then the BC shall reject the request before writing.
        R2_11("R2.11", "If no terminal is available to ask for confirmation and the administrator password, then the BC shall reject the request before writing.");

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
