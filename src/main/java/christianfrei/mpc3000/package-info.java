/// # mpc3000
/// > Read, extract, convert and restore the contents of AKAI MPC3000 disk images on a modern computer.
///
/// ## Components
/// - `image`, `sound` and `floppy` are independent; none calls another. `floppy-read` writes images that `list`/`extract` read; `extract` writes SND files that `convert` reads.
///
/// ## Decisions
/// - D1 — Multi-file `java-cli-app` built with zb, tested with zunit. _(why: sound conversion and image restore outgrow one file and need round-trip tests; rejected: single-file `java-cli-script`)_
/// - D2 — `image` owns the MPC3000 disk format in both directions; restore extends it instead of a separate BC. _(why: reading and writing share partition table, FAT12 and MPC naming rules; rejected: separate `restore` BC)_
/// - D3 — Commands and arguments are positional words, no dashed options. _(why: user preference; rejected: `-f`, `-help`, `-version`)_
/// - D4 — `convert` takes SND files or folders, not images; WAVs keep the whole sample with the start point as `cue` and the loop as `smpl`. _(why: keeps image reading and sound conversion separate, nothing is cut away; rejected: converting straight from images, trimming to start/end, plain PCM without markers)_
///
/// - D5 — Copying a physical floppy is its own `floppy` BC, not part of `image`. _(why: it talks to the operating system and the drive, not to the MPC3000 format; rejected: a `read-floppy` op in `image`)_
///
/// ## Stack
/// - java-cli-app · base package `christianfrei.mpc3000`
package christianfrei.mpc3000;
