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
 * All portions are Copyright (C) 2016-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.test.security;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.RoleOrganization;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.access.WindowAccess;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.module.Module;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.test.base.TestConstants.Roles;

/**
 * Test cases covering special cases for cross organization references, where they are allowed based
 * on ad_column.allowed_cross_org_link setting.
 * 
 * @author alostale
 *
 */
public class ExplicitCrossOrganizationReference extends CrossOrganizationReference {
  private static final String SALES_ORDER_WINDOW = "143";
  private static String QA_ONLY_SPAIN_ROLE;
  private static final String CORE = "0";
  private static final String ORDER_WAREHOUSE_COLUMN = "2202";
  private static final String ORDER_CREATEDBY_COLUMN = "2166";
  private static final String ORDERLINE_ORDER_COLUMN = "2213";

  private static final List<String> COLUMNS_TO_ALLOW_CROSS_ORG = Arrays
      .asList(ORDER_WAREHOUSE_COLUMN, ORDERLINE_ORDER_COLUMN);

  /**
   * References from org Spain to USA should not be allowed on insertion even in a column allowing
   * it if not in admin mode
   */
  @Test
  @Ignore("Expected exception is not thrown on insert, see issue #32063")
  public void shouldBeIllegalOnInsert() {
    createOrder(SPAIN_ORG, USA_WAREHOUSE);

    exception.expect(OBSecurityException.class);

    OBDal.getInstance().commitAndClose();
  }

  /**
   * References from org Spain to USA should not be allowed on update even in a column allowing it
   * if not in admin mode
   */
  @Test
  public void shouldBeIllegalOnUpdate() {
    Order order = createOrder(SPAIN_ORG, SPAIN_WAREHOUSE);
    order.setWarehouse(OBDal.getInstance().getProxy(Warehouse.class, USA_WAREHOUSE));

    exception.expect(OBSecurityException.class);

    OBDal.getInstance().commitAndClose();
  }

  /**
   * References from org Spain to USA should be allowed on insertion if in cross org admin mode for
   * columns that allow it
   */
  @Test
  public void shouldBeAllowedOnInsertInCrossOrgAdminMode() {
    OBContext.setCrossOrgReferenceAdminMode();
    try {
      createOrder(SPAIN_ORG, USA_WAREHOUSE);

      OBDal.getInstance().commitAndClose();
    } finally {
      OBContext.restorePreviousCrossOrgReferenceMode();
    }
  }

  /**
   * References from org Spain to USA should not be allowed on update if in cross org admin mode for
   * columns that allow it
   */
  @Test
  public void shouldBeAllowedOnUpdateInCrossOrgAdminMode() {
    Order order = createOrder(SPAIN_ORG, SPAIN_WAREHOUSE);
    OBContext.setCrossOrgReferenceAdminMode();
    try {
      order.setWarehouse(OBDal.getInstance().getProxy(Warehouse.class, USA_WAREHOUSE));

      OBDal.getInstance().commitAndClose();
    } finally {
      OBContext.restorePreviousCrossOrgReferenceMode();
    }
  }

  /**
   * For columns not flagged to allow cross org refs, it should not be possible to do it even in
   * cross org admin mode
   */
  @SuppressWarnings("serial")
  @Test
  @Ignore("Expected exception is not thrown on insert, see issue #32063")
  public void shouldBeIllegalOnInsertAdminModeIfColumnNotSet() {
    OBContext.setCrossOrgReferenceAdminMode();
    try {
      createOrder(SPAIN_ORG, new HashMap<String, Object>() {
        {
          put(Order.PROPERTY_BUSINESSPARTNER,
              OBDal.getInstance().getProxy(BusinessPartner.class, USA_BP));
        }
      });

      exception.expect(OBSecurityException.class);

      OBDal.getInstance().commitAndClose();
    } finally {
      OBContext.restorePreviousCrossOrgReferenceMode();
    }
  }

  /**
   * For columns not flagged to allow cross org refs, it should not be possible to do it even in
   * cross org admin mode
   */
  @Test
  public void shouldBeIllegalOnUpdateAdminModeIfColumnNotSet() {
    Order order = createOrder(SPAIN_ORG, SPAIN_WAREHOUSE);
    OBContext.setCrossOrgReferenceAdminMode();
    try {
      exception.expect(OBSecurityException.class);

      order.setCancelledorder(OBDal.getInstance().getProxy(Order.class, USA_ORDER));
      OBDal.getInstance().commitAndClose();
    } finally {
      OBContext.restorePreviousCrossOrgReferenceMode();
    }
  }

  @Test
  @Ignore("Expected exception is not thrown on insert, see issue #32063")
  public void shouldBeIllegalOnChildInsert() {
    createCrossOrgOrderOrderLine();

    exception.expect(OBSecurityException.class);

    OBDal.getInstance().commitAndClose();
  }

