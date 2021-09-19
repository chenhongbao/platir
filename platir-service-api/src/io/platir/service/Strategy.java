package io.platir.service;

public interface Strategy {
	void onStart(String[] args, PlatirClient platir);
	
	void onStop(int reason);
	
	void onDestroy(int reason);
	
	void onTrade(Trade trade);
	
	void onTick(Tick tick);
	
	void onBar(Bar bar);
	
	void onNotice(Notice notice);
}
