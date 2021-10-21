package io.platir.core.internal;

import io.platir.queries.Utils;
import io.platir.service.Constants;
import io.platir.service.Notice;
import io.platir.service.RiskNotice;
import io.platir.service.Trade;
import io.platir.service.DataQueryException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import io.platir.service.api.RiskManager;

/**
 *
 * @author Chen Hongbao
 */
class OrderExecutionContext {

    private Notice responseNotice;
    private OrderContextImpl orderContext;
    private TransactionContextImpl transactionContext;

    private final AtomicInteger volumeCounter = new AtomicInteger(0);
    private final RiskManager riskManager;

    /* first notice waiting facilities */
    private final Lock responseLock = new ReentrantLock();
    private final Condition responseCondition = responseLock.newCondition();
    private final int responseTimeoutSeconds = 5;

    OrderExecutionContext(OrderContextImpl orderContext, TransactionContextImpl transactionContext, RiskManager riskManager) {
        this.orderContext = orderContext;
        this.transactionContext = transactionContext;
        this.riskManager = riskManager;
    }
    
    TransactionContextImpl getTransactionContext() {
        return transactionContext;
    }

    void processTrade(Trade trade) {
        var strategyContext = transactionContext.getStrategyContext();
        try {
            /* save trade to data source. */
            strategyContext.getPlatirClientImpl().queries().insert(trade);
        } catch (DataQueryException exception) {
            /* worker thread sees this exception, just log it */
            Utils.err().write("Can't insert trade(" + trade.getTradeId() + ") for transaction(" + transactionContext.getTransaction() + ") and strategy(" + strategyContext.getProfile().getStrategyId() + ") into data source: " + exception.getMessage(), exception);
        }
        /* add trade to order context. */
        orderContext.addTrade(trade);
        /* update contracts' states */
        updateContracts(trade);
        strategyContext.processTrade(trade);
        checkCompleted(trade.getVolume());
        /* risk assess */
        afterRisk(trade);
    }

    void processNotice(int code, String message) {
        signalResponse(code, message);
        pushNotice(code, message);
    }

    void signalResponse(int code, String message) {
        if (responseNotice != null) {
            return;
        }
        responseLock.lock();
        try {
            if (responseNotice == null) {
                responseNotice = transactionContext.getQueryClient().queries().getFactory().newNotice();
                responseNotice.setCode(code);
                responseNotice.setMessage(message);
                responseCondition.signalAll();
            }
        } finally {
            responseLock.unlock();
        }
    }

    Notice waitResponse() {
        if (responseNotice != null) {
            return responseNotice;
        }
        responseLock.lock();
        try {
            if (responseNotice == null) {
                responseCondition.await(responseTimeoutSeconds, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            responseNotice = transactionContext.getQueryClient().queries().getFactory().newNotice();
            responseNotice.setCode(Constants.CODE_RESPONSE_TIMEOUT);
            responseNotice.setMessage("response timeout");
            responseNotice.setError(e);
            responseNotice.setContext(transactionContext);
        } finally {
            responseLock.unlock();
        }
        return responseNotice;
    }

    private void afterRisk(Trade trade) {
        try {
            var riskNotice = riskManager.after(trade, transactionContext);
            if (!riskNotice.isGood()) {
                TransactionFacilities.saveRiskNotice(riskNotice.getCode(), riskNotice.getMessage(), RiskNotice.WARNING, transactionContext);
            }
        } catch (Throwable throwable) {
            Utils.err().write("Risk assess after() throws exception: " + throwable.getMessage(), throwable);
            TransactionFacilities.saveRiskNotice(1005, "after(Trade) throws exception", RiskNotice.ERROR, transactionContext);
        }
    }

    private void updateContracts(Trade trade) {
        var strategyContext = transactionContext.getStrategyContext();
        int updateCount = 0;
        var lockedContractIterator = orderContext.lockedContracts().iterator();
        while (++updateCount <= trade.getVolume() && lockedContractIterator.hasNext()) {
            var lockedContract = lockedContractIterator.next();
            var prevState = lockedContract.getState();
            if (lockedContract.getState().compareToIgnoreCase(Constants.FLAG_CONTRACT_OPENING) == 0) {
                /* Update open price because the real traded price may be different. */
                lockedContract.setState(Constants.FLAG_CONTRACT_OPEN);
                lockedContract.setPrice(trade.getPrice());
                lockedContract.setOpenTime(Utils.datetime());
                lockedContract.setOpenTradingDay(strategyContext.getPlatirClientImpl().getTradingDay());
            } else if (lockedContract.getState().compareToIgnoreCase(Constants.FLAG_CONTRACT_CLOSING) == 0) {
                /* Don't forget the close price here. */
                lockedContract.setState(Constants.FLAG_CONTRACT_CLOSED);
                lockedContract.setClosePrice(trade.getPrice());
            } else {
                Utils.err().write("Incorrect contract state(" + lockedContract.getState() + "/" + lockedContract.getContractId() + ") before completing trade.");
                continue;
            }
            try {
                strategyContext.getPlatirClientImpl().queries().update(lockedContract);
            } catch (DataQueryException exception) {
                Utils.err().write("Fail updating user(" + lockedContract.getUserId() + ") contract(" + lockedContract.getContractId() + ") state(" + lockedContract.getState() + ").", exception);
                /* Roll back state. */
                lockedContract.setState(prevState);
                continue;
            }
            lockedContractIterator.remove();
        }
        if (updateCount <= trade.getVolume()) {
            Utils.err().write("Insufficent(" + updateCount + "<" + trade.getVolume() + ") locked contracts.");
        }
    }

    private void pushNotice(int code, String message) {
        pushNotice(code, message, null);
    }

    private void pushNotice(int code, String message, Throwable error) {
        var notice = transactionContext.getQueryClient().queries().getFactory().newNotice();
        notice.setCode(code);
        notice.setMessage(message);
        notice.setError(error);
        notice.setContext(transactionContext);
        transactionContext.getStrategyContext().processNotice(notice);
    }

    private void checkCompleted(int addedVolume) {
        int tradedVolume = volumeCounter.addAndGet(addedVolume);
        var totalVolume = orderContext.getOrder().getVolume();
        if (tradedVolume >= totalVolume) {
            /* Let garbage collection reclaim the objects. */
            orderContext = null;
            transactionContext = null;
            /* Tell strategy trades are completed. */
            pushNotice(Constants.CODE_OK, "trade completed");
        }
        if (tradedVolume > totalVolume) {
            Utils.err().write("Order(" + orderContext.getOrder().getOrderId() + ") over traded.");
        }
    }

}
