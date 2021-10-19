package io.platir.service.api;

import io.platir.service.RiskNotice;
import io.platir.service.Tick;
import io.platir.service.Trade;
import io.platir.service.TransactionContext;

public interface RiskManager {

    RiskNotice before(Tick current, TransactionContext transaction);

    RiskNotice after(Trade trade, TransactionContext transaction);
}
