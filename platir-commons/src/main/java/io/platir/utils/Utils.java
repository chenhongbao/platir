package io.platir.utils;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Utils {

    private static final Gson gson;

    public static final String STDOUT_FILE = "stdout.txt";
    public static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
    public static final DateTimeFormatter datetimeFormat = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");

    static {
        gson = new GsonBuilder()
                .serializeNulls()
                .setPrettyPrinting()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
    }

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
            var file = file(Paths.get(STDOUT_FILE));
            if (file != null) {
                return new PrintStream(new FileOutputStream(file, true));
            } else {
                return System.out;
            }
        } catch (FileNotFoundException ex) {
            return System.out;
        }
    }

    public static File file(Path path) {
        try {
            dir(path.getParent());
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
            var file = path.toFile();
            file.setReadable(true);
            file.setWritable(true);
            return file;
        } catch (IOException exception) {
            exception.printStackTrace(stdout());
            return null;
        }
    }

    public static File dir(Path path) {
        try {
            if (!Files.isDirectory(path)) {
                Files.createDirectories(path);
            }
            var file = path.toFile();
            file.setReadable(true);
            file.setWritable(true);
            return file;
        } catch (IOException exception) {
            exception.printStackTrace(stdout());
            return null;
        }
    }

    public static Path cwd() {
        return Paths.get(System.getProperty("user.dir")).toAbsolutePath();
    }

    public static void writeJson(File outfile, Object data) {
        try (FileWriter writer = new FileWriter(outfile, false)) {
            writer.write(gson.toJson(data));
        } catch (IOException exception) {
            exception.printStackTrace(stdout());
        }
    }

    public static <T> T readJson(File infile, Class<T> clazz) {
        try (FileReader reader = new FileReader(infile)) {
            return gson.fromJson(reader, clazz);
        } catch (IOException exception) {
            exception.printStackTrace(stdout());
            return null;
        }
    }

    public static String classFilePath(Class clazz) {
        try {
            return new File(clazz.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
        } catch (URISyntaxException exception) {
            exception.printStackTrace(stdout());
            return "";
        }
    }
}
