package io.platir.core.internal;

import io.platir.queries.Utils;
import io.platir.service.Constants;
import io.platir.service.Contract;
import io.platir.service.Instrument;
import io.platir.service.Notice;
import io.platir.service.Order;
import io.platir.service.RiskNotice;
import io.platir.service.StrategyProfile;
import io.platir.service.Transaction;
import io.platir.service.DataQueryException;
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

    private final static AtomicInteger orderIdCounter = new AtomicInteger(0);

    static Set<Contract> opening(String orderId, PlatirInfoClientImpl client, Transaction transaction) {
        /*
         * Add contracts for opening. The opening margin and commission are computed
         * through the opening contracts, so just add opening contracts and account will
         * be changed.
         */
        HashSet<Contract> contracts = new HashSet<>();
        String userId = client.getStrategyProfile().getUserId();
        for (int i = 0; i < transaction.getVolume(); ++i) {
            Contract contract = client.queries().getFactory().newContract();
            /*
             * Contract ID = <order-id>.<some-digits>
             */
            contract.setContractId(orderId + "." + Integer.toString(i));
            contract.setUserId(userId);
            contract.setInstrumentId(transaction.getInstrumentId());
            contract.setDirection(transaction.getDirection());
            contract.setPrice(transaction.getPrice());
            contract.setState("opening");
            contract.setOpenTradingDay(client.getTradingDay());
            contract.setOpenTime(Utils.datetime());
            contracts.add(contract);
            try {
                client.queries().insert(contract);
            } catch (DataQueryException e) {
                Utils.err.write("Can't insert user(" + contract.getUserId() + ") contract(" + contract.getContractId() + ") opening: " + e.getMessage(), e);
            }
        }
        return contracts;
    }

    static CheckReturn checkOpen(String oid, PlatirInfoClientImpl query, Transaction t) {
        var checkReturn = new CheckReturn();
        Double available = query.getAccount().getAvailable();
        if (available <= 0) {
            checkReturn.getNotice().setCode(Constants.CODE_INVALID_AVAILABLE);
            checkReturn.getNotice().setMessage("no available(" + available + ") for opening");
            return checkReturn;
        }
        Instrument instrument = query.getInstrument(t.getInstrumentId());
        if (instrument == null) {
            checkReturn.getNotice().setCode(Constants.CODE_NO_INSTRUMENT);
            checkReturn.getNotice().setMessage("no instrument information for " + t.getInstrumentId());
            return checkReturn;
        }
        double margin = SettlementFacilities.computeRatio(t.getPrice(), instrument.getMultiple(), instrument.getAmountMargin(), instrument.getVolumeMargin()) * t.getVolume();
        double commission = SettlementFacilities.computeRatio(t.getPrice(), instrument.getMultiple(), instrument.getAmountCommission(), instrument.getVolumeCommission()) * t.getVolume();
        if (available < margin + commission) {
            checkReturn.getNotice().setCode(Constants.CODE_NO_MONEY);
            checkReturn.getNotice().setMessage("no available(" + available + ") for opening(" + (commission + margin) + ")");
            return checkReturn;
        }
        checkReturn.getNotice().setCode(Constants.CODE_OK);
        checkReturn.getNotice().setMessage("good");
        /* Lock contracts for opening and return those contracts. */
        checkReturn.getContracts().addAll(opening(oid, query, t));
        return checkReturn;
    }

    static CheckReturn checkClose(PlatirInfoClientImpl query, String instrumentId, String direction, Integer volume) {
        /* buy-open for sell-closed, sell-open for buy-closed */
        var checkReturn = new CheckReturn();
        Set<Contract> available = query.getContracts(instrumentId).stream().filter(c -> c.getDirection().compareToIgnoreCase(direction) != 0).filter(c -> c.getState().compareToIgnoreCase("open") == 0).collect(Collectors.toSet());
        if (available.size() < volume) {
            checkReturn.getNotice().setCode(Constants.CODE_NO_POSITION);
            checkReturn.getNotice().setMessage("no available contracts(" + available.size() + ") for closing(" + volume + ")");
            return checkReturn;
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
        checkReturn.getNotice().setCode(Constants.CODE_OK);
        checkReturn.getNotice().setMessage("good");
        checkReturn.getContracts().addAll(available);
        return checkReturn;
    }

    static void closing(Set<Contract> available, PlatirInfoClientImpl client) {
        available.stream().map(contract -> {
            contract.setState("closing");
            return contract;
        }).forEachOrdered(contract -> {
            try {
                client.queries().update(contract);
            } catch (DataQueryException e) {
                Utils.err.write("Can't update user(" + contract.getUserId() + ") + contract(" + contract.getContractId() + ") state(" + contract.getState() + "): " + e.getMessage(), e);
            }
        });
    }

    static OrderContextImpl createOrderContext(String orderId, String transactionId, String instrumentId, Double price, Integer volume, String direction, Collection<Contract> contracts, String offset, TransactionContextImpl transactionContect) {
        PlatirClientImpl client = transactionContect.getStrategyContext().getPlatirClientImpl();
        Order order = client.queries().getFactory().newOrder();
        order.setOrderId(orderId);
        order.setTransactionId(transactionId);
        order.setInstrumentId(instrumentId);
        order.setPrice(price);
        order.setVolume(volume);
        order.setDirection(direction);
        order.setOffset(offset);
        order.setTradingDay(client.getTradingDay());
        try {
            /* save order to data source */
            client.queries().insert(order);
        } catch (DataQueryException exception) {
            /* worker thread can't pass out the exception, just log it */
            Utils.err.write("Can't insert order(" + order.getOrderId() + ") to data source: " + exception.getMessage(), exception);
        }
        /* create order context. */
        OrderContextImpl orderContext = new OrderContextImpl(order, transactionContect);
        orderContext.lockedContracts().addAll(contracts);
        /* add order context to transaction context */
        transactionContect.addOrderContext(orderContext);
        return orderContext;
    }

    static String getOrderId(String transactionId) {
        /* <transaction-id>.<some-digits> */
        return transactionId + "." + Integer.toString(orderIdCounter.incrementAndGet());
    }

    static void saveRiskNotice(int code, String message, Integer level, TransactionContextImpl transactionContext) {
        RiskNotice riskNotice = transactionContext.getQueryClient().queries().getFactory().newRiskNotice();
        StrategyProfile profile = transactionContext.getStrategyContext().getProfile();
        riskNotice.setCode(code);
        riskNotice.setMessage(message);
        riskNotice.setLevel(level);
        riskNotice.setUserId(profile.getUserId());
        riskNotice.setStrategyId(profile.getStrategyId());
        riskNotice.setUpdateTime(Utils.datetime());
        try {
            transactionContext.getQueryClient().queries().insert(riskNotice);
        } catch (DataQueryException exception) {
            Utils.err.write("Can't inert RiskNotice(" + code + ", " + message + "): " + exception.getMessage(), exception);
        }
    }

    static void processNotice(int code, String message, TransactionContextImpl transactionContext) {
        Notice notice = transactionContext.getQueryClient().queries().getFactory().newNotice();
        notice.setCode(code);
        notice.setMessage(message);
        notice.setContext(transactionContext);
        transactionContext.getStrategyContext().processNotice(notice);
    }

}
