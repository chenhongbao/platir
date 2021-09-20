package io.platir.core.internals;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import io.platir.core.AnnotationParsingException;
import io.platir.service.Bar;
import io.platir.service.Notice;
import io.platir.service.PlatirClient;
import io.platir.service.PlatirQuery;
import io.platir.service.Strategy;
import io.platir.service.Tick;
import io.platir.service.Trade;
import io.platir.service.annotations.OnBar;
import io.platir.service.annotations.OnDestroy;
import io.platir.service.annotations.OnNotice;
import io.platir.service.annotations.OnStart;
import io.platir.service.annotations.OnStop;
import io.platir.service.annotations.OnTick;
import io.platir.service.annotations.OnTrade;

/**
 * Wrapper for invoking annotated methods or interface.
 * 
 * @author Chen Hongbao
 * @since 1.0.0
 */
class AnnotatedStrategy implements Strategy {

	private final AnnotatedStrategyInvoker invoker = new AnnotatedStrategyInvoker();
	private Strategy stg;

	/**
	 * Create a new strategy wrapper and parse the annotated methods if they are
	 * presented.
	 * 
	 * @param object An object that is a subclass of {@link Strategy} or has
	 *               annotated callback methods.
	 * @throws AnnotationParsingException if the specified object has errors in
	 *                                    annotation.
	 */
	AnnotatedStrategy(Object object) throws AnnotationParsingException {
		Objects.requireNonNull(object, "Constructed strategy is null.");
		invoker.parse(object);
		if (object instanceof Strategy) {
			stg = (Strategy) object;
		}
	}

	Object getStrategy() {
		if (stg != null) {
			return stg;
		} else {
			return invoker.getStrategy();
		}
	}

	@Override
	public void onTrade(Trade trade) {
		Objects.requireNonNull(trade, "Input trade is null.");
		if (stg != null) {
			stg.onTrade(trade);
		}
		invoker.invokeTrade(trade);
	}

	@Override
	public void onTick(Tick tick) {
		Objects.requireNonNull(tick, "Input tick is null.");
		if (stg != null) {
			stg.onTick(tick);
		}
		invoker.invokeTick(tick);
	}

	@Override
	public void onBar(Bar bar) {
		Objects.requireNonNull(bar, "Input bar is null.");
		if (stg != null) {
			stg.onBar(bar);
		}
		invoker.invokeBar(bar);
	}

	@Override
	public void onNotice(Notice notice) {
		Objects.requireNonNull(notice, "Input notice is null.");
		if (stg != null) {
			stg.onNotice(notice);
		}
		invoker.invokeNotice(notice);
	}

	@Override
	public void onStart(String[] args, PlatirClient platir) {
		Objects.requireNonNull(args, "Input arguments array is null.");
		Objects.requireNonNull(platir, "Input client is null.");
		if (stg != null) {
			stg.onStart(args, platir);
		}
		invoker.invokeStart(args, platir);
	}

	@Override
	public void onStop(int reason) {
		if (stg != null) {
			stg.onStop(reason);
		}
		invoker.invokeStop(reason);
	}

	@Override
	public void onDestroy() {
		if (stg != null) {
			stg.onDestroy();
		}
		invoker.invokeDestroy();
	}

	/*
	 * Internal class for parsing annotation and invoke annotated methods.
	 */
	private class AnnotatedStrategyInvoker {

		private Object obj;
		private Method start, stop, destroy, noticer;
		private Map<String, Method> trades = new ConcurrentHashMap<>();
		private Map<String, Method> ticks = new ConcurrentHashMap<>();
		private Map<String, Map<Integer, Method>> bars = new ConcurrentHashMap<>();

		AnnotatedStrategyInvoker() {
		}

		void parse(Object object) throws AnnotationParsingException {
			obj = object;
			for (Method m : obj.getClass().getMethods()) {
				parseMethod(m);
			}
		}

		private void parseMethod(Method m) throws AnnotationParsingException {
			if (parseOnBar(m) || parseOnTick(m) || parseOnTrade(m) || parseOnNotice(m) || parseOnStart(m)) {
				return;
			}
			parseOnStop(m);
			parseOnDestroy(m);
		}

		private boolean parseOnStart(Method m) throws AnnotationParsingException {
			if (checkMethodAndParameters(OnStart.class, m, String[].class, PlatirClient.class)) {
				if (start != null) {
					throw new AnnotationParsingException("Ambiguious OnStart annotation.");
				}
				start = m;
				return true;
			}
			return false;
		}

		private boolean parseOnNotice(Method m) throws AnnotationParsingException {
			if (checkMethodAndParameters(OnNotice.class, m, Notice.class)) {
				if (noticer != null) {
					throw new AnnotationParsingException("Ambiguious OnNotice annotation.");
				}
				noticer = m;
				return true;
			}
			return false;
		}

