package io.platir.core;

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

public final class PlatirSystem {

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
        var p = Paths.get(System.getProperty("user.dir"), "PlatirCWD");
        dir(p);
        return p;
    }

    public static Path dir(Path p) {
        if (!Files.isDirectory(p)) {
            try {
                Files.createDirectories(p);
                setAccessible(p);
            } catch (IOException e) {
                err.write("Can't create working directory \'" + p.toAbsolutePath().toString() + "\'.", e);
            }
        }
        return p;
    }

    public static File file(Path p) {
        if (Files.isRegularFile(p)) {
            try {
                Files.createFile(p);
                setAccessible(p);
            } catch (IOException e) {
                err.write("Can't create file \'" + p.toAbsolutePath().toString() + "\'.", e);
            }
        }
        return null;
    }

    protected static void setAccessible(Path p) {
        if (!Files.exists(p)) {
            return;
        }
        if (!Files.isWritable(p)) {
            p.toFile().setWritable(true);
        }
        if (!Files.isReadable(p)) {
            p.toFile().setReadable(true);
        }
    }
}
