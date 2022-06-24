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
 * All portions are Copyright (C) 2015-2019 Openbravo SLU 
 * All Rights Reserved. 
 ************************************************************************
 */
package org.openbravo.role.inheritance.access;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.structure.InheritedAccessEnabled;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.alert.AlertRecipient;

/**
 * AccessTypeInjector for the AlertRecipient class
 */
@ApplicationScoped
@AccessTypeInjector.Qualifier(AlertRecipient.class)
public class AlertRecipientAccessInjector extends AccessTypeInjector {

  @Override
  public String getSecuredElementGetter() {
    return "getAlertRule";
  }

  @Override
  public String getSecuredElementName() {
    return AlertRecipient.PROPERTY_ALERTRULE;
  }

  @Override
  public boolean isInheritable(InheritedAccessEnabled access) {
    // An inheritable alert recipient should have the User/Contact field empty.
    AlertRecipient alertRecipient = (AlertRecipient) access;
    if (alertRecipient.getUserContact() != null) {
      return false;
    } else {
      return true;
    }
  }

  @Override
  public void checkAccessExistence(InheritedAccessEnabled access) {
    AlertRecipient alertRecipient = (AlertRecipient) access;
    if (existsAlertRecipient(alertRecipient)) {
      Utility.throwErrorMessage("DuplicatedAlertRecipientForTemplate");
    }
  }

  @Override
  public String addEntityWhereClause(String whereClause) {
    // Inheritable alert recipients are those with empty User/Contact field
    return whereClause + " and p.userContact is null";
  }

  @Override
  public List<String> getSkippedProperties() {
    List<String> skippedProperties = super.getSkippedProperties();
    skippedProperties.add("role");
    return skippedProperties;
  }

  /**
   * Utility method to determine if already exists an alert recipient with the same settings (alert
   * rule, role and user) as the alertRecipient passed as parameter
   * 
   * @param alertRecipient
   *          The alert recipient with the settings to find
   * @return true if already exists an alert recipient with the same settings as the entered alert
   *         recipient, false otherwise
   */
  private boolean existsAlertRecipient(AlertRecipient alertRecipient) {
    final OBCriteria<AlertRecipient> obCriteria = OBDal.getInstance()
        .createCriteria(AlertRecipient.class);
    obCriteria
        .add(Restrictions.eq(AlertRecipient.PROPERTY_ALERTRULE, alertRecipient.getAlertRule()));
    obCriteria.add(Restrictions.eq(AlertRecipient.PROPERTY_ROLE, alertRecipient.getRole()));
    if (alertRecipient.getUserContact() == null) {
      obCriteria.add(Restrictions.isNull(AlertRecipient.PROPERTY_USERCONTACT));
    } else {
      obCriteria.add(
          Restrictions.eq(AlertRecipient.PROPERTY_USERCONTACT, alertRecipient.getUserContact()));
    }
    obCriteria.setMaxResults(1);
    return (obCriteria.list().size() > 0);
  }
}
