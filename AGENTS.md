# AKAI MPC3000 Suite

A zero-dependency Java 25 CLI suite (`mpc3000`) for AKAI MPC3000 disk images: list, extract, convert sounds, and restore folders into new images. Built with the `/java-cli-app` skill (zb, zunit, BCE).

## Commands

- No dashed options: commands are `list`, `extract`, `convert`, `help`, `version`, arguments are positional; no arguments shows help
- `mpc3000 list <image>` — list partitions and files of an image
- `mpc3000 extract <image> [<folder>]` — one subfolder per partition containing sounds (`SND`), named after its label, holding all files of that partition; without a folder argument `./<image name without extension>` is used; a given folder is created or added to; existing files are replaced
- `mpc3000 convert <snd|folder> [<folder>]` — every `SND` below a folder (or one file) → 16-bit mono 44.1 kHz `WAV`, whole sample, start point as `cue`, loop as `smpl`; next to each SND or mirrored into folder; failed files are listed, the rest converted, exit code 1

## Roadmap

1. `list` — done
2. `extract` — done
3. `convert` — done (`sound` BC)
4. `restore` — write host folders (one per partition) back into a new MPC3000 image file; extends the `image` BC (decision D2), verified by a round trip extract → restore → extract
5. later: `../zaudioconvert` reuses the `sound` BC from this project's JAR instead of duplicating the SND parser

## Structure and specs

- Base package `christianfrei.mpc3000`; `App` parses argv and dispatches to BC boundaries
- System doc (decisions, stack): `src/main/java/christianfrei/mpc3000/package-info.java`
- Each BC's spec is its `package-info.java` (`/sbce` format): `## Boundary` ops, `## Requirements` as EARS statements with stable ids `Rn.m`, `## Entities`, `## Out of scope`
- `Requirement.java` per BC is generated from the spec (one `Rn` constant per statement) — regenerate, never hand-edit
- New behaviour starts as an EARS statement in the spec; never renumber ids, retire removed ones

## Build and test

- Build: `zb.sh` (installed in `~/bin`) → `zbo/app.jar`; `./mpc3000` is a launcher for it
- Test: `zunit` runs `test/*Test.java` against `zbo/app.jar`
- One test file per requirement group (`image`: `ListImageTest` ↔ R1, `ExtractImageTest` ↔ R2; `sound`: `ConvertSoundsTest` ↔ R1, `PlaybackPointsTest` ↔ R2), one `Case` row per statement id; spec ids and rows stay bijective
- WAV tests read files back with `javax.sound.sampled.AudioSystem` (import the class, not `module java.desktop` — `java.awt.List` clashes)
- `test/FatImage.java` builds synthetic FAT12 floppy and partitioned images, `test/SndFile.java` builds SND files and reads WAV chunks, so EARS tests need no real image
- `test/Hd0ImageTest.java` and `test/Hd0SoundTest.java` are regression tests against `test/HD0.iso` (git-ignored, skipped when absent); image checksums come from an independent FAT12 reader, all 165 sounds must convert

## Image format

- Hard disk images: MBR with an extended partition (type `0x05`) chaining logical FAT12 partitions (type `0x01`), each ~32 MB, labeled `MPC-PART--A`, `MPC-PART--B`, ...
- Floppy images: a single raw FAT12 volume without partition table
- FAT12 boot sector carries OEM name `MPC-3000`; root directory is flat, no dates; files can be fragmented (follow the FAT chain)
- MPC names have 16 characters: bytes 0–7 of the directory entry plus bytes 12–19 (`11042026` + `B` → `11042026B.PGM`); ignoring the tail produces duplicate names
- The first character may be a variant prefix and is not always a letter (`AOUND298`, `(OUND038`) — only replace characters reserved in host file names
- File types by extension: `SND` sound, `PGM` program, `SEQ` sequence, `ALL` all sequences and songs, `APS` all programs and sounds
- `SND`: 38-byte header + `end` 16-bit signed little-endian mono samples (`size = 38 + 2 × end`, verified on all 165 sounds); no sample rate field, always 44.1 kHz
  - bytes 0–1 `01 02`, 2–17 name, 19 level, 20 tune (signed), 21 stereo flag (always 0 seen), 22–25 start, 26–29 loop point, 30–33 end (u32 LE); start ≤ loop ≤ end
  - bytes 34–37 unknown (u16 + small u16, not a loop length); the loop on/off flag is not located yet

## Workflow

- Test image: `test/HD0.iso`: 23 partitions, A–F hold 203 files
- Never extract into the repo; use a temp folder
- Git: commit often using conventional commits (`feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `chore:`)
