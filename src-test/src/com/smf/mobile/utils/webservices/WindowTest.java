package com.smf.mobile.utils.webservices;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.model.Reference;
import org.openbravo.base.model.domaintype.DomainType;
import org.openbravo.base.model.domaintype.ForeignKeyDomainType;
import org.openbravo.base.util.CheckException;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.domain.ReferencedTree;
import org.openbravo.model.ad.domain.ReferencedTreeField;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.service.datasource.DataSource;
import org.openbravo.service.datasource.DatasourceField;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.userinterface.selector.Selector;
import org.openbravo.userinterface.selector.SelectorField;

/**
 * Test class for WindowTest.
 */
@RunWith(MockitoJUnitRunner.class)
public class WindowTest {

  private Window window;

  @Mock
  private Tab tab;

  @Mock
  private Column column;

  @Mock
  private Table table;

  @Mock
  private Entity entity;

  @Mock
  private Property property;

  @Mock
  private SelectorField selectorField;

  @Mock
  private DatasourceField datasourceField;

  @Mock
  private ModelProvider provider;

  @Mock
  private DomainType domainType;

  @Mock
  private Selector selector;

  @Mock
  private DataSource dataSource;

  @Mock
  private Reference reference;

  @Mock
  private org.openbravo.model.ad.domain.Reference obReference;

  @Mock
  private ForeignKeyDomainType foreignKeyDomainType;

  @Mock
  private ModelProvider modelProvider;

  @Mock
  private ReferencedTree treeSelector;

  @Mock
  private ReferencedTreeField treeField;

  /**
   * Sets up the test environment before each test.
   * Initializes the WindowTest instance.
   */
  @Before
  public void setup() {
    MockitoAnnotations.openMocks(this);

    window = spy(new Window());
    selectorField = spy(new SelectorField());

    doReturn("displayFieldProperty").when(window).getDisplayField(any());
    doReturn("valueFieldProperty").when(window).getValueField(any());
    doReturn("extraField").when(window).getExtraSearchFields(any());
  }

  /**
   * Tests that the getTabWhereClause method constructs the correct where clause
   * when there are two linked columns and an HQL where clause is provided.
   */
  @Test
  public void testGetTabWhereClause_TwoLinkedColumnsWithHQL() {
    Column column1 = mock(Column.class);
    Column column2 = mock(Column.class);

    when(tab.getTabLevel()).thenReturn(1L);
    when(tab.getTable()).thenReturn(table);
    when(column1.isLinkToParentColumn()).thenReturn(true);
    when(column2.isLinkToParentColumn()).thenReturn(true);
    when(table.getADColumnList()).thenReturn(List.of(column1, column2));

    when(tab.getHqlwhereclause()).thenReturn("extraHQLCondition = 'Y'");

    doReturn("linkedEntity1").when(window).getEntityColumnName(column1);
    doReturn("linkedEntity2").when(window).getEntityColumnName(column2);

    String result = window.getTabWhereClause(tab);

    assertEquals(
        "linkedEntity1.id='PARENT_RECORD_ID' or linkedEntity2.id='PARENT_RECORD_ID' and extraHQLCondition = 'Y'",
        result);
  }

  /**
   * Tests that the getEntityColumnName method returns the correct property name.
   */
  @Test
  public void testGetEntityColumnName_ReturnsCorrectPropertyName() {
    when(column.getTable()).thenReturn(table);
    when(table.getName()).thenReturn("TestTable");
    when(column.getDBColumnName()).thenReturn("test_column");

    when(entity.getPropertyByColumnName("test_column")).thenReturn(property);
    when(property.getName()).thenReturn("propertyNameResult");

    try (MockedStatic<ModelProvider> modelProvider = mockStatic(ModelProvider.class)) {
      modelProvider.when(ModelProvider::getInstance).thenReturn(provider);
      when(provider.getEntity("TestTable")).thenReturn(entity);

      String result = window.getEntityColumnName(column);

      assertEquals("propertyNameResult", result);
    }
  }

  /**
   * Tests that the getTabEntityName method returns the table name.
   */
  @Test
  public void testGetTabEntityName_ReturnsTableName() {
    when(tab.getTable()).thenReturn(table);
    when(table.getName()).thenReturn("TestTableName");

    String result = window.getTabEntityName(tab);

    assertEquals("TestTableName", result);
  }

  /**
   * Tests that the getPropertyOrDataSourceField method returns the selector field property as is when
   * it is not null.
   */
  @Test
  public void testGetPropertyOrDataSourceField_WithProperty() {
    when(selectorField.getProperty()).thenReturn("property.name");

    String result = Window.getPropertyOrDataSourceField(selectorField);

    assertEquals("property$name", result);
  }

