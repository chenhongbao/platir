package io.platir.engine.core;

import io.platir.utils.Utils;
import static io.platir.utils.Utils.classFilePath;
import java.nio.file.Path;
import java.nio.file.Paths;

class Commons {

    static Path instanceDirectory() {
        var code = Integer.toString(classFilePath(Utils.class).hashCode());
        return Paths.get(Utils.cwd().toString(), "Platir", code);
    }

    static Path settlementBackupDirectory() {

        return Paths.get(instanceDirectory().toString(), "Settlement", Utils.date());
    }

    static String userPreBackupFilename(String userId) {
        return userId + ".pre";
    }

    static String userBackupFilename(String userId) {
        return userId;
    }

    static Path clearBackupDirectory() {
        return Paths.get(instanceDirectory().toString(), "Clear", Utils.date());
    }

    static String infoCenterBackupFilename() {
        return "InfoCenter.json";
    }

    static Path loggingDirectory() {
        return Paths.get(instanceDirectory().toString(), "Logging");
    }
}
