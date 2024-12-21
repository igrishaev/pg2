package org.pg.util;

import org.pg.error.PGError;

public class SysTool {

    public enum OS {WINDOWS, MACOS, LINUX, SOLARIS, UNKNOWN}

    public static OS getOs () {

        final String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            return OS.LINUX;
        }
        else if (osName.contains("windows")) {
            return OS.WINDOWS;
        }
        else if (osName.contains("mac") || osName.contains("darwin")) {
            return OS.MACOS;
        }
        else if (osName.contains("sunos")) {
            return OS.SOLARIS;
        }
        else {
            return OS.UNKNOWN;
        }
    }

    public static String guessUnixSocketPath(final int port) {
        final SysTool.OS os = SysTool.getOs();
        return switch (os) {
            case LINUX -> String.format("/var/run/postgresql/.s.PGSQL.%s", port);
            case MACOS -> String.format("/private/tmp/.s.PGSQL.%s", port);
            default -> throw new PGError("cannot guess unix socket path for os: %s", os);
        };
    }
}