  /**
   * Tests that the getPropertyOrDataSourceField method returns the selector field property as is
   * when it is not null.
   */
  @Test
  public void testGetPropertyOrDataSourceField_WithDisplayColumnAlias() {
    when(selectorField.getProperty()).thenReturn(null);
    when(selectorField.getDisplayColumnAlias()).thenReturn("alias.name");

    String result = Window.getPropertyOrDataSourceField(selectorField);

    assertEquals("alias$name", result);
  }

  /**
   * Tests that the getPropertyOrDataSourceField method returns the selector field property as is
   * when it is not null.
   */
  @Test
  public void testGetPropertyOrDataSourceField_WithDatasourceField() {
    when(selectorField.getProperty()).thenReturn(null);
    when(selectorField.getDisplayColumnAlias()).thenReturn(null);
    when(selectorField.getObserdsDatasourceField()).thenReturn(datasourceField);
    when(datasourceField.getName()).thenReturn("datasource.name");

    String result = Window.getPropertyOrDataSourceField(selectorField);

    assertEquals("datasource$name", result);
  }

  /**
   * Tests that the getPropertyOrDataSourceField method throws an
   * IllegalStateException if all inputs are null.
   */
  @Test
  public void testGetPropertyOrDataSourceField_AllNull_ThrowsException() {
    when(selectorField.getProperty()).thenReturn(null);
    when(selectorField.getDisplayColumnAlias()).thenReturn(null);
    when(selectorField.getObserdsDatasourceField()).thenReturn(null);

    try {
      Window.getPropertyOrDataSourceField(selectorField);
      fail("Expected IllegalStateException to be thrown");
    } catch (IllegalStateException e) {
      assertTrue(e.getMessage().contains("Selectorfield"));
    }
  }

  /**
   * Tests that the getValueField method returns the correct value field.
   */
  @Test
  public void testGetValueField_ValueFieldNormal_ReturnsValueField() {
    doReturn("testField").when(window).getValueField(selector);

    String result = window.getValueField(selector);

    assertEquals("testField", result);
  }

  /**
   * Tests that the getValueField method returns the correct value field when the selector's datasource has
   * no table.
   */
  @Test
  public void testGetValueField_DataSourceWithoutTable_ReturnsFirstFieldName() {
    doReturn("datasourceFieldName").when(window).getValueField(selector);

    String result = window.getValueField(selector);

    assertEquals("datasourceFieldName", result);
  }

  /**
   * Tests that the getDisplayField method returns the correct display field.
   */
  @Test
  public void testGetDisplayField_DisplayFieldExists_ReturnsPropertyOrDataSourceField() {
    doReturn("displayFieldTest").when(window).getDisplayField(selector);

    try (MockedStatic<Window> windowStatic = mockStatic(Window.class)) {
      windowStatic.when(() -> Window.getPropertyOrDataSourceField(selectorField)).thenReturn("displayFieldTest");

      String result = window.getDisplayField(selector);

      assertEquals("displayFieldTest", result);
    }
  }

  /**
   * Tests that the getDisplayField method returns the correct display field when the selector's
   * datasource has no table.
   */
  @Test
  public void testGetDisplayField_DataSourceWithoutTable_ReturnsFirstFieldName() {
    doReturn("field$name").when(window).getDisplayField(selector);

    String result = window.getDisplayField(selector);

    assertEquals("field$name", result);
  }

  /**
   * Tests that the getDisplayField method returns "_identifier" if the selector's value field,
   * display field and datasource field are all null.
   */
  @Test
  public void testGetDisplayField_AllNull_ReturnsIdentifier() {
    doReturn("_identifier").when(window).getDisplayField(selector);

    String result = window.getDisplayField(selector);

    assertEquals("_identifier", result);
  }

  /**
   * Tests that the getDomainType method returns the correct domain type when the reference exists.
   */
  @Test
  public void testGetDomainType_ReferenceExists_ReturnsDomainType() {
    try (MockedStatic<ModelProvider> modelProviderStatic = mockStatic(ModelProvider.class)) {
      modelProviderStatic.when(ModelProvider::getInstance).thenReturn(provider);

      when(provider.getReference("refId123")).thenReturn(reference);
      when(reference.getDomainType()).thenReturn(domainType);

      DomainType result = Window.getDomainType("refId123");

      assertEquals(domainType, result);
    }
  }

