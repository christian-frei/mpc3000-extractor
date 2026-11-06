package christianfrei.mpc3000.image;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Generated from the capability spec in [christianfrei.mpc3000.image] — do not edit.
/// Marks the boundary method or test that realizes the given requirement statements.
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Requirement {

    /// One constant per statement id in the spec's `## Requirements`.
    enum Rn {
        /// When a partitioned hard disk image is listed, the BC shall report every FAT partition in partition-table order with its label, capacity, file count and used space.
        R1_1("R1.1", "When a partitioned hard disk image is listed, the BC shall report every FAT partition in partition-table order with its label, capacity, file count and used space."),
        /// When an unpartitioned floppy image is listed, the BC shall report its FAT volume as a single partition.
        R1_2("R1.2", "When an unpartitioned floppy image is listed, the BC shall report its FAT volume as a single partition."),
        /// When an image is listed, the BC shall report every file with its full 16-character MPC name, its size and its MPC file type.
        R1_3("R1.3", "When an image is listed, the BC shall report every file with its full 16-character MPC name, its size and its MPC file type."),
        /// If the image file does not exist, then the BC shall reject the request naming the file.
        R1_4("R1.4", "If the image file does not exist, then the BC shall reject the request naming the file."),
        /// If the file holds neither a partition table with FAT partitions nor a FAT volume, then the BC shall reject the request as not an MPC3000 image.
        R1_5("R1.5", "If the file holds neither a partition table with FAT partitions nor a FAT volume, then the BC shall reject the request as not an MPC3000 image."),
        /// When a partition without volume label is listed, the BC shall report it as `(no label)`.
        R1_6("R1.6", "When a partition without volume label is listed, the BC shall report it as `(no label)`."),
        /// When an image is extracted, the BC shall write each partition containing sounds into its own folder named after the partition label, holding all files of that partition byte-identical to the image.
        R2_1("R2.1", "When an image is extracted, the BC shall write each partition containing sounds into its own folder named after the partition label, holding all files of that partition byte-identical to the image."),
        /// When an image is extracted, the BC shall skip partitions that contain no sounds.
        R2_2("R2.2", "When an image is extracted, the BC shall skip partitions that contain no sounds."),
        /// When no target folder is given, the BC shall extract into a folder in the current directory named after the image without its extension.
        R2_3("R2.3", "When no target folder is given, the BC shall extract into a folder in the current directory named after the image without its extension."),
        /// While the target folder already exists, the BC shall add to it and replace files of the same name.
        R2_4("R2.4", "While the target folder already exists, the BC shall add to it and replace files of the same name."),
        /// When a file is stored in non-contiguous clusters, the BC shall reassemble it by following its cluster chain.
        R2_5("R2.5", "When a file is stored in non-contiguous clusters, the BC shall reassemble it by following its cluster chain."),
        /// If two files of a partition map to the same host file name, then the BC shall keep both by suffixing the later one with `~2`, `~3`, ….
        R2_6("R2.6", "If two files of a partition map to the same host file name, then the BC shall keep both by suffixing the later one with `~2`, `~3`, …."),
        /// If a file name contains characters reserved on host file systems, then the BC shall replace each of them with `_`.
        R2_7("R2.7", "If a file name contains characters reserved on host file systems, then the BC shall replace each of them with `_`."),
        /// If the target exists and is not a folder, then the BC shall reject the request.
        R2_8("R2.8", "If the target exists and is not a folder, then the BC shall reject the request."),
        /// If a file's cluster chain ends before its size is reached, then the BC shall reject the extraction naming the file.
        R2_9("R2.9", "If a file's cluster chain ends before its size is reached, then the BC shall reject the extraction naming the file."),
        /// When an unpartitioned floppy image without volume label is extracted, the BC shall write its files into a folder named after the image without its extension, inside the target folder or, without a target folder, in the current directory.
        R2_10("R2.10", "When an unpartitioned floppy image without volume label is extracted, the BC shall write its files into a folder named after the image without its extension, inside the target folder or, without a target folder, in the current directory.");

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
