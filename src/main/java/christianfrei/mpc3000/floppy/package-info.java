/// # Floppy
/// > Copy MPC3000 floppies between a USB floppy drive and image files.
///
/// ## Boundary
/// - `read-floppy` — copy the disk in the floppy drive, sector by sector, into an image file
/// - `write-floppy` — copy an image file, sector by sector, onto the disk in the floppy drive and verify it
///
/// ## Requirements
/// ### R1: Read a floppy
/// - R1.1 — When a floppy is read, the BC shall copy every sector of the disk into the image file without writing to the disk, and report the number of bytes copied.
/// - R1.2 — When no image name is given, the BC shall write the lowest-numbered free `FLOPPYnn.img` (`FLOPPY01.img`, `FLOPPY02.img`, …) in the current directory.
/// - R1.3 — When an image name is given, the BC shall write the image to that file.
/// - R1.4 — When exactly one external disk of floppy size is attached, the BC shall read from that disk. _(why: 1.44 MB high-density and 720 KB double-density floppies; the drive needs no configuration)_
/// - R1.5 — If no external disk of floppy size is attached, then the BC shall reject the request.
/// - R1.6 — If several external disks of floppy size are attached, then the BC shall reject the request naming them.
/// - R1.7 — If the image file already exists, then the BC shall reject the request without touching the file.
/// - R1.8 — If sectors of the disk cannot be read, then the BC shall fill them with zeros, copy the remaining sectors, keep the image and report the read as failed with the number of unreadable sectors. _(why: old floppies develop bad sectors; a partial image still saves the readable files)_
/// - R1.9 — If the administrator password prompt is cancelled or fails, then the BC shall reject the request and leave no image file behind.
/// - R1.10 — When the image is written, the BC shall make the current user its owner. _(why: the copy runs with administrator rights, which would leave a root-owned file)_
/// - R1.11 — If the disk cannot be copied at all, then the BC shall report the reason given by the copy tool and leave no image file behind. _(why: a failed first run surfaced only a follow-up error, hiding the cause)_
/// - R1.12 — If no terminal is available to ask for the administrator password, then the BC shall reject the request before copying and ask for it to be run in a terminal. _(why: sudo can only ask for the password in a terminal)_
///
/// ### R2: Write a floppy
/// - R2.1 — When an image is written to a floppy, the BC shall unmount the disk, copy every sector of the image onto it, read the disk back and report the write as verified when every sector matches the image.
/// - R2.2 — When the disk has been written, the BC shall eject it. _(why: macOS writes `.fseventsd` onto every MPC floppy it mounts)_
/// - R2.3 — Before writing, the BC shall ask for confirmation naming the disk and the image, and shall leave the disk untouched unless the answer is `y`. _(why: writing destroys everything on the disk, and the drive is picked automatically)_
/// - R2.4 — When exactly one external disk of floppy size is attached, the BC shall write to that disk, and reject the request otherwise as reading does.
/// - R2.5 — If the image file does not exist, then the BC shall reject the request naming the file.
/// - R2.6 — If the image's size differs from the disk's or the image does not start with a FAT boot sector, then the BC shall reject the request before writing. _(why: a 720 KB disk cannot hold a 1.44 MB image, and only floppy images belong on a floppy)_
/// - R2.7 — If the disk is write-protected, then the BC shall reject the request before writing and say so.
/// - R2.8 — If sectors read back from the disk differ from the image, then the BC shall report the write as failed with the number of differing sectors. _(why: old disks have bad sectors the MPC3000 would only find while loading)_
/// - R2.9 — If the disk cannot be unmounted or written, then the BC shall report the reason given by the tool.
/// - R2.10 — If the administrator password prompt is cancelled or fails, then the BC shall reject the request with the disk untouched.
/// - R2.11 — If no terminal is available to ask for confirmation and the administrator password, then the BC shall reject the request before writing.
///
/// ## Entities
/// - Disk, CopyReport, FloppyImage
///
/// ## Decisions
/// - D1 — Administrator rights come from the macOS password dialog; the raw device is read while the disk stays mounted. _(why: works from any prompt, no sudo terminal needed; unmounting is often blocked by macOS scanning the disk; rejected: requiring `sudo mpc3000`, unmounting first)_ — superseded by D2
/// - D2 — Administrator rights come from `sudo` in the user's terminal; the raw device is read while the disk stays mounted. _(why: macOS grants removable-volume access to the terminal app, but the password dialog runs its command through a helper outside it, so dd failed with "Operation not permitted"; rejected: the password dialog (D1), granting Full Disk Access to a system helper)_
/// - D3 — Writing unmounts the disk first, verifies by reading it back and ejects it afterwards, all in one `sudo` shell. _(why: a mounted disk's FAT driver would write its cached FAT, directory and `.fseventsd` over the new sectors, and a remount after writing would do the same; rejected: writing to the mounted disk, verifying from the image only)_
///
/// ## Out of scope
/// - Formatting disks: the disk must already be formatted for its size, by the MPC3000 or a computer
/// - Floppy drives on Linux and Windows
package christianfrei.mpc3000.floppy;
