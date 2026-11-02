# AKAI MPC3000 Image Reader

A zero-dependency, single-file Java 25 script (`mpc3000`) that reads AKAI MPC3000 disk images, built with the `/java-cli-script` skill.

## Commands

- `mpc3000 list <image>` — list partitions and files of an image
- `mpc3000 extract <image> [-f <folder>]` — extract all files; without `-f` a folder named after the image (without extension) is created in the current directory; with `-f` the given folder is created or added to

## Roadmap

1. `list` — done
2. `extract`
3. later: MPC3000 `.SND` → `.wav` conversion in the sibling project `../zaudioconvert`

## Image format

- Hard disk images: MBR with an extended partition (type `0x05`) chaining logical FAT12 partitions (type `0x01`), each ~32 MB, labeled `MPC-PART--A`, `MPC-PART--B`, ...
- Floppy images: a single raw FAT12 volume without partition table
- FAT12 boot sector carries OEM name `MPC-3000`; root directory is flat with plain 8.3 names (no long names, no dates)
- File types by extension: `SND` sound, `PGM` program, `SEQ` sequence, `ALL` all sequences and songs, `APS` all programs and sounds

## Workflow

- Test image: `test/HD0.iso` (not committed — `test/` is git-ignored)
- Git: commit often using conventional commits (`feat:`, `fix:`, `docs:`, `chore:`, `refactor:`)
