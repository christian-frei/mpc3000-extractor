package christianfrei.mpc3000.floppy.control;

import christianfrei.mpc3000.floppy.entity.Disk;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/// Disks via `diskutil`; copies via `dd` behind the macOS administrator password dialog
public record MacDrives() implements Drives {

    static final String CANCELLED = "(-128)";

    @Override
    public List<Disk> externalDisks() throws IOException {
        var result = run(List.of("diskutil", "list", "-plist", "external", "physical"));
        if (result.exitCode() != 0)
            throw new IOException("diskutil failed: " + result.output());
        return disks(result.output());
    }

    @Override
    public String copy(Disk disk, Path image) throws IOException {
        var result = run(copyCommand(disk, image, System.getProperty("user.name")));
        if (result.exitCode() != 0 && result.output().contains(CANCELLED))
            throw new CancellationException("administrator password dialog cancelled");
        if (result.exitCode() != 0)
            throw new IOException("copy failed: " + result.output().strip());
        return result.output();
    }

    /// Paths and user travel as AppleScript arguments and are shell-quoted by `quoted form of`, never spliced into the script
    public static List<String> copyCommand(Disk disk, Path image, String user) {
        var script = """
                do shell script "/bin/dd if=" & quoted form of (item 1 of argv) & " of=" & quoted form of (item 2 of argv) \
                & " bs=512 conv=noerror,sync 2>&1; /usr/sbin/chown " & quoted form of (item 3 of argv) \
                & " " & quoted form of (item 2 of argv) with administrator privileges""";
        return List.of("osascript", "-e", "on run argv", "-e", script, "-e", "end run",
                disk.rawDevice(), image.toAbsolutePath().normalize().toString(), user);
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

    static Result run(List<String> command) throws IOException {
        var process = new ProcessBuilder(command).redirectErrorStream(true).start();
        var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        try {
            return new Result(process.waitFor(), output);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted while running " + command.getFirst(), e);
        }
    }
}
