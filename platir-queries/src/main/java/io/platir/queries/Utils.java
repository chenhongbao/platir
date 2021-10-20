package io.platir.queries;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Utils {

    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter datetimeFormat = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");

    private static Console out;
    private static Console err;

    private static final ExecutorService threads = Executors.newCachedThreadPool();
    private static final Random random = new Random();

    static {

    }

    public static ExecutorService threads() {
        return threads;
    }

    public synchronized static Console err() {
        if (err == null) {
            setupConsoles();
        }
        return err;
    }

    public synchronized static Console out() {
        if (out == null) {
            setupConsoles();
        }
        return out;
    }

    public static LocalDate date(String dateOrDatetime) {
        switch (dateOrDatetime.length()) {
            case 8:
                return LocalDate.parse(dateOrDatetime, dateFormat);
            case 17:
                return LocalDateTime.parse(dateOrDatetime, datetimeFormat).toLocalDate();
            default:
                err.write("Invalid date(" + dateOrDatetime + ").");
                throw new RuntimeException("Invalid date(" + dateOrDatetime + ").");
        }

    }

    public static LocalDateTime datetime(String dateOrDatetime) {
        switch (dateOrDatetime.length()) {
            case 8:
                return LocalDateTime.of(LocalDate.parse(dateOrDatetime, dateFormat), LocalTime.of(0, 0));
            case 17:
                return LocalDateTime.parse(dateOrDatetime, datetimeFormat);
            default:
                err.write("Invalid datetime(" + dateOrDatetime + ").");
                throw new RuntimeException("Invalid datetime(" + dateOrDatetime + ").");
        }

    }

    public static String date() {
        return LocalDate.now().format(dateFormat);
    }

    public static String datetime() {
        return LocalDateTime.now().format(datetimeFormat);
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

    public static void delete(Path root, boolean deleteRoot) throws IOException {
        if (Files.isDirectory(root)) {
            Files.walkFileTree(root, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes bfa) throws IOException {
                    delete(directory, true);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes bfa) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException ioe) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path directory, IOException ioe) throws IOException {
                    Files.delete(directory);
                    return FileVisitResult.CONTINUE;
                }
            });
            if (deleteRoot) {
                Files.delete(root);
            }
        } else if (Files.isRegularFile(root) || Files.isSymbolicLink(root)) {
            Files.delete(root);
        }
    }

    public static Path schemaDirectory() {
        var path = Paths.get(Utils.cwd().toString(), "Schema");
        dir(path);
        return path;
    }

    public static Path backupDirectory() {
        var path = Paths.get(Utils.cwd().toString(), "Backup");
        dir(path);
        return path;
    }

    public static int randomInteger() {
        return random.nextInt() + 1;
    }

    public static <T> boolean beanEquals(Class<T> clazz, Object o1, Object o2) {
        if (o1.getClass() != clazz || o2.getClass() != clazz) {
            return false;
        }
        for (var method : clazz.getMethods()) {
            if (!method.getName().startsWith("get")) {
                continue;
            }
            try {
                if (!method.invoke(o1).equals(method.invoke(o2))) {
                    return false;
                }
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                return false;
            }
        }
        return true;
    }

    public static <T> boolean collectionEquals(Class<T> clazz, Collection<T> c1, Collection<T> c2) {
        for (var o1 : c1) {
            boolean hit = false;
            for (var o2 : c2) {
                if (beanEquals(clazz, o1, o2)) {
                    hit = true;
                    break;
                }
            }
            if (!hit) {
                return false;
            }
        }
        return true;
    }

    protected static void access(Path path) {
        if (!Files.isWritable(path)) {
            path.toFile().setWritable(true);
        }
        if (!Files.isReadable(path)) {
            path.toFile().setReadable(true);
        }
    }

    private static void setupConsoles() {
        try {
            out = new Console(new PrintStream(
                    new FileOutputStream(file(Paths.get(cwd().toString(), "console.out.txt")), true), true));
            err = new Console(new PrintStream(
                    new FileOutputStream(file(Paths.get(cwd().toString(), "console.err.txt")), true), true));
        } catch (FileNotFoundException e) {
            System.err.println("Can't create console to external file: " + e.getMessage() + ".");
            /* Console output is essential, just fallback to stdout/err. */
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
}
