/*
 * The contents of this file are subject to the Openbravo Public License
 * Version 1.1 (the "License"), being the Mozilla Public License Version 1.1
 * with a permitted attribution clause. You may not use this file except in
 * compliance with the License, available at http://www.openbravo.com/legal/license.html.
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * The Original Code is Openbravo ERP. The Initial Developer is Openbravo SLU.
 * All portions are Copyright (C) 2008-2020 Openbravo SLU. All Rights Reserved.
 * ERP initialization extracted from OBContext.
 */
package org.openbravo.dal.core;

import org.hibernate.Hibernate;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.enterprise.Warehouse;

/**
 * ERP-specific initialization retained by the legacy context facade.
 *
 * <p>MODULE-BOUNDARY CTX-ERP-INIT: destination platform-compat-etendo, not platform-core.
 * See docs/platform-module-boundaries.md for the remaining typed facade dependency.
 *
 * <p>This compatibility boundary deliberately preserves the existing initialization order and
 * typed API. It is not part of the eventual ERP-free context contract.
 */
final class ErpContextSupport {
  private ErpContextSupport() {
  }

  static void initializeDefaultWarehouse(User user) {
    Hibernate.initialize(user.getDefaultWarehouse());
  }

  static void initializeBusinessPartner(User user) {
    // Platform dictionaries may omit this ERP association and its generated Java type.
    if (user.getEntity().hasProperty("businessPartner")) {
      Hibernate.initialize(user.get("businessPartner"));
    }
  }

  static void selectWarehouse(OBContext context, String warehouseId) {
    // Session variables can supply an empty identifier; retain the default-selection behavior.
    if (warehouseId != null && warehouseId.trim().length() > 0) {
      context.setWarehouse(SessionHandler.getInstance()
          .createQuery("select w from Warehouse w where w.id=:id", Warehouse.class)
          .setParameter("id", warehouseId).uniqueResult());
    } else if (context.getUser().getDefaultWarehouse() != null) {
      context.setWarehouse(context.getUser().getDefaultWarehouse());
    }
  }

  static void initializeWarehouse(OBContext context) {
    Warehouse warehouse = context.getWarehouse();
    if (warehouse != null) {
      Hibernate.initialize(warehouse);
      Hibernate.initialize(warehouse.getClient());
      Hibernate.initialize(warehouse.getOrganization());
    }
  }
}
