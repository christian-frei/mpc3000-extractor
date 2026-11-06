/// # Image
/// > Read the partitions and files of AKAI MPC3000 disk images, copy them to the host, and write host folders back into new images.
///
/// ## Boundary
/// - `list-image` — report the partitions of an image and the files on each
/// - `extract-image` — copy the files of every partition holding sounds into host folders
/// - `restore-image` — write a host folder into a new hard disk or floppy image
///
/// ## Requirements
/// ### R1: List an image
/// - R1.1 — When a partitioned hard disk image is listed, the BC shall report every FAT partition in partition-table order with its label, capacity, file count and used space.
/// - R1.2 — When an unpartitioned floppy image is listed, the BC shall report its FAT volume as a single partition.
/// - R1.3 — When an image is listed, the BC shall report every file with its full 16-character MPC name, its size and its MPC file type. _(why: the 8.3 part alone is ambiguous — `11042026B.PGM` and `11042026C.PGM` share it)_
/// - R1.4 — If the image file does not exist, then the BC shall reject the request naming the file.
/// - R1.5 — If the file holds neither a partition table with FAT partitions nor a FAT volume, then the BC shall reject the request as not an MPC3000 image.
/// - R1.6 — When a partition without volume label is listed, the BC shall report it as `(no label)`. _(why: MPC3000 floppies carry no label)_
///
/// ### R2: Extract an image
/// - R2.1 — When an image is extracted, the BC shall write each partition containing sounds into its own folder named after the partition label, holding all files of that partition byte-identical to the image.
/// - R2.2 — When an image is extracted, the BC shall skip partitions that contain no sounds.
/// - R2.3 — When no target folder is given, the BC shall extract into a folder in the current directory named after the image without its extension.
/// - R2.4 — While the target folder already exists, the BC shall add to it and replace files of the same name.
/// - R2.5 — When a file is stored in non-contiguous clusters, the BC shall reassemble it by following its cluster chain.
/// - R2.6 — If two files of a partition map to the same host file name, then the BC shall keep both by suffixing the later one with `~2`, `~3`, ….
/// - R2.7 — If a file name contains characters reserved on host file systems, then the BC shall replace each of them with `_`. _(why: the first character of an MPC name may be any symbol, e.g. `(OUND038`)_
/// - R2.8 — If the target exists and is not a folder, then the BC shall reject the request.
/// - R2.9 — If a file's cluster chain ends before its size is reached, then the BC shall reject the extraction naming the file.
/// - R2.10 — When an unpartitioned floppy image without volume label is extracted, the BC shall write its files into a folder named after the image without its extension, inside the target folder or, without a target folder, in the current directory. _(why: several floppies extracted into one target must not overwrite each other)_
///
/// ### R3: Restore an image
/// - R3.1 — When a folder of partition folders is restored, the BC shall write a hard disk image with the MPC3000's 23-partition layout (`MPC-PART--A` … `MPC-PART--W`, 64,260 sectors per partition, 779,091,968 bytes), putting each folder's files into the partition of the same label and leaving the other partitions empty. _(why: the layout of a disk the MPC3000 formatted and used)_
/// - R3.2 — When a folder of files is restored, the BC shall write a 1.44 MB floppy image holding them.
/// - R3.3 — When an image is restored, the BC shall write partition table, boot sectors, FATs and directory entries in the MPC3000's own layout: no boot code, no timestamps, 16-character names, the first file at cluster 4 on hard disk partitions and at cluster 2 on floppies. _(why: byte-identical to what the MPC3000 writes, so the MPC3000 accepts the image)_
/// - R3.4 — When an image is restored, the BC shall store every file contiguously in name order with its content unchanged, so that extracting the image yields the same files.
/// - R3.5 — When a file name contains lowercase letters, the BC shall store it in uppercase. _(why: the MPC3000 writes uppercase names only)_
/// - R3.6 — When no image name is given, the BC shall write `<folder name>.iso` for a hard disk or `<folder name>.img` for a floppy next to the folder.
/// - R3.7 — If the image file already exists, then the BC shall reject the request without touching the file.
/// - R3.8 — If a folder holds files that are not MPC files (`SND`, `PGM`, `SEQ`, `ALL`, `APS`), then the BC shall leave them out of the image and list them.
/// - R3.9 — The BC shall ignore hidden files and folders, whose names start with `.`, when restoring. _(why: macOS adds `.DS_Store` and `._` files to folders)_
/// - R3.10 — If a file name is longer than 16 characters before its extension or holds characters other than printable ASCII, then the BC shall reject the request naming the file.
/// - R3.11 — If two files of a partition or floppy map to the same uppercase name, then the BC shall reject the request naming them.
/// - R3.12 — If the files of a partition or floppy need more clusters or directory entries than it has, then the BC shall reject the request naming it with the space needed and available.
/// - R3.13 — If a subfolder is not named `MPC-PART--A` … `MPC-PART--W`, or MPC files lie next to partition folders, then the BC shall reject the request naming them.
/// - R3.14 — If the folder does not exist or holds no MPC files, then the BC shall reject the request.
///
/// ## Entities
/// - Volume, PartitionEntry, BootSector, DirectoryEntry, Fat, FileType, ExtractedPartition, VolumeFormat, DiskLayout, RestoredFile, RestoredVolume, RestoredImage
///
/// ## Out of scope
/// - Converting sounds to other audio formats
/// - 720 KB double-density floppy images and hard disk layouts other than the 23-partition one
/// - Writing images onto physical floppies or SCSI devices
/// - Subdirectories and recovery of deleted files
package christianfrei.mpc3000.image;
