package io.platir.core.internal;

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
import io.platir.service.Strategy;
import io.platir.service.Tick;
import io.platir.service.Trade;
import io.platir.service.annotation.OnBar;
import io.platir.service.annotation.OnDestroy;
import io.platir.service.annotation.OnNotice;
import io.platir.service.annotation.OnStart;
import io.platir.service.annotation.OnStop;
import io.platir.service.annotation.OnTick;
import io.platir.service.annotation.OnTrade;
import io.platir.service.PlatirInfoClient;

/**
 * Wrapper for invoking annotated methods or interface.
 *
 * @author Chen Hongbao
 * @since 1.0.0
 */
class CompositeStrategy implements Strategy {

    private final AnnotatedStrategyInvoker invoker = new AnnotatedStrategyInvoker();
    private Strategy strategy;

    CompositeStrategy(Object strategyObject) throws AnnotationParsingException {
        invoker.parse(strategyObject);
        if (strategyObject instanceof Strategy) {
            strategy = (Strategy) strategyObject;
        }
    }

    Object getStrategy() {
        if (strategy != null) {
            return strategy;
        } else {
            return invoker.getStrategy();
        }
    }

    @Override
    public void onTrade(Trade trade) {
        if (strategy != null) {
            strategy.onTrade(trade);
        }
        invoker.invokeTrade(trade);
    }

    @Override
    public void onTick(Tick tick) {
        if (strategy != null) {
            strategy.onTick(tick);
        }
        invoker.invokeTick(tick);
    }

    @Override
    public void onBar(Bar bar) {
        if (strategy != null) {
            strategy.onBar(bar);
        }
        invoker.invokeBar(bar);
    }

    @Override
    public void onNotice(Notice notice) {
        if (strategy != null) {
            strategy.onNotice(notice);
        }
        invoker.invokeNotice(notice);
    }

    @Override
    public void onStart(String[] args, PlatirClient platirClient) {
        if (strategy != null) {
            strategy.onStart(args, platirClient);
        }
        invoker.invokeStart(args, platirClient);
    }

    @Override
    public void onStop(int reason) {
        if (strategy != null) {
            strategy.onStop(reason);
        }
        invoker.invokeStop(reason);
    }

    @Override
    public void onDestroy() {
        if (strategy != null) {
            strategy.onDestroy();
        }
        invoker.invokeDestroy();
    }

    /*
     * Internal class for parsing annotation and invoke annotated methods.
     */
    private class AnnotatedStrategyInvoker {

        private Object target;
        private Method startMethod, stopMethod, destroyMethod, noticeMethod;
        private final Map<String, Method> tradeMethods = new ConcurrentHashMap<>();
        private final Map<String, Method> tickMethods = new ConcurrentHashMap<>();
        private final Map<String, Map<Integer, Method>> barMethods = new ConcurrentHashMap<>();

        AnnotatedStrategyInvoker() {
        }

        void parse(Object strategyObject) throws AnnotationParsingException {
            if (strategyObject == null) {
                throw new AnnotationParsingException("Strategy object is null.");
            }
            target = strategyObject;
            for (Method m : target.getClass().getMethods()) {
                parseMethod(m);
            }
        }

        private void parseMethod(Method method) throws AnnotationParsingException {
            if (parseOnBar(method) || parseOnTick(method) || parseOnTrade(method) || parseOnNotice(method) || parseOnStart(method)) {
                return;
            }
            parseOnStop(method);
            parseOnDestroy(method);
        }

        private boolean parseOnStart(Method method) throws AnnotationParsingException {
            if (checkMethodAndParameters(OnStart.class, method, String[].class, PlatirClient.class)) {
                if (startMethod != null) {
                    throw new AnnotationParsingException("Ambiguious OnStart annotation.");
                }
                startMethod = method;
                return true;
            }
            return false;
        }

        private boolean parseOnNotice(Method method) throws AnnotationParsingException {
            if (checkMethodAndParameters(OnNotice.class, method, Notice.class)) {
                if (noticeMethod != null) {
                    throw new AnnotationParsingException("Ambiguious OnNotice annotation.");
                }
                noticeMethod = method;
                return true;
            }
            return false;
        }

        private boolean parseOnTrade(Method method) throws AnnotationParsingException {
            if (checkMethodAndParameters(OnTrade.class, method, Trade.class)) {
                var a = method.getAnnotation(OnTrade.class);
                if (a.id().length == 0) {
                    tradeMethods.put("", method);
                }
                for (String id : a.id()) {
                    if (tradeMethods.containsKey(id)) {
                        throw new AnnotationParsingException("Ambiguious OnTrade annotation for instrument(" + id + ").");
                    }
                    tradeMethods.put(id, method);
                }
                return true;
            }
            return false;
        }

