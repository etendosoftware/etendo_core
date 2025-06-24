package org.openbravo.role.inheritance;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.system.Language;
import org.openbravo.role.inheritance.RoleInheritanceManager.CalculationResult;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.db.DbUtility;

/**
 * Unit tests for the {@link RecalculatePermissionsHandler} class.
 * Verifies the behavior of the `execute` method under various scenarios,
 * including recalculating permissions for single and multiple roles.
 */
@ExtendWith(MockitoExtension.class)
public class RecalculatePermissionsHandlerTest {

  @Mock
  private RoleInheritanceManager mockManager;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private Role mockRole;

  @Mock
  private OBContext mockOBContext;

  @Mock
  private Language mockLanguage;

  @InjectMocks
  private RecalculatePermissionsHandler handler;

  private MockedStatic<OBDal> staticOBDal;
  private MockedStatic<OBContext> staticOBContext;
  private MockedStatic<OBMessageUtils> staticOBMessageUtils;
  private MockedStatic<Utility> staticUtility;
  private MockedStatic<DbUtility> staticDbUtility;

  /**
   * Sets up the test environment before each test.
   * Initializes static mocks and configures default behavior for mocked objects.
   */
  @BeforeEach
  public void setUp() {
    staticOBDal = mockStatic(OBDal.class);
    staticOBContext = mockStatic(OBContext.class);
    staticOBMessageUtils = mockStatic(OBMessageUtils.class);
    staticUtility = mockStatic(Utility.class);
    staticDbUtility = mockStatic(DbUtility.class);

    staticOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
    staticOBContext.when(OBContext::getOBContext).thenReturn(mockOBContext);

    when(mockOBContext.getLanguage()).thenReturn(mockLanguage);
    when(mockLanguage.getLanguage()).thenReturn("en_US");

    when(mockRole.getName()).thenReturn("TestRole");
  }

  /**
   * Cleans up the test environment after each test.
   * Closes all static mocks to release resources.
   */
  @AfterEach
  public void tearDown() {
    if (staticOBDal != null) staticOBDal.close();
    if (staticOBContext != null) staticOBContext.close();
    if (staticOBMessageUtils != null) staticOBMessageUtils.close();
    if (staticUtility != null) staticUtility.close();
    if (staticDbUtility != null) staticDbUtility.close();
  }

  /**
   * Tests the `execute` method for recalculating permissions for multiple roles.
   * Verifies that the response contains a success message with the correct details.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  @DisplayName("Test recalculate permissions for multiple roles")
  public void testRecalculateForMultipleRoles() throws Exception {
    // Arrange
    String roleId1 = "TEST_ROLE_ID_1";
    String roleId2 = "TEST_ROLE_ID_2";
    Map<String, Object> parameters = new HashMap<>();
    String content = "{\"roles\":[\"" + roleId1 + "\", \"" + roleId2 + "\"]}";

    Role mockRole1 = mockRole;
    Role mockRole2 = mock(Role.class);
    when(mockRole2.getName()).thenReturn("TestRole2");

    when(mockOBDal.get(Role.class, roleId1)).thenReturn(mockRole1);
    when(mockOBDal.get(Role.class, roleId2)).thenReturn(mockRole2);

    Map<String, CalculationResult> calculationResults = new HashMap<>();

    when(mockManager.recalculateAllAccessesForRole(any(Role.class), anyBoolean())).thenReturn(calculationResults);

    staticUtility.when(
        () -> Utility.messageBD(any(DalConnectionProvider.class), eq("RecalculatePermissionsMultipleSuccess"),
            anyString())).thenReturn("Permissions recalculated for multiple roles");

    staticOBMessageUtils.when(
        () -> OBMessageUtils.getI18NMessage(eq("RecalculatePermissionsMultipleRoles"), any(String[].class))).thenReturn(
        "Permissions recalculated for TestRole, TestRole2");

    JSONObject response = handler.execute(parameters, content);

    assertNotNull(response);
    assertNotNull(response.get("message"));
    JSONObject message = response.getJSONObject("message");

    assertAll(() -> assertEquals("success", message.getString("severity")),
        () -> assertEquals("Permissions recalculated for multiple roles", message.getString("title")),
        () -> assertEquals("Permissions recalculated for TestRole, TestRole2", message.getString("text")));
  }

}