  /**
   * Tests that the getDomainType method throws a CheckException if the reference does not exist.
   * @throws CheckException if the reference does not exist
   */
  @Test(expected = CheckException.class)
  public void testGetDomainType_ReferenceNotFound_ThrowsException() {
    try (MockedStatic<ModelProvider> modelProviderStatic = mockStatic(ModelProvider.class)) {
      modelProviderStatic.when(ModelProvider::getInstance).thenReturn(provider);

      when(provider.getReference("invalidRefId")).thenReturn(null);

      Window.getDomainType("invalidRefId");
    }
  }

  /**
   * Tests that the isBoolean method returns false when the domain type is not primitive.
   */
  @Test
  public void testIsBoolean_DomainTypeIsNotPrimitive_ReturnsFalse() {
    when(selectorField.getObuiselSelector()).thenReturn(selector);
    when(selector.getTable()).thenReturn(table);
    when(table.getName()).thenReturn("TestEntity");
    when(selectorField.getProperty()).thenReturn("someProperty");

    try (MockedStatic<ModelProvider> modelProvider = mockStatic(
        ModelProvider.class); MockedStatic<DalUtil> dalUtil = mockStatic(DalUtil.class)) {
      modelProvider.when(ModelProvider::getInstance).thenReturn(provider);
      when(provider.getEntity("TestEntity")).thenReturn(entity);
      dalUtil.when(() -> DalUtil.getPropertyFromPath(entity, "someProperty")).thenReturn(property);

      when(property.getDomainType()).thenReturn(domainType);

      boolean result = window.isBoolean(selectorField);

      assertFalse(result);
    }
  }

  /**
   * Tests that the getDomainType method returns the correct domain type when the selector field is part of a custom
   * query and the reference exists.
   */
  @Test
  public void testGetDomainType_CustomQueryWithReference() {
    doReturn(selector).when(selectorField).getObuiselSelector();
    doReturn(null).when(selectorField).getProperty();
    doReturn(obReference).when(selectorField).get(SelectorField.PROPERTY_REFERENCE);

    when(selector.getTable()).thenReturn(mock(Table.class));
    when(selector.isCustomQuery()).thenReturn(true);
    when(obReference.getId()).thenReturn("ref123");

    try (MockedStatic<ModelProvider> modelProviderStatic = mockStatic(ModelProvider.class)) {
      modelProviderStatic.when(ModelProvider::getInstance).thenReturn(provider);
      when(provider.getReference("ref123")).thenReturn(reference);
      when(reference.getDomainType()).thenReturn(domainType);

      DomainType result = Window.getDomainType(selectorField);

      assertEquals(domainType, result);
    }
  }

  /**
   * Tests that the getDomainType method returns the correct domain type when the selector field is a datasource field
   * and the reference exists.
   */
  @Test
  public void testGetDomainType_DatasourceFieldReferenceNotNull() {
    when(selector.getTable()).thenReturn(null);
    doReturn(selector).when(selectorField).getObuiselSelector();

    doReturn(datasourceField).when(selectorField).getObserdsDatasourceField();
    when(datasourceField.getReference()).thenReturn(obReference);
    when(obReference.getId()).thenReturn("dsref123");

    try (MockedStatic<ModelProvider> modelProviderStatic = mockStatic(ModelProvider.class)) {
      modelProviderStatic.when(ModelProvider::getInstance).thenReturn(provider);
      when(provider.getReference("dsref123")).thenReturn(reference);
      when(reference.getDomainType()).thenReturn(domainType);

      DomainType result = Window.getDomainType(selectorField);

      assertEquals(domainType, result);
    }
  }

  /**
   * Tests that the getExtraSearchFields method returns an empty string if the selector fields list is empty.
   */
  @Test
  public void testGetExtraSearchFields_EmptyList_ReturnsEmptyString() {
    doReturn(List.of()).when(window).getExtraSearchFields(selector);

    String result = window.getExtraSearchFields(selector);

    assertEquals(StringUtils.EMPTY, result);
  }

  /**
   * Tests that the getExtraSearchFields method skips adding a field when the field's property
   * equals the display field.
   */
  @Test
  public void testGetExtraSearchFields_FieldEqualsDisplayField_SkipsField() {
    SelectorField selectorField = new SelectorField();
    selectorField.setProperty("sameField");
    selectorField.setActive(true);
    selectorField.setSearchinsuggestionbox(true);

    doReturn(StringUtils.EMPTY).when(window).getExtraSearchFields(selector);

    String result = window.getExtraSearchFields(selector);

    assertEquals(StringUtils.EMPTY, result);
  }

