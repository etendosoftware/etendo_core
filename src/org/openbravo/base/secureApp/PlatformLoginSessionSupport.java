/*
 * Copyright (C) 2026 Etendo Software
 * Licensed under the Apache Software License version 2.0.
 * You may obtain a copy at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.openbravo.base.secureApp;

import org.openbravo.database.ConnectionProvider;
import org.openbravo.model.ad.system.Client;

/**
 * Explicit ERP-free session composition: no warehouse or accounting contribution.
 *
 * <p>This is not an authentication implementation. The original login must still
 * validate credentials and authorize the complete role/client/organization scope.
 * Select this class only in the ERP-free deployment's trusted server properties.
 */
public final class PlatformLoginSessionSupport extends LoginSessionSupport {
  @Override
  public String getContextWarehouseId() {
    return null;
  }

  @Override
  public String getUserDefaultWarehouse(ConnectionProvider connection, String user,
      String client, String organization, String role) {
    return "";
  }

  @Override
  public String getDefaultWarehouse(ConnectionProvider connection, String client,
      String organization, String role) {
    return "";
  }

  @Override
  public boolean isAccountingDimensionConfigCentrally(Client client) {
    return false;
  }

  @Override
  public void initializeAccounting(ConnectionProvider connection, VariablesSecureApp vars,
      Client client, boolean centrallyMaintained, String organization, String clientId) {
    // The ERP-free domain has no accounting state to initialize.
  }
}
