package org.openbravo.role.inheritance;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.structure.InheritedAccessEnabled;
import org.openbravo.client.application.ApplicationConstants;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.RoleInheritance;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.ui.Tab;
/**
 * Unit tests for the {@link RoleInheritanceWarningFICExtension} class.
 * This class verifies the behavior of the `execute` method under various scenarios,
 * including valid and invalid modes, table-based tabs, and role inheritance.
 */
@ExtendWith(MockitoExtension.class)
public class RoleInheritanceWarningFICExtensionTest {

  private static final String VALID_TABLE_ID = "123";
  private static final String ROLE_ID = "456";
  private static final String ENTITY_CLASS_NAME = "org.openbravo.test.TestEntity";
  private static final String WARN_MESSAGE = "Warning Message";
  private static final String CHILD_ROLE_NAME = "Child Role";

  @Mock
  private RoleInheritanceManager manager;

  @Mock
  private Tab tab;

  @Mock
  private Table table;

  @Mock
  private OBDal obDal;

  @Mock
  private ModelProvider modelProvider;

  @Mock
  private Entity entity;

  @Mock
  private Role role;

  @Mock
  private RoleInheritance roleInheritance;

  @Mock
  private Role childRole;

  @Mock
  private OBContext obContext;

  private BaseOBObject baseOBObjectRow;

  /**
   * Unit tests for the {@link RoleInheritanceWarningFICExtension} class.
   * Verifies the behavior of the `execute` method under various scenarios,
   * including valid and invalid modes, table-based tabs, and role inheritance.
   */
  @InjectMocks
  private RoleInheritanceWarningFICExtension extension;

  private Map<String, JSONObject> columnValues;
  private List<JSONObject> calloutMessages;
  private List<String> changeEventCols;
  private List<JSONObject> attachments;
  private List<String> jsExcuteCode;
  private Map<String, Object> hiddenInputs;
  private List<String> overwrittenAuxiliaryInputs;

  /**
   * Sets up the test environment before each test.
   * Initializes mocked objects and test data.
   *
   * @throws JSONException
   *     if an error occurs while creating JSON objects
   */
  @BeforeEach
  public void setup() throws JSONException {
    columnValues = new HashMap<>();
    calloutMessages = new ArrayList<>();
    changeEventCols = new ArrayList<>();
    attachments = new ArrayList<>();
    jsExcuteCode = new ArrayList<>();
    hiddenInputs = new HashMap<>();
    overwrittenAuxiliaryInputs = new ArrayList<>();

    baseOBObjectRow = mock(BaseOBObject.class);

    when(tab.getTable()).thenReturn(table);
    when(table.getId()).thenReturn(VALID_TABLE_ID);

    JSONObject roleColumn = new JSONObject();
    roleColumn.put("value", ROLE_ID);
    columnValues.put("inpadRoleId", roleColumn);
  }

  /**
   * Tests the `execute` method when the mode is valid but the tab is not table-based.
   * Verifies that no actions are performed.
   */
  @Test
  public void testExecuteValidModeButNonTableBasedTabShouldDoNothing() {
    String mode = "EDIT";
    when(table.getDataOriginType()).thenReturn("NonTableBased");

    extension.execute(mode, tab, columnValues, baseOBObjectRow, changeEventCols, calloutMessages, attachments,
        jsExcuteCode, hiddenInputs, 0, overwrittenAuxiliaryInputs);

    verify(manager, never()).existsInjector(anyString());
  }

  /**
   * Tests the `execute` method when the mode and tab are valid but no injector exists.
   * Verifies that no actions are performed.
   */
  @Test
  public void testExecuteValidModeAndTabButNoInjectorShouldDoNothing() {
    String mode = "EDIT";
    when(table.getDataOriginType()).thenReturn(ApplicationConstants.TABLEBASEDTABLE);

    try (MockedStatic<ModelProvider> mockedModelProvider = Mockito.mockStatic(ModelProvider.class)) {
      mockedModelProvider.when(ModelProvider::getInstance).thenReturn(modelProvider);
      when(modelProvider.getEntityByTableId(VALID_TABLE_ID)).thenReturn(entity);
      when(entity.getClassName()).thenReturn(ENTITY_CLASS_NAME);
      when(manager.existsInjector(ENTITY_CLASS_NAME)).thenReturn(false);

      extension.execute(mode, tab, columnValues, baseOBObjectRow, changeEventCols, calloutMessages, attachments,
          jsExcuteCode, hiddenInputs, 0, overwrittenAuxiliaryInputs);

      verify(manager).existsInjector(ENTITY_CLASS_NAME);
      verify(manager, never()).getRole(any(), anyString());
    }
  }