  /**
   * Tests that the getExtraSearchFields method successfully adds a field to the extra search fields property.
   */
  @Test
  public void testGetExtraSearchFields_FieldAddedSuccessfully() {
    when(selector.getOBUISELSelectorFieldList()).thenReturn(List.of(selectorField));
    doReturn(true).when(selectorField).isActive();
    doReturn(selector).when(selectorField).getObuiselSelector();
    doReturn(datasourceField).when(selectorField).getObserdsDatasourceField();
    when(datasourceField.getReference()).thenReturn(obReference);
    when(obReference.getId()).thenReturn("ref123");

    when(window.getDisplayField(selector)).thenReturn("otherField");
    doReturn("fieldToAdd").when(selectorField).getProperty();
    doReturn(true).when(selectorField).isSearchinsuggestionbox();

    doCallRealMethod().when(window).getExtraSearchFields(selector);

    try (MockedStatic<ModelProvider> modelProviderStatic = mockStatic(ModelProvider.class)) {
      modelProviderStatic.when(ModelProvider::getInstance).thenReturn(modelProvider);
      when(modelProvider.getReference("ref123")).thenReturn(reference);
      when(reference.getDomainType()).thenReturn(foreignKeyDomainType);

      String result = window.getExtraSearchFields(selector);

      assertEquals("fieldToAdd$_identifier", result);
    }
  }

  /**
   * Tests that the setSelectorProperties method sets the correct values in the selectorInfo object.
   *
   * @throws JSONException
   *             if an error occurs when modifying the selectorInfo object.
   */
  @Test
  public void testSetSelectorProperties_SetsCorrectValues() throws JSONException {
    doReturn("extraProperty").when(selectorField).getProperty();
    when(selectorField.isOutfield()).thenReturn(true);

    JSONObject selectorInfo = new JSONObject();

    doReturn("displayFieldProperty").when(window).getDisplayField(any());
    doReturn("valueFieldProperty").when(window).getValueField(any());

    window.setSelectorProperties(List.of(selectorField), selectorField, selectorField, selectorInfo);

    assertEquals("id,displayFieldProperty", selectorInfo.getString(JsonConstants.SELECTEDPROPERTIES_PARAMETER));
  }

  /**
   * Tests that the setSelectorProperties method sets the correct values in the selectorInfo object
   * when the selectorField is an out field.
   *
   * @throws JSONException
   *             if an error occurs when modifying the selectorInfo object.
   */
  @Test
  public void testSetSelectorProperties_SetsCorrectValues2() throws JSONException {
    JSONObject selectorInfo = new JSONObject();

    doReturn("extraProperty").when(selectorField).getProperty();
    when(selectorField.isOutfield()).thenReturn(true);

    window.setSelectorProperties(List.of(selectorField), null, null, selectorInfo);

    assertEquals("id", selectorInfo.getString(JsonConstants.SELECTEDPROPERTIES_PARAMETER));
    assertEquals("_identifier,extraProperty,", selectorInfo.getString(JsonConstants.ADDITIONAL_PROPERTIES_PARAMETER));
  }

  /**
   * Tests that the setSelectorProperties method adds the correct derived properties in the selectorInfo
   * object.
   *
   * @throws JSONException
   *             if an error occurs when modifying the selectorInfo object.
   */
  @Test
  public void testSetSelectorProperties_AddsDerivedProperties() throws JSONException {
    JSONObject selectorInfo = new JSONObject();

    doReturn("field$property").when(selectorField).getProperty();
    when(selectorField.isOutfield()).thenReturn(false);

    window.setSelectorProperties(List.of(selectorField), null, null, selectorInfo);

    assertEquals("id", selectorInfo.getString(JsonConstants.SELECTEDPROPERTIES_PARAMETER));
    assertEquals("_identifier,field$property", selectorInfo.getString(JsonConstants.ADDITIONAL_PROPERTIES_PARAMETER));
  }

  /**
   * Tests that the setSelectorProperties method adds the display field to the selectorInfo object.
   *
   * @throws JSONException
   *             if an error occurs when modifying the selectorInfo object.
   */
  @Test
  public void testSetSelectorProperties_AddsDisplayField() throws JSONException {
    JSONObject selectorInfo = new JSONObject();

    doReturn("displayProperty").when(window).getDisplayField(any());

    window.setSelectorProperties(List.of(), selectorField, null, selectorInfo);

    assertEquals("id,displayProperty", selectorInfo.getString(JsonConstants.SELECTEDPROPERTIES_PARAMETER));
    assertTrue(selectorInfo.getString(JsonConstants.ADDITIONAL_PROPERTIES_PARAMETER).startsWith("_identifier,displayProperty"));
  }

