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
 * All portions are Copyright (C) 2016 Openbravo SLU 
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


import java.math.BigDecimal;
import java.util.List;

import jakarta.servlet.ServletException;

import jakarta.persistence.criteria.CriteriaBuilder;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOMConversion;

/**
 * 
 * Callout to get the conversion rate between the aum and the base unit of the product
 *
 */
public class AUM_ConversionRate extends SimpleCallout {

  /**
   * Get the conversion rate between the unit and the base unit of the product, both provided as
   * parameters
   */

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String srtcUOMId = info.getStringParameter("inpcUomId", null);
    String strmProductId = info.getStringParameter("inpmProductId", null);

    OBContext.setAdminMode(true);
    try {
      Product product = OBDal.getInstance().get(Product.class, strmProductId);
      String strpUOM = product.getUOM().getId();

      OBCriteria<UOMConversion> uOMConversionCriteria = OBDal.getInstance()
          .createCriteria(UOMConversion.class);
      // TODO: Migrar // TODO: Migrar // TODO: Migrar 
 Restrictions.and() a CriteriaBuilder.and() manualmente
 Restrictions.and() a CriteriaBuilder.and() manualmente
 Restrictions.and() a CriteriaBuilder.and() manualmente
      uOMConversionCriteria.add(Restrictions.and(cb.equal(root.get("uOM.id"), srtcUOMId),
          cb.equal(root.get("toUOM.id"), strpUOM)));
      uOMConversionCriteria.setMaxResults(1);
      List<UOMConversion> uOmConversionList = uOMConversionCriteria.list();
      if (uOmConversionList.size() > 0) {
        UOMConversion conversion = uOmConversionList.get(0);
        BigDecimal rate = conversion.getMultipleRateBy();
        info.addResult("inpconversionrate", rate);
      } else {
        uOMConversionCriteria = OBDal.getInstance().createCriteria(UOMConversion.class);
        // TODO: Migrar Restrictions.and() a CriteriaBuilder.and() manualmente
        uOMConversionCriteria.add(Restrictions.and(cb.equal(root.get("uOM.id"), strpUOM),
            cb.equal(root.get("toUOM.id"), srtcUOMId)));
        uOMConversionCriteria.setMaxResults(1);
        uOmConversionList = uOMConversionCriteria.list();
        if (uOmConversionList.size() > 0) {
          UOMConversion conversion = uOmConversionList.get(0);
          BigDecimal rate = conversion.getDivideRateBy();
          info.addResult("inpconversionrate", rate);
        } else {
          info.addResult("inpconversionrate", BigDecimal.ZERO);
        }
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
