package io.platir.engine.core;

import io.platir.commons.UserCore;
import io.platir.engine.InitializeEngineException;
import io.platir.engine.timer.EngineTimer;
import io.platir.engine.timer.TimerJob;
import io.platir.utils.Utils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

class ReinitEngineJob implements TimerJob {

    private final PlatirEngineCore engine;

    ReinitEngineJob(PlatirEngineCore engine) {
        this.engine = engine;
    }

    @Override
    public void onTime(LocalDateTime datetime, EngineTimer timer) {
        try {
            var setting = engine.getGlobalSetting();
            if (setting.reinitTime().check(datetime)) {
                reinitEngine();
                reloadData();
            }
        } catch (Throwable throwable) {
            PlatirEngineCore.logger().log(Level.SEVERE, "Re-initialize engine throws exception. {0}", throwable.getMessage());
        }
    }

    private void reinitEngine() {
        try {
            engine.initializeNow();
        } catch (InitializeEngineException exception) {
            PlatirEngineCore.logger().log(Level.SEVERE, "Re-initialize engine throws exception. {0}", exception.getMessage());
        }
    }

    private void reloadData() throws IOException {
        InfoCenter.read(Paths.get(findLatestDate(Commons.clearBackupDirectory().getParent()).toString(), Commons.infoCenterBackupFilename()).toFile());
        Set<UserCore> users = new HashSet<>();
        Files.list(findLatestDate(Commons.settlementBackupDirectory().getParent())).forEach(path -> {
            users.add(Utils.readJson(path.toFile(), UserCore.class));
        });
        engine.getUserStrategyManager().reload(users);
        engine.getUserManager().reload(users);
    }

    private Path findLatestDate(Path root) throws IOException {
        try {
            return Files.list(root).filter(path -> Files.isRegularFile(path))
                    .reduce((Path first, Path second) -> first.getFileName().toString().compareTo(second.getFileName().toString()) > 0 ? first : second)
                    .get();
        } catch (IOException exception) {
            PlatirEngineCore.logger().log(Level.WARNING, "Can''t access info center record and loading may be incompleted. {0}", exception.getMessage());
            throw exception;
        }
    }

}
