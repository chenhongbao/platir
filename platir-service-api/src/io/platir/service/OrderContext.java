package io.platir.service;

import java.util.Set;

public interface OrderContext {
	Order getOrder();
	
	Set<Trade> getTrades();
	
	Set<Contract> lockedContracts();
}