  /**
   * Tests that the getSelectorInfo method returns the correct JSON object when the selector reference is null.
   *
   * @throws JSONException
   *             if an error occurs when creating the JSON object.
   */
  @Test
  public void testGetSelectorInfo_NullReference() throws JSONException {
    JSONObject result = window.getSelectorInfo("testFieldId", null);

    assertFalse(result.has("selectorDefinition"));
    assertEquals(JsonConstants.IDENTIFIER, result.get(JsonConstants.SORTBY_PARAMETER));
    assertEquals(JsonConstants.TEXTMATCH_SUBSTRING, result.get(JsonConstants.TEXTMATCH_PARAMETER));
    assertTrue(result.getBoolean(JsonConstants.NOCOUNT_PARAMETER));
    assertEquals("testFieldId", result.get("fieldId"));
    assertEquals(JsonConstants.IDENTIFIER, result.get("displayField"));
    assertEquals(JsonConstants.ID, result.get("valueField"));
    assertEquals(JsonConstants.ID, result.get(JsonConstants.SELECTEDPROPERTIES_PARAMETER));
    assertEquals(JsonConstants.ID + ",", result.get(JsonConstants.ADDITIONAL_PROPERTIES_PARAMETER));
  }

  /**
   * Tests that the getSelectorInfo method returns the correct JSON object when using a selector.
   *
   * @throws Exception if an error occurs during the test.
   */
  @Test
  public void testGetSelectorInfo_WithSelector() throws Exception {
    String fieldId = "testFieldId";

    when(obReference.getOBUISELSelectorList()).thenReturn(List.of(selector));
    when(obReference.getADReferencedTreeList()).thenReturn(List.of());
    when(selector.getObserdsDatasource()).thenReturn(null);
    when(selector.isCustomQuery()).thenReturn(false);
    when(selector.getTable()).thenReturn(mock(Table.class));
    when(selector.getTable().getName()).thenReturn("tableName");
    when(selector.getId()).thenReturn("selectorId");
    when(selector.getDisplayfield()).thenReturn(selectorField);
    when(selectorField.getDisplayColumnAlias()).thenReturn("displayColumnAlias");
    when(selector.getSuggestiontextmatchstyle()).thenReturn("substring");
    when(selector.getOBUISELSelectorFieldList()).thenReturn(List.of());

    JSONObject result = window.getSelectorInfo(fieldId, obReference);

    assertNotNull(result);
    assertEquals("org.openbravo.userinterface.selector.SelectorDataSourceFilter", result.getString("filterClass"));
    assertEquals("displayColumnAlias", result.getString("_sortBy"));
    assertEquals("substring", result.getString("_textMatchStyle"));
    assertTrue(result.getBoolean("_noCount"));
    assertEquals(fieldId, result.getString("fieldId"));
    assertEquals("extraField", result.getString("extraSearchFields"));
    assertEquals("displayFieldProperty", result.getString("displayField"));
    assertEquals("valueFieldProperty", result.getString("valueField"));
  }

  /**
   * Tests that the getSelectorInfo method returns the correct JSON object when using a tree selector.
   *
   * @throws Exception if an error occurs during the test.
   */
  @Test
  public void testGetSelectorInfo_WithTreeSelector() throws Exception {
    String fieldId = "testFieldId";

    when(obReference.getOBUISELSelectorList()).thenReturn(List.of());
    when(obReference.getADReferencedTreeList()).thenReturn(List.of(treeSelector));
    when(treeSelector.getId()).thenReturn("treeSelectorId");
    when(treeSelector.getDisplayfield()).thenReturn(treeField);
    when(treeSelector.getValuefield()).thenReturn(treeField);
    when(treeField.getProperty()).thenReturn("treeProperty");

    JSONObject result = window.getSelectorInfo(fieldId, obReference);

    assertNotNull(result);
    assertEquals("90034CAE96E847D78FBEF6D38CB1930D", result.getString("datasourceName"));
    assertEquals("treeSelectorId", result.getString("_selectorDefinitionId"));
    assertEquals("treeSelectorId", result.getString("treeReferenceId"));
    assertEquals("treeProperty", result.getString("_sortBy"));
    assertEquals("treeProperty", result.getString("displayField"));
    assertEquals("treeProperty", result.getString("valueField"));
    assertEquals(JsonConstants.TEXTMATCH_SUBSTRING, result.getString("_textMatchStyle"));
    assertTrue(result.getBoolean("_noCount"));
    assertEquals(fieldId, result.getString("fieldId"));
    assertEquals(JsonConstants.ID, result.getString("_selectedProperties"));
  }
}