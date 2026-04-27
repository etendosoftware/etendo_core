package org.openbravo.erpCommon.businessUtility;

public interface InitialOrgSetupAccountingHandler {
  default int getPriority() {
    return 100;
  }

  boolean applies(InitialOrgSetupAccountingContext context);

  InitialOrgSetupAccountingResult wire(InitialOrgSetupAccountingContext context);
}
