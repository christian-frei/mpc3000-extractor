# AKAI MPC3000 Image Reader

A zero-dependency, single-file Java 25 script (`mpc3000`) that reads AKAI MPC3000 disk images, built with the `/java-cli-script` skill.

## Commands

- Commands and flags `help`, `version` take no dash; no arguments shows help
- `mpc3000 list <image>` — list partitions and files of an image
- `mpc3000 extract <image> [-f <folder>]` — extract all files, one subfolder per partition containing sounds (`SND`), named after its label, holding all files of that partition; without `-f` a folder named after the image (without extension) is created in the current directory; with `-f` the given folder is created or added to; existing files are replaced

## Roadmap

1. `list` — done
2. `extract` — done
3. later: MPC3000 `.SND` → `.wav` conversion in the sibling project `../zaudioconvert`

## Image format

- Hard disk images: MBR with an extended partition (type `0x05`) chaining logical FAT12 partitions (type `0x01`), each ~32 MB, labeled `MPC-PART--A`, `MPC-PART--B`, ...
- Floppy images: a single raw FAT12 volume without partition table
- FAT12 boot sector carries OEM name `MPC-3000`; root directory is flat, no dates; files can be fragmented (follow the FAT chain)
- MPC names have 16 characters: bytes 0–7 of the directory entry plus bytes 12–19 (`11042026` + `B` → `11042026B.PGM`); ignoring the tail produces duplicate names
- The first character may be a variant prefix and is not always a letter (`AOUND298`, `(OUND038`) — only replace characters reserved in host file names
- File types by extension: `SND` sound, `PGM` program, `SEQ` sequence, `ALL` all sequences and songs, `APS` all programs and sounds

## Workflow

- Test image: `test/HD0.iso` (not committed — `test/` is git-ignored): 23 partitions, A–F hold 203 files
- Extract into the scratchpad, never into the repo
- Git: commit often using conventional commits (`feat:`, `fix:`, `docs:`, `chore:`, `refactor:`)