		private boolean parseOnTrade(Method m) throws AnnotationParsingException {
			if (checkMethodAndParameters(OnTrade.class, m, Trade.class)) {
				var a = m.getAnnotation(OnTrade.class);
				if (a.id().length == 0) {
					throw new AnnotationParsingException("No instrument ID specified for OnTrade annotation.");
				}
				for (String id : a.id()) {
					if (trades.containsKey(id)) {
						throw new AnnotationParsingException(
								"Ambiguious OnTrade annotation for instrument(" + id + ").");
					}
					trades.put(id, m);
				}
				return true;
			}
			return false;
		}

		private boolean parseOnTick(Method m) throws AnnotationParsingException {
			if (checkMethodAndParameters(OnTick.class, m, Tick.class)) {
				var a = m.getAnnotation(OnTick.class);
				if (a.id().length == 0) {
					throw new AnnotationParsingException("No instrument ID specified for OnTick annotation.");
				}
				for (String id : a.id()) {
					if (ticks.containsKey(id)) {
						throw new AnnotationParsingException(
								"Ambiguious OnTick annotation for instrument(" + id + ").");
					}
					ticks.put(id, m);
				}
				return true;
			}
			return false;
		}

		private boolean parseOnBar(Method m) throws AnnotationParsingException {
			if (checkMethodAndParameters(OnBar.class, m, Bar.class)) {
				var a = m.getAnnotation(OnBar.class);
				if (a.id().length == 0) {
					throw new AnnotationParsingException("No instrument ID specified for OnBar annotation.");
				}
				for (String id : a.id()) {
					var ms = bars.computeIfAbsent(id, k -> new ConcurrentHashMap<Integer, Method>());
					if (a.minute().length == 0) {
						if (ms.containsKey(0)) {
							throw new AnnotationParsingException("Ambiguious OnBar annotation for unspecified minute.");
						}
						ms.put(0, m);
					} else {
						for (int minute : a.minute()) {
							if (ms.containsKey(minute)) {
								throw new AnnotationParsingException(
										"Ambiguious OnBar annotation for minute(" + minute + ").");
							}
							ms.put(minute, m);
						}
					}
				}
				return true;
			}
			return false;
		}

		private void parseOnDestroy(Method m) throws AnnotationParsingException {
			if (checkMethodAndParameters(OnDestroy.class, m, int.class)) {
				if (destroy != null) {
					throw new AnnotationParsingException("Ambiguious OnDestroy annotation.");
				}
				destroy = m;
			}
		}

		private void parseOnStop(Method m) throws AnnotationParsingException {
			if (checkMethodAndParameters(OnStop.class, m, int.class)) {
				if (stop != null) {
					throw new AnnotationParsingException("Ambiguious OnStop annotation.");
				}
				stop = m;
			}
		}

		private <T extends Annotation> boolean checkMethodAndParameters(Class<T> annotationClass, Method m,
				Class<?>... paramTypes) {
			var a = m.getAnnotation(annotationClass);
			if (a == null || paramTypes.length != m.getParameterCount()) {
				return false;
			}
			var p = m.getParameterTypes();
			for (int i = 0; i < m.getParameterCount(); ++i) {
				if (paramTypes[i] != p[i]) {
					return false;
				}
			}
			return true;
		}

		void invokeStart(String[] args, PlatirQuery platir) {
			if (start != null) {
				try {
					start.invoke(obj, args, platir);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new RuntimeException("Fail invoking onStart(String[], PlatirClient).", e);
				}
			}
		}

		void invokeStop(int reason) {
			if (stop != null) {
				try {
					stop.invoke(obj, reason);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new RuntimeException("Fail invoking onStop(int).", e);
				}
			}
		}

		void invokeDestroy() {
			if (destroy != null) {
				try {
					destroy.invoke(obj);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new RuntimeException("Fail invoking onDestroy(int).", e);
				}
			}
		}

		void invokeTrade(Trade trade) {
			Objects.requireNonNull(trade.getInstrumentId(), "Trade instrument ID is null.");
			var m = trades.get(trade.getInstrumentId());
			if (m != null) {
				try {
					m.invoke(obj, trade);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new RuntimeException("Fail invoking onTrade(Trade).", e);
				}
			}
		}

		void invokeTick(Tick tick) {
			Objects.requireNonNull(tick.getInstrumentId(), "Tick instrument ID is null.");
			var m = ticks.get(tick.getInstrumentId());
			if (m != null) {
				try {
					m.invoke(obj, tick);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new RuntimeException("Fail invoking onTick(Tick).", e);
				}
			}
		}

		void invokeBar(Bar bar) {
			Objects.requireNonNull(bar.getInstrumentId(), "Bar instrument ID is null.");
			Objects.requireNonNull(bar.getMinute(), "Bar minute is null.");
			var set = bars.get(bar.getInstrumentId());
			if (set != null) {
				var m = set.get(bar.getMinute());
				if (m != null) {
					try {
						m.invoke(obj, bar);
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						throw new RuntimeException("Fail invoking onBar(Bar).", e);
					}
				}
			}
		}

		void invokeNotice(Notice notice) {
			if (noticer != null) {
				try {
					noticer.invoke(obj, notice);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new RuntimeException("Fail invoking onNotice(Notice).", e);
				}
			}
		}

		Object getStrategy() {
			return obj;
		}
	}
}
