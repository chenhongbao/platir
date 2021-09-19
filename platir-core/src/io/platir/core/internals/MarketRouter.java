package io.platir.core.internals;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import io.platir.core.PlatirSystem;
import io.platir.service.Tick;
import io.platir.service.api.MarketAdaptor;
import io.platir.service.api.MarketListener;

class MarketRouter implements MarketListener {
	private final Map<StrategyContextImpl, TickDaemon> daemons = new ConcurrentHashMap<>();
	private final Map<String, Set<StrategyContextImpl>> subs = new ConcurrentHashMap<>();
	private final Map<String, Tick> ticks = new ConcurrentHashMap<>();
	private final ExecutorService es = Executors.newCachedThreadPool();
	private final TransactionQueue trQueue;
	private final MarketAdaptor adaptor;

	MarketRouter(MarketAdaptor market, TransactionQueue queue) {
		adaptor = market;
		trQueue = queue;
	}

	Set<Tick> getLastTicks() {
		return new HashSet<>(ticks.values());
	}

	void refreshSubscription() {
		MarketListener ml = this;
		subs.keySet().forEach(key -> {
			var tick = ticks.get(key);
			if (tick != null) {
				var datetime = PlatirSystem.datetime(tick.getUpdateTime());
				/* if tick doesn't arrive for over 30 days, the instrument has expired. */
				if (Duration.between(datetime, LocalDateTime.now()).toDays() > 15) {
					return;
				}
			}
			adaptor.add(key, ml);
		});
	}

	void subscribe(StrategyContextImpl strategy) {
		MarketListener ml = this;
		for (var i : strategy.getPofile().getInstrumentIds()) {
			var p = subs.computeIfAbsent(i, key -> {
				adaptor.add(key, ml);
				createDaemon(strategy);
				return new ConcurrentSkipListSet<>();
			});
			p.add(strategy);
		}
	}

	void remove(StrategyContextImpl strategy) {
		for (var i : strategy.getPofile().getInstrumentIds()) {
			var p = subs.get(i);
			if (p != null) {
				p.remove(strategy);
			}
			/* if no strategy subscribes the instrument, remove it from market */
			if (p.isEmpty()) {
				subs.remove(i);
			}
		}
		var dm = daemons.remove(strategy);
		if (dm != null) {
			dm.abort();
		}
	}

	@Override
	public void onTick(Tick tick) {
		var x = subs.get(tick.getInstrumentId());
		x.parallelStream().forEach(ctx -> {
			if (daemons.get(ctx) == null) {
				createDaemon(ctx);
			}
			daemons.get(ctx).push(tick);
		});
		/* signal transaction queue to work on pending transactions. */
		tryAwake(tick);
		/* save ticks for settlement */
		ticks.put(tick.getInstrumentId(), tick);
	}

	private void tryAwake(Tick tick) {
		var ut = tick.getUpdateTime();
		if (ut.length() != 17) {
			PlatirSystem.err.write("Malformed update time " + ut + ".");
			return;
		}
		var sec = tick.getUpdateTime().substring(15, 16);
		if (sec.equals("00") || sec.equals("59")) {
			/*
			 * don't awake transaction at the edge of a minute, it may be the end of a
			 * session
			 */
			return;
		}
		trQueue.awake(tick);
	}

	private void createDaemon(StrategyContextImpl ctx) {
		var d = new TickDaemon(ctx);
		var fut = es.submit(d);
		d.setFuture(fut);
		daemons.put(ctx, d);
	}

	private class TickDaemon implements Runnable {
		private final BlockingQueue<Tick> ticks = new LinkedBlockingQueue<>();
		private StrategyContextImpl ctx;
		private Future<?> future;

		TickDaemon(StrategyContextImpl ctx) {
			this.ctx = ctx;
		}

		void push(Tick tick) {
			if (!ticks.offer(tick)) {
				PlatirSystem.err.write("Tick queue is full.");
			}
		}

		void setFuture(Future<?> fut) {
			future = fut;
		}

		void abort() {
			if (future != null && !future.isDone()) {
				future.cancel(true);
			}
		}

		@Override
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				try {
					ctx.timedOnTick(ticks.poll(24, TimeUnit.HOURS));
				} catch (InterruptedException e) {
					PlatirSystem.err.write("Strategy(" + ctx.getPofile().getStrategyId() + ") onTick(Tick) timeout.");
				} catch (Throwable th) {
					PlatirSystem.err.write("Uncaught error: " + th.getMessage(), th);
				}
			}
			PlatirSystem.err.write("Tick daemon for strategy(" + ctx.getPofile().getStrategyId() + ") is about to exit.");
		}
	}
}
