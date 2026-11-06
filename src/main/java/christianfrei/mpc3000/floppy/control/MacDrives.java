package christianfrei.mpc3000.floppy.control;

import christianfrei.mpc3000.floppy.entity.Disk;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/// Disks via `diskutil`; copies via `dd` run by `sudo`, which asks for the password in the terminal
public record MacDrives() implements Drives {

    static final String PROMPT = "mpc3000: administrator password for %u to read the floppy: ";

    @Override
    public List<Disk> externalDisks() throws IOException {
        var result = run(List.of("diskutil", "list", "-plist", "external", "physical"));
        if (result.exitCode() != 0)
            throw new IOException("diskutil failed: " + result.output());
        return disks(result.output());
    }

    /// sudo reads the password from the controlling terminal, which only exists when run from one
    @Override
    public boolean canAskForPassword() {
        try (var terminal = new RandomAccessFile("/dev/tty", "r")) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /// The shell command always exits 0, so a failing exit status comes from sudo itself
    @Override
    public String copy(Disk disk, Path image) throws IOException {
        var result = runInTerminal(copyCommand(disk, image, System.getProperty("user.name")));
        if (result.exitCode() != 0)
            throw new CancellationException("administrator password prompt cancelled or failed");
        return ddOutput(result.output(), image);
    }

    /// sudo runs the copy inside the terminal app, which macOS lets read removable disks
    public static List<String> copyCommand(Disk disk, Path image, String user) {
        return List.of("sudo", "-p", PROMPT, "/bin/sh", "-c", shellCommand(disk.rawDevice(), image, user));
    }

    /// Reads device into image with dd, makes user the owner when an image was written, and always succeeds,
    /// printing dd's output followed by `exit <status>` — a failing last command would hide dd's own messages
    public static String shellCommand(String device, Path image, String user) {
        var target = quoted(image.toAbsolutePath().normalize().toString());
        return "out=$(/bin/dd if=%s of=%s bs=512 conv=noerror,sync 2>&1); status=$?; if [ -f %s ]; then /usr/sbin/chown %s %s; fi; echo \"$out\"; echo \"exit $status\""
                .formatted(quoted(device), target, target, quoted(user), target);
    }

    /// dd's output without the trailing exit line
    /// @throws IOException with dd's messages when no image was written
    public static String ddOutput(String shellOutput, Path image) throws IOException {
        var lines = shellOutput.strip().lines().toList();
        var status = lines.isEmpty() ? "" : lines.getLast();
        var output = String.join("\n", lines.isEmpty() ? lines : lines.subList(0, lines.size() - 1));
        if (!Files.exists(image))
            throw new IOException("copy failed (%s): %s".formatted(status, output.strip()));
        return output;
    }

    /// Single-quotes for the shell; an embedded quote becomes '\''
    static String quoted(String text) {
        return "'" + text.replace("'", "'\\''") + "'";
    }

    /// Reads `DeviceIdentifier` and `Size` of each whole disk from `diskutil list -plist` output
    public static List<Disk> disks(String plist) throws IOException {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            var document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(plist.getBytes(StandardCharsets.UTF_8)));
            var root = firstElement(document.getDocumentElement().getFirstChild());
            var disks = new ArrayList<Disk>();
            var wholeDisks = value(root, "AllDisksAndPartitions");
            if (wholeDisks == null)
                return disks;
            for (var dict = firstElement(wholeDisks.getFirstChild()); dict != null; dict = firstElement(dict.getNextSibling())) {
                var identifier = value(dict, "DeviceIdentifier");
                var size = value(dict, "Size");
                if (identifier != null && size != null)
                    disks.add(new Disk(identifier.getTextContent(), Long.parseLong(size.getTextContent().strip())));
            }
            return disks;
        } catch (ParserConfigurationException | SAXException | NumberFormatException e) {
            throw new IOException("cannot read diskutil output: " + e.getMessage(), e);
        }
    }

    /// The element following `<key>name</key>` in a plist dict
    static Element value(Element dict, String name) {
        for (var key = firstElement(dict.getFirstChild()); key != null; key = firstElement(key.getNextSibling())) {
            if (key.getTagName().equals("key") && key.getTextContent().equals(name))
                return firstElement(key.getNextSibling());
        }
        return null;
    }

    static Element firstElement(Node node) {
        while (node != null && node.getNodeType() != Node.ELEMENT_NODE)
            node = node.getNextSibling();
        return (Element) node;
    }

    record Result(int exitCode, String output) {}

    /// Shares the terminal for sudo's prompt and messages; captures only standard output
    static Result runInTerminal(List<String> command) throws IOException {
        var process = new ProcessBuilder(command)
                .redirectInput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start();
        return result(process, command);
    }

    static Result run(List<String> command) throws IOException {
        return result(new ProcessBuilder(command).redirectErrorStream(true).start(), command);
    }

    static Result result(Process process, List<String> command) throws IOException {
        var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        try {
            return new Result(process.waitFor(), output);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted while running " + command.getFirst(), e);
        }
    }
}
