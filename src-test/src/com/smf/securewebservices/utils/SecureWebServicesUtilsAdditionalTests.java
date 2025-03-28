package com.smf.securewebservices.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.RoleOrganization;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;

/**
 * Unit tests for the SecureWebServicesUtils class.
 */
public class SecureWebServicesUtilsAdditionalTests {

  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<OBDal> mockedOBDal;

  /**
   * Sets up the test environment before each test.
   */
  @Before
  public void setUp() {
    mockedOBContext = mockStatic(OBContext.class);
    mockedOBDal = mockStatic(OBDal.class);
  }

  /**
   * Cleans up the test environment after each test.
   */
  @After
  public void tearDown() {
    if (mockedOBContext != null) mockedOBContext.close();
    if (mockedOBDal != null) mockedOBDal.close();
  }

  /**
   * Tests the getRootCause method.
   */
  @Test
  public void testGetRootCause() {
    Exception rootException = new Exception("Root cause");
    Exception wrapperException = new Exception("Wrapper", rootException);

    Throwable result = SecureWebServicesUtils.getRootCause(wrapperException);
    assertEquals(rootException, result);
  }

  /**
   * Tests the getUserRolesAndOrg method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetUserRolesAndOrg() throws JSONException {
    User mockUser = mock(User.class);
    Role mockRole = mock(Role.class);
    when(mockRole.getId()).thenReturn("role1");
    when(mockRole.getName()).thenReturn("Role 1");

    UserRoles mockUserRole = mock(UserRoles.class);
    when(mockUserRole.getRole()).thenReturn(mockRole);

    List<UserRoles> userRolesList = new ArrayList<>();
    userRolesList.add(mockUserRole);
    when(mockUser.getADUserRolesList()).thenReturn(userRolesList);

    JSONArray result = SecureWebServicesUtils.getUserRolesAndOrg(mockUser, false, false);
    assertEquals(1, result.length());

    JSONObject role = result.getJSONObject(0);
    assertEquals("role1", role.getString("id"));
    assertEquals("Role 1", role.getString("name"));
  }

  /**
   * Tests the getRoleOrgs method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetRoleOrgs() throws JSONException {
    Role mockRole = mock(Role.class);
    Organization mockOrg = mock(Organization.class);
    when(mockOrg.getId()).thenReturn("org1");
    when(mockOrg.getName()).thenReturn("Organization 1");

    RoleOrganization mockRoleOrg = mock(RoleOrganization.class);
    when(mockRoleOrg.getOrganization()).thenReturn(mockOrg);

    List<RoleOrganization> roleOrgList = new ArrayList<>();
    roleOrgList.add(mockRoleOrg);
    when(mockRole.getADRoleOrganizationList()).thenReturn(roleOrgList);

    JSONArray result = SecureWebServicesUtils.getRoleOrgs(mockRole, false);
    assertEquals(1, result.length());

    JSONObject org = result.getJSONObject(0);
    assertEquals("org1", org.getString("id"));
    assertEquals("Organization 1", org.getString("name"));
  }

  /**
   * Tests the getOrgWarehouses method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetOrgWarehouses() throws JSONException {
    // GIVEN
    Organization mockOrg = mock(Organization.class);
    Warehouse mockWarehouse = mock(Warehouse.class);
    when(mockWarehouse.getId()).thenReturn("wh1");
    when(mockWarehouse.getName()).thenReturn("Warehouse 1");

    List<Warehouse> warehouseList = new ArrayList<>();
    warehouseList.add(mockWarehouse);

    try (MockedStatic<SecureWebServicesUtils> mockedUtils = mockStatic(SecureWebServicesUtils.class)) {
      mockedUtils.when(() -> SecureWebServicesUtils.getOrganizationWarehouses(mockOrg)).thenReturn(warehouseList);
      mockedUtils.when(() -> SecureWebServicesUtils.getOrgWarehouses(mockOrg)).thenCallRealMethod();

      // WHEN
      JSONArray result = SecureWebServicesUtils.getOrgWarehouses(mockOrg);

      // THEN
      assertNotNull(result);
      assertEquals(1, result.length());

      JSONObject warehouse = result.getJSONObject(0);
      assertEquals("wh1", warehouse.getString("id"));
      assertEquals("Warehouse 1", warehouse.getString("name"));
    }
  }

  /**
   * Tests the getExceptionMessage method.
   */
  @Test
  public void testGetExceptionMessage() {
    Throwable mockThrowable = mock(Throwable.class);
    BatchUpdateException mockBatchUpdateException = mock(BatchUpdateException.class);
    SQLException mockSQLException = mock(SQLException.class);

    when(mockThrowable.getCause()).thenReturn(mockBatchUpdateException);
    when(mockBatchUpdateException.getNextException()).thenReturn(mockSQLException);
    when(mockSQLException.getMessage()).thenReturn("SQL Error");

    String result = SecureWebServicesUtils.getExceptionMessage(mockThrowable);
    assertEquals("SQL Error", result);
  }
}
