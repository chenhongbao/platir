package io.platir.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Utils {

    public static final String STDOUT_FILE = "stdout.txt";
    public static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
    public static final DateTimeFormatter datetimeFormat = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");

    public static String date() {
        return LocalDate.now().format(dateFormat);
    }

    public static String time() {
        return LocalTime.now().format(timeFormat);
    }

    public static String datetime() {
        return LocalDateTime.now().format(datetimeFormat);
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
