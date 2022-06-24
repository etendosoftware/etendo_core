/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2013-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.event;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.client.kernel.event.TransactionBeginEvent;
import org.openbravo.client.kernel.event.TransactionCompletedEvent;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.common.plm.ProductCharacteristicValue;
import org.openbravo.service.importprocess.ImportEntryManager;

class ProductCharacteristicValueEventHandler extends EntityPersistenceEventObserver {
  private static final int IMPORT_ENTRY_SIZE = 100;
  private static Logger logger = LogManager.getLogger();
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ProductCharacteristicValue.ENTITY_NAME) };
  private static ThreadLocal<Set<String>> prodchvalueUpdated = new ThreadLocal<>();

  @Inject
  ImportEntryManager importEntryManager;

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onTransactionBegin(@Observes TransactionBeginEvent event) {
    prodchvalueUpdated.set(new HashSet<String>());
  }

  public void onNew(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final ProductCharacteristicValue pchv = (ProductCharacteristicValue) event.getTargetInstance();
    addProductToList(pchv);
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final ProductCharacteristicValue pchv = (ProductCharacteristicValue) event.getTargetInstance();
    addProductToList(pchv);
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final ProductCharacteristicValue pchv = (ProductCharacteristicValue) event.getTargetInstance();
    addProductToList(pchv);
  }

  public void onTransactionCompleted(@Observes TransactionCompletedEvent event) {
    try {
      Set<String> productList = prodchvalueUpdated.get();
      prodchvalueUpdated.set(null);
      prodchvalueUpdated.remove();
      if (productList == null || productList.isEmpty()
          || event.getTransaction().getStatus() == TransactionStatus.ROLLED_BACK) {
        return;
      }
      ArrayList<String> products = new ArrayList<>(productList);
      int productCount = productList.size();
      for (int i = 0; i < productCount; i += IMPORT_ENTRY_SIZE) {
        int currentLimit = (i + IMPORT_ENTRY_SIZE) < productCount ? (i + IMPORT_ENTRY_SIZE)
            : productCount;
        JSONArray productSubListIds = new JSONArray(products.subList(i, currentLimit));
        JSONObject entryJson = new JSONObject();
        entryJson.put("productIds", productSubListIds);
        if (!SessionHandler.getInstance().isCurrentTransactionActive()) {
          SessionHandler.getInstance().beginNewTransaction();
        }
        importEntryManager.createImportEntry(SequenceIdData.getUUID(), "VariantChDescUpdate",
            entryJson.toString(), true);
      }
    } catch (JSONException e) {
      logger.error("Error in ProductCharacteristicValueEventHandler.onTransactionCompleted", e);
    }
  }

  private void addProductToList(ProductCharacteristicValue pchv) {
    Set<String> productList = prodchvalueUpdated.get();
    if (productList == null) {
      productList = new HashSet<>();
    }
    productList.add(pchv.getProduct().getId());
    prodchvalueUpdated.set(productList);
  }
}
