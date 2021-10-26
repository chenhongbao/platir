package io.platir.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class Basics {

    public static final String STDOUT_FILE = "stdout.txt";

    public static Logger logger() {
        return null;
    }

    public static PrintStream stdout() {
        try {
            var file = file(STDOUT_FILE);
            if (file != null) {
                return new PrintStream(new FileOutputStream(file, true));
            } else {
                return System.out;
            }
        } catch (FileNotFoundException ex) {
            return System.out;
        }
    }

    public static File file(String path) {
        var filePath = Paths.get(path);
        try {
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
            }
            var file = filePath.toFile();
            file.setReadable(true);
            file.setWritable(true);
            return file;
        } catch (IOException exception) {
            return null;
        }
    }
}
