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
        R2_10("R2.10", "When an unpartitioned floppy image without volume label is extracted, the BC shall write its files into a folder named after the image without its extension, inside the target folder or, without a target folder, in the current directory."),
        /// When a folder of partition folders is restored, the BC shall write a hard disk image with the MPC3000's 23-partition layout (`MPC-PART--A` … `MPC-PART--W`, 64,260 sectors per partition, 779,091,968 bytes), putting each folder's files into the partition of the same label and leaving the other partitions empty.
        R3_1("R3.1", "When a folder of partition folders is restored, the BC shall write a hard disk image with the MPC3000's 23-partition layout (`MPC-PART--A` … `MPC-PART--W`, 64,260 sectors per partition, 779,091,968 bytes), putting each folder's files into the partition of the same label and leaving the other partitions empty."),
        /// When a folder of files is restored, the BC shall write a 1.44 MB floppy image holding them.
        R3_2("R3.2", "When a folder of files is restored, the BC shall write a 1.44 MB floppy image holding them."),
        /// When an image is restored, the BC shall write partition table, boot sectors, FATs and directory entries in the MPC3000's own layout: no boot code, no timestamps, 16-character names, the first file at cluster 4 on hard disk partitions and at cluster 2 on floppies.
        R3_3("R3.3", "When an image is restored, the BC shall write partition table, boot sectors, FATs and directory entries in the MPC3000's own layout: no boot code, no timestamps, 16-character names, the first file at cluster 4 on hard disk partitions and at cluster 2 on floppies."),
        /// When an image is restored, the BC shall store every file contiguously in name order with its content unchanged, so that extracting the image yields the same files.
        R3_4("R3.4", "When an image is restored, the BC shall store every file contiguously in name order with its content unchanged, so that extracting the image yields the same files."),
        /// When a file name contains lowercase letters, the BC shall store it in uppercase.
        R3_5("R3.5", "When a file name contains lowercase letters, the BC shall store it in uppercase."),
        /// When no image name is given, the BC shall write `<folder name>.iso` for a hard disk or `<folder name>.img` for a floppy next to the folder.
        R3_6("R3.6", "When no image name is given, the BC shall write `<folder name>.iso` for a hard disk or `<folder name>.img` for a floppy next to the folder."),
        /// If the image file already exists, then the BC shall reject the request without touching the file.
        R3_7("R3.7", "If the image file already exists, then the BC shall reject the request without touching the file."),
        /// If a folder holds files that are not MPC files (`SND`, `PGM`, `SEQ`, `ALL`, `APS`), then the BC shall leave them out of the image and list them.
        R3_8("R3.8", "If a folder holds files that are not MPC files (`SND`, `PGM`, `SEQ`, `ALL`, `APS`), then the BC shall leave them out of the image and list them."),
        /// The BC shall ignore hidden files and folders, whose names start with `.`, when restoring.
        R3_9("R3.9", "The BC shall ignore hidden files and folders, whose names start with `.`, when restoring."),
        /// If a file name is longer than 16 characters before its extension or holds characters other than printable ASCII, then the BC shall reject the request naming the file.
        R3_10("R3.10", "If a file name is longer than 16 characters before its extension or holds characters other than printable ASCII, then the BC shall reject the request naming the file."),
        /// If two files of a partition or floppy map to the same uppercase name, then the BC shall reject the request naming them.
        R3_11("R3.11", "If two files of a partition or floppy map to the same uppercase name, then the BC shall reject the request naming them."),
        /// If the files of a partition or floppy need more clusters or directory entries than it has, then the BC shall reject the request naming it with the space needed and available.
        R3_12("R3.12", "If the files of a partition or floppy need more clusters or directory entries than it has, then the BC shall reject the request naming it with the space needed and available."),
        /// If a subfolder is not named `MPC-PART--A` … `MPC-PART--W`, or MPC files lie next to partition folders, then the BC shall reject the request naming them.
        R3_13("R3.13", "If a subfolder is not named `MPC-PART--A` … `MPC-PART--W`, or MPC files lie next to partition folders, then the BC shall reject the request naming them."),
        /// If the folder does not exist or holds no MPC files, then the BC shall reject the request.
        R3_14("R3.14", "If the folder does not exist or holds no MPC files, then the BC shall reject the request."),
        /// When a hard disk image is formatted, the BC shall write the MPC3000's 23-partition layout with every partition empty, identical to an image restored from no files.
        R4_1("R4.1", "When a hard disk image is formatted, the BC shall write the MPC3000's 23-partition layout with every partition empty, identical to an image restored from no files."),
        /// When a floppy image is formatted, the BC shall write an empty 1.44 MB floppy in the MPC3000's floppy layout.
        R4_2("R4.2", "When a floppy image is formatted, the BC shall write an empty 1.44 MB floppy in the MPC3000's floppy layout."),
        /// If the image file already exists, then the BC shall reject the request without touching the file.
        R4_3("R4.3", "If the image file already exists, then the BC shall reject the request without touching the file.");

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
