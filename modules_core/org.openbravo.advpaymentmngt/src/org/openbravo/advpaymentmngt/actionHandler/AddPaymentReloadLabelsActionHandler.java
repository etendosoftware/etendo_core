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
 * All portions are Copyright (C) 2014-2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.advpaymentmngt.actionHandler;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.client.application.Parameter;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.system.Language;
import org.openbravo.model.ad.ui.Element;
import org.openbravo.model.ad.ui.ElementTrl;
import org.openbravo.service.db.DbUtility;

public class AddPaymentReloadLabelsActionHandler extends BaseActionHandler {
  private static final Logger log = LogManager.getLogger();

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String data) {
    JSONObject result = new JSONObject();
    JSONObject values = new JSONObject();
    JSONObject errorMessage = new JSONObject();
    OBContext.setAdminMode(true);
    try {
      final String strBusinessPartner = (String) parameters.get("businessPartner");
      final String strFinancialAccount = (String) parameters.get("financialAccount");
      final String strIssotrx = (String) parameters.get("issotrx");
      boolean issotrx = "true".equals(strIssotrx) ? true : false;

      final String labelProperty = issotrx ? Element.PROPERTY_NAME
          : Element.PROPERTY_PURCHASEORDERNAME;
      final Language lang = OBContext.getOBContext().getLanguage();

      final Element businessPartnerElement = OBDal.getInstance()
          .get(Parameter.class, strBusinessPartner)
          .getApplicationElement();
      final Element financialAccountElement = OBDal.getInstance()
          .get(Parameter.class, strFinancialAccount)
          .getApplicationElement();

      values.put("businessPartner", businessPartnerElement.get(labelProperty));
      values.put("financialAccount", financialAccountElement.get(labelProperty));
      if (!StringUtils.equals(lang.getLanguage(), "en_US")) {
        final OBCriteria<ElementTrl> obcBP = OBDal.getInstance().createCriteria(ElementTrl.class);
        obcBP.add(Restrictions.eq(ElementTrl.PROPERTY_APPLICATIONELEMENT, businessPartnerElement));
        obcBP.add(Restrictions.eq(ElementTrl.PROPERTY_LANGUAGE, lang));
        obcBP.setMaxResults(1);
        final ElementTrl elementBP = (ElementTrl) obcBP.uniqueResult();
        if (elementBP != null) {
          values.put("businessPartner", elementBP.get(labelProperty));
        }

        final OBCriteria<ElementTrl> obcFA = OBDal.getInstance().createCriteria(ElementTrl.class);
        obcFA.add(Restrictions.eq(ElementTrl.PROPERTY_APPLICATIONELEMENT, financialAccountElement));
        obcFA.add(Restrictions.eq(ElementTrl.PROPERTY_LANGUAGE, lang));
        obcFA.setMaxResults(1);
        final ElementTrl elementFA = (ElementTrl) obcFA.uniqueResult();
        if (elementFA != null) {
          values.put("financialAccount", elementFA.get(labelProperty));
        }
      }

      result.put("values", values);
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Error obtaining labels when executing AddPaymentReloadLabelsActionHandler", e);
      try {
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("title", "Error");
        errorMessage.put("text", message);
        result.put("message", errorMessage);
      } catch (Exception e2) {
        log.error("Error message could not be built", e2);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }
}
