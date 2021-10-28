package io.platir.engine.core;

import io.platir.engine.timer.EngineTimer;
import io.platir.engine.timer.TimerJob;
import io.platir.utils.Utils;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.logging.Level;

class ClearEngineJob implements TimerJob {

    private final PlatirEngineCore engine;

    ClearEngineJob(PlatirEngineCore engine) {
        this.engine = engine;
    }

    @Override
    public void onTime(LocalDateTime datetime, EngineTimer timer) {
        if (engine.getGlobalSetting().clearTime().check(datetime)) {
            /* Roll trading day to next working day after settlement so no
             * matter whether re-init succeeds the accounts are always
             * settled to a new day. If it is holiday(for example), account
             * is settled without trades that means it replicates what it
             * has at the end of last trading day, and so forth until market 
             * re-opens.
             */
            rollTradingDay();
            tryForceCancelAll();
            InfoCenter.write(Utils.file(Paths.get(Commons.clearBackupDirectory().toString(), Commons.infoCenterBackupFilename())));
        }
    }

    private void tryForceCancelAll() {
        if (!engine.getTradingAdapter().isTransactionAllDone()) {
            PlatirEngineCore.logger().warning("Some transactions remain incompleted or unsettled, force canceling all.");
            try {
                engine.getTradingAdapter().forceCancelAll();
            } catch (ForceCancelException exception) {
                PlatirEngineCore.logger().log(Level.WARNING, "Force canceling all throws exception. {0}", exception.getMessage());
            }
        }
    }

    private void rollTradingDay() {
        LocalDate today = LocalDate.now();
        var tomorrow = today.plusDays(1);
        while (tomorrow.getDayOfWeek() == DayOfWeek.SATURDAY || tomorrow.getDayOfWeek() == DayOfWeek.SUNDAY) {
            tomorrow = tomorrow.plusDays(1);
        }
        InfoCenter.setTradingDay(tomorrow.format(Utils.dateFormat));
    }
}
