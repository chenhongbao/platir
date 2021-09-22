package io.platir.service.api;

import io.platir.service.Tick;

public interface MarketListener {
	void onTick(Tick tick);
}
