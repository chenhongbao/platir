package io.platir.queries;

import io.platir.service.Account;
import io.platir.service.Bar;
import io.platir.service.Contract;
import io.platir.service.Factory;
import io.platir.service.Instrument;
import io.platir.service.Notice;
import io.platir.service.Order;
import io.platir.service.Position;
import io.platir.service.RiskNotice;
import io.platir.service.StrategyProfile;
import io.platir.service.Tick;
import io.platir.service.Trade;
import io.platir.service.TradingDay;
import io.platir.service.Transaction;
import io.platir.service.User;

/**
 *
 * @author Chen Hongbao
 */
public class FactoryImpl implements Factory {

    @Override
    public Account newAccount() {
        return new AccountImpl();
    }

    @Override
    public Bar newBar() {
        return new BarImpl();
    }

    @Override
    public Contract newContract() {
        return new ContractImpl();
    }

    @Override
    public Instrument newInstrument() {
        return new InstrumentImpl();
    }

    @Override
    public Notice newNotice() {
        return new NoticeImpl();
    }

    @Override
    public Order newOrder() {
        return new OrderImpl();
    }

    @Override
    public Position newPosition() {
        return new PositionImpl();
    }

    @Override
    public RiskNotice newRiskNotice() {
        return new RiskNoticeImpl();
    }

    @Override
    public StrategyProfile newStrategyProfile() {
        return new StrategyProfileImpl();
    }

    @Override
    public TradingDay newTradingDay() {
        return new TradingDayImpl();
    }

    @Override
    public Transaction newTransaction() {
        return new TransactionImpl();
    }

    @Override
    public User newUser() {
        return new UserImpl();
    }

    @Override
    public Tick newTick() {
        return new TickImpl();
    }

    @Override
    public Trade newTrade() {
        return new TradeImpl();
    }
}
