package io.platir.service.api;

import io.platir.service.PlatirQuery;
import io.platir.service.RiskNotice;
import io.platir.service.Tick;
import io.platir.service.Trade;
import io.platir.service.Transaction;

public interface RiskAssess {
	RiskNotice before(Tick current, Transaction transaction, PlatirQuery query);
	
	RiskNotice after(Trade trade, Transaction transaction, PlatirQuery query);
}
