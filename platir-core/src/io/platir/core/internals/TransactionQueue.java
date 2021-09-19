package io.platir.core.internals;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import io.platir.core.PlatirSystem;
import io.platir.service.Contract;
import io.platir.service.Notice;
import io.platir.service.Order;
import io.platir.service.Tick;
import io.platir.service.Trade;
import io.platir.service.Transaction;
import io.platir.service.api.RiskAssess;
import io.platir.service.api.TradeAdaptor;
import io.platir.service.api.TradeListener;

/**
 * Error code explanation:
 * <ul>
 * <li>1001: Available money is zero or below zero.
 * <li>1002: Missing instrument information.
 * <li>1003: Not enough available money to open.
 * <li>1004: Not enough position to close.
 * </ul>
 * 
 * @author Chen Hongbao
 * @since 1.0.0
 */
class TransactionQueue implements Runnable {
	private final RiskAssess rsk;
	private final TradeAdaptor tr;
	private final AtomicInteger increId = new AtomicInteger(0);
	private final BlockingQueue<TransactionContextImpl> queueing = new LinkedBlockingQueue<>();
	private final Set<TransactionContextImpl> pending = new ConcurrentSkipListSet<>();

	TransactionQueue(TradeAdaptor trader, RiskAssess risk) {
		tr = trader;
		rsk = risk;
	}

	void settle() {
		pending.clear();
		queueing.clear();
		increId.set(0);
	}

	int countTransactionRunning(StrategyContextImpl strategy) {
		int count = 0;
		count += queueing.stream().mapToInt(tr -> tr.getStrategyContext() == strategy ? 1 : 0).sum();
		count += pending.stream().mapToInt(tr -> tr.getStrategyContext() == strategy ? 1 : 0).sum();
		return count;
	}

	void push(TransactionContextImpl ctx) {
		var t = ctx.getTransaction();
		/* Update states. */
		t.setState("pending");
		t.setStateMessage("never enqueued");
		// Initialize adding transaction to data source.
		ctx.getQuery().add(t);
		pending.add(ctx);
	}

	void awake(Tick tick) {
		var id = tick.getInstrumentId();
		var it = pending.iterator();
		while (it.hasNext()) {
			var t = it.next();
			if (t.getTransaction().getInstrumentId().compareTo(id) == 0) {
				it.remove();
				/* Change state. */
				t.getTransaction().setState("queueing");
				t.getTransaction().setStateMessage("tick triggers queueing");
				t.getQuery().update(t.getTransaction());
				/* Set trigger tick. */
				t.setTriggerTick(tick);
				if (!queueing.offer(t)) {
					/*
					 * if it can't offer transaction to be executed, don't check more transaction.
					 */
					PlatirSystem.err.write("Transaction queueing queue is full.");
					break;
				}
			}
		}
	}

	private Set<Contract> opening(String orderId, PlatirQueryImpl query, Transaction transaction) {
		/*
		 * Add contracts for opening. The opening margin and commission are computed
		 * through the opening contracts, so just add opening contracts and account will
		 * be changed.
		 */
		var r = new HashSet<Contract>();
		var uid = query.getStrategyProfile().getUserId();
		for (int i = 0; i < transaction.getVolume(); ++i) {
			var c = new Contract();
			/*
			 * Contract ID = <order-id>.<some-digits>
			 */
			c.setContractId(orderId + "." + Integer.toString(i));
			c.setUserId(uid);
			c.setInstrumentId(transaction.getInstrumentId());
			c.setDirection(transaction.getDirection());
			c.setPrice(transaction.getPrice());
			c.setState("opening");
			c.setOpenTradingDay(query.getTradingDay());
			c.setOpenTime(PlatirSystem.datetime());
			query.add(c);
			r.add(c);
		}
		return r;
	}