  @Test
  public void shouldBeIllegalOnChildUpdate() {
    Order order = createOrder(SPAIN_ORG, SPAIN_WAREHOUSE);
    OrderLine ol = createOrderLine(order);

    ol.setOrganization(OBDal.getInstance().getProxy(Organization.class, USA_ORG));

    exception.expect(OBSecurityException.class);

    OBDal.getInstance().commitAndClose();
  }

  @Test
  public void shouldBeAllowedOnChildInsertInOrgAdminMode() {
    OBContext.setCrossOrgReferenceAdminMode();
    try {
      createCrossOrgOrderOrderLine();

      OBDal.getInstance().commitAndClose();
    } finally {
      OBContext.restorePreviousCrossOrgReferenceMode();
    }
  }

  @Test
  public void shouldBeAllowedOnChildUpdateInOrgAdminMode() {
    OBContext.setCrossOrgReferenceAdminMode();
    try {
      Order order = createOrder(SPAIN_ORG, SPAIN_WAREHOUSE);
      OrderLine ol = createOrderLine(order);

      ol.setOrganization(OBDal.getInstance().getProxy(Organization.class, USA_ORG));

      // warehouse needs to be modified as line-warehouse is not cross-org
      ol.setWarehouse(OBDal.getInstance().getProxy(Warehouse.class, USA_WAREHOUSE));

      OBDal.getInstance().commitAndClose();
    } finally {
      OBContext.restorePreviousCrossOrgReferenceMode();
    }
  }

  /**
   * Fetching children (order.getOrderLineList) should retrieve cross-org elements if role has
   * access to children org
   */
  @Test
  public void childListShouldBeRetrivedIfRoleHasAccess() {
    OBContext.setCrossOrgReferenceAdminMode();
    Order order;
    try {
      order = createCrossOrgOrderOrderLine();
      OBDal.getInstance().flush();
    } finally {
      OBContext.restorePreviousCrossOrgReferenceMode();
    }

    OBDal.getInstance().refresh(order); // force fetch
    assertThat("Number of lines in order", order.getOrderLineList(), hasSize(1));
  }

  /**
   * When fetching children elements (order.getOrderLineList), children's organization is not
   * checked, so even they are cross-org and the role has no access to them, they are present in the
   * bag.
   */
  @Test
  public void childListShouldBeRetrivedEvenIfRoleHasNoAccess() {
    OBContext.setCrossOrgReferenceAdminMode();
    Order order;
    try {
      order = createCrossOrgOrderOrderLine();
      OBDal.getInstance().flush();
    } finally {
      OBContext.restorePreviousCrossOrgReferenceMode();
    }

    setSpainQARole();

    OBDal.getInstance().refresh(order); // force fetch
    assertThat("Number of lines in order", order.getOrderLineList(), hasSize(1));
  }

  @Test
  public void byDefaultCrossOrgAdminShouldBeDisabled() {
    assertThat("isCrossOrgAdministratorMode",
        OBContext.getOBContext().isInCrossOrgAdministratorMode(), is(false));
  }

  @Test
  public void crossOrgAdminModeShoudBePossibleToEnable() {
    OBContext.setCrossOrgReferenceAdminMode();

    assertThat("isCrossOrgAdministratorMode",
        OBContext.getOBContext().isInCrossOrgAdministratorMode(), is(true));

    OBContext.restorePreviousCrossOrgReferenceMode();
  }

  @Test
  public void crossOrgAdminShouldWorkAsAnStack() {
    OBContext.setCrossOrgReferenceAdminMode();
    OBContext.setCrossOrgReferenceAdminMode(); // twice in the stack

    assertThat("isCrossOrgAdministratorMode",
        OBContext.getOBContext().isInCrossOrgAdministratorMode(), is(true));

    OBContext.restorePreviousCrossOrgReferenceMode(); // pops 1

    assertThat("isCrossOrgAdministratorMode",
        OBContext.getOBContext().isInCrossOrgAdministratorMode(), is(true));

    OBContext.restorePreviousCrossOrgReferenceMode();
    assertThat("isCrossOrgAdministratorMode",
        OBContext.getOBContext().isInCrossOrgAdministratorMode(), is(false));
  }

