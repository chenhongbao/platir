package io.platir.core.internals;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import io.platir.core.AnnotationParsingException;
import io.platir.core.IntegrityException;
import io.platir.core.PlatirSystem;
import io.platir.service.Bar;
import io.platir.service.Notice;
import io.platir.service.Order;
import io.platir.service.OrderContext;
import io.platir.service.PlatirClient;
import io.platir.service.StrategyContext;
import io.platir.service.StrategyDrestroyException;
import io.platir.service.StrategyProfile;
import io.platir.service.Tick;
import io.platir.service.Trade;
import io.platir.service.Transaction;
import io.platir.service.TransactionContext;
import io.platir.service.api.Queries;

/**
 * Error code explanation:
 * <ul>
 * <li>4001: Callback throws exception.
 * <li>4002: Callback is timeout.
 * </ul>
 * 
 * @author Chen Hongbao
 * @since 1.0.0
 */
class StrategyContextImpl implements StrategyContext {

	private final StrategyProfile prof;
	private final AnnotatedStrategy annStg;
	private final PlatirClientImpl cli;
	private final StrategyContextPool ctxes;
	private final ExecutorService pool = Executors.newCachedThreadPool();
	private final Set<TransactionContextImpl> transactions = new ConcurrentSkipListSet<>();

	/*
	 * If the strategy is shutdown, no more market data input, but trade response
	 * still comes in.
	 */
	private final AtomicBoolean isShutdown = new AtomicBoolean(true);

	StrategyContextImpl(StrategyProfile profile, Object strategy, TransactionQueue trQueue, MarketRouter mkRouter,
			StrategyContextPool ctxPool, Queries queries) throws AnnotationParsingException {
		prof = profile;
		ctxes = ctxPool;
		annStg = new AnnotatedStrategy(strategy);
		cli = new PlatirClientImpl(this, trQueue, mkRouter, queries);
	}

	PlatirClientImpl getPlatirClientImpl() {
		return null;
	}

	void addTransactionContext(TransactionContextImpl transaction) {
		transactions.add(transaction);
	}

	AnnotatedStrategy getAnnotatedStrategy() {
		return annStg;
	}

	void timedOnTick(Tick tick) {
		/* if the strategy is shutdown, no more tick input */
		if (isShutdown.get()) {
			return;
		}
		timedOperation(true, 1, () -> {
			annStg.onTick(tick);
		});
	}

	void timedOnBar(Bar bar) {
		/* if the strategy is shutdown, no more bar input */
		if (isShutdown.get()) {
			return;
		}
		timedOperation(true, 1, () -> {
			annStg.onBar(bar);
		});
	}

	void timedOnTrade(Trade trade) {
		checkTransactionCompleted();
		timedOperation(true, 1, () -> {
			annStg.onTrade(trade);
		});
	}

	private void checkTransactionCompleted() {
		for (var trans : transactions) {
			var total = trans.getOrderContexts().stream().mapToInt(ctx -> {
				return ctx.getTrades().stream().mapToInt(trade -> trade.getVolume()).sum();
			}).sum();
			var target = trans.getTransaction().getVolume();
			if (total == target) {
				/* transaction is completed, awake. */
				trans.awake();
			} else if (total > target) {
				/* trade more than expected */
				trans.awake();
				PlatirSystem.err.write("Trade(" + total + ") more than expected(" + target + ").");
			}
		}
	}

	void timedOnNotice(Notice notice) {
		timedOperation(false, 1, () -> {
			annStg.onNotice(notice);
		});
	}

	void timedOnStart(String[] args, PlatirClientImpl cli) {
		timedOperation(true, 5, () -> {
			annStg.onStart(args, cli);
		});
	}

	void timedOnStop(int reason) {
		timedOperation(true, 5, () -> {
			annStg.onStop(reason);
		});
	}

	void timedOnDestroy(int reason) {
		timedOperation(true, 5, () -> {
			annStg.onDestroy(reason);
		});
	}

	private void timedOperation(boolean needNotice, int timeoutSec, TimedJob job) {
		var fut = pool.submit(() -> {
			var r = new Notice();
			try {
				job.work();

				r.setCode(0);
				r.setMessage("good");
			} catch (Throwable th) {
				r.setCode(4001);
				r.setMessage("Callback throws exception: " + th.getMessage());
				r.setObject(th);
			}
			return r;
		});

		try {
			var r = fut.get(timeoutSec, TimeUnit.SECONDS);
			if (!r.isGood()) {
				PlatirSystem.err.write(r.getMessage());
				if (needNotice) {
					/* tell strategy its callback fails */
					timedOnNotice(r);
				}
			}
		} catch (InterruptedException | ExecutionException e) {
			PlatirSystem.err.write("Timed operation is interrupted: " + e.getMessage(), e);
		} catch (TimeoutException e) {
			var r = new Notice();
			r.setCode(4002);
			r.setMessage("Callback operation is timeout.");
			r.setObject(e);
			timedOnNotice(r);
		} finally {
			/* the task has to be aborted */
			if (!fut.isDone()) {
				fut.cancel(true);
			}
		}
	}

