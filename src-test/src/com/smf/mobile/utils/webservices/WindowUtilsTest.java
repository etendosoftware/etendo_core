package com.smf.mobile.utils.webservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.ui.AuxiliaryInput;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.geography.Country;
import org.openbravo.service.db.DalConnectionProvider;

import com.smf.mobile.utils.data.MobileIdentifier;
import com.smf.mobile.utils.data.TabConfiguration;

/**
 * Test class for WindowUtilsTest.
 */
@RunWith(MockitoJUnitRunner.class)
public class WindowUtilsTest {

  private WindowUtilsTest windowUtilsTest;

  @Mock
  private Column column;

  @Mock
  private Entity entity;

  @Mock
  private Property property;

  @Mock
  private Table table;

  @Mock
  private Tab tab;

  @Mock
  private TabConfiguration tabConfiguration;

  @Mock
  private OBDal obDalInstance;

  @Mock
  private ModelProvider provider;

  @Mock
  private OBCriteria obCriteria;

  @Mock
  private Field field;

  @Mock
  private BaseOBObject parent;

  @Mock
  private Organization organization;

  @Mock
  private Client client;

  @Mock
  private User user;

  @Mock
  private Currency currency;

  @Mock
  private Window window;

  @Mock
  private OBContext ctx;

  @Mock
  private AuxiliaryInput auxiliaryInput;

  @Mock
  private Country country;

  @Mock
  private PreparedStatement ps;

  @Mock
  private ResultSet rs;

  /**
   * Sets up the test environment before each test.
   * Initializes the WindowUtilsTest instance.
   */
  @Before
  public void setup() {
    windowUtilsTest = new WindowUtilsTest();
  }

  @Test
  public void testGetEntityColumnName_ReturnsCorrectPropertyName() {
    when(column.getTable()).thenReturn(table);
    when(table.getName()).thenReturn("MyTable");
    when(column.getDBColumnName()).thenReturn("my_column");

    when(property.getName()).thenReturn("myProperty");

    try (MockedStatic<ModelProvider> modelProvider = mockStatic(ModelProvider.class)) {
      modelProvider.when(ModelProvider::getInstance).thenReturn(provider);
      when(provider.getEntity("MyTable")).thenReturn(entity);
      when(entity.getPropertyByColumnName("my_column")).thenReturn(property);

      String result = WindowUtils.getEntityColumnName(column);

      assertEquals("myProperty", result);
    }
  }

  @Test
  public void testGetTabIdentifiers_WithValidConfigurationAndIdentifiers() throws Exception {
    MobileIdentifier identifier1 = mock(MobileIdentifier.class);
    MobileIdentifier identifier2 = mock(MobileIdentifier.class);
    Field field1 = mock(Field.class);
    Field field2 = mock(Field.class);

    when(identifier1.getId()).thenReturn("id1");
    when(identifier1.getField()).thenReturn(field1);
    when(field1.getId()).thenReturn("field1");
    when(identifier1.getSequenceNumber()).thenReturn(1L);

    when(identifier2.getId()).thenReturn("id2");
    when(identifier2.getField()).thenReturn(field2);
    when(field2.getId()).thenReturn("field2");
    when(identifier2.getSequenceNumber()).thenReturn(2L);

    try (MockedStatic<OBDal> obDal = mockStatic(OBDal.class)) {
      obDal.when(OBDal::getInstance).thenReturn(obDalInstance);

      when(obDalInstance.createCriteria(TabConfiguration.class)).thenReturn(obCriteria);
      when(obCriteria.add(any())).thenReturn(obCriteria);
      when(obCriteria.setMaxResults(1)).thenReturn(obCriteria);
      when(obCriteria.uniqueResult()).thenReturn(tabConfiguration);

      when(obDalInstance.createCriteria(MobileIdentifier.class)).thenReturn(obCriteria);
      when(obCriteria.add(any())).thenReturn(obCriteria);
      when(obCriteria.addOrderBy(any(), anyBoolean())).thenReturn(obCriteria);
      when(obCriteria.list()).thenReturn(List.of(identifier1, identifier2));

      JSONArray result = WindowUtils.getTabIdentifiers(tab);

      assertEquals(2, result.length());
      assertEquals("id1", result.getJSONObject(0).getString("id"));
      assertEquals("field1", result.getJSONObject(0).getString("field"));
      assertEquals(1L, result.getJSONObject(0).getLong("sequenceNumber"));

      assertEquals("id2", result.getJSONObject(1).getString("id"));
      assertEquals("field2", result.getJSONObject(1).getString("field"));
      assertEquals(2L, result.getJSONObject(1).getLong("sequenceNumber"));
    }
  }

