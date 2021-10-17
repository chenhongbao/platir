package io.platir.core.internals.persistence.object;

import io.platir.service.Account;
import io.platir.service.Bar;
import io.platir.service.Contract;
import io.platir.service.Instrument;
import io.platir.service.Notice;
import io.platir.service.Order;
import io.platir.service.Position;
import io.platir.service.RiskNotice;
import io.platir.service.StrategyProfile;
import io.platir.service.TradingDay;
import io.platir.service.Transaction;
import io.platir.service.User;

/**
 *
 * @author Chen Hongbao
 */
public class ObjectFactory {

    public static Account newAccount() {
        return new AccountImpl();
    }

    public static Bar newBar() {
        return new BarImpl();
    }

    public static Contract newContract() {
        return new ContractImpl();
    }

    public static Instrument newInstrument() {
        return new InstrumentImpl();
    }

    public static Notice newNotice() {
        return new NoticeImpl();
    }

    public static Order newOrder() {
        return new OrderImpl();
    }

    public static Position newPosition() {
        return new PositionImpl();
    }

    public static RiskNotice newRiskNotice() {
        return new RiskNoticeImpl();
    }

    public static StrategyProfile newStrategyProfile() {
        return new StrategyProfileImpl();
    }

    public static TradingDay newTradingDay() {
        return new TradingDayImpl();
    }

    public static Transaction newTransaction() {
        return new TransactionImpl();
    }

    public static User newUser() {
        return new UserImpl();
    }
}
