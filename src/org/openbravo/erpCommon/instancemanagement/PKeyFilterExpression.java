package org.openbravo.erpCommon.instancemanagement;

import java.util.Map;

import org.hibernate.criterion.Restrictions;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.System;

/**
 * Filter expression used to obtain the active system instance key.
 *
 * <p>This implementation retrieves the first active {@link System} record
 * from the database and returns its instance key. The value is typically used
 * to apply dynamic filtering based on the current system configuration.</p>
 */
public class PKeyFilterExpression implements FilterExpression {
  @Override
  public String getExpression(Map<String, String> requestMap) {

    System systemObj = (System) OBDal.getInstance().createCriteria(System.class)
        .add(Restrictions.eq(System.PROPERTY_ACTIVE, true))
        .setMaxResults(1).uniqueResult();
    return systemObj != null ? systemObj.getInstanceKey() : "";
  }
}
