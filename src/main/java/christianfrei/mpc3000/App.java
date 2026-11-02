package christianfrei.mpc3000;

import christianfrei.mpc3000.image.boundary.Images;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/// Parses the command line and dispatches to the business components
public interface App {

    String NAME = "mpc3000";
    String VERSION = "2026-11-02.6";

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
                  help                       Show this help (default)
                  version                    Show version""".formatted(NAME));
    }
}
