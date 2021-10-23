package io.platir.core.internal;

import io.platir.service.ServiceConstants;
import io.platir.queries.Utils;
import io.platir.service.RiskNotice;
import io.platir.service.Trade;
import io.platir.service.DataQueryException;
import java.util.concurrent.atomic.AtomicInteger;
import io.platir.service.api.RiskManager;

/**
 *
 * @author Chen Hongbao
 */
class OrderExecutionContext {

    private OrderContextImpl orderContext;
    private TransactionContextImpl transactionContext;

    private final AtomicInteger volumeCounter = new AtomicInteger(0);
    private final RiskManager riskManager;

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
        updateTradedContracts(trade);
        strategyContext.processTrade(trade);
        checkCompleted(trade.getVolume());
        /* risk assess */
        afterRisk(trade);
    }

    private void afterRisk(Trade trade) {
        try {
            var riskNotice = riskManager.after(trade, transactionContext);
            if (riskNotice.getCode() != ServiceConstants.CODE_OK) {
                TransactionFacilities.saveRiskNotice(riskNotice.getCode(), riskNotice.getMessage(), RiskNotice.WARNING, transactionContext);
            }
        } catch (Throwable throwable) {
            Utils.err().write("Risk assess after() throws exception: " + throwable.getMessage(), throwable);
        }
    }

    private void updateTradedContracts(Trade trade) {
        var strategyContext = transactionContext.getStrategyContext();
        int updateCount = 0;
        var lockedContractIterator = orderContext.lockedContracts().iterator();
        while (++updateCount <= trade.getVolume() && lockedContractIterator.hasNext()) {
            var lockedContract = lockedContractIterator.next();
            var prevState = lockedContract.getState();
            switch (lockedContract.getState()) {
                case ServiceConstants.FLAG_CONTRACT_OPENING:
                    /* Update open price because the real traded price may be different. */
                    lockedContract.setState(ServiceConstants.FLAG_CONTRACT_OPEN);
                    lockedContract.setPrice(trade.getPrice());
                    lockedContract.setOpenTime(Utils.datetime());
                    lockedContract.setOpenTradingDay(strategyContext.getPlatirClientImpl().getTradingDay());
                    break;
                case ServiceConstants.FLAG_CONTRACT_CLOSING:
                    /* Don't forget the close price here. */
                    lockedContract.setState(ServiceConstants.FLAG_CONTRACT_CLOSED);
                    lockedContract.setClosePrice(trade.getPrice());
                    break;
                default:
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

    private void checkCompleted(int addedVolume) {
        int tradedVolume = volumeCounter.addAndGet(addedVolume);
        var totalVolume = orderContext.getOrder().getVolume();
        if (tradedVolume >= totalVolume) {
            /* Let garbage collection reclaim the objects. */
            orderContext = null;
            transactionContext = null;
        }
        if (tradedVolume == totalVolume) {
            TransactionFacilities.processTradeUpdate(ServiceConstants.CODE_TRANSACTION_COMPLETED, "completed", orderContext, transactionContext, null);
        } else if (tradedVolume > totalVolume) {
            Utils.err().write("Order(" + orderContext.getOrder().getOrderId() + ") over traded.");
        }
    }

}
