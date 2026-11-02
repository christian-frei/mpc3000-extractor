package christianfrei.mpc3000.image.control;

import java.util.Set;

/// Maps MPC names to file names valid on the host
interface HostNames {

    /// Replaces control characters and characters reserved in file names on common file systems
    static String safe(String name) {
        return name.replaceAll("[\\x00-\\x1F\\x7F/\\\\:*?\"<>|]", "_");
    }

    /// Appends ~2, ~3, ... before the extension until the name is not taken
    static String unique(String fileName, Set<String> taken) {
        var dot = fileName.lastIndexOf('.');
        var stem = dot > 0 ? fileName.substring(0, dot) : fileName;
        var extension = dot > 0 ? fileName.substring(dot) : "";
        var candidate = fileName;
        for (var n = 2; !taken.add(candidate.toLowerCase()); n++)
            candidate = stem + "~" + n + extension;
        return candidate;
    }
}