  @Test
  public void testComputeColumnValues_ForDocumentNoField_SetsAutoValue() throws Exception {
    when(tab.getTable()).thenReturn(table);
    when(table.getId()).thenReturn("tableId");
    when(tab.getADFieldList()).thenReturn(List.of(field));

    when(field.getColumn()).thenReturn(column);
    when(field.getProperty()).thenReturn(null);

    when(column.getDBColumnName()).thenReturn("documentno");
    when(column.isLinkToParentColumn()).thenReturn(false);

    when(property.isId()).thenReturn(false);
    when(property.isActiveColumn()).thenReturn(false);
    when(property.getName()).thenReturn("documentNo");

    try (MockedStatic<ModelProvider> modelProvider = mockStatic(
        ModelProvider.class); MockedStatic<OBDal> obDal = mockStatic(OBDal.class)) {
      modelProvider.when(ModelProvider::getInstance).thenReturn(provider);
      when(provider.getEntityByTableId("tableId")).thenReturn(entity);
      when(entity.getPropertyByColumnName("documentno")).thenReturn(property);

      obDal.when(OBDal::getInstance).thenReturn(obDalInstance);

      JSONObject result = WindowUtils.computeColumnValues(tab, null, null, null);

      assertEquals("<auto>", result.getString("documentNo"));
    }
  }

  @Test
  public void testComputeColumnValues_LinkToParentColumn_SetsParentIdAndIdentifier() throws Exception {
    when(tab.getTable()).thenReturn(table);
    when(table.getId()).thenReturn("tableId");
    when(tab.getADFieldList()).thenReturn(List.of(field));

    when(field.getColumn()).thenReturn(column);
    when(field.getProperty()).thenReturn(null);

    when(column.getDBColumnName()).thenReturn("linkedcolumn");
    when(column.isLinkToParentColumn()).thenReturn(true);

    when(property.isId()).thenReturn(false);
    when(property.isActiveColumn()).thenReturn(false);
    when(property.getName()).thenReturn("parentProperty");
    when(property.getTargetEntity()).thenReturn(entity);

    when(parent.getId()).thenReturn("parent-id-123");
    when(parent.getIdentifier()).thenReturn("Parent Identifier");

    try (MockedStatic<ModelProvider> modelProvider = mockStatic(
        ModelProvider.class); MockedStatic<OBDal> obDal = mockStatic(OBDal.class)) {
      modelProvider.when(ModelProvider::getInstance).thenReturn(provider);
      when(provider.getEntityByTableId("tableId")).thenReturn(entity);
      when(entity.getPropertyByColumnName("linkedcolumn")).thenReturn(property);
      when(entity.getName()).thenReturn("ParentEntity");

      obDal.when(OBDal::getInstance).thenReturn(obDalInstance);
      when(obDalInstance.get("ParentEntity", "parent456")).thenReturn(parent);

      JSONObject result = WindowUtils.computeColumnValues(tab, "parent456", entity, null);

      assertEquals("parent-id-123", result.get("parentProperty"));
      assertEquals("Parent Identifier", result.get("parentProperty$_identifier"));
    }
  }

  @Test
  public void testComputeColumnValues_PropertyIsId_SkipsField() throws Exception {
    when(tab.getTable()).thenReturn(table);
    when(table.getId()).thenReturn("tableId");
    when(tab.getADFieldList()).thenReturn(List.of(field));

    when(field.getColumn()).thenReturn(column);
    when(field.getProperty()).thenReturn(null);

    when(column.getDBColumnName()).thenReturn("id_column");

    when(property.isId()).thenReturn(true);

    try (MockedStatic<ModelProvider> modelProvider = mockStatic(
        ModelProvider.class); MockedStatic<OBDal> obDal = mockStatic(OBDal.class)) {
      modelProvider.when(ModelProvider::getInstance).thenReturn(provider);
      when(provider.getEntityByTableId("tableId")).thenReturn(entity);
      when(entity.getPropertyByColumnName("id_column")).thenReturn(property);

      obDal.when(OBDal::getInstance).thenReturn(obDalInstance);

      JSONObject result = WindowUtils.computeColumnValues(tab, null, null, null);

      assertFalse(result.has("id"));
    }
  }

