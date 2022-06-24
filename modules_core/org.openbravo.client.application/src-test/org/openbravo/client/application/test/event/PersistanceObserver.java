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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.test.base.Issue;

/** Test cases ensuring PersistanceObservers are executed when they should. */
@Issue("35060")
public class PersistanceObserver extends ObserverBaseTest {

  @Test
  public void checkDirtyShouldNotExecuteUpdateObservers() {
    OBContext.setAdminMode(false);

    observerExecutionType = ObserverExecutionType.CREATE_NOTE;
    OrderLine ol = pickARandomOrderLine();
    updateLineDescription(ol);
    OBDal.getInstance().isSessionDirty();
    OBContext.restorePreviousMode();
    OBDal.getInstance().rollbackAndClose();
    assertThat("Observer executions", OrderLineTestObserver.getNumberOfExecutions(), is(0));
  }

  @Test
  public void checkDirtyShouldNotExecuteInsertObservers() {
    observerExecutionType = ObserverExecutionType.ON_NOOP;
    OBContext.setAdminMode(false);
    OrderLine ol = OBProvider.getInstance().get(OrderLine.class);
    ol.setSalesOrder(pickARandomOrder());
    OBDal.getInstance().save(ol);
    assertThat("Observer executions", OrderLineTestObserver.getNumberOfExecutions(), is(1));

    OBDal.getInstance().isSessionDirty();
    OBContext.restorePreviousMode();
    OBDal.getInstance().rollbackAndClose();
    assertThat("Observer executions", OrderLineTestObserver.getNumberOfExecutions(), is(1));
  }

  @Test
  public void checkDirtyShouldNotExecuteDeleteObservers() {
    observerExecutionType = ObserverExecutionType.ON_NOOP;
    OBContext.setAdminMode(false);
    OrderLine ol = pickARandomOrderLine();
    OBDal.getInstance().remove(ol);

    assertThat("Observer executions", OrderLineTestObserver.getNumberOfExecutions(), is(1));

    OBDal.getInstance().isSessionDirty();
    OBContext.restorePreviousMode();
    OBDal.getInstance().rollbackAndClose();
    assertThat("Observer executions", OrderLineTestObserver.getNumberOfExecutions(), is(1));
  }

  @Test
  public void flushShouldExecuteUpdateObserversOnce() {
    OBContext.setAdminMode(false);

    observerExecutionType = ObserverExecutionType.CREATE_NOTE;
    OrderLine ol = pickARandomOrderLine();
    int notesBeforeUpdate = countNotes(ol);

    updateLineDescription(ol);
    OBDal.getInstance().flush();
    int notesAfterUpdate = countNotes(ol);

    OBContext.restorePreviousMode();

    assertThat("Observer executions", OrderLineTestObserver.getNumberOfExecutions(), is(1));
    assertThat("created notes", notesAfterUpdate - notesBeforeUpdate, is(1));
  }

  @Test
  public void commitAndCloseShouldExecuteUpdateObserversOnce() {
    OBContext.setAdminMode(false);

    observerExecutionType = ObserverExecutionType.CREATE_NOTE;
    OrderLine ol = pickARandomOrderLine();
    int notesBeforeUpdate = countNotes(ol);

    updateLineDescription(ol);
    OBDal.getInstance().commitAndClose();
    int notesAfterUpdate = countNotes(ol);

    OBContext.restorePreviousMode();

    assertThat("Observer executions", OrderLineTestObserver.getNumberOfExecutions(), is(1));
    assertThat("created notes", notesAfterUpdate - notesBeforeUpdate, is(1));
  }

  @Test
  public void flushShouldExecuteInsertObservers() {
    observerExecutionType = ObserverExecutionType.ON_NOOP;
    OBContext.setAdminMode(false);
    OrderLine ol = OBProvider.getInstance().get(OrderLine.class);
    ol.setSalesOrder(pickARandomOrder());
    OBDal.getInstance().save(ol);

    try {
      OBDal.getInstance().getSession().flush();
    } catch (Exception expected) {
    }

    OBContext.restorePreviousMode();
    OBDal.getInstance().rollbackAndClose();
    assertThat("Observer executions", OrderLineTestObserver.getNumberOfExecutions(), is(1));
  }

  @Test
  public void flushShouldExecuteDeleteObservers() {
    observerExecutionType = ObserverExecutionType.ON_NOOP;
    OBContext.setAdminMode(false);
    OrderLine ol = pickARandomOrderLine();
    OBDal.getInstance().remove(ol);

    try {
      OBDal.getInstance().getSession().flush();
    } catch (Exception expected) {
    }

    OBContext.restorePreviousMode();
    OBDal.getInstance().rollbackAndClose();
    assertThat("Observer executions", OrderLineTestObserver.getNumberOfExecutions(), is(1));
  }

  private void updateLineDescription(OrderLine ol) {
    String randomDescription = Long.toString(System.currentTimeMillis());
    ol.setDescription(randomDescription);
  }
}