  @Test
  public void adminAndCrossOrgAdminAreIndependent() {
    setTestLogAppenderLevel(Level.WARN);
    OBContext.setAdminMode();
    assertThat("admin mode", OBContext.getOBContext().isInAdministratorMode(), is(true));
    assertThat("cross org admin mode", OBContext.getOBContext().isInCrossOrgAdministratorMode(),
        is(false));

    OBContext.setCrossOrgReferenceAdminMode();
    assertThat("admin mode", OBContext.getOBContext().isInAdministratorMode(), is(true));
    assertThat("cross org admin mode", OBContext.getOBContext().isInCrossOrgAdministratorMode(),
        is(true));

    OBContext.restorePreviousMode();
    assertThat("admin mode", OBContext.getOBContext().isInAdministratorMode(), is(false));
    assertThat("cross org admin mode", OBContext.getOBContext().isInCrossOrgAdministratorMode(),
        is(true));

    OBContext.restorePreviousCrossOrgReferenceMode();
    assertThat("admin mode", OBContext.getOBContext().isInAdministratorMode(), is(false));
    assertThat("cross org admin mode", OBContext.getOBContext().isInCrossOrgAdministratorMode(),
        is(false));
    assertThat(getTestLogAppender().getMessages(Level.WARN), hasSize(0));
  }

  @Test
  public void unbalancedRestorePreviousCrossOrgAdminShouldLogWarn() {
    setTestLogAppenderLevel(Level.WARN);

    OBContext.setCrossOrgReferenceAdminMode();
    OBContext.restorePreviousCrossOrgReferenceMode();
    assertThat(getTestLogAppender().getMessages(Level.WARN), hasSize(0));

    OBContext.restorePreviousCrossOrgReferenceMode();
    assertThat(getTestLogAppender().getMessages(Level.WARN),
        hasItem(containsString("Unbalanced calls to setCrossOrgReferenceAdminMode")));
  }

  @Test
  public void unbalancedRestorePreviousCrossOrgAdminShouldLogStackIfEnabled() {
    setTestLogAppenderLevel(Level.WARN);
    OBContext.setAdminTraceSize(1);

    OBContext.setCrossOrgReferenceAdminMode();
    OBContext.restorePreviousCrossOrgReferenceMode();
    assertThat(getTestLogAppender().getMessages(Level.WARN), hasSize(0));

    OBContext.restorePreviousCrossOrgReferenceMode();
    assertThat(getTestLogAppender().getMessages(Level.WARN), hasItem(containsString(
        "org.openbravo.test.security.ExplicitCrossOrganizationReference.unbalancedRestorePreviousCrossOrgAdminShouldLogStackIfEnabled")));
  }

  @Test
  public void unbalancedOrgAdminThreadFinalizationShouldLogWarn()
      throws NoSuchMethodException, SecurityException, IllegalAccessException,
      IllegalArgumentException, InvocationTargetException {
    setTestLogAppenderLevel(Level.WARN);
    OBContext.setCrossOrgReferenceAdminMode();

    // OBContext.clearAdminModeStack is invoked on request thread finalization, invoke it here
    // directly making it accessible first
    Method clearAdminModeStack = OBContext.class.getDeclaredMethod("clearAdminModeStack");
    clearAdminModeStack.setAccessible(true);
    clearAdminModeStack.invoke(null);

    assertThat(getTestLogAppender().getMessages(Level.WARN),
        hasItem(containsString("Unbalanced calls to setCrossOrgReferenceAdminMode")));
  }

  /**
   * Creates an order in org Spain with 2 lines: 1 in Spain another in USA. Then fetches OrderLine
   * data source with QA Admin role which has access to both organizations, so both lines should be
   * retrieved
   */
  @Test
  public void orderLineDSShouldShowAllChildren() throws Exception {
    JSONObject resp = createOrderAndFetchLines(Roles.QA_ADMIN_ROLE);
    assertThat("number of fetched order lines", resp.getJSONObject("response").getInt("totalRows"),
        is(2));
  }

  /**
   * Creates same order than {@link #orderLineDSShouldShowAllChildren()}. Fetch is done with role
   * with access to only Spain, so only 1 line is expected to be retrieved.
   * 
   * Note this test will be needed to be changed in case cross org references is also implemented
   * for the UI.
   */
  @Test
  public void orderLineDSShouldHideNonAccessibleChildren() throws Exception {
    JSONObject resp = createOrderAndFetchLines(QA_ONLY_SPAIN_ROLE);
    assertThat("number of fetched order lines", resp.getJSONObject("response").getInt("totalRows"),
        is(1));
  }

  /**
   * Cross org column value cannot changed if column's module is not in dev. The opposite (value can
   * be changed if mod in dev), is implicitly tested in @BeforeClass method.
   */
  @Test
  public void itShouldntBeAllowedToSetCrossOrgIfModNotInDev() {
    OBContext prevCtxt = OBContext.getOBContext();
    OBContext.setOBContext("0");

    Module core = OBDal.getInstance().get(Module.class, CORE);
    try {
      core.setInDevelopment(false);

      Column orderCreatedBy = OBDal.getInstance().get(Column.class, ORDER_CREATEDBY_COLUMN);
      orderCreatedBy.setAllowedCrossOrganizationReference(true);

      exception.expect(Exception.class);
      OBDal.getInstance().commitAndClose();
    } finally {
      OBDal.getInstance().rollbackAndClose();
      OBContext.setOBContext(prevCtxt);
    }
  }