  @Test
  public void testComputeColumnValues_PropertyIsActiveColumn_SetsTrue() throws Exception {
    when(tab.getTable()).thenReturn(table);
    when(table.getId()).thenReturn("tableId");
    when(tab.getADFieldList()).thenReturn(List.of(field));

    when(field.getColumn()).thenReturn(column);
    when(field.getProperty()).thenReturn(null);

    when(column.getDBColumnName()).thenReturn("active_col");

    when(property.isId()).thenReturn(false); // no se salta por ID
    when(property.isActiveColumn()).thenReturn(true); // se salta por activo
    when(property.getName()).thenReturn("isActive");

    try (MockedStatic<ModelProvider> modelProvider = mockStatic(
        ModelProvider.class); MockedStatic<OBDal> obDal = mockStatic(OBDal.class)) {
      modelProvider.when(ModelProvider::getInstance).thenReturn(provider);
      when(provider.getEntityByTableId("tableId")).thenReturn(entity);
      when(entity.getPropertyByColumnName("active_col")).thenReturn(property);

      obDal.when(OBDal::getInstance).thenReturn(obDalInstance);

      JSONObject result = WindowUtils.computeColumnValues(tab, null, null, null);

      assertTrue(result.getBoolean("isActive"));
    }
  }

  @Test
  public void testParseDefaultValue_NullInput_ReturnsNull() throws JSONException {
    Object result = WindowUtils.parseDefaultValue(null, false, null, null, null, null);
    assertNull(result);
  }

  @Test
  public void testParseDefaultValue_YesBoolean_ReturnsTrue() throws JSONException {
    Object result = WindowUtils.parseDefaultValue("Y", true, null, null, null, null);
    assertEquals(true, result);
  }

  @Test
  public void testParseDefaultValue_NoBoolean_ReturnsFalse() throws JSONException {
    Object result = WindowUtils.parseDefaultValue("N", true, null, null, null, null);
    assertEquals(false, result);
  }

  @Test
  public void testParseDefaultValue_ReplacesAuxInputsAndContext() throws JSONException {
    JSONObject context = new JSONObject();
    context.put("@CTX_KEY@", "ctx-value");

    when(auxiliaryInput.getName()).thenReturn("myinput");

    when(window.getId()).thenReturn("window-123");
    when(window.isSalesTransaction()).thenReturn(true);

    when(tab.getADAuxiliaryInputList()).thenReturn(List.of(auxiliaryInput));
    when(tab.getWindow()).thenReturn(window);

    when(organization.getId()).thenReturn("org-1");
    when(organization.getCurrency()).thenReturn(currency);
    when(currency.getId()).thenReturn("currency-1");
    when(client.getId()).thenReturn("client-1");
    when(user.getId()).thenReturn("user-1");

    try (MockedStatic<WindowUtils> utils = mockStatic(WindowUtils.class,
        CALLS_REAL_METHODS); MockedStatic<OBContext> obContext = mockStatic(OBContext.class)) {
      obContext.when(OBContext::getOBContext).thenReturn(ctx);
      when(ctx.getUser()).thenReturn(user);
      when(ctx.getCurrentOrganization()).thenReturn(organization);
      when(ctx.getCurrentClient()).thenReturn(client);

      utils.when(() -> WindowUtils.parseAuxiliaryInput(eq(auxiliaryInput), eq("window-123"),
          any(JSONObject.class))).thenReturn("aux-value");

      String input = "@MYINPUT@ + @CTX_KEY@ + @AD_USER_ID@ + @C_CURRENCY_ID@ + @ISSOTRX@";
      Object result = WindowUtils.parseDefaultValue(input, false, tab, null, null, context);

      assertEquals("aux-value + ctx-value + user-1 + currency-1 + true", result);
    }
  }

