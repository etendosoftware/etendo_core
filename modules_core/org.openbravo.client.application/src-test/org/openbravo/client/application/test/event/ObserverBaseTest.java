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
 * All portions are Copyright (C) 2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.client.application.test.event;

import org.hibernate.criterion.Restrictions;
import org.junit.AfterClass;
import org.junit.Before;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.application.Note;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;

/** Base class to test Dal observer execution. Handles state of {@link OrderLineTestObserver}. */
public class ObserverBaseTest extends WeldBaseTest {
  protected enum ObserverExecutionType {
    OFF, UPDATE_DESCRIPTION, CREATE_NOTE, COUNT_LINES, UPDATE_PARENT, UPDATE_PARENT_RANDOM, ON_NOOP
  }

  protected static ObserverExecutionType observerExecutionType = ObserverExecutionType.OFF;

  @AfterClass
  public static void reset() {
    observerExecutionType = ObserverExecutionType.OFF;
  }

  @Before
  @Override
  public void setUp() throws Exception {
    OrderTestObserver.resetExecutionCount();
    OrderLineTestObserver.resetExecutionCount();
    super.setUp();
    OrderTestObserver.refreshObservedEntities();
    OrderLineTestObserver.refreshObservedEntities();
  }

  protected OrderLine pickARandomOrderLine() {
    return (OrderLine) OBDal.getInstance()
        .createCriteria(OrderLine.class)
        .setMaxResults(1)
        .uniqueResult();
  }

  protected Order pickARandomOrder() {
    return (Order) OBDal.getInstance().createCriteria(Order.class).setMaxResults(1).uniqueResult();
  }

  protected int countNotes(BaseOBObject obj) {
    OBContext.setAdminMode(true);
    try {
      OBCriteria<Note> q = OBDal.getInstance().createCriteria(Note.class);
      q.add(Restrictions.eq(Note.PROPERTY_RECORD, obj.getId()));
      q.add(Restrictions.eq(Note.PROPERTY_TABLE,
          OBDal.getInstance().getProxy(Table.class, obj.getEntity().getTableId())));
      return q.count();
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
