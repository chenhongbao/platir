package io.platir.core.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Utils {

    private static DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static DateTimeFormatter datetimeFmt = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");

    public static Console out;
    public static Console err;

    static {
        try {
            out = new Console(new PrintStream(
                    new FileOutputStream(file(Paths.get(cwd().toString(), "console.out.txt")), true), true));
            err = new Console(new PrintStream(
                    new FileOutputStream(file(Paths.get(cwd().toString(), "console.err.txt")), true), true));
        } catch (FileNotFoundException e) {
            System.err.println("Can't create console to external file: " + e.getMessage() + ".");
            /* console output is essential, just fallback to stdout/err */
            out = new Console(System.out);
            err = new Console(System.err);
        }
    }

    public static final ExecutorService threads = Executors.newCachedThreadPool();

    public static class Console {

        private final PrintStream out;

        Console(PrintStream ps) {
            out = ps;
        }

        public void write(Object message) {
            write(out, message);
        }

        public void write(Object message, Throwable error) {
            write(out, message);
            error.printStackTrace(out);
        }

        public void write(Object message, Object... follow) {
            write(message);
            for (var m : follow) {
                write(m);
            }
        }

        private void write(PrintStream out, Object message) {
            out.println(datetime());
            out.println(message.toString());
        }
    }

    public static LocalDate date(String dateOrDatetime) {
        switch (dateOrDatetime.length()) {
            case 8:
                return LocalDate.parse(dateOrDatetime, dateFmt);
            case 17:
                return LocalDateTime.parse(dateOrDatetime, datetimeFmt).toLocalDate();
            default:
                err.write("Invalid date(" + dateOrDatetime + ").");
                throw new RuntimeException("Invalid date(" + dateOrDatetime + ").");
        }

    }

    public static LocalDateTime datetime(String dateOrDatetime) {
        switch (dateOrDatetime.length()) {
            case 8:
                return LocalDateTime.of(LocalDate.parse(dateOrDatetime, dateFmt), LocalTime.of(0, 0));
            case 17:
                return LocalDateTime.parse(dateOrDatetime, datetimeFmt);
            default:
                err.write("Invalid datetime(" + dateOrDatetime + ").");
                throw new RuntimeException("Invalid datetime(" + dateOrDatetime + ").");
        }

    }

    public static String date() {
        return LocalDate.now().format(dateFmt);
    }

    public static String datetime() {
        return LocalDateTime.now().format(datetimeFmt);
    }

    public static Path cwd() {
        var code = Integer.toString(physicalJarLocation().hashCode());
        var path = Paths.get(System.getProperty("user.dir"), "PlatirWorking", code);
        dir(path);
        return path;
    }

    public static Path dir(Path path) {
        if (!Files.isDirectory(path)) {
            try {
                Files.createDirectories(path);
                access(path);
            } catch (IOException exception) {
                err.write("Can't create directory \'" + path.toAbsolutePath().toString() + "\'.", exception);
            }
        }
        return path;
    }

    public static String physicalJarLocation() {
        try {
            return new File(Utils.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();
        } catch (Throwable throwable) {
            throw new RuntimeException("Fail obtaining jar physical path: " + throwable.getMessage(), throwable);
        }
    }

    public static File file(Path path) {
        File file = null;
        if (!Files.isRegularFile(path)) {
            try {
                Files.createFile(path);
                access(path);
                file = path.toFile();
            } catch (IOException exception) {
                err.write("Can't create file \'" + path.toAbsolutePath().toString() + "\', " + exception.getMessage(), exception);
            }
        }
        return file;
    }

    protected static void access(Path path) {
        if (!Files.isWritable(path)) {
            path.toFile().setWritable(true);
        }
        if (!Files.isReadable(path)) {
            path.toFile().setReadable(true);
        }
    }
}