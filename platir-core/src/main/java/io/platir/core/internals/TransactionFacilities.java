package io.platir.core.internals;

import io.platir.core.PlatirSystem;
import io.platir.core.internals.persistence.object.ObjectFactory;
import io.platir.service.Contract;
import io.platir.service.Instrument;
import io.platir.service.Notice;
import io.platir.service.Order;
import io.platir.service.RiskNotice;
import io.platir.service.StrategyProfile;
import io.platir.service.Transaction;
import io.platir.service.api.DataQueryException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 *
 * @author Chen Hongbao
 */
class TransactionFacilities {

    private final static AtomicInteger increId = new AtomicInteger(0);

    static Set<Contract> opening(String orderId, PlatirInfoClientImpl client, Transaction transaction) {
        /*
         * Add contracts for opening. The opening margin and commission are computed
         * through the opening contracts, so just add opening contracts and account will
         * be changed.
         */
        HashSet<Contract> r = new HashSet<>();
        String uid = client.getStrategyProfile().getUserId();
        for (int i = 0; i < transaction.getVolume(); ++i) {
            Contract c = ObjectFactory.newContract();
            /*
             * Contract ID = <order-id>.<some-digits>
             */
            c.setContractId(orderId + "." + Integer.toString(i));
            c.setUserId(uid);
            c.setInstrumentId(transaction.getInstrumentId());
            c.setDirection(transaction.getDirection());
            c.setPrice(transaction.getPrice());
            c.setState("opening");
            c.setOpenTradingDay(client.getTradingDay());
            c.setOpenTime(PlatirSystem.datetime());
            r.add(c);
            try {
                client.queries().insert(c);
            } catch (DataQueryException e) {
                PlatirSystem.err.write("Can't insert user(" + c.getUserId() + ") contract(" + c.getContractId() + ") opening: " + e.getMessage(), e);
            }
        }
        return r;
    }

    static Notice checkOpen(String oid, PlatirInfoClientImpl query, Transaction t) {
        Notice r = ObjectFactory.newNotice();
        Double available = query.getAccount().getAvailable();
        if (available <= 0) {
            r.setCode(1001);
            r.setMessage("no available(" + available + ") for opening");
            return r;
        }
        Instrument instrument = query.getInstrument(t.getInstrumentId());
        if (instrument == null) {
            r.setCode(1002);
            r.setMessage("no instrument information for " + t.getInstrumentId());
            return r;
        }
        double margin = SettlementFacilities.computeRatio(t.getPrice(), instrument.getMultiple(), instrument.getAmountMargin(), instrument.getVolumeMargin()) * t.getVolume();
        double commission = SettlementFacilities.computeRatio(t.getPrice(), instrument.getMultiple(), instrument.getAmountCommission(), instrument.getVolumeCommission()) * t.getVolume();
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

    static Notice checkClose(PlatirInfoClientImpl query, String instrumentId, String direction, Integer volume) {
        /* buy-open for sell-closed, sell-open for buy-closed */
        Notice r = ObjectFactory.newNotice();
        Set<Contract> available = query.getContracts(instrumentId).stream().filter(c -> c.getDirection().compareToIgnoreCase(direction) != 0).filter(c -> c.getState().compareToIgnoreCase("open") == 0).collect(Collectors.toSet());
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
            Contract h = available.iterator().next();
            available.remove(h);
        }
        closing(available, query);
        /* return good */
        r.setCode(0);
        r.setMessage("good");
        r.setObject(available);
        return r;
    }

    static void closing(Set<Contract> available, PlatirInfoClientImpl client) {
        available.stream().map(c -> {
            c.setState("closing");
            return c;
        }).forEachOrdered(c -> {
            try {
                client.queries().update(c);
            } catch (DataQueryException e) {
                PlatirSystem.err.write("Can't update user(" + c.getUserId() + ") + contract(" + c.getContractId() + ") state(" + c.getState() + "): " + e.getMessage(), e);
            }
        });
    }

    static OrderContextImpl createOrderContext(String orderId, String transactionId, String instrumentId, Double price, Integer volume, String direction, Collection<Contract> contracts, String offset, TransactionContextImpl transCtx) {
        PlatirClientImpl cli = transCtx.getStrategyContext().getPlatirClientImpl();
        Order o = ObjectFactory.newOrder();
        o.setOrderId(orderId);
        o.setTransactionId(transactionId);
        o.setInstrumentId(instrumentId);
        o.setPrice(price);
        o.setVolume(volume);
        o.setDirection(direction);
        o.setOffset(offset);
        o.setTradingDay(cli.getTradingDay());
        try {
            /* save order to data source */
            cli.queries().insert(o);
        } catch (DataQueryException e) {
            /* worker thread can't pass out the exception, just log it */
            PlatirSystem.err.write("Can't insert order(" + o.getOrderId() + ") to data source: " + e.getMessage(), e);
        }
        /* create order context. */
        OrderContextImpl ctx = new OrderContextImpl(o, transCtx);
        ctx.lockedContracts().addAll(contracts);
        /* add order context to transaction context */
        transCtx.addOrderContext(ctx);
        return ctx;
    }

    static String getOrderId(String tid) {
        /* <transaction-id>.<some-digits> */
        return tid + "." + Integer.toString(increId.incrementAndGet());
    }

    static void saveRiskNotice(int code, String message, Integer level, TransactionContextImpl ctx) {
        RiskNotice r = ObjectFactory.newRiskNotice();
        StrategyProfile profile = ctx.getStrategyContext().getProfile();
        r.setCode(code);
        r.setMessage(message);
        r.setLevel(level);
        r.setUserId(profile.getUserId());
        r.setStrategyId(profile.getStrategyId());
        r.setUpdateTime(PlatirSystem.datetime());
        try {
            ctx.getQueryClient().queries().insert(r);
        } catch (DataQueryException e) {
            PlatirSystem.err.write("Can't inert RiskNotice(" + code + ", " + message + "): " + e.getMessage(), e);
        }
    }

    static void processNotice(int code, String message, TransactionContextImpl ctx) {
        Notice n = ObjectFactory.newNotice();
        n.setCode(code);
        n.setMessage(message);
        ctx.getStrategyContext().processNotice(n);
    }

}
