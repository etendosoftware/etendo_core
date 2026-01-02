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
 * All portions are Copyright (C) 2013-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.dal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.QueryException;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.query.sqm.PathElementException;
import org.hibernate.stat.SessionStatistics;
import org.junit.jupiter.api.Test;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.dal.service.Restrictions;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.Order_ComputedColumns;
import org.openbravo.service.json.DataResolvingMode;
import org.openbravo.service.json.DataToJsonConverter;
import org.openbravo.test.base.OBBaseTest;

/**
 * Set of tests for computed columns lazy loading
 *
 * @author alostale
 */
public class ComputedColumnsTest extends OBBaseTest {
  private static final String EXCEPTION_MSG = "Could not resolve attribute 'deliveryStatus' of 'Order'";

  /**
   * Tests computed columns are lazily loaded
   */
  @Test
  public void testLazyLoad() {
    setTestUserContext();

    // load one order
    OBCriteria<Order> qOrder = OBDal.getInstance().createCriteria(Order.class);
    qOrder.setMaxResults(1);
    Order order = (Order) qOrder.uniqueResult();

    // check it is in memory but computed columns are not already loaded
    assertTrue(dalObjectLoaded(Order.ENTITY_NAME, order.getId()), "DAL Order loaded");
    assertFalse(dalObjectLoaded(Order_ComputedColumns.ENTITY_NAME, order.getId()),
        "DAL Order computed columns shouldn't be loaded");

    // load computed columns
    order.getDeliveryStatus();

    // check they are now loaded in memory
    assertTrue(dalObjectLoaded(Order_ComputedColumns.ENTITY_NAME, order.getId()),
        "DAL Order computed columns should be loaded");
  }

  /**
   * Test it is possible to filter in HQL by computed columns accessing them through the proxy.
   * <p>
   * Note this way of filtering is potentially harmful in terms of performance because computed
   * column need to be calculated in order to do the filtering.
   */
  @Test
  public void testComputedColumnHQLFilter() {
    setTestUserContext();

    // filtering by computed column through proxy
    OBQuery<Order> qOrder = OBDal.getInstance()
        .createQuery(Order.class, "as o where o._computedColumns.deliveryStatus = 100");

    qOrder.count();
  }

  /**
   * Tests computed columns can not be used in OBCriteria
   */
  @Test
  public void testComputedColumnCriteriaFilter() {
    setTestUserContext();
    boolean thrown = false;
    try {
      // try to filter in Criteria by computed column...
      OBCriteria<Order> qOrder = OBDal.getInstance().createCriteria(Order.class);
      qOrder.add(Restrictions.eq(Order.COMPUTED_COLUMN_DELIVERYSTATUS, 100));
      qOrder.count();
    } catch (QueryException | PathElementException e) {
      thrown = e.getMessage().startsWith(EXCEPTION_MSG);
    }

    // ... it shouldn't be possible
    assertTrue(thrown, "Computed columns shouldn't be usable in OBCriteria");
  }

  /**
   * Direct access to computed columns in HQL was allowed prior to MP27, now it is not anymore and
   * proxy needs to be used.
   */
  @Test
  public void testComputedColumnHQLFilterOldWay() {
    setTestUserContext();

    // try to filter in HQL directly by computed column...
    OBQuery<Order> qOrder = OBDal.getInstance()
        .createQuery(Order.class, "as o where o.deliveryStatus = 100");

    boolean thrown = false;
    try {
      qOrder.count();
    } catch (IllegalArgumentException e) {
      thrown = e.getCause().getMessage().startsWith(EXCEPTION_MSG);
    }

    // ... it shouldn't be possible, proxy should be used to reach it
    assertTrue(thrown, "Computed columns can't be directly used in HQL");
  }

  @SuppressWarnings("unchecked")
  private boolean dalObjectLoaded(String entityName, String id) {
    SessionStatistics stats = SessionHandler.getInstance().getSession().getStatistics();
    for (EntityKey k : (Set<EntityKey>) stats.getEntityKeys()) {
      if (entityName.equals(k.getEntityName()) && id.equals(k.getIdentifier())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Tests issue #25862
   * <p>
   * Computed columns for a new object should be null when converting to JSON
   */
  @Test
  public void testJSONConverter() throws JSONException {
    Order myOrder = OBProvider.getInstance().get(Order.class);
    DataToJsonConverter toJsonConverter = OBProvider.getInstance().get(DataToJsonConverter.class);
    JSONObject json = toJsonConverter.toJsonObject(myOrder, DataResolvingMode.FULL);

    assertTrue(json.isNull(Order.COMPUTED_COLUMN_DELIVERYSTATUS),
        "delivery status property should be present and null");
  }
}
