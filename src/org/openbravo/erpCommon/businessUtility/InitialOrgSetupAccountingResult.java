package org.openbravo.erpCommon.businessUtility;

public final class InitialOrgSetupAccountingResult {
  private final boolean handled;
  private final boolean success;
  private final String message;

  private InitialOrgSetupAccountingResult(boolean handled, boolean success, String message) {
    this.handled = handled;
    this.success = success;
    this.message = message;
  }

  public static InitialOrgSetupAccountingResult notHandled() {
    return new InitialOrgSetupAccountingResult(false, true, null);
  }

  public static InitialOrgSetupAccountingResult success() {
    return new InitialOrgSetupAccountingResult(true, true, null);
  }

  public static InitialOrgSetupAccountingResult error(String message) {
    return new InitialOrgSetupAccountingResult(true, false, message);
  }

  public boolean isHandled() {
    return handled;
  }

  public boolean isSuccess() {
    return success;
  }

  public String getMessage() {
    return message;
  }
}
