package christianfrei.mpc3000;

import christianfrei.mpc3000.floppy.boundary.Floppies;
import christianfrei.mpc3000.image.boundary.Images;
import christianfrei.mpc3000.sound.boundary.Sounds;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/// Parses the command line and dispatches to the business components
public interface App {

    String NAME = "mpc3000";
    String VERSION = "2026-11-06.6";

    record Invocation(String command, List<String> parameters) {

        static Invocation parse(List<String> args) {
            if (args.isEmpty())
                return new Invocation("help", List.of());
            return new Invocation(args.getFirst(), args.subList(1, args.size()));
        }
    }

    static void main(String... args) {
        IO.println(NAME + " " + VERSION);
        try {
            run(Invocation.parse(List.of(args)));
        } catch (IllegalArgumentException | IOException e) {
            System.err.println(NAME + ": " + e.getMessage());
            System.exit(1);
        }
    }

    static void run(Invocation invocation) throws IOException {
        var parameters = invocation.parameters();
        switch (invocation.command()) {
            case "help" -> usage();
            case "version" -> {}
            case "list" -> {
                if (parameters.size() != 1)
                    throw new IllegalArgumentException("list expects exactly one image file");
                IO.println(Images.list(Path.of(parameters.getFirst())));
            }
            case "extract" -> {
                if (parameters.isEmpty() || parameters.size() > 2)
                    throw new IllegalArgumentException("extract expects an image file and an optional folder");
                var image = Path.of(parameters.getFirst());
                var target = parameters.size() == 2 ? Path.of(parameters.get(1)) : Images.defaultTarget(image);
                IO.println(Images.extract(image, target));
            }
            case "convert" -> {
                if (parameters.isEmpty() || parameters.size() > 2)
                    throw new IllegalArgumentException("convert expects an SND file or folder and an optional target folder");
                var target = parameters.size() == 2 ? Optional.of(Path.of(parameters.get(1))) : Optional.<Path>empty();
                Sounds.convert(Path.of(parameters.getFirst()), target, IO::println);
            }
            case "restore" -> {
                if (parameters.isEmpty() || parameters.size() > 2)
                    throw new IllegalArgumentException("restore expects a folder and an optional image file");
                var image = parameters.size() == 2 ? Optional.of(Path.of(parameters.get(1))) : Optional.<Path>empty();
                IO.println(Images.restore(Path.of(parameters.getFirst()), image));
            }
            case "floppy-read" -> {
                if (parameters.size() > 1)
                    throw new IllegalArgumentException("floppy-read expects an optional image file");
                Floppies.read(parameters.stream().findFirst().map(Path::of), IO::println);
            }
            default -> throw new IllegalArgumentException("unknown command: " + invocation.command());
        }
    }

    static void usage() {
        IO.println("""
                Usage: %s <command> [arguments]
                  list <image>               List partitions and files of an MPC3000 disk image
                  extract <image> [<folder>] Extract partitions containing sounds, one subfolder each,
                                             into folder (created or added to) or ./<image name>;
                                             existing files are replaced
                  convert <snd|folder> [<folder>]
                                             Convert an SND file or all SND files below a folder to WAV,
                                             next to each SND or mirrored into folder
                  restore <folder> [<image>] Write a folder into a new image: partition folders MPC-PART--A…W
                                             make a hard disk (.iso), files alone a 1.44 MB floppy (.img);
                                             by default <folder>.iso or .img next to the folder
                  floppy-read [<image>]      Copy the floppy in the USB drive into an image file,
                                             by default the next free ./FLOPPYnn.img
                  help                       Show this help (default)
                  version                    Show version""".formatted(NAME));
    }
}
