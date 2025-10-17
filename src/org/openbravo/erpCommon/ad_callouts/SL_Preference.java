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
 * All portions are Copyright (C) 2010-2016 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_callouts;

/**
 * MIGRATED TO HIBERNATE 6
 * - Replaced org.hibernate.criterion.* with jakarta.persistence.criteria.*
 * - This file was automatically migrated from Criteria API to JPA Criteria API
 * - Review and test thoroughly before committing
 */


import jakarta.servlet.ServletException;

import jakarta.persistence.criteria.CriteriaBuilder;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.filter.ValueListFilter;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.domain.Preference;

/**
 * Checks there is only one selected preference for the current visibility.
 * 
 */
public class SL_Preference extends SimpleCallout {

  private static final ValueListFilter booleanFilter = new ValueListFilter("Y", "N", "");
  private static final IsIDFilter idFilter = new IsIDFilter();

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    boolean selected = info.getStringParameter("inpselected", booleanFilter).equals("Y");
    if (selected) {
      OBCriteria<Preference> qPref = OBDal.getInstance().createCriteria(Preference.class);

      String prefId = info.getStringParameter("inpadPreferenceId", idFilter);
      if (!prefId.isEmpty()) {
        qPref.addNotEqual(Preference.PROPERTY_ID, prefId);
      }

      if (info.getStringParameter("inpispropertylist", booleanFilter).equals("Y")) {
        qPref.addEqual(Preference.PROPERTY_PROPERTY,
            info.getStringParameter("inpproperty", null));
      } else {
        qPref.addEqual(Preference.PROPERTY_ATTRIBUTE,
            info.getStringParameter("inpattribute", null));
      }

      qPref.addEqual(Preference.PROPERTY_SELECTED, true);

      String client = info.getStringParameter("inpvisibleatClientId", idFilter);
      if (client.isEmpty() || client.equals("0")) {
        qPref.addOr(
            (cb, obc) -> cb.isNull(obc.getPath(Preference.PROPERTY_VISIBLEATCLIENT)),
            (cb, obc) -> cb.equal(obc.getPath(Preference.PROPERTY_VISIBLEATCLIENT + ".id"), "0")
        );
      } else {
        qPref.addEqual(Preference.PROPERTY_VISIBLEATCLIENT + ".id", client);
      }

      String org = info.getStringParameter("inpvisibleatOrgId", idFilter);
      if (org.isEmpty() || org.equals("0")) {
        qPref.addOr(
            (cb, obc) -> cb.isNull(obc.getPath(Preference.PROPERTY_VISIBLEATORGANIZATION)),
            (cb, obc) -> cb.equal(obc.getPath(Preference.PROPERTY_VISIBLEATORGANIZATION + ".id"), "0")
        );
      } else {
        qPref.addEqual(Preference.PROPERTY_VISIBLEATORGANIZATION + ".id", org);
      }

      String user = info.getStringParameter("inpadUserId", idFilter);
      if (user.isEmpty()) {
        qPref.addIsNull(Preference.PROPERTY_USERCONTACT);
      } else {
        qPref.addEqual(Preference.PROPERTY_USERCONTACT + ".id", user);
      }

      String role = info.getStringParameter("inpvisibleatRoleId", idFilter);
      if (role.isEmpty()) {
        qPref.addIsNull(Preference.PROPERTY_VISIBLEATROLE);
      } else {
        qPref.addEqual(Preference.PROPERTY_VISIBLEATROLE + ".id", role);
      }

      String window = info.getStringParameter("inpadWindowId", idFilter);
      if (window.isEmpty()) {
        qPref.addIsNull(Preference.PROPERTY_WINDOW);
      } else {
        qPref.addEqual(Preference.PROPERTY_WINDOW + ".id", window);
      }

      if (qPref.count() > 0) {
        info.addResult("inpselected", "N");
        info.addResult("MESSAGE", Utility.messageBD(this, "MultipleSelectedPreferences",
            OBContext.getOBContext().getLanguage().getLanguage()));
      }
    }
  }
}