  /**
   * Tests the `execute` method when the mode, tab, and injector are valid,
   * but the role is not a template. Verifies that no actions are performed.
   */
  @Test
  public void testExecuteValidModeTabAndInjectorRoleFromColumnValuesNotTemplateShouldDoNothing() {
    String mode = "EDIT";
    when(table.getDataOriginType()).thenReturn(ApplicationConstants.TABLEBASEDTABLE);

    try (MockedStatic<ModelProvider> mockedModelProvider = Mockito.mockStatic(
        ModelProvider.class); MockedStatic<OBDal> mockedOBDal = Mockito.mockStatic(
        OBDal.class); MockedStatic<OBContext> mockedOBContext = Mockito.mockStatic(OBContext.class)) {

      mockedModelProvider.when(ModelProvider::getInstance).thenReturn(modelProvider);
      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
      mockedOBContext.when(OBContext::getOBContext).thenReturn(obContext);

      when(modelProvider.getEntityByTableId(VALID_TABLE_ID)).thenReturn(entity);
      when(entity.getClassName()).thenReturn(ENTITY_CLASS_NAME);
      when(manager.existsInjector(ENTITY_CLASS_NAME)).thenReturn(true);
      when(obDal.get(Role.class, ROLE_ID)).thenReturn(role);
      when(role.isTemplate()).thenReturn(false);

      extension.execute(mode, tab, columnValues, baseOBObjectRow, changeEventCols, calloutMessages, attachments,
          jsExcuteCode, hiddenInputs, 0, overwrittenAuxiliaryInputs);

      verify(manager).existsInjector(ENTITY_CLASS_NAME);
      verify(role).isTemplate();
      verify(role, never()).getADRoleInheritanceInheritFromList();
    }
  }

  /**
   * Tests the `execute` method when the mode, tab, and injector are valid,
   * and the role is a template with active child roles. Verifies that a warning is added.
   */
  @Test
  public void testExecuteValidModeTabAndInjectorRoleTemplateWithActiveChildRolesShouldAddWarning() {
    // Arrange
    String mode = "EDIT";
    when(table.getDataOriginType()).thenReturn(ApplicationConstants.TABLEBASEDTABLE);

    try (MockedStatic<ModelProvider> mockedModelProvider = Mockito.mockStatic(
        ModelProvider.class); MockedStatic<OBDal> mockedOBDal = Mockito.mockStatic(
        OBDal.class); MockedStatic<OBContext> mockedOBContext = Mockito.mockStatic(
        OBContext.class); MockedStatic<OBMessageUtils> mockedOBMessageUtils = Mockito.mockStatic(
        OBMessageUtils.class)) {

      mockedModelProvider.when(ModelProvider::getInstance).thenReturn(modelProvider);
      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
      mockedOBContext.when(OBContext::getOBContext).thenReturn(obContext);
      mockedOBMessageUtils.when(() -> OBMessageUtils.getI18NMessage(anyString(), any())).thenReturn(WARN_MESSAGE);

      when(modelProvider.getEntityByTableId(VALID_TABLE_ID)).thenReturn(entity);
      when(entity.getClassName()).thenReturn(ENTITY_CLASS_NAME);
      when(manager.existsInjector(ENTITY_CLASS_NAME)).thenReturn(true);
      when(obDal.get(Role.class, ROLE_ID)).thenReturn(role);
      when(role.isTemplate()).thenReturn(true);

      List<RoleInheritance> inheritanceList = new ArrayList<>();
      inheritanceList.add(roleInheritance);
      when(role.getADRoleInheritanceInheritFromList()).thenReturn(inheritanceList);
      when(roleInheritance.isActive()).thenReturn(true);
      when(roleInheritance.getRole()).thenReturn(childRole);
      when(childRole.getName()).thenReturn(CHILD_ROLE_NAME);

      extension.execute(mode, tab, columnValues, baseOBObjectRow, changeEventCols, calloutMessages, attachments,
          jsExcuteCode, hiddenInputs, 0, overwrittenAuxiliaryInputs);

      verify(manager).existsInjector(ENTITY_CLASS_NAME);
      verify(role).isTemplate();
      verify(role).getADRoleInheritanceInheritFromList();
      verify(roleInheritance).isActive();
      verify(roleInheritance).getRole();
      verify(childRole).getName();
      assert (!calloutMessages.isEmpty());
      mockedOBMessageUtils.verify(() -> OBMessageUtils.getI18NMessage(eq("EditTemplateRoleAccess"), any()), times(1));
    }
  }

