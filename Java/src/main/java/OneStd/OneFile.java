package OneStd;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

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
            // for (var file : files)
            //     console.log(file);
            return files;
        } catch(Exception e) {
            console.error(e.toString());
            return null;
        }
    }
}