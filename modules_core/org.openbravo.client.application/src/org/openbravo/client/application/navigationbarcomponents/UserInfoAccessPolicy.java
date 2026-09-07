/* Copyright (C) 2026 Etendo Software. Licensed under Apache License 2.0. */
package org.openbravo.client.application.navigationbarcomponents;

import jakarta.servlet.http.HttpSession;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.util.OBClassLoader;

/**
 * Optional domain restriction on the original profile role list, not role authorization.
 * MODULE-BOUNDARY UI-ACCESS: ERP licensing/build state belongs to the ERP UI contribution.
 * Selection is immutable per application classloader and uses trusted server properties only.
 */
public abstract class UserInfoAccessPolicy {
  public static final String IMPLEMENTATION_PROPERTY = "ui.userInfo.accessPolicy.class";

  /** Returns the configured policy, retaining ERP behavior when no selection is provided. */
  public static UserInfoAccessPolicy getInstance() {
    return Holder.INSTANCE;
  }

  private static final class Holder {
    private static final UserInfoAccessPolicy INSTANCE = create();

    private static UserInfoAccessPolicy create() {
      String implementation = OBPropertiesProvider.getInstance().getOpenbravoProperties()
          .getProperty(IMPLEMENTATION_PROPERTY,
              "org.openbravo.client.application.navigationbarcomponents.ErpUserInfoAccessPolicy");
      try {
        return OBClassLoader.getInstance().loadClass(implementation)
            .asSubclass(UserInfoAccessPolicy.class).getDeclaredConstructor().newInstance();
      } catch (ReflectiveOperationException | ClassCastException failure) {
        throw new OBException("Invalid server profile access policy: " + implementation, failure);
      }
    }
  }

  /** Whether the original profile must display only the system administrator role. */
  public abstract boolean isSystemAdministratorOnly(HttpSession session);
}
