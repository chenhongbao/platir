package io.platir.engine.core;

import io.platir.commons.AccountCore;
import io.platir.engine.timer.EngineTimer;
import io.platir.engine.timer.TimerJob;
import io.platir.utils.Utils;
import java.time.DayOfWeek;
import java.time.LocalDate;
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
                /* Roll trading day to next working day after settlement so no
                 * matter whether re-init succeeds the accounts are always
                 * settled to a new day. If it is holiday(for example), account
                 * is settled without trades that means it replicates what it
                 * has at the end of last trading day, and so forth until market 
                 * re-opens.
                 */
                rollTradingDay();
            } catch (Throwable throwable) {
                PlatirEngineCore.logger().log(Level.SEVERE, "Account({0}) settlement throws exception. {1}", new Object[]{account.getAccountId(), throwable.getMessage()});
            }
        }
    }

    private void settleAccount() {
        //TODO Implement settleAccount() in SettleAccountJob.
    }

    private void rollTradingDay() {
        LocalDate today = LocalDate.now();
        var tomorrow = today.plusDays(1);
        while (tomorrow.getDayOfWeek() == DayOfWeek.SATURDAY || tomorrow.getDayOfWeek() == DayOfWeek.SUNDAY) {
            tomorrow = tomorrow.plusDays(1);
        }
        InfoCenter.setTradingDay(tomorrow.format(Utils.dateFormat));
    }

    private boolean isAccountRemoved() {
        return !account.getUser().accounts().containsValue(account);
    }

}
