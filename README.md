# mpc3000

Lists and extracts the files on AKAI MPC3000 hard disk and floppy images. It's a zero-dependency, single-file Java 25 script.

## Prerequisites

Java 25+

## Install

```
chmod +x mpc3000
sudo ln -s $(pwd)/mpc3000 /usr/local/bin/mpc3000
```

## Usage

```
mpc3000 list HD0.iso                 # partitions, files, sizes, MPC file types
mpc3000 extract HD0.iso              # into ./HD0/<partition label>/
mpc3000 extract HD0.iso -f samples   # into ./samples/<partition label>/, created or added to
```

`extract` writes one subfolder per non-empty partition and replaces existing files.

## Image Format

- Hard disk images use an MBR whose extended partition chains logical FAT12 partitions of about 32 MB each, labeled `MPC-PART--A` to `MPC-PART--W`. Floppy images are a single FAT12 volume with no partition table.
- File names have 16 characters. FAT's 8.3 fields hold the first 8; directory entry bytes 12–19 hold the rest (`11042026` + `B` → `11042026B.PGM`).
- File types: `SND` sound, `PGM` program, `SEQ` sequence, `ALL` all sequences and songs, `APS` all programs and sounds.
