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
 * All portions are Copyright (C) 2010-2017 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.test.preference;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Test;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.Restrictions;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyConflictException;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.module.Module;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.test.base.OBBaseTest;

/**
 * Test cases covering Preferences visibility and conflict resolution handling.
 * 
 * Note these test cases are dependent one on each other, therefore all of them must be executed one
 * after the other sorted alphabetically.
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class PreferenceTest extends OBBaseTest {

  private static final String SALES_ROLE_ID = "FA057013C10148828D2430F66F42EF1A";

  /**
   * F&B International Group
   */
  private static final String ORG_B = "19404EAD144C49A0AF37D54377CF452D";

  /**
   * F&B US, Inc.
   */
  private static final String ORG_B1 = "2E60544D37534C0B89E765FE29BC0B43";

  /**
   * F&B US East Coast
   */
  private static final String ORG_B11 = "7BABA5FF80494CAFA54DEBD22EC46F01";

  /**
   * F&B US West Coast
   */
  private static final String ORG_B12 = "BAE22373FEBE4CCCA24517E23F0C8A48";

  /**
   * F&B España, S.A.
   */
  private static final String ORG_B2 = "B843C30461EA4501935CB1D125C9C25A";

  @Test
  public void test00CreatePreference() {
    setSystemAdministratorContext();

    Preferences.setPreferenceValue("testProperty", "testValue", false, null, null, null, null, null,
        null);
    OBDal.getInstance().flush();

    OBCriteria<Preference> qPref = OBDal.getInstance().createCriteria(Preference.class);
    qPref.add(Restrictions.eq(Preference.PROPERTY_ATTRIBUTE, "testProperty"));

    List<Preference> prefs = qPref.list();
    assertFalse(prefs.isEmpty(), "No property has been set");
    assertEquals("testValue", prefs.get(0).getSearchKey(), "Property does not contain the expected value");
    OBDal.getInstance().commitAndClose();
  }

  @Test
  public void test01OverwritePreference() {
    setSystemAdministratorContext();

    Preferences.setPreferenceValue("testProperty", "newValue", false, null, null, null, null, null,
        null);
    OBDal.getInstance().flush();

    OBCriteria<Preference> qPref = OBDal.getInstance().createCriteria(Preference.class);
    qPref.add(Restrictions.eq(Preference.PROPERTY_ATTRIBUTE, "testProperty"));

    List<Preference> prefs = qPref.list();
    assertFalse(prefs.isEmpty(), "No property has been set");
    assertEquals(1, prefs.size(), "There should be only one property, found:" + prefs.size());
    assertEquals("newValue", prefs.get(0).getSearchKey(), "Property does not contain the expected value");
    OBDal.getInstance().commitAndClose();
  }

  @Test
  public void test02SamePropertyDifferentVisibility() {
    setSystemAdministratorContext();

    Role role = OBDal.getInstance().get(Role.class, SALES_ROLE_ID); // Sales

    Preferences.setPreferenceValue("testProperty", "salesValue", false, null, null, null, role,
        null, null);
    OBDal.getInstance().flush();

    OBCriteria<Preference> qPref = OBDal.getInstance().createCriteria(Preference.class);
    qPref.add(Restrictions.eq(Preference.PROPERTY_ATTRIBUTE, "testProperty"));

    List<Preference> prefs = qPref.list();
    assertEquals(2, prefs.size(), "There should be only 2 properties, found:" + prefs.size());
    OBDal.getInstance().commitAndClose();
  }

  @Test
  public void test03PropertyGet() throws PropertyException {
    setSystemAdministratorContext();
    String value = Preferences.getPreferenceValue("testProperty", false,
        OBContext.getOBContext().getCurrentClient(),
        OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
        OBContext.getOBContext().getRole(), null);
    assertEquals("newValue", value, "Not found expected value.");

    Role role = OBDal.getInstance().get(Role.class, SALES_ROLE_ID); // Sales
    value = Preferences.getPreferenceValue("testProperty", false,
        OBContext.getOBContext().getCurrentClient(),
        OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(), role,
        null);
    assertEquals("salesValue", value, "Not found expected value.");
  }

  @Test
  public void test04PLPropertyGet() throws SQLException {
    setSystemAdministratorContext();
    String value = getPLPreference("testProperty", false,
        OBContext.getOBContext().getCurrentClient(),
        OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
        OBContext.getOBContext().getRole(), null);
    assertEquals("newValue", value, "Not found expected value.");

    Role role = OBDal.getInstance().get(Role.class, SALES_ROLE_ID); // Sales
    value = getPLPreference("testProperty", false, OBContext.getOBContext().getCurrentClient(),
        OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(), role,
        null);
    assertEquals("salesValue", value, "Not found expected value.");
  }

  @Test
  public void test05WindowVisibility() throws PropertyException {
    setSystemAdministratorContext();
    Window window = OBDal.getInstance().get(Window.class, "276"); // Alert window
    Preferences.setPreferenceValue("testProperty", "alertGeneral", false, null, null, null, null,
        window, null);

    Role role = OBDal.getInstance().get(Role.class, SALES_ROLE_ID); // Sales
    Preferences.setPreferenceValue("testProperty", "alertSales", false, null, null, null, role,
        window, null);
    OBDal.getInstance().flush();

    String value = Preferences.getPreferenceValue("testProperty", false,
        OBContext.getOBContext().getCurrentClient(),
        OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
        OBContext.getOBContext().getRole(), null);
    assertEquals("newValue", value, "Not found expected value.");

    value = Preferences.getPreferenceValue("testProperty", false,
        OBContext.getOBContext().getCurrentClient(),
        OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(), role,
        null);
    assertEquals("salesValue", value, "Not found expected value.");

    value = Preferences.getPreferenceValue("testProperty", false,
        OBContext.getOBContext().getCurrentClient(),
        OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
        OBContext.getOBContext().getRole(), window);
    assertEquals("alertGeneral", value, "Not found expected value.");

    value = Preferences.getPreferenceValue("testProperty", false,
        OBContext.getOBContext().getCurrentClient(),
        OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(), role,
        window);
    assertEquals("alertSales", value, "Not found expected value.");
    OBDal.getInstance().commitAndClose();
  }

  @Test
  public void test06PLWindowVisibility() throws SQLException {
    setSystemAdministratorContext();
    Window window = OBDal.getInstance().get(Window.class, "276"); // Alert window
    Preferences.setPreferenceValue("testProperty", "alertGeneral", false, null, null, null, null,
        window, null);

    Role role = OBDal.getInstance().get(Role.class, SALES_ROLE_ID); // Sales
    String value = getPLPreference("testProperty", false,
        OBContext.getOBContext().getCurrentClient(),
        OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
        OBContext.getOBContext().getRole(), null);
    assertEquals("newValue", value, "Not found expected value.");

    value = getPLPreference("testProperty", false, OBContext.getOBContext().getCurrentClient(),
        OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(), role,
        null);
    assertEquals("salesValue", value, "Not found expected value.");

    value = getPLPreference("testProperty", false, OBContext.getOBContext().getCurrentClient(),
        OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
        OBContext.getOBContext().getRole(), window);
    assertEquals("alertGeneral", value, "Not found expected value.");

    value = getPLPreference("testProperty", false, OBContext.getOBContext().getCurrentClient(),
        OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(), role,
        window);
    assertEquals("alertSales", value, "Not found expected value.");
    OBDal.getInstance().commitAndClose();
  }

  @Test
  public void test07OrgVisibility() throws PropertyException {
    setSystemAdministratorContext();
    Client client = OBDal.getInstance().get(Client.class, TEST_CLIENT_ID);
    Organization orgB = OBDal.getInstance().get(Organization.class, ORG_B);
    Organization orgB1 = OBDal.getInstance().get(Organization.class, ORG_B1);
    Organization orgB2 = OBDal.getInstance().get(Organization.class, ORG_B2);
    Organization orgB11 = OBDal.getInstance().get(Organization.class, ORG_B11);
    Organization orgB12 = OBDal.getInstance().get(Organization.class, ORG_B12);

    Preference p = Preferences.setPreferenceValue("testProperty", "B", false, null, orgB, null,
        null, null, null);
    Preferences.setPreferenceValue("testProperty", "B2", false, null, orgB2, null, null, null,
        null);
    Preferences.setPreferenceValue("testProperty", "B12", false, null, orgB12, null, null, null,
        null);
    OBDal.getInstance().flush();

    assertEquals(orgB, p.getVisibleAtOrganization(), "Preference not set in the expected visible org");

    String value = Preferences.getPreferenceValue("testProperty", false,
        OBContext.getOBContext().getCurrentClient(),
        OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
        OBContext.getOBContext().getRole(), null);
    assertEquals("newValue", value, "Not found expected value.");

    value = Preferences.getPreferenceValue("testProperty", false, client, orgB,
        OBContext.getOBContext().getUser(), OBContext.getOBContext().getRole(), null);
    assertEquals("B", value, "Not found expected value.");

    value = Preferences.getPreferenceValue("testProperty", false, client, orgB1,
        OBContext.getOBContext().getUser(), OBContext.getOBContext().getRole(), null);
    assertEquals("B", value, "Not found expected value.");

    value = Preferences.getPreferenceValue("testProperty", false, client, orgB2,
        OBContext.getOBContext().getUser(), OBContext.getOBContext().getRole(), null);
    assertEquals("B2", value, "Not found expected value.");

    value = Preferences.getPreferenceValue("testProperty", false, client, orgB11,
        OBContext.getOBContext().getUser(), OBContext.getOBContext().getRole(), null);
    assertEquals("B", value, "Not found expected value.");

    value = Preferences.getPreferenceValue("testProperty", false, client, orgB12,
        OBContext.getOBContext().getUser(), OBContext.getOBContext().getRole(), null);
    assertEquals("B12", value, "Not found expected value.");
    OBDal.getInstance().commitAndClose();
  }

  @Test
  public void test08ClientVisibility() throws PropertyException {
    setSystemAdministratorContext();
    Client testClient = OBDal.getInstance().getProxy(Client.class, TEST_CLIENT_ID);
    Client systemClient = OBDal.getInstance().getProxy(Client.class, "0");

    final String propAttribute = "clientPropertyTest";
    Preference p1 = Preferences.setPreferenceValue(propAttribute, "test client", false, testClient,
        null, null, null, null, null);
    Preference p2 = Preferences.setPreferenceValue(propAttribute, "system", false, systemClient,
        null, null, null, null, null);

    OBDal.getInstance().flush();

    String valueClient = Preferences.getPreferenceValue(propAttribute, false, testClient,
        OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
        OBContext.getOBContext().getRole(), null);
    assertThat("preference for client", valueClient, is("test client"));

    String valueSystem = Preferences.getPreferenceValue(propAttribute, false, systemClient,
        OBDal.getInstance().getProxy(Organization.class, "0"), OBContext.getOBContext().getUser(),
        OBContext.getOBContext().getRole(), null);

    assertThat("preference for system", valueSystem, is("system"));

    OBDal.getInstance().remove(p1);
    OBDal.getInstance().remove(p2);
  }

  @Test
  public void test09PLOrgVisibility() throws SQLException {

    setSystemAdministratorContext();
    Client client = OBDal.getInstance().get(Client.class, TEST_CLIENT_ID);
    Organization orgB = OBDal.getInstance().get(Organization.class, ORG_B);
    Organization orgB1 = OBDal.getInstance().get(Organization.class, ORG_B1);
    Organization orgB2 = OBDal.getInstance().get(Organization.class, ORG_B2);
    Organization orgB11 = OBDal.getInstance().get(Organization.class, ORG_B11);
    Organization orgB12 = OBDal.getInstance().get(Organization.class, ORG_B12);

    String value = getPLPreference("testProperty", false,
        OBContext.getOBContext().getCurrentClient(),
        OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
        OBContext.getOBContext().getRole(), null);
    assertEquals("newValue", value, "Not found expected value.");

    value = getPLPreference("testProperty", false, client, orgB, OBContext.getOBContext().getUser(),
        OBContext.getOBContext().getRole(), null);
    assertEquals("B", value, "Not found expected value.");

    value = getPLPreference("testProperty", false, client, orgB1,
        OBContext.getOBContext().getUser(), OBContext.getOBContext().getRole(), null);
    assertEquals("B", value, "Not found expected value.");

    value = getPLPreference("testProperty", false, client, orgB2,
        OBContext.getOBContext().getUser(), OBContext.getOBContext().getRole(), null);
    assertEquals("B2", value, "Not found expected value.");

    value = getPLPreference("testProperty", false, client, orgB11,
        OBContext.getOBContext().getUser(), OBContext.getOBContext().getRole(), null);
    assertEquals("B", value, "Not found expected value.");

    value = getPLPreference("testProperty", false, client, orgB12,
        OBContext.getOBContext().getUser(), OBContext.getOBContext().getRole(), null);
    assertEquals("B12", value, "Not found expected value.");
    OBDal.getInstance().commitAndClose();
  }

  @Test
  public void test10ExceptionNotFound() {
    PropertyException exception = null;
    try {
      Preferences.getPreferenceValue("testNotExists", false,
          OBContext.getOBContext().getCurrentClient(),
          OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
          null, null);
    } catch (PropertyException e) {
      exception = e;
    }
    assertNotNull(exception, "Expected exception PropertyNotFoundException");
    assertTrue(exception instanceof org.openbravo.erpCommon.utility.PropertyNotFoundException, "Expected exception PropertyNotFoundException");
    OBDal.getInstance().commitAndClose();
  }

  @Test
  public void test11PLExceptionNotFound() {
    SQLException exception = null;
    try {
      getPLPreference("testNotExists", false, OBContext.getOBContext().getCurrentClient(),
          OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
          null, null);
    } catch (SQLException e) {
      exception = e;
    }
    assertNotNull(exception, "Expected exception PropertyNotFoundException");
    assertTrue(exception.getMessage().contains("@PropertyNotFound@"), "Expected exception PropertyNotFoundException, found: " + exception.getMessage());
    OBDal.getInstance().commitAndClose();
  }

  @Test
  public void test12Conflict() {
    setSystemAdministratorContext();
    Preference newPref = OBProvider.getInstance().get(Preference.class);
    newPref.setPropertyList(false);
    newPref.setAttribute("testProperty");
    newPref.setSearchKey("anotherValue");
    OBDal.getInstance().save(newPref);
    OBDal.getInstance().flush();

    PropertyException exception = null;
    try {
      Preferences.getPreferenceValue("testProperty", false,
          OBContext.getOBContext().getCurrentClient(),
          OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
          null, null);
    } catch (PropertyException e) {
      exception = e;
    }
    assertNotNull(exception, "Expected exception PropertyConflictException");
    assertTrue(exception instanceof PropertyConflictException, "Expected exception PropertyConflictException");
    OBDal.getInstance().commitAndClose();
  }

  @Test
  public void test13PLConflict() {
    setSystemAdministratorContext();
    SQLException exception = null;
    try {
      getPLPreference("testProperty", false, OBContext.getOBContext().getCurrentClient(),
          OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
          null, null);
    } catch (SQLException e) {
      exception = e;
    }
    assertNotNull(exception, "Expected exception PropertyConflictException");
    assertTrue(exception.getMessage().contains("@PropertyConflict@"), "Expected exception PropertyConflictException, found: " + exception.getMessage());
    OBDal.getInstance().commitAndClose();
  }

  @Test
  public void test14SolvedConflict() throws PropertyException {
    setSystemAdministratorContext();

    // This piece of code doesn't work because of issue #13153
    // OBCriteria<Preference> qPref = OBDal.getInstance().createCriteria(Preference.class);
    // qPref.addEqual(Preference.PROPERTY_ATTRIBUTE, "testProperty");
    // qPref.addEqual(Preference.PROPERTY_SEARCHKEY, "anotherValue");
    //
    // Preference newPref = qPref.list().get(0);

    Preference newPref = null;
    OBCriteria<Preference> qPref = OBDal.getInstance().createCriteria(Preference.class);
    qPref.add(Restrictions.eq(Preference.PROPERTY_ATTRIBUTE, "testProperty"));
    for (Preference p : qPref.list()) {
      if (p.getSearchKey().equals("anotherValue")) {
        newPref = p;
      }
    }
    newPref.setSelected(true);
    OBDal.getInstance().flush();

    String value = Preferences.getPreferenceValue("testProperty", false,
        OBContext.getOBContext().getCurrentClient(),
        OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
        OBContext.getOBContext().getRole(), null);
    assertEquals("anotherValue", value, "Not found expected value.");
    OBDal.getInstance().commitAndClose();
  }

  @Test
  public void test15PLSolvedConflict() throws SQLException {
    setSystemAdministratorContext();
    String value = getPLPreference("testProperty", false,
        OBContext.getOBContext().getCurrentClient(),
        OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
        OBContext.getOBContext().getRole(), null);
    assertEquals("anotherValue", value, "Not found expected value.");
  }

  @Test
  public void test16PreferenceClientOrgSetting() {
    setTestAdminContext();
    Preference p = Preferences.setPreferenceValue("testProperty2", "testValue", false, null, null,
        null, null, null, null);
    assertEquals("0", p.getClient().getId(), "Incorrect Client ID");
    assertEquals("0", p.getOrganization().getId(), "Incorrect Org ID");
    OBDal.getInstance().commitAndClose();
  }

  @Test
  public void test17PreferenceListSetAndGet() throws PropertyException {
    setSystemAdministratorContext();

    // Property configuration list
    Reference refProperties = OBDal.getInstance()
        .get(Reference.class, "A26BA480E2014707B47257024C3CBFF7");
    Module mod = OBDal.getInstance().get(Module.class, "0");
    boolean devStatus = mod.isInDevelopment();
    mod.setInDevelopment(true);
    OBDal.getInstance().commitAndClose(); // commit core in dev, to prevent trigger fail

    org.openbravo.model.ad.domain.List listValue = OBProvider.getInstance()
        .get(org.openbravo.model.ad.domain.List.class);
    listValue.setCreatedBy(OBContext.getOBContext().getUser());
    listValue.setCreationDate(new Date());
    listValue.setUpdatedBy(OBContext.getOBContext().getUser());
    listValue.setReference(refProperties);
    listValue.setModule(mod);
    listValue.setName("test Property List");
    listValue.setSearchKey("testPropertyList");
    OBDal.getInstance().save(listValue);
    OBDal.getInstance().flush();
    mod.setInDevelopment(devStatus);

    Entity e = ModelProvider.getInstance().getEntity(Preference.ENTITY_NAME);
    Property p = e.getProperty(Preference.PROPERTY_PROPERTY);
    p.getAllowedValues().add(listValue.getSearchKey());

    Preference pref = Preferences.setPreferenceValue("testPropertyList", "testPropValue", true,
        null, null, null, null, null, null);
    OBDal.getInstance().flush();

    assertTrue(pref.isPropertyList(), "Pref type is not properly set");

    String value = Preferences.getPreferenceValue("testPropertyList", true,
        OBContext.getOBContext().getCurrentClient(),
        OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(), null,
        null);
    assertEquals("testPropValue", value, "Not found expected value.");
    OBDal.getInstance().commitAndClose();
  }

  @Test
  public void test18GetAllPreferences() throws PropertyException {
    // setSystemAdministratorContext();
    setTestUserContext();

    List<Preference> allPrefs = Preferences.getAllPreferences(
        OBContext.getOBContext().getCurrentClient().getId(),
        OBContext.getOBContext().getCurrentOrganization().getId(),
        OBContext.getOBContext().getUser().getId(), OBContext.getOBContext().getRole().getId());
    List<String> testPreferenceValues = new ArrayList<>();
    for (Preference pref : allPrefs) {
      String key = pref.isPropertyList() ? pref.getProperty() : pref.getAttribute();
      if (key.startsWith("testProperty")) {
        testPreferenceValues.add(key + " " + pref.getSearchKey());
      }
    }
    assertThat(testPreferenceValues,
        allOf(hasItem("testProperty2 testValue"), hasItem("testProperty B2"),
            hasItem("testProperty alertGeneral"), hasItem("testPropertyList testPropValue")));
    assertThat(testPreferenceValues, hasSize(4));
  }

  @Test
  public void test99Clean() {
    setSystemAdministratorContext();
    OBCriteria<Preference> qPref = OBDal.getInstance().createCriteria(Preference.class);
    qPref.add(Restrictions.or(Restrictions.like(Preference.PROPERTY_ATTRIBUTE, "testProperty%"),
        Restrictions.eq(Preference.PROPERTY_PROPERTY, "testPropertyList")));
    for (Preference pref : qPref.list()) {
      OBDal.getInstance().remove(pref);
    }

    OBCriteria<org.openbravo.model.ad.domain.List> qList = OBDal.getInstance()
        .createCriteria(org.openbravo.model.ad.domain.List.class);
    qList.add(
        Restrictions.eq(org.openbravo.model.ad.domain.List.PROPERTY_SEARCHKEY, "testPropertyList"));
    for (org.openbravo.model.ad.domain.List l : qList.list()) {
      OBDal.getInstance().refresh(l);
      OBDal.getInstance().remove(l);
    }
    OBDal.getInstance().commitAndClose();
  }

  private String getPLPreference(String property, boolean isListProperty, Client client,
      Organization org, User user, Role role, Window window) throws SQLException {
    ResultSet result = null;
    PreparedStatement st = null;
    try {
      Connection conn = OBDal.getInstance().getConnection();
      st = conn.prepareStatement("select ad_get_preference_value(?, ?, ?, ?, ?, ?, ?) from dual ");
      st.setString(1, property);
      st.setString(2, isListProperty ? "Y" : "N");
      st.setString(3, client == null ? null : client.getId());
      st.setString(4, org == null ? null : org.getId());
      st.setString(5, user == null ? null : user.getId());
      st.setString(6, role == null ? null : role.getId());
      st.setString(7, window == null ? null : window.getId());
      result = st.executeQuery();
      String rt = null;
      if (result.next()) {
        rt = result.getString(1);
      }
      return rt;
    } finally {
      if (result != null) {
        result.close();
      }
      if (st != null) {
        st.close();
      }
      OBDal.getInstance().flush();
    }
  }

}
