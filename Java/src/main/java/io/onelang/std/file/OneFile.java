package io.onelang.std.file;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import io.onelang.std.core.console;

public class OneFile {
    public static String readText(String fileName) {
        try {
            return Files.readString(Path.of(fileName));
        } catch(Exception e) {
            console.error("[ERROR] readText (fn = " + fileName + "): " + e);
            return null;
        }
    }

    public static void writeText(String fileName, String data) {
        try {
            Files.writeString(Path.of(fileName), data);
        } catch(Exception e) {
            console.error("[ERROR] writeText (fn = " + fileName + "): " + e);
        }
    }

    public static String[] listFiles(String directory, Boolean recursive) {
        try {
            final var dirLen = directory.length();
            var files = Files.walk(Paths.get(directory)).filter(Files::isRegularFile).map(Path::toString).map(x -> x.substring(dirLen)).sorted().toArray(String[]::new);
            return files;
        } catch(Exception e) {
            console.error(e.toString());
            return null;
        }
    }

    public static void copy(String srcFn, String dstFn) {
        try {
            Files.copy(Path.of(srcFn), Path.of(dstFn));
        } catch(Exception e) {
            console.error("[ERROR] copy (srcFn = " + srcFn + ", dstFn = " + dstFn + "): " + e);
        }
    }
}