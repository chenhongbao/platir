package io.platir.engine.core;

import io.platir.commons.AccountCore;
import io.platir.engine.timer.EngineTimer;
import io.platir.engine.timer.TimerJob;
import java.time.LocalDateTime;
import java.util.logging.Level;

public class SettleAccountJob implements TimerJob {

    private final AccountCore account;

    public SettleAccountJob(AccountCore account) {
        this.account = account;
    }

    @Override
    public void onTime(LocalDateTime datetime, EngineTimer timer) {
        if (isAccountRemoved()) {
            timer.removeJob(this);
            return;
        }
        var setting = account.getAccountSetting();
        if (setting.settlementTime().check(datetime)) {
            try {
                settleAccount();
            } catch (Throwable throwable) {
                PlatirEngineCore.logger().log(Level.SEVERE, "Account({0}) settlement throws exception. {1}", new Object[]{account.getAccountId(), throwable.getMessage()});
            }
        }
    }

    private void settleAccount() {
        //TODO Implement settleAccount() in SettleAccountJob.
    }

    private boolean isAccountRemoved() {
        return !account.getUser().accounts().containsValue(account);
    }

}