	private Notice checkOpen(String oid, PlatirQueryImpl query, Transaction t) {
		var r = new Notice();
		var available = query.getAccount().getAvailable();
		if (available <= 0) {
			r.setCode(1001);
			r.setMessage("no available(" + available + ") for opening");
			return r;
		}
		var instrument = query.getInstrument(t.getInstrumentId());
		if (instrument == null) {
			r.setCode(1002);
			r.setMessage("no instrument information for " + t.getInstrumentId());
			return r;
		}
		var margin = SettlementFacilities.computeRatio(t.getPrice(), instrument.getMultiple(),
				instrument.getAmountMargin(), instrument.getVolumeMargin()) * t.getVolume();
		var commission = SettlementFacilities.computeRatio(t.getPrice(), instrument.getMultiple(),
				instrument.getAmountCommission(), instrument.getVolumeCommission()) * t.getVolume();
		if (available < margin + commission) {
			r.setCode(1003);
			r.setMessage("no available(" + available + ") for opening(" + (commission + margin) + ")");
			return r;
		}

		r.setCode(0);
		r.setMessage("good");
		/* Lock contracts for opening and return those contracts. */
		r.setObject(opening(oid, query, t));
		return r;
	}

	private OrderContextImpl createOrderContext(String orderId, String transactionId, String instrumentId, Double price,
			Integer volume, String direction, Collection<Contract> contracts, String offset,
			TransactionContextImpl transCtx) {
		var query = transCtx.getStrategyContext().getPlatirClientImpl();
		var o = new Order();
		o.setOrderId(orderId);
		o.setTransactionId(transactionId);
		o.setInstrumentId(instrumentId);
		o.setPrice(price);
		o.setVolume(volume);
		o.setDirection(direction);
		o.setOffset(offset);
		o.setTradingDay(query.getTradingDay());
		/* save order to data source */
		query.add(o);
		/* create order context. */
		var ctx = new OrderContextImpl(o);
		ctx.lockedContracts().addAll(contracts);
		/* add order context to transaction context */
		transCtx.addOrderContext(ctx);
		return ctx;
	}