	void checkIntegrity() throws IntegrityException {
		for (var t : transactions) {
			checkTransactionIntegrity(t);
		}
	}

	private void checkTransactionIntegrity(TransactionContextImpl t) throws IntegrityException {
		var tid = t.getTransaction().getTransactionId();
		var t0 = getTransactionById(tid);
		if (t0 == null) {
			throw new IntegrityException("Transaction(" + tid + ") not found in data source.");
		}
		if (!equals(t.getTransaction(), t0)) {
			throw new IntegrityException("Transaction(" + tid + ") don't match between data source and runtime.");
		}
		var orders = cli.getOrders(t.getTransaction().getTransactionId());
		for (var oc : t.getOrderContexts()) {
			var found = false;
			var o1 = oc.getOrder();
			for (var o2 : orders) {
				if (o1.getOrderId().compareTo(o2.getOrderId()) == 0) {
					found = true;
					checkOrderIntegrity(oc, o2);
					break;
				}
			}
			if (!found) {
				throw new IntegrityException("Order(" + o1.getOrderId() + ") not found in data source.");
			}
		}
	}

	private Transaction getTransactionById(String tid) {
		for (var t : cli.getTransactions()) {
			if (t.getTransactionId().equals(tid)) {
				return t;
			}
		}
		return null;
	}

	private void checkOrderIntegrity(OrderContext oc, Order o2) throws IntegrityException {
		var o1 = oc.getOrder();
		if (!equals(o1, o2)) {
			throw new IntegrityException("Order(" + o1.getOrderId() + ") not match between data source and runtime.");
		}
		var trades = cli.getTrades(o1.getOrderId());
		for (var tr0 : oc.getTrades()) {
			var found = false;
			for (var tr1 : trades) {
				if (tr1.getTradeId().compareTo(tr0.getTradeId()) == 0) {
					found = true;
					if (!equals(tr0, tr1)) {
						throw new IntegrityException(
								"Trade(" + tr1.getTradeId() + ") don't match between data source and runtime.");
					}
				}
			}
			if (!found) {
				throw new IntegrityException("Trade(" + tr0.getTradeId() + ") not found in data source.");
			}
		}
	}

	private boolean equals(Transaction t0, Transaction t1) {
		return t0.getTransactionId().equals(t1.getTransactionId()) && t0.getStrategyId().equals(t1.getStrategyId())
				&& t0.getInstrumentId().equals(t1.getInstrumentId()) && t0.getPrice().equals(t1.getPrice())
				&& t0.getVolume().equals(t1.getVolume()) && t0.getOffset().equals(t1.getOffset())
				&& t0.getDirection().equals(t1.getDirection()) && t0.getState().equals(t1.getState())
				&& t0.getStateMessage().equals(t1.getStateMessage()) && t0.getTradingDay().equals(t1.getTradingDay())
				&& t0.getUpdateTime().equals(t1.getUpdateTime());
	}

	private boolean equals(Order o1, Order o2) {
		return o1.getOrderId().equals(o2.getOrderId()) && o1.getTransactionId().equals(o2.getTransactionId())
				&& o1.getInstrumentId().equals(o2.getInstrumentId()) && o1.getPrice().equals(o2.getPrice())
				&& o1.getVolume().equals(o2.getVolume()) && o1.getDirection().equals(o2.getDirection())
				&& o1.getOffset().equals(o2.getOffset()) && o1.getTradingDay().equals(o2.getTradingDay());
	}

	private boolean equals(Trade tr0, Trade tr1) {
		return tr0.getTradeId().equals(tr1.getTradeId()) && tr0.getOrderId().equals(tr1.getOrderId())
				&& tr0.getInstrumentId().equals(tr1.getInstrumentId()) && tr0.getPrice().equals(tr1.getPrice())
				&& tr0.getVolume().equals(tr1.getVolume()) && tr0.getDirection().equals(tr1.getDirection())
				&& tr0.getTradingDay().equals(tr1.getTradingDay()) && tr0.getUpdateTime().equals(tr1.getUpdateTime());
	}

	void settle() throws IntegrityException {
		checkIntegrity();
		transactions.clear();
	}

	@Override
	public void initialize() {
		timedOnStart(prof.getArgs(), cli);
		isShutdown.set(false);
	}

	@Override
	public void shutdown(int reason) {
		isShutdown.set(true);
		timedOnStop(reason);
	}

	@Override
	public void destroy(int reason) throws StrategyDrestroyException {
		try {
			checkIntegrity();
		} catch (IntegrityException e) {
			throw new StrategyDrestroyException("Integrity check fails: " + e.getMessage(), e);
		}
		ctxes.remove(this);
		isShutdown.set(true);
		timedOnDestroy(reason);
		pool.shutdown();
	}

	@Override
	public StrategyProfile getPofile() {
		return prof;
	}

	@Override
	public Object getStrategy() {
		return annStg.getStrategy();
	}

	@Override
	public PlatirClient getPlatirClient() {
		return getPlatirClientImpl();
	}

	@Override
	public Set<TransactionContext> getTransactionContexts() {
		return new HashSet<>(transactions);
	}

	@FunctionalInterface
	private interface TimedJob {
		void work();
	}
}