        private boolean parseOnTick(Method method) throws AnnotationParsingException {
            if (checkMethodAndParameters(OnTick.class, method, Tick.class)) {
                var annotation = method.getAnnotation(OnTick.class);
                if (annotation.id().length == 0) {
                    throw new AnnotationParsingException("No instrument ID specified for OnTick annotation.");
                }
                for (String id : annotation.id()) {
                    if (tickMethods.containsKey(id)) {
                        throw new AnnotationParsingException("Ambiguious OnTick annotation for instrument(" + id + ").");
                    }
                    tickMethods.put(id, method);
                }
                return true;
            }
            return false;
        }

        private boolean parseOnBar(Method method) throws AnnotationParsingException {
            if (checkMethodAndParameters(OnBar.class, method, Bar.class)) {
                var annotation = method.getAnnotation(OnBar.class);
                if (annotation.id().length == 0) {
                    throw new AnnotationParsingException("No instrument ID specified for OnBar annotation.");
                }
                for (String id : annotation.id()) {
                    var ms = barMethods.computeIfAbsent(id, k -> new ConcurrentHashMap<Integer, Method>());
                    if (annotation.minute().length == 0) {
                        if (ms.containsKey(0)) {
                            throw new AnnotationParsingException("Ambiguious OnBar annotation for unspecified minute.");
                        }
                        ms.put(0, method);
                    } else {
                        for (int minute : annotation.minute()) {
                            if (ms.containsKey(minute)) {
                                throw new AnnotationParsingException("Ambiguious OnBar annotation for minute(" + minute + ").");
                            }
                            ms.put(minute, method);
                        }
                    }
                }
                return true;
            }
            return false;
        }

        private void parseOnDestroy(Method method) throws AnnotationParsingException {
            if (checkMethodAndParameters(OnDestroy.class, method)) {
                if (destroyMethod != null) {
                    throw new AnnotationParsingException("Ambiguious OnDestroy annotation.");
                }
                destroyMethod = method;
            }
        }

        private void parseOnStop(Method method) throws AnnotationParsingException {
            if (checkMethodAndParameters(OnStop.class, method, int.class)) {
                if (stopMethod != null) {
                    throw new AnnotationParsingException("Ambiguious OnStop annotation.");
                }
                stopMethod = method;
            }
        }

        private <T extends Annotation> boolean checkMethodAndParameters(Class<T> annotationClass, Method m, Class<?>... paramTypes) {
            var annotation = m.getAnnotation(annotationClass);
            if (annotation == null || paramTypes.length != m.getParameterCount()) {
                return false;
            }
            var types = m.getParameterTypes();
            for (int i = 0; i < m.getParameterCount(); ++i) {
                if (paramTypes[i] != types[i]) {
                    return false;
                }
            }
            return true;
        }

        void invokeStart(String[] args, PlatirInfoClient infoClient) {
            if (startMethod != null) {
                try {
                    startMethod.invoke(target, args, infoClient);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    throw new RuntimeException("Fail invoking onStart(String[], PlatirClient).", e);
                }
            }
        }

        void invokeStop(int reason) {
            if (stopMethod != null) {
                try {
                    stopMethod.invoke(target, reason);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
                    throw new RuntimeException("Fail invoking onStop(int).", exception);
                }
            }
        }

        void invokeDestroy() {
            if (destroyMethod != null) {
                try {
                    destroyMethod.invoke(target);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
                    throw new RuntimeException("Fail invoking onDestroy(int).", exception);
                }
            }
        }

        void invokeTrade(Trade trade) {
            Objects.requireNonNull(trade.getInstrumentId(), "Trade instrument ID is null.");
            callTrade(trade, tradeMethods.get(trade.getInstrumentId()));
            callTrade(trade, tradeMethods.get(""));
        }

        private void callTrade(Trade trade, Method method) {
            if (method != null) {
                try {
                    method.invoke(target, trade);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
                    throw new RuntimeException("Fail invoking onTrade(Trade).", exception);
                }
            }
        }

        void invokeTick(Tick tick) {
            Objects.requireNonNull(tick.getInstrumentId(), "Tick instrument ID is null.");
            var method = tickMethods.get(tick.getInstrumentId());
            if (method != null) {
                try {
                    method.invoke(target, tick);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
                    throw new RuntimeException("Fail invoking onTick(Tick).", exception);
                }
            }
        }

        void invokeBar(Bar bar) {
            Objects.requireNonNull(bar.getInstrumentId(), "Bar instrument ID is null.");
            Objects.requireNonNull(bar.getMinute(), "Bar minute is null.");
            var methods = barMethods.get(bar.getInstrumentId());
            if (methods != null) {
                callBar(bar, methods.get(bar.getMinute()));
                /* Always call minute = 0 */
                callBar(bar, methods.get(0));
            }
        }

        private void callBar(Bar bar, Method method) {
            if (method != null) {
                try {
                    method.invoke(target, bar);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
                    throw new RuntimeException("Fail invoking onBar(Bar).", exception);
                }
            }
        }

        void invokeNotice(Notice notice) {
            if (noticeMethod != null) {
                try {
                    noticeMethod.invoke(target, notice);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
                    throw new RuntimeException("Fail invoking onNotice(Notice).", exception);
                }
            }
        }

        Object getStrategy() {
            return target;
        }
    }
}
