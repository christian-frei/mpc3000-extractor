/// # mpc3000
/// > Read, extract, convert and restore the contents of AKAI MPC3000 disk images on a modern computer.
///
/// ## Decisions
/// - D1 — Multi-file `java-cli-app` built with zb, tested with zunit. _(why: sound conversion and image restore outgrow one file and need round-trip tests; rejected: single-file `java-cli-script`)_
/// - D2 — `image` owns the MPC3000 disk format in both directions; restore extends it instead of a separate BC. _(why: reading and writing share partition table, FAT12 and MPC naming rules; rejected: separate `restore` BC)_
/// - D3 — Commands and arguments are positional words, no dashed options. _(why: user preference; rejected: `-f`, `-help`, `-version`)_
///
/// ## Stack
/// - java-cli-app · base package `christianfrei.mpc3000`
package christianfrei.mpc3000;