  @Test
  public void testParseDefaultValue_ReplacesCountryDef() throws JSONException {
    when(organization.getId()).thenReturn("org");
    when(organization.getCurrency()).thenReturn(currency);
    when(currency.getId()).thenReturn("curr");
    when(client.getId()).thenReturn("client");
    when(user.getId()).thenReturn("user");

    when(tab.getADAuxiliaryInputList()).thenReturn(List.of());
    when(tab.getWindow()).thenReturn(window);
    when(window.isSalesTransaction()).thenReturn(true);

    when(country.getId()).thenReturn("COUNTRY-XYZ");

    try (MockedStatic<OBContext> obContext = mockStatic(OBContext.class); MockedStatic<OBDal> obDal = mockStatic(
        OBDal.class)) {
      obContext.when(OBContext::getOBContext).thenReturn(ctx);
      when(ctx.getUser()).thenReturn(user);
      when(ctx.getCurrentOrganization()).thenReturn(organization);
      when(ctx.getCurrentClient()).thenReturn(client);

      obDal.when(OBDal::getInstance).thenReturn(obDalInstance);

      OBCriteria countryCriteria = obCriteria;
      when(obDalInstance.createCriteria(Country.class)).thenReturn(countryCriteria);
      when(countryCriteria.add(any())).thenReturn(countryCriteria);
      when(countryCriteria.setMaxResults(1)).thenReturn(countryCriteria);
      when(countryCriteria.uniqueResult()).thenReturn(country);

      String input = "DEFAULT IS @COUNTRYDEF@";
      Object result = WindowUtils.parseDefaultValue(input, false, tab, null, null, null);

      assertEquals("DEFAULT IS COUNTRY-XYZ", result);
    }
  }

  @Test
  public void testParseDefaultValue_ReplacesParentLinkColumn() throws JSONException {
    String parentId = "parent-999";

    when(tab.getTable()).thenReturn(table);
    when(tab.getWindow()).thenReturn(window);
    when(tab.getADAuxiliaryInputList()).thenReturn(List.of());
    when(window.isSalesTransaction()).thenReturn(true);

    when(column.getDBColumnName()).thenReturn("parent_column");

    when(property.getTargetEntity()).thenReturn(entity);

    try (MockedStatic<OBDal> obDal = mockStatic(OBDal.class); MockedStatic<ModelProvider> modelProvider = mockStatic(
        ModelProvider.class); MockedStatic<OBContext> obContext = mockStatic(OBContext.class)) {
      obDal.when(OBDal::getInstance).thenReturn(obDalInstance);

      OBCriteria columnCriteria = obCriteria;
      when(obDalInstance.createCriteria(Column.class)).thenReturn(columnCriteria);
      when(columnCriteria.add(any())).thenReturn(columnCriteria);
      when(columnCriteria.list()).thenReturn(List.of(column));

      modelProvider.when(ModelProvider::getInstance).thenReturn(provider);
      when(provider.getEntityByTableId(any())).thenReturn(entity);
      when(entity.getPropertyByColumnName(any())).thenReturn(property);

      obContext.when(OBContext::getOBContext).thenReturn(ctx);
      when(ctx.getUser()).thenReturn(user);
      when(ctx.getCurrentOrganization()).thenReturn(organization);
      when(ctx.getCurrentClient()).thenReturn(client);

      String input = "@PARENT_COLUMN@";
      Object result = WindowUtils.parseDefaultValue(input, false, tab, parentId, entity, null);

      assertEquals("parent-999", result);
    }
  }

  @Test
  public void testParseDefaultValue_SQLExecutesAndReturnsResult() throws Exception {
    when(organization.getId()).thenReturn("org");
    when(organization.getCurrency()).thenReturn(currency);
    when(currency.getId()).thenReturn("curr");
    when(client.getId()).thenReturn("client");
    when(user.getId()).thenReturn("user");

    when(tab.getADAuxiliaryInputList()).thenReturn(List.of());
    when(tab.getWindow()).thenReturn(window);
    when(window.isSalesTransaction()).thenReturn(true);

    try (MockedStatic<OBContext> obContext = mockStatic(
        OBContext.class); MockedConstruction<DalConnectionProvider> mockedConnectionProvider = mockConstruction(
        DalConnectionProvider.class, (mock, context) -> {

          when(mock.getPreparedStatement(anyString())).thenReturn(ps);
          when(ps.executeQuery()).thenReturn(rs);
          when(rs.next()).thenReturn(true);
          when(rs.getString(1)).thenReturn("SQL-RESULT");
        })) {
      obContext.when(OBContext::getOBContext).thenReturn(ctx);
      when(ctx.getUser()).thenReturn(user);
      when(ctx.getCurrentOrganization()).thenReturn(organization);
      when(ctx.getCurrentClient()).thenReturn(client);

      String input = "@SQL=SELECT 'SQL-RESULT'";
      Object result = WindowUtils.parseDefaultValue(input, false, tab, null, null, null);

      assertEquals("SQL-RESULT", result);
    }
  }
}