	private String getOrderId(String tid) {
		/* <transaction-id>.<some-digits> */
		return tid + "." + Integer.toString(increId.incrementAndGet());
	}

	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted()) {
			try {
				var ctx = queueing.poll(24, TimeUnit.HOURS);
				var t = ctx.getTransaction();
				/* In-front risk assessment. */
				var r = rsk.before(ctx.getLastTriggerTick(), ctx.getTransaction(), ctx.getQuery());
				if (!r.isGood()) {
					t.setState("in-front-risk-accessment;" + r.getCode());
					t.setStateMessage(r.getMessage());
					ctx.awake();
				} else {
					if (!ctx.pendingOrder().isEmpty()) {
						/* the transaction has been processed but order is not completed. */
						sendPending(ctx);
					} else {
						if ("open".compareToIgnoreCase(t.getOffset()) == 0) {
							open(ctx);
						} else if ("close".compareToIgnoreCase(t.getOffset()) == 0) {
							close(ctx);
						} else {
							t.setState("invalid");
							t.setStateMessage("invalid offset(" + t.getOffset() + ")");
							ctx.getQuery().update(t);
							/* Notify the transaction has failed. */
							ctx.awake();
						}
					}
				}
			} catch (InterruptedException e) {
				PlatirSystem.err.write("Transaction queue worker thread is interrupted.", e);
			} catch (Throwable th) {
				PlatirSystem.err.write("Uncaught error: " + th.getMessage(), th);
			}
		}
	}

	private void close(TransactionContextImpl ctx) {
		var t = ctx.getTransaction();
		var oid = getOrderId(t.getTransactionId());
		var query = ctx.getQuery();
		var r = checkClose(ctx.getQuery(), t.getInstrumentId(), t.getDirection(), t.getVolume());
		if (!r.isGood()) {
			t.setState("check-close;" + r.getCode());
			t.setStateMessage(r.getMessage());
			query.update(t);
			ctx.awake();
		} else {
			@SuppressWarnings("unchecked")
			var contracts = (Collection<Contract>) r.getObject();
			var tradingDay = query.getTradingDay();
			/* process today's contracts */
			var today = contracts.stream().filter(c -> c.getOpenTradingDay().equals(tradingDay) ? true : false)
					.collect(Collectors.toSet());
			var orderCtxToday = createOrderContext(oid, t.getTransactionId(), t.getInstrumentId(), t.getPrice(),
					t.getVolume(), t.getDirection(), today, "close-today", ctx);
			/* Set relation of order and contracts of today. */
			for (var c : today) {
				query.map(orderCtxToday.getOrder(), c);
			}
			send(orderCtxToday, ctx);

			/* process history contracts */
			var history = contracts.stream().filter(c -> !today.contains(c)).collect(Collectors.toSet());
			var orderCtxHistory = createOrderContext(oid, t.getTransactionId(), t.getInstrumentId(), t.getPrice(),
					t.getVolume(), t.getDirection(), history, "close-history", ctx);
			/* Set relation of order and contracts of today. */
			for (var c : history) {
				query.map(orderCtxHistory.getOrder(), c);
			}
			send(orderCtxHistory, ctx);
		}
	}

	private Notice checkClose(PlatirQueryImpl query, String instrumentId, String direction, Integer volume) {
		/* buy-open for sell-closed, sell-open for buy-closed */
		var r = new Notice();
		var available = query.getContracts(instrumentId).stream()
				.filter(c -> c.getDirection().compareToIgnoreCase(direction) != 0)
				.filter(c -> c.getState().compareToIgnoreCase("open") == 0 ? true : false).collect(Collectors.toSet());
		if (available.size() < volume) {
			r.setCode(1004);
			r.setMessage("no available contracts(" + available.size() + ") for closing(" + volume + ")");
			return r;
		}
		/*
		 * Remove extra contracts from container until it only has the contracts for
		 * closing and lock those contracts.
		 */
		while (available.size() > volume) {
			var h = available.iterator().next();
			available.remove(h);
		}
		closing(available, query);

		r.setCode(0);
		r.setMessage("good");
		r.setObject(available);
		return r;
	}

	private void closing(Set<Contract> available, PlatirQueryImpl query) {
		for (var c : available) {
			c.setState("closing");
			query.update(c);
		}
	}

	private void open(TransactionContextImpl ctx) {
		var t = ctx.getTransaction();
		var oid = getOrderId(t.getTransactionId());
		var query = ctx.getQuery();
		/* Check resource. */
		var r = checkOpen(oid, query, t);
		if (!r.isGood()) {
			t.setState("check-open;" + r.getCode());
			t.setStateMessage(r.getMessage());
			query.update(t);
			/* notify joiner the transaction fails. */
			ctx.awake();
		} else {
			/* Lock resource for opening. */
			@SuppressWarnings("unchecked")
			var contracts = (Collection<Contract>) r.getObject();
			var orderCtx = createOrderContext(oid, t.getTransactionId(), t.getInstrumentId(), t.getPrice(),
					t.getVolume(), t.getDirection(), contracts, "open", ctx);
			/* Set relation of order and contracts. */
			for (var c : contracts) {
				query.map(orderCtx.getOrder(), c);
			}
			send(orderCtx, ctx);
		}
	}

	private void sendPending(TransactionContextImpl ctx) {
		var it = ctx.pendingOrder().iterator();
		while (it.hasNext()) {
			var orderCtx = it.next();
			it.remove();
			/* send order until error. */
			if (!send(orderCtx, ctx).isGood()) {
				break;
			}
		}
	}

	private Notice send(OrderContextImpl orderCtx, TransactionContextImpl ctx) {
		/*
		 * Precondition: order context is not on transaction's pending order list.
		 * 
		 * If order is successful, map order to its locked contracts, or add order
		 * context to pending list of the transaction, and put transaction to queue's
		 * pending list.
		 */
		var t = ctx.getTransaction();
		var query = ctx.getQuery();
		var order = orderCtx.getOrder();
		var tl = new SyncTradeListener(orderCtx, ctx.getStrategyContext());
		tr.require(order.getOrderId(), order.getInstrumentId(), order.getOffset(), order.getDirection(),
				order.getPrice(), order.getVolume(), tl);
		/* Wait for the first response telling if the order is accepted. */
		var ro = tl.waitResponse();
		if (!ro.isGood()) {
			/* Update state. */
			t.setState("pending;" + ro.getCode());
			t.setStateMessage(ro.getMessage());
			query.update(t);
			/*
			 * Put order context to pending list of the transaction, and put transaction to
			 * queue's pending list.
			 */
			ctx.pendingOrder().add(orderCtx);
			pending.add(ctx);
		} else {
			t.setState("running");
			t.setStateMessage("order submitted");
			query.update(t);
		}
		return ro;
	}

	/**
	 * Error code explanation:
	 * <ul>
	 * <li>3001: Trade response timeout.
	 * <li>3002: Over open/close position.
	 * </ul>
	 */
	private class SyncTradeListener implements TradeListener {

		private final AtomicInteger count = new AtomicInteger(0);
		private final AtomicReference<Notice> res = new AtomicReference<>();
		private final Lock l = new ReentrantLock();
		private final Condition cond = l.newCondition();
		private final int timeoutSec = 5;
		/* when order is completed, set references to null so GC collect the objects */
		private OrderContextImpl oCtx;
		private StrategyContextImpl stgCtx;

		SyncTradeListener(OrderContextImpl order, StrategyContextImpl strategy) {
			oCtx = order;
			stgCtx = strategy;
		}

		Notice waitResponse() {
			l.lock();
			try {
				cond.await(timeoutSec, TimeUnit.SECONDS);
				return res.get();
			} catch (InterruptedException e) {
				var r = new Notice();
				r.setCode(3001);
				r.setMessage("fail waiting for trade response");
				r.setObject(e);
				return r;
			} finally {
				l.unlock();
			}
		}

		@Override
		public void onTrade(Trade trade) {
			/* save trade to data source. */
			stgCtx.getPlatirClientImpl().add(trade);
			/* add trade to order context. */
			oCtx.addTrade(trade);
			/* update contracts' states */
			updateContracts(trade);
			stgCtx.timedOnTrade(trade);
			checkCompleted(trade.getVolume());
		}

		private void updateContracts(Trade trade) {
			var count = 0;
			var it = oCtx.lockedContracts().iterator();
			while (++count <= trade.getVolume() && it.hasNext()) {
				var c = it.next();
				var prevState = c.getState();
				if (c.getState().compareToIgnoreCase("opening") == 0) {
					/* Update open price because the real traded price may be different. */
					c.setState("open");
					c.setPrice(trade.getPrice());
					c.setOpenTime(PlatirSystem.datetime());
					c.setOpenTradingDay(stgCtx.getPlatirClientImpl().getTradingDay());
				} else if (c.getState().compareToIgnoreCase("closing") == 0) {
					/* don't forget the close price here */
					c.setState("closed");
					c.setClosePrice(trade.getPrice());
				} else {
					PlatirSystem.err.write("Incorrect contract state(" + c.getState() + "/" + c.getContractId()
							+ ") before completing trade.");
					continue;
				}
				try {
					stgCtx.getPlatirClientImpl().update(c);
				} catch (Throwable th) {
					PlatirSystem.err
							.write("Fail updating contract(" + c.getContractId() + ") for " + c.getState() + ".", th);

					/* roll back state */
					c.setState(prevState);
					continue;
				}
				it.remove();
			}
			if (count <= trade.getVolume()) {
				PlatirSystem.err.write("Fail updating data source for trades(still need "
						+ (trade.getVolume() - count + 1) + " more).");
			}
		}

		private void timedOnNotice(int code, String message) {
			timedOnNotice(code, message, null);
		}

		private void timedOnNotice(int code, String message, Throwable error) {
			var n = new Notice();
			n.setCode(code);
			n.setMessage(message);
			n.setObject(error);
			stgCtx.timedOnNotice(n);
		}

		private void checkCompleted(int addedVolume) {
			var cur = count.addAndGet(addedVolume);
			var vol = oCtx.getOrder().getVolume();
			if (cur >= vol) {
				/* let garbage collection reclaim the objects */
				oCtx = null;
				stgCtx = null;
			}
			if (cur == vol) {
				timedOnNotice(0, "trade completed");
			} else if (cur > vol) {
				timedOnNotice(3002, "over traded(" + cur + ">" + vol + ")");
			}
		}

		@Override
		public void onError(int code, String message) {
			if (res.get() == null) {
				signalJoiner(code, message);
			}
			timedOnNotice(code, message);
		}

		private void signalJoiner(int code, String message) {
			var r = new Notice();
			r.setCode(code);
			r.setMessage(message);
			res.set(r);

			l.lock();
			try {
				cond.signal();
			} finally {
				l.unlock();
			}
		}
	}
}
