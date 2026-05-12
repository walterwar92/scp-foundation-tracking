package ru.scp.foundation.util;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

/**
 * Распаковщик .txz архивов (xz+tar) — нужен на Windows, где встроенный tar.exe
 * не умеет xz без внешнего liblzma/xz.exe.
 *
 * Зависит от commons-compress и xz-java JAR-ов (скачиваются db-setup в db-runtime/tools/).
 *
 * Usage: TxzExtractor &lt;input.txz&gt; &lt;output-dir&gt;
 */
public class TxzExtractor {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: TxzExtractor <input.txz> <output-dir>");
            System.exit(1);
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]).toAbsolutePath().normalize();
        Files.createDirectories(output);

        int fileCount = 0;
        try (InputStream raw = Files.newInputStream(input);
             InputStream buf = new BufferedInputStream(raw);
             XZCompressorInputStream xz = new XZCompressorInputStream(buf);
             TarArchiveInputStream tar = new TarArchiveInputStream(xz)) {

            TarArchiveEntry entry;
            while ((entry = tar.getNextEntry()) != null) {
                if (!tar.canReadEntryData(entry)) continue;

                Path target = output.resolve(entry.getName()).normalize();
                if (!target.startsWith(output)) {
                    throw new IOException("Suspicious tar entry escapes output: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                    continue;
                }
                if (entry.isSymbolicLink()) {
                    continue;
                }
                Path parent = target.getParent();
                if (parent != null) Files.createDirectories(parent);
                try (OutputStream out = Files.newOutputStream(target)) {
                    tar.transferTo(out);
                }
                fileCount++;

                // На POSIX системах сохраняем executable-бит — критично для pg_ctl/postgres/initdb.
                try {
                    Set<PosixFilePermission> perms = modeToPosix(entry.getMode());
                    if (!perms.isEmpty()) Files.setPosixFilePermissions(target, perms);
                } catch (UnsupportedOperationException ignored) {
                    // Windows — без POSIX-permissions, executable определяется по расширению.
                }
            }
        }
        System.out.println("[txz] Extracted " + fileCount + " files to " + output);
    }

    private static Set<PosixFilePermission> modeToPosix(int mode) {
        Set<PosixFilePermission> perms = new HashSet<>();
        if ((mode & 0400) != 0) perms.add(PosixFilePermission.OWNER_READ);
        if ((mode & 0200) != 0) perms.add(PosixFilePermission.OWNER_WRITE);
        if ((mode & 0100) != 0) perms.add(PosixFilePermission.OWNER_EXECUTE);
        if ((mode & 0040) != 0) perms.add(PosixFilePermission.GROUP_READ);
        if ((mode & 0020) != 0) perms.add(PosixFilePermission.GROUP_WRITE);
        if ((mode & 0010) != 0) perms.add(PosixFilePermission.GROUP_EXECUTE);
        if ((mode & 0004) != 0) perms.add(PosixFilePermission.OTHERS_READ);
        if ((mode & 0002) != 0) perms.add(PosixFilePermission.OTHERS_WRITE);
        if ((mode & 0001) != 0) perms.add(PosixFilePermission.OTHERS_EXECUTE);
        return perms;
    }
}
