/// # Floppy
/// > Copy an MPC3000 floppy from a USB floppy drive into an image file.
///
/// ## Boundary
/// - `read-floppy` — copy the disk in the floppy drive, sector by sector, into an image file
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
/// - R1.9 — If the administrator password dialog is cancelled, then the BC shall reject the request and leave no image file behind.
/// - R1.10 — When the image is written, the BC shall make the current user its owner. _(why: the copy runs with administrator rights, which would leave a root-owned file)_
/// - R1.11 — If the disk cannot be copied at all, then the BC shall report the reason given by the copy tool and leave no image file behind. _(why: a failed first run surfaced only a follow-up error, hiding the cause)_
///
/// ## Entities
/// - Disk, CopyReport
///
/// ## Decisions
/// - D1 — Administrator rights come from the macOS password dialog; the raw device is read while the disk stays mounted. _(why: works from any prompt, no sudo terminal needed; unmounting is often blocked by macOS scanning the disk; rejected: requiring `sudo mpc3000`, unmounting first)_
///
/// ## Out of scope
/// - Writing images back to floppies
/// - Floppy drives on Linux and Windows
package christianfrei.mpc3000.floppy;
