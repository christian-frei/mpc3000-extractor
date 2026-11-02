/// # Image
/// > Read the partitions and files of AKAI MPC3000 disk images and copy them to the host.
///
/// ## Boundary
/// - `list-image` — report the partitions of an image and the files on each
/// - `extract-image` — copy the files of every partition holding sounds into host folders
///
/// ## Requirements
/// ### R1: List an image
/// - R1.1 — When a partitioned hard disk image is listed, the BC shall report every FAT partition in partition-table order with its label, capacity, file count and used space.
/// - R1.2 — When an unpartitioned floppy image is listed, the BC shall report its FAT volume as a single partition.
/// - R1.3 — When an image is listed, the BC shall report every file with its full 16-character MPC name, its size and its MPC file type. _(why: the 8.3 part alone is ambiguous — `11042026B.PGM` and `11042026C.PGM` share it)_
/// - R1.4 — If the image file does not exist, then the BC shall reject the request naming the file.
/// - R1.5 — If the file holds neither a partition table with FAT partitions nor a FAT volume, then the BC shall reject the request as not an MPC3000 image.
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
///
/// ## Entities
/// - Volume, PartitionEntry, BootSector, DirectoryEntry, Fat, FileType, ExtractedPartition
///
/// ## Out of scope
/// - Converting sounds to other audio formats
/// - Writing images from host folders (restore)
/// - Subdirectories and recovery of deleted files
package christianfrei.mpc3000.image;
