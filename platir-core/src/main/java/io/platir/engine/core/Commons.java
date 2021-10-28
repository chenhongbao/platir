package io.platir.engine.core;

import io.platir.utils.Utils;
import java.nio.file.Path;
import java.nio.file.Paths;

class Commons {

    static Path settlementBackupDirectory() {
        return Paths.get(Utils.cwd().toString(), "Settlement", Utils.date());
    }

    static String userPreBackupFilename(String userId) {
        return userId + ".pre";
    }

    static String userBackupFilename(String userId) {
        return userId;
    }
    
    static Path clearBackupDirectory() {
        return Paths.get(Utils.cwd().toString(), "Clear", Utils.date());
    }
    
    static String infoCenterBackupFilename() {
        return "InfoCenter.json";
    }
}
