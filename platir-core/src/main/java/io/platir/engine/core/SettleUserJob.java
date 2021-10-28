package io.platir.engine.core;

import io.platir.Order;
import io.platir.commons.AccountCore;
import io.platir.commons.OrderCore;
import io.platir.commons.UserCore;
import io.platir.engine.timer.EngineTimer;
import io.platir.engine.timer.TimerJob;
import io.platir.utils.Utils;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

class SettleUserJob implements TimerJob {

    private final UserCore user;
    private final PlatirEngineCore engine;

    SettleUserJob(UserCore user, PlatirEngineCore engine) {
        this.user = user;
        this.engine = engine;
    }

    @Override
    public void onTime(LocalDateTime datetime, EngineTimer timer) {
        var setting = user.getUserSetting();
        if (setting.settlementTime().check(datetime)) {
            try {
                writeUser(Commons.userPreBackupFilename(user.getUserId()));
                settleUser();
                writeUser(Commons.userBackupFilename(user.getUserId()));
            } catch (Throwable throwable) {
                PlatirEngineCore.logger().log(Level.SEVERE, "User({0}) settlement throws exception. {1}", new Object[]{user.getUserId(), throwable.getMessage()});
            }
        }
    }

    private void settleUser() {
        user.accounts().values().forEach(account -> {
            try {
                for (OrderCore orderCancel : findAliveOrders(account)) {
                    engine.getTradingAdapter().forceCancel(orderCancel.getOrderId());
                }
                AccountUtils.settleAccount(account, AccountUtils.findInstruments(account), AccountUtils.findLatestPrices(account), InfoCenter.getTradingDay());
            } catch (InsufficientInfoException exception) {
                PlatirEngineCore.logger().log(Level.SEVERE, "Account({0}) under user({1}) not enough information for settlement. {2}", new Object[]{account.getAccountId(), user.getUserId(), exception.getMessage()});
            } catch (ForceCancelException exception) {
                PlatirEngineCore.logger().log(Level.SEVERE, "Force cancel before account({0}) settlement throws exception. {1}", new Object[]{account.getAccountId(), exception.getMessage()});
            }
        });
    }

    private void writeUser(String filename) {
        Utils.writeJson(Utils.file(Paths.get(Commons.settlementBackupDirectory().toString(), filename)), user);
    }

    private Set<OrderCore> findAliveOrders(AccountCore account) {
        Set<OrderCore> toCancel = new HashSet<>();
        account.strategies().values().forEach(strategy -> {
            strategy.transactions().values().forEach(transaction -> {
                transaction.orders().values().forEach(order -> {
                    if (order.getState().equals(Order.QUEUEING)) {
                        toCancel.add(order);
                    }
                });
            });
        });
        return toCancel;
    }

}
