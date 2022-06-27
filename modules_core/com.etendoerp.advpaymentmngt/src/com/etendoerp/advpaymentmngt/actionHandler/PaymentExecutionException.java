package com.etendoerp.advpaymentmngt.actionHandler;

public class PaymentExecutionException extends RuntimeException {
  public PaymentExecutionException(Exception e) {
    super(e);
  }

  public PaymentExecutionException(String message) {
    super(message);
  }
}