  /**
   * Tests the `execute` method when the mode, tab, and injector are valid,
   * and the role is a template with inactive child roles. Verifies that no actions are performed.
   */
  @Test
  public void testExecuteValidModeTabAndInjectorRoleTemplateWithInactiveChildRolesShouldDoNothing() {
    String mode = "EDIT";
    when(table.getDataOriginType()).thenReturn(ApplicationConstants.TABLEBASEDTABLE);

    try (MockedStatic<ModelProvider> mockedModelProvider = Mockito.mockStatic(
        ModelProvider.class); MockedStatic<OBDal> mockedOBDal = Mockito.mockStatic(
        OBDal.class); MockedStatic<OBContext> mockedOBContext = Mockito.mockStatic(OBContext.class)) {

      mockedModelProvider.when(ModelProvider::getInstance).thenReturn(modelProvider);
      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
      mockedOBContext.when(OBContext::getOBContext).thenReturn(obContext);

      when(modelProvider.getEntityByTableId(VALID_TABLE_ID)).thenReturn(entity);
      when(entity.getClassName()).thenReturn(ENTITY_CLASS_NAME);
      when(manager.existsInjector(ENTITY_CLASS_NAME)).thenReturn(true);
      when(obDal.get(Role.class, ROLE_ID)).thenReturn(role);
      when(role.isTemplate()).thenReturn(true);

      List<RoleInheritance> inheritanceList = new ArrayList<>();
      inheritanceList.add(roleInheritance);
      when(role.getADRoleInheritanceInheritFromList()).thenReturn(inheritanceList);
      when(roleInheritance.isActive()).thenReturn(false);

      extension.execute(mode, tab, columnValues, baseOBObjectRow, changeEventCols, calloutMessages, attachments,
          jsExcuteCode, hiddenInputs, 0, overwrittenAuxiliaryInputs);

      verify(manager).existsInjector(ENTITY_CLASS_NAME);
      verify(role).isTemplate();
      verify(role).getADRoleInheritanceInheritFromList();
      verify(roleInheritance).isActive();
      verify(roleInheritance, never()).getRole();
      assert (calloutMessages.isEmpty());
    }
  }

  /**
   * Tests the caching mechanism for the same tab ID.
   * Verifies that the cache is reused.
   */
  @Test
  public void testCacheReuseSameTabIdShouldUseCache() {
    String mode = "EDIT";
    when(table.getDataOriginType()).thenReturn(ApplicationConstants.TABLEBASEDTABLE);

    try (MockedStatic<ModelProvider> mockedModelProvider = Mockito.mockStatic(
        ModelProvider.class); MockedStatic<OBDal> mockedOBDal = Mockito.mockStatic(
        OBDal.class); MockedStatic<OBContext> mockedOBContext = Mockito.mockStatic(OBContext.class)) {

      mockedModelProvider.when(ModelProvider::getInstance).thenReturn(modelProvider);
      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
      mockedOBContext.when(OBContext::getOBContext).thenReturn(obContext);

      when(modelProvider.getEntityByTableId(VALID_TABLE_ID)).thenReturn(entity);
      when(entity.getClassName()).thenReturn(ENTITY_CLASS_NAME);
      when(manager.existsInjector(ENTITY_CLASS_NAME)).thenReturn(true);
      when(obDal.get(Role.class, ROLE_ID)).thenReturn(role);

      extension.execute(mode, tab, columnValues, baseOBObjectRow, changeEventCols, calloutMessages, attachments,
          jsExcuteCode, hiddenInputs, 0, overwrittenAuxiliaryInputs);

      extension.execute(mode, tab, columnValues, baseOBObjectRow, changeEventCols, calloutMessages, attachments,
          jsExcuteCode, hiddenInputs, 0, overwrittenAuxiliaryInputs);

      verify(manager, times(1)).existsInjector(ENTITY_CLASS_NAME);
    }
  }

  /**
   * Mocked class to simulate the behavior of InheritedAccessEnabled.
   * This is used to test the functionality of the RoleInheritanceWarningFICExtension class.
   */
  public interface TestInheritedAccessEnabledClass extends InheritedAccessEnabled {
  }

}
