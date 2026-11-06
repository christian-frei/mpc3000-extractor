# mpc3000

Copies MPC3000 floppies between a USB floppy drive and image files, lists and extracts the files on AKAI MPC3000 hard disk and floppy images, converts MPC3000 sounds to WAV, and writes host folders back into new images. A zero-dependency Java 25 CLI app.

## Prerequisites

Java 25+, [zb](https://github.com/AdamBien/zb), [zunit](https://github.com/AdamBien/zunit) for tests

## Build and Install

```
zb.sh
sudo ln -s $(pwd)/mpc3000 /usr/local/bin/mpc3000
```

`mpc3000` is a launcher for `zbo/app.jar`.

## Usage

```
mpc3000 floppy-read               # floppy in the USB drive → ./FLOPPY01.img (next free number)
mpc3000 floppy-write kit.img      # kit.img → floppy in the USB drive, verified, then ejected
mpc3000 list HD0.iso              # partitions, files, sizes, MPC file types
mpc3000 extract HD0.iso           # into ./HD0/<partition label>/
mpc3000 extract HD0.iso samples   # into ./samples/<partition label>/, created or added to
mpc3000 convert samples           # every SND below samples/ → WAV next to it
mpc3000 convert samples wav       # mirrored into ./wav/
mpc3000 restore HD0               # ./HD0/MPC-PART--A … → ./HD0.iso (23 partitions)
mpc3000 restore kit kit.img       # a folder of files → 1.44 MB floppy kit.img
mpc3000 format HD1.iso            # empty 23-partition hard disk, e.g. for a ZuluSCSI SD card
mpc3000 format blank.img floppy   # empty 1.44 MB floppy
```

`floppy-read` finds the floppy drive on its own and runs the copy with `sudo`, so start it in a terminal (iTerm, Terminal) — that terminal app needs access to removable volumes, which macOS asks for on first use; it never overwrites an image and fills unreadable sectors with zeros. `floppy-write` asks before it overwrites the disk, unmounts it, writes the image, reads it back to verify every sector and ejects the disk so macOS cannot write onto it again; the disk must be formatted for 1.44 MB already (by the MPC3000 or a computer), and the image must be a floppy image of that size. `extract` writes one subfolder per partition that contains sounds (`SND`) and replaces existing files; an unlabeled floppy gets a folder named after the image. `convert` writes 16-bit mono 44.1 kHz WAVs holding the whole sample, with the MPC start point as a `cue` marker and the loop as a `smpl` loop; files it cannot read are listed and the exit code is 1. `restore` turns a folder of partition folders (`MPC-PART--A` … `MPC-PART--W`) into a hard disk image laid out like one the MPC3000 formatted, and a folder of files into a floppy image; it never overwrites an image, uppercases names, leaves out and lists files that are not MPC files, and ignores hidden files like `.DS_Store`. Run `mpc3000` without arguments or with `help` for usage, `version` for the version.

## Test

```
zb.sh && zunit
```

Requirements are EARS statements in the [image](src/main/java/christianfrei/mpc3000/image/package-info.java), [sound](src/main/java/christianfrei/mpc3000/sound/package-info.java) and [floppy](src/main/java/christianfrei/mpc3000/floppy/package-info.java) specs; each statement id is one row in the tests.

## Image Format

- Hard disk images use an MBR whose extended partition chains logical FAT12 partitions of about 32 MB each, labeled `MPC-PART--A` to `MPC-PART--W`. Floppy images are a single FAT12 volume with no partition table.
- File names have 16 characters. FAT's 8.3 fields hold the first 8; directory entry bytes 12–19 hold the rest (`11042026` + `B` → `11042026B.PGM`).
- `SND` files are a 38-byte header (name, level, tune, start, loop and end points) followed by 16-bit little-endian mono samples at 44.1 kHz.
- File types: `SND` sound, `PGM` program, `SEQ` sequence, `ALL` all sequences and songs, `APS` all programs and sounds.
