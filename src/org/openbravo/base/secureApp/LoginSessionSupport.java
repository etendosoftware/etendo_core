/*
 * Copyright (C) 2026 Etendo Software
 * Licensed under the Apache Software License version 2.0.
 * You may obtain a copy at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.openbravo.base.secureApp;

import jakarta.servlet.ServletException;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.util.OBClassLoader;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.model.ad.system.Client;

/**
 * Optional business-domain contribution to the original login session.
 *
 * <p>MODULE-BOUNDARY LOGIN-SESSION: the contract belongs to shared session services;
 * the ERP implementation belongs to platform-compat-etendo. This contract neither
 * authenticates users nor authorizes roles, clients or organizations. Those checks
 * remain in the original login implementation and must run in every composition.
 * The implementation is selected once per application class loader from trusted
 * server configuration, never from request parameters. An invalid implementation
 * fails initialization rather than silently disabling ERP initialization.
 */
public abstract class LoginSessionSupport {
  public static final String IMPLEMENTATION_PROPERTY = "login.session.support.class";
  private static final String DEFAULT_IMPLEMENTATION =
      "org.openbravo.base.secureApp.ErpLoginSessionSupport";

  /** Returns the application-owned contribution; the unchanged ERP behavior is the default. */
  public static LoginSessionSupport getInstance() {
    return Holder.INSTANCE;
  }

  private static final class Holder {
    private static final LoginSessionSupport INSTANCE = create();

    private static LoginSessionSupport create() {
      String implementation = OBPropertiesProvider.getInstance().getOpenbravoProperties()
          .getProperty(IMPLEMENTATION_PROPERTY, DEFAULT_IMPLEMENTATION);
      try {
        return OBClassLoader.getInstance().loadClass(implementation)
            .asSubclass(LoginSessionSupport.class).getDeclaredConstructor().newInstance();
      } catch (ReflectiveOperationException | ClassCastException failure) {
        throw new OBException("Invalid server login session contribution: " + implementation, failure);
      }
    }
  }

  /** Returns the current warehouse identifier, or null when this context has no warehouse. */
  public abstract String getContextWarehouseId();

  /** Resolves a user's warehouse default, including the original organization fallback. */
  public abstract String getUserDefaultWarehouse(ConnectionProvider connection, String user,
      String client, String organization, String role) throws ServletException;

  /** Resolves a warehouse for an organization and role without changing the context. */
  public abstract String getDefaultWarehouse(ConnectionProvider connection, String client,
      String organization, String role) throws ServletException;

  /** Reads domain configuration while LoginUtils holds its original metadata admin scope. */
  public abstract boolean isAccountingDimensionConfigCentrally(Client client);

  /** Adds accounting session values at the original full-login initialization point. */
  public abstract void initializeAccounting(ConnectionProvider connection, VariablesSecureApp vars,
      Client client, boolean centrallyMaintained, String organization, String clientId)
      throws ServletException;
}
