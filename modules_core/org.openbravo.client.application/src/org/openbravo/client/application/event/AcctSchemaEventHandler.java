/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2012-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.client.application.event;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.event.Observes;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchema;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchemaElement;
import org.openbravo.model.financialmgmt.accounting.coa.Element;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;

class AcctSchemaEventHandler extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(AcctSchema.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    boolean eval = false;
    if ((Boolean) event.getCurrentState(getProperty(AcctSchema.PROPERTY_CENTRALMAINTENANCE))) {
      if (!event.getPreviousState(getProperty(AcctSchema.PROPERTY_CENTRALMAINTENANCE))
          .equals(event.getCurrentState(getProperty(AcctSchema.PROPERTY_CENTRALMAINTENANCE)))) {
        eval = true;
      }
      if (!event.getPreviousState(getProperty(AcctSchema.PROPERTY_ASSETPOSITIVE))
          .equals(event.getCurrentState(getProperty(AcctSchema.PROPERTY_ASSETPOSITIVE)))) {
        eval = true;
      }
      if (!event.getPreviousState(getProperty(AcctSchema.PROPERTY_LIABILITYPOSITIVE))
          .equals(event.getCurrentState(getProperty(AcctSchema.PROPERTY_LIABILITYPOSITIVE)))) {
        eval = true;
      }
      if (!event.getPreviousState(getProperty(AcctSchema.PROPERTY_EQUITYPOSITIVE))
          .equals(event.getCurrentState(getProperty(AcctSchema.PROPERTY_EQUITYPOSITIVE)))) {
        eval = true;
      }
      if (!event.getPreviousState(getProperty(AcctSchema.PROPERTY_EXPENSEPOSITIVE))
          .equals(event.getCurrentState(getProperty(AcctSchema.PROPERTY_EXPENSEPOSITIVE)))) {
        eval = true;
      }
      if (!event.getPreviousState(getProperty(AcctSchema.PROPERTY_REVENUEPOSITIVE))
          .equals(event.getCurrentState(getProperty(AcctSchema.PROPERTY_REVENUEPOSITIVE)))) {
        eval = true;
      }
    }
    if (eval) {
      final AcctSchema acctSchema = (AcctSchema) event.getTargetInstance();
      Element element = getAccountElement(acctSchema);
      if (element == null) {
        return;
      }
      if (countSchemas(element) > 1) {
        throw new OBException(OBMessageUtils.messageBD("SharedAccountTree"));
      }
      updateElementValues(element.getId(),
          (Boolean) event.getCurrentState(getProperty(AcctSchema.PROPERTY_ASSETPOSITIVE)),
          (Boolean) event.getCurrentState(getProperty(AcctSchema.PROPERTY_LIABILITYPOSITIVE)),
          (Boolean) event.getCurrentState(getProperty(AcctSchema.PROPERTY_EQUITYPOSITIVE)),
          (Boolean) event.getCurrentState(getProperty(AcctSchema.PROPERTY_EXPENSEPOSITIVE)),
          (Boolean) event.getCurrentState(getProperty(AcctSchema.PROPERTY_REVENUEPOSITIVE)));
    }
  }

  private Property getProperty(String property) {
    return entities[0].getProperty(property);
  }

  private Element getAccountElement(AcctSchema acctSchema) {
    final String ELEMENTTYPE_ACCOUNT = "AC";
    OBCriteria<AcctSchemaElement> aee = OBDal.getInstance().createCriteria(AcctSchemaElement.class);
    aee.add(Restrictions.eq(AcctSchemaElement.PROPERTY_ACCOUNTINGSCHEMA, acctSchema));
    aee.add(Restrictions.isNotNull(AcctSchemaElement.PROPERTY_ACCOUNTINGELEMENT));
    aee.add(Restrictions.eq(AcctSchemaElement.PROPERTY_TYPE, ELEMENTTYPE_ACCOUNT));
    aee.setMaxResults(1);
    List<AcctSchemaElement> aees = aee.list();
    if (!aees.isEmpty()) {
      return aees.get(0).getAccountingElement();
    } else {
      return null;
    }
  }

  private int countSchemas(Element element) {
    Set<AcctSchema> schemas = new HashSet<>();
    OBCriteria<AcctSchemaElement> aee = OBDal.getInstance().createCriteria(AcctSchemaElement.class);
    aee.add(Restrictions.eq(AcctSchemaElement.PROPERTY_ACCOUNTINGELEMENT, element));
    for (AcctSchemaElement acctSchemaElement : aee.list()) {
      schemas.add(acctSchemaElement.getAccountingSchema());
    }
    return schemas.size();

  }

  private void updateElementValues(String elementId, boolean assetPositive,
      boolean liabilityPositive, boolean ownersEquityPositive, boolean expensePositive,
      boolean revenuePositive) {
    final String ACCOUNTSIGN_CREDIT = "C";
    final String ACCOUNTSIGN_DEBIT = "D";
    final String ACCOUNTTYPE_MEMO = "M";
    Element element = OBDal.getInstance().get(Element.class, elementId);
    String where = "accountingElement.id = :element";
    OBQuery<ElementValue> elementValueQry = OBDal.getInstance()
        .createQuery(ElementValue.class, where)
        .setFilterOnActive(false)
        .setFilterOnReadableClients(false)
        .setFilterOnReadableOrganization(false)
        .setNamedParameter("element", element.getId())
        .setFetchSize(1000);

    ScrollableResults elementvalues = elementValueQry.scroll(ScrollMode.FORWARD_ONLY);
    try {
      while (elementvalues.next()) {
        ElementValue elementValue = (ElementValue) elementvalues.get(0);
        boolean isCredit = getAccountSign(elementValue.getAccountType(), assetPositive,
            liabilityPositive, ownersEquityPositive, expensePositive, revenuePositive);
        if (!ACCOUNTTYPE_MEMO.equals(elementValue.getAccountType())) {
          elementValue.setAccountSign(isCredit ? ACCOUNTSIGN_CREDIT : ACCOUNTSIGN_DEBIT);
        }
      }
    } finally {
      elementvalues.close();
    }
  }

  private boolean getAccountSign(String accountType, boolean assetPositive,
      boolean liabilityPositive, boolean ownersEquityPositive, boolean expensePositive,
      boolean revenuePositive) {
    final String ACCOUNTTYPE_ASSET = "A";
    final String ACCOUNTTYPE_LIABILITY = "L";
    final String ACCOUNTTYPE_OWNERSEQUITY = "O";
    final String ACCOUNTTYPE_EXPENSE = "E";
    final String ACCOUNTTYPE_REVENUE = "R";
    if (ACCOUNTTYPE_ASSET.equals(accountType) && assetPositive) {
      return false;
    } else if (ACCOUNTTYPE_LIABILITY.equals(accountType) && liabilityPositive) {
      return true;
    } else if (ACCOUNTTYPE_OWNERSEQUITY.equals(accountType) && ownersEquityPositive) {
      return true;
    } else if (ACCOUNTTYPE_EXPENSE.equals(accountType) && expensePositive) {
      return false;
    } else if (ACCOUNTTYPE_REVENUE.equals(accountType) && revenuePositive) {
      return true;
    } else if (ACCOUNTTYPE_ASSET.equals(accountType) && !assetPositive) {
      return true;
    } else if (ACCOUNTTYPE_LIABILITY.equals(accountType) && !liabilityPositive) {
      return false;
    } else if (ACCOUNTTYPE_OWNERSEQUITY.equals(accountType) && !ownersEquityPositive) {
      return false;
    } else if (ACCOUNTTYPE_EXPENSE.equals(accountType) && !expensePositive) {
      return true;
    } else if (ACCOUNTTYPE_REVENUE.equals(accountType) && !revenuePositive) {
      return false;
    }
    return false;

  }
}
