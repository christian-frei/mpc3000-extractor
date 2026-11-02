# mpc3000

Lists and extracts the files on AKAI MPC3000 hard disk and floppy images. A zero-dependency Java 25 CLI app; sound conversion to WAV and restoring folders into new images are planned.

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
mpc3000 list HD0.iso              # partitions, files, sizes, MPC file types
mpc3000 extract HD0.iso           # into ./HD0/<partition label>/
mpc3000 extract HD0.iso samples   # into ./samples/<partition label>/, created or added to
```

`extract` writes one subfolder per partition that contains sounds (`SND`) and replaces existing files. Run `mpc3000` without arguments or with `help` for usage, `version` for the version.

## Test

```
zb.sh && zunit
```

Requirements are EARS statements in [the image spec](src/main/java/christianfrei/mpc3000/image/package-info.java); each statement id is one row in the tests.

## Image Format

- Hard disk images use an MBR whose extended partition chains logical FAT12 partitions of about 32 MB each, labeled `MPC-PART--A` to `MPC-PART--W`. Floppy images are a single FAT12 volume with no partition table.
- File names have 16 characters. FAT's 8.3 fields hold the first 8; directory entry bytes 12–19 hold the rest (`11042026` + `B` → `11042026B.PGM`).
- File types: `SND` sound, `PGM` program, `SEQ` sequence, `ALL` all sequences and songs, `APS` all programs and sounds.
