package io.platir.service;

/**
 *
 * @author Chen Hongbao
 */
public interface Factory {

    Account newAccount();

    Bar newBar();

    Contract newContract();

    Instrument newInstrument();

    Notice newNotice();

    Order newOrder();

    Position newPosition();

    RiskNotice newRiskNotice();

    StrategyProfile newStrategyProfile();

    TradingDay newTradingDay();

    Transaction newTransaction();

    User newUser();
}