  @SuppressWarnings("serial")
  private JSONObject createOrderAndFetchLines(String roleId) throws Exception {
    Order order = createOrder(SPAIN_ORG);
    createOrderLine(order);
    createOrderLine(order, new HashMap<String, Object>() {
      {
        put(Order.PROPERTY_ORGANIZATION, OBDal.getInstance().getProxy(Organization.class, USA_ORG));
      }
    });
    OBDal.getInstance().commitAndClose();

    changeProfile(roleId, "192", SPAIN_ORG, SPAIN_WAREHOUSE);

    Map<String, String> params = new HashMap<String, String>();

    JSONObject criteria = new JSONObject();
    criteria.put("fieldName", "salesOrder");
    criteria.put("operator", "equals");
    criteria.put("value", order.getId());
    params.put("criteria", criteria.toString());

    params.put("_operationType", "fetch");
    params.put("_startRow", "0");
    params.put("_endRow", "100");
    return new JSONObject(
        doRequest("/org.openbravo.service.datasource/OrderLine", params, 200, "POST"));
  }

  @SuppressWarnings("serial")
  private Order createCrossOrgOrderOrderLine() {
    Order order = createOrder(SPAIN_ORG, SPAIN_WAREHOUSE);
    createOrderLine(order, new HashMap<String, Object>() {
      {
        put(OrderLine.PROPERTY_ORGANIZATION, OBDal.getInstance().get(Organization.class, USA_ORG));
      }
    });
    return order;
  }

  private void setSpainQARole() {
    OBContext.setOBContext("100", QA_ONLY_SPAIN_ROLE, QA_TEST_CLIENT_ID, SPAIN_ORG);
  }

  @BeforeClass
  public static void setUpAllowedCrossOrg() throws Exception {
    CrossOrganizationReference.setUpAllowedCrossOrg(COLUMNS_TO_ALLOW_CROSS_ORG, true);

    Role qaRole = createOrgUserLevelRole();
    QA_ONLY_SPAIN_ROLE = qaRole.getId();
    grantWindowAccess(qaRole, SALES_ORDER_WINDOW);

    OBDal.getInstance().commitAndClose();
  }

  /** Creates a role with Org user level, and access to Spain Org */
  static Role createOrgUserLevelRole() {
    setQAAdminContext();
    Role spainRole = OBProvider.getInstance().get(Role.class);
    spainRole.setName("QA Only Spain - " + System.currentTimeMillis()); // some randomness
    spainRole.setOrganization(OBDal.getInstance().getProxy(Organization.class, "0"));
    spainRole.setManual(true);
    spainRole.setUserLevel("  O");
    OBDal.getInstance().save(spainRole);
    createdObjects.add(spainRole);

    RoleOrganization orgAccess = OBProvider.getInstance().get(RoleOrganization.class);
    orgAccess.setOrganization(OBDal.getInstance().getProxy(Organization.class, SPAIN_ORG));
    orgAccess.setRole(spainRole);
    OBDal.getInstance().save(orgAccess);
    createdObjects.add(orgAccess);

    UserRoles userRole = OBProvider.getInstance().get(UserRoles.class);
    userRole.setOrganization(OBDal.getInstance().getProxy(Organization.class, "0"));
    userRole.setRole(spainRole);
    userRole.setUserContact(OBDal.getInstance().getProxy(User.class, "100"));
    OBDal.getInstance().save(userRole);
    createdObjects.add(userRole);

    return spainRole;
  }

  static void grantWindowAccess(Role role, String windowId) {
    WindowAccess windowAccess = OBProvider.getInstance().get(WindowAccess.class);
    final OBCriteria<Window> obCriteria = OBDal.getInstance().createCriteria(Window.class);
    obCriteria.add(Restrictions.eq(Window.PROPERTY_ID, windowId));
    obCriteria.setMaxResults(1);
    windowAccess.setClient(role.getClient());
    windowAccess.setOrganization(role.getOrganization());
    windowAccess.setRole(role);
    windowAccess.setWindow((Window) obCriteria.uniqueResult());
    windowAccess.setEditableField(true);
    OBDal.getInstance().save(windowAccess);
    createdObjects.add(windowAccess);
  }

  @AfterClass
  public static void resetAD() throws Exception {
    CrossOrganizationReference.setUpAllowedCrossOrg(COLUMNS_TO_ALLOW_CROSS_ORG, false);

    OBDal.getInstance().commitAndClose();
  }

}
