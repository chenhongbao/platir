package io.platir.broker;

public interface ExecutionReport {
    final static String ALL_TRADED = "ALL_TRADED";
    
    final static String QUEUEING = "QUEUEING";
    
    final static String CANCELED = "CANCELED";
    
    final static String RJECTED = "REJECTED";

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
