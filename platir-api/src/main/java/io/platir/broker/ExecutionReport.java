package io.platir.broker;

public interface ExecutionReport {

    String getExecutionReportId();

    String getOrderId();

    String getInstrumentId();

    String getExchangeId();

    String getDirection();

    String getOffset();

    Integer getLastTradedQuantity();

    Double getLastTradedPirce();

    Integer getTradedQuantity();

    Integer getQuantity();

    String getTradingDay();

    String getUpdateTime();

    String getState();
}
