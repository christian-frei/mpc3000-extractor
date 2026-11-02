/// # Sound
/// > Convert AKAI MPC3000 sounds (`SND`) to WAV files.
///
/// ## Boundary
/// - `convert-sounds` — convert one SND file, or every SND file below a folder, into WAV files
///
/// ## Requirements
/// ### R1: Convert sounds
/// - R1.1 — When an SND file is converted, the BC shall write a 16-bit mono 44.1 kHz PCM WAV holding all of its samples unchanged, named after the SND file with the extension `WAV` in the same letter case. _(why: the MPC3000 records 16-bit mono at a fixed 44.1 kHz; the SND header carries no sample rate)_
/// - R1.2 — When a folder is converted, the BC shall convert every file below it whose extension is `SND` in any letter case.
/// - R1.3 — When no target folder is given, the BC shall write each WAV next to its SND file.
/// - R1.4 — When a target folder is given, the BC shall write the WAVs into it, mirroring the subfolders of the converted folder and creating missing folders.
/// - R1.5 — While a WAV of the same name already exists, the BC shall replace it.
/// - R1.6 — If the input does not exist, then the BC shall reject the request naming it.
/// - R1.7 — If the target exists and is not a folder, then the BC shall reject the request.
/// - R1.8 — If an SND file does not start with the MPC3000 marker or its length does not match its sample count, then the BC shall skip it, name it with the reason, convert the remaining files and report the conversion as failed.
/// - R1.9 — If an SND file is flagged as stereo, then the BC shall skip it as unsupported, convert the remaining files and report the conversion as failed. _(why: no stereo MPC3000 sound has been seen; its sample layout is unverified)_
/// - R1.10 — The BC shall ignore hidden files, whose names start with `.`. _(why: macOS writes `._` resource files next to copies on FAT media)_
///
/// ### R2: Preserve playback points
/// - R2.1 — When a sound is converted, the BC shall mark its start point as a cue point in the WAV.
/// - R2.2 — While the loop point of a sound lies before its end, the BC shall write a forward sampler loop from the loop point to the last sample into the WAV. _(why: the loop on/off flag is not located in the header yet, so every loop that has a length is kept)_
/// - R2.3 — While the loop point of a sound equals its end, the BC shall write no loop.
///
/// ## Entities
/// - Sound, ConvertedSound
///
/// ## Out of scope
/// - Reading sounds directly from disk images (use `extract` first)
/// - Converting WAV back to SND
/// - Level, tune and the header bytes 34–37
package christianfrei.mpc3000.sound;
