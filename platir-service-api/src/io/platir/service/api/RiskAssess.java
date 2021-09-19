package io.platir.service.api;

import io.platir.service.Notice;
import io.platir.service.PlatirQuery;
import io.platir.service.Tick;
import io.platir.service.Trade;
import io.platir.service.Transaction;

public interface RiskAssess {
	Notice before(Tick current, Transaction transaction, PlatirQuery query);
	
	Notice after(Trade trade, Transaction transaction, PlatirQuery query);
}
