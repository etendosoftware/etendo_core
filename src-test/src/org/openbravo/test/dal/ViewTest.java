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
 * All portions are Copyright (C) 2010-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.dal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

import org.hibernate.criterion.Restrictions;
import org.junit.Test;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.test.base.Issue;
import org.openbravo.test.base.OBBaseTest;

/**
 * Test if views work properly
 * 
 * @author mtaal
 */
public class ViewTest extends OBBaseTest {

  /**
   * Tests that it is possible to build a query using an entity based on a view. Iterates over all
   * views until one record from a view is retrieved.
   */
  @Test
  public void viewsCanBeQueried() {
    setSystemAdministratorContext();
    BaseOBObject aViewBOB = null;
    Entity aViewEntity = null;
    for (Entity entity : ModelProvider.getInstance().getModel()) {
      if (!entity.isView()) {
        continue;
      }

      OBQuery<BaseOBObject> query = OBDal.getInstance().createQuery(entity.getName(), "");
      query.setMaxResult(1);
      aViewBOB = query.uniqueResult();
      if (aViewBOB != null) {
        aViewEntity = entity;
        break;
      }
    }
    assertThat("There is at least an object queried from a view", aViewBOB, not(nullValue()));
    assertThat("Entity is correctly set", aViewBOB.getEntity(), is(aViewEntity));
  }

  /** View objects are not copied. */
  @Test
  @Issue("14914")
  public void test14914() {
    setTestUserContext();
    OBContext.setAdminMode();
    try {
      OBCriteria<Invoice> payedInvoice = OBDal.getInstance().createCriteria(Invoice.class);
      payedInvoice.add(Restrictions.eq(Invoice.PROPERTY_PAYMENTCOMPLETE, true));
      payedInvoice.setMaxResults(1);
      Invoice anyPayedInvoice = (Invoice) payedInvoice.uniqueResult();
      assertThat("Original invoice has payments " + anyPayedInvoice,
          anyPayedInvoice.getFINPaymentSchedInvVList(), not(empty()));

      final Invoice copied = (Invoice) DalUtil.copy(anyPayedInvoice);
      assertThat("Copied invoice has NO payments " + copied, copied.getFINPaymentSchedInvVList(),
          is(empty()));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
