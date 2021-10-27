package io.platir.engine.core;

import io.platir.Account;
import io.platir.Contract;
import io.platir.Instrument;
import io.platir.Order;
import io.platir.util.Utils;
import java.util.Map;
import java.util.logging.Level;

public class AccountUtils {

    static Double computeCommission(Instrument instrument, Double price, Integer quantity) {
        var commissionByAmount = instrument.getCommissionByAmount();
        if (commissionByAmount.equals(0D)) {
            return quantity * instrument.getCommissionByQuantity();
        } else {
            return instrument.getMultiple() * quantity * price * commissionByAmount;
        }
    }

    static Double computeMargin(Instrument instrument, Double price, Integer quantity) {
        var marginByAmount = instrument.getMarginByAmount();
        if (marginByAmount.equals(0D)) {
            return quantity * instrument.getMarginByQuantity();
        } else {
            return instrument.getMultiple() * quantity * price * marginByAmount;
        }
    }

    static Double computeProfit(Instrument instrument, Contract contract, Double price) {
        switch (contract.getDirection()) {
            case Order.BUY:
                return instrument.getMultiple() * (price - contract.getPrice());
            case Order.SELL:
                return instrument.getMultiple() * (contract.getPrice() - price);
            default:
                Utils.logger().log(Level.SEVERE, "Invalid contract direction {0}.", contract.getDirection());
                return 0D;
        }
    }

    static void settleAccount(AccountCore account, Map<String, Instrument> instruments, Map<String, Double> prices, String tradingDay) {
        Double openingCommission = 0D;
        Double openingMargin = 0D;
        Double closingCommission = 0D;
        Double margin = 0D;
        Double commission = 0D;
        Double closeProfit = 0D;
        Double positionProfit = 0D;
        
        for (var contract : account.getContracts()) {
            var instrument = instruments.get(contract.getInstrumentId());
            var price = prices.get(contract.getInstrumentId());
            switch (contract.getState()) {
                case Contract.OPENING:
                    openingCommission += computeCommission(instrument, price, 1);
                    openingMargin += computeMargin(instrument, price, 1);
                    break;
                case Contract.OPEN:
                    if (contract.getOpenTradingDay().equals(tradingDay)) {
                        commission += computeCommission(instrument, contract.getPrice(), 1);
                    }
                    margin += computeMargin(instrument, contract.getPrice(), 1);
                    positionProfit += computeProfit(instrument, contract, price);
                    break;
                case Contract.CLOSING:
                    if (contract.getOpenTradingDay().equals(tradingDay)) {
                        commission += computeCommission(instrument, contract.getPrice(), 1);
                    }
                    margin += computeMargin(instrument, contract.getPrice(), 1);
                    closingCommission += computeCommission(instrument, contract.getClosePrice(), 1);
                    positionProfit += computeProfit(instrument, contract, price);
                case Contract.CLOSED:
                    if (contract.getOpenTradingDay().equals(tradingDay)) {
                        commission += computeCommission(instrument, contract.getPrice(), 1);
                    }
                    commission += computeCommission(instrument, contract.getClosePrice(), 1);
                    closeProfit = computeProfit(instrument, contract, contract.getClosePrice());
                default:
                    Utils.logger().log(Level.SEVERE, "Invalid contract state {0}.", contract.getState());
                    break;
            }
        }
        
        Double balance = account.getYdBalance() + positionProfit + closeProfit - commission;
        Double available = balance - openingCommission - closingCommission - openingMargin - margin;
        
        account.setAvailable(available);
        account.setBalance(balance);
        account.setCloseProfit(closeProfit);
        account.setClosingCommission(closingCommission);
        account.setCommission(commission);
        account.setMargin(margin);
        account.setOpeningCommission(openingCommission);
        account.setOpeningMargin(openingMargin);
        account.setPositionProfit(positionProfit);
        account.setSettleTime(Utils.datetime());
        account.setTradingDay(tradingDay);
    }
}
