package org.openbravo.base.model.domaintype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import java.lang.reflect.InvocationTargetException;

import java.lang.reflect.Method;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.base.model.BaseOBObjectDef;
import org.openbravo.base.model.Column;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.model.Reference;
import org.openbravo.base.model.Table;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.validation.ValidationException;

/**
 * Tests for {@link BaseForeignKeyDomainType}.
 * Focuses on the getReferedTableName special cases and checkIsValidValue logic.
 */
@SuppressWarnings("java:S112")
@RunWith(MockitoJUnitRunner.Silent.class)
public class BaseForeignKeyDomainTypeTest {

  private static final String C_BPARTNER_ID = "C_BPartner_ID";
  private static final String C_BPARTNER = "C_BPartner";
  private static final String BUSINESS_PARTNER = "BusinessPartner";

  private static final String TEST_REF_ID = "TEST_REF_123";

  @Mock
  private ModelProvider mockModelProvider;

  @Mock
  private Reference mockReference;

  private MockedStatic<ModelProvider> modelProviderStatic;

  private BaseForeignKeyDomainType instance;
  /**
   * Sets up test fixtures.
   * @throws Exception if an error occurs
   */

  @Before
  public void setUp() throws Exception {
    modelProviderStatic = mockStatic(ModelProvider.class);
    modelProviderStatic.when(ModelProvider::getInstance).thenReturn(mockModelProvider);

    // Use a concrete subclass for testing
    instance = new BaseForeignKeyDomainType() {};
    instance.setReference(mockReference);
    instance.setModelProvider(mockModelProvider);

    when(mockReference.getId()).thenReturn(TEST_REF_ID);
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (modelProviderStatic != null) {
      modelProviderStatic.close();
    }
  }

  // --- getReferedTableName tests ---

  private String invokeGetReferedTableName(String columnName) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method method = BaseForeignKeyDomainType.class.getDeclaredMethod("getReferedTableName",
        String.class);
    method.setAccessible(true);
    return (String) method.invoke(instance, columnName);
  }
  /**
   * Get refered table name standard column.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetReferedTableNameStandardColumn() throws Exception {
    String result = invokeGetReferedTableName(C_BPARTNER_ID);
    assertEquals(C_BPARTNER, result);
  }
  /**
   * Get refered table name removes id suffix.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetReferedTableNameRemovesIdSuffix() throws Exception {
    String result = invokeGetReferedTableName("AD_Org_ID");
    assertEquals("AD_Org", result);
  }
  /**
   * Get refered table name ref order line.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetReferedTableNameRefOrderLine() throws Exception {
    String result = invokeGetReferedTableName("Ref_OrderLine_ID");
    assertEquals("C_OrderLine", result);
  }
  /**
   * Get refered table name settlement cancel.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetReferedTableNameSettlementCancel() throws Exception {
    String result = invokeGetReferedTableName("C_Settlement_Cancel_ID");
    assertEquals("C_Settlement", result);
  }
  /**
   * Get refered table name settlement generate.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetReferedTableNameSettlementGenerate() throws Exception {
    String result = invokeGetReferedTableName("C_Settlement_Generate_ID");
    assertEquals("C_Settlement", result);
  }
  /**
   * Get refered table name fact acct ref.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetReferedTableNameFactAcctRef() throws Exception {
    String result = invokeGetReferedTableName("Fact_Acct_Ref_ID");
    assertEquals("Fact_Acct", result);
  }
  /**
   * Get refered table name account id.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetReferedTableNameAccountId() throws Exception {
    String result = invokeGetReferedTableName("Account_ID");
    assertEquals("C_ElementValue", result);
  }
  /**
   * Get refered table name created by.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetReferedTableNameCreatedBy() throws Exception {
    String result = invokeGetReferedTableName("CreatedBy");
    assertEquals("AD_User", result);
  }
  /**
   * Get refered table name updated by.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetReferedTableNameUpdatedBy() throws Exception {
    String result = invokeGetReferedTableName("UpdatedBy");
    assertEquals("AD_User", result);
  }
  /**
   * Get refered table name product attribute.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetReferedTableNameProductAttribute() throws Exception {
    when(mockReference.getId()).thenReturn(Reference.PRODUCT_ATTRIBUTE);
    String result = invokeGetReferedTableName("M_Product_ID");
    assertEquals("M_AttributeSetInstance", result);
  }
  /**
   * Get refered table name image blob.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetReferedTableNameImageBlob() throws Exception {
    when(mockReference.getId()).thenReturn(Reference.IMAGE_BLOB);
    String result = invokeGetReferedTableName("AD_Image_ID");
    assertEquals("AD_Image", result);
  }
  /**
   * Get refered table name null reference.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetReferedTableNameNullReference() throws Exception {
    instance.setReference(null);
    String result = invokeGetReferedTableName("M_Product_ID");
    assertEquals("M_Product", result);
  }

  // --- checkIsValidValue tests ---
  /**
   * Check is valid value null is valid.
   * @throws Exception if an error occurs
   */

  @Test
  public void testCheckIsValidValueNullIsValid() throws Exception {
    Property mockProperty = mock(Property.class);
    instance.checkIsValidValue(mockProperty, null);
    // Should not throw
  }
  /** Check is valid value non base ob object throws. */

  @Test
  public void testCheckIsValidValueNonBaseOBObjectThrows() {
    Property mockProperty = mock(Property.class);
    try {
      instance.checkIsValidValue(mockProperty, "not a BaseOBObject");
      fail("Expected ValidationException");
    } catch (ValidationException e) {
      // expected
    }
  }
  /**
   * Check is valid value matching entity.
   * @throws Exception if an error occurs
   */

  @Test
  public void testCheckIsValidValueMatchingEntity() throws Exception {
    Property mockProperty = mock(Property.class);
    when(mockProperty.getColumnName()).thenReturn(C_BPARTNER_ID);

    Entity mockEntity = mock(Entity.class);
    when(mockEntity.getName()).thenReturn(BUSINESS_PARTNER);

    BaseOBObjectDef mockObj = mock(BaseOBObjectDef.class);
    when(mockObj.getEntity()).thenReturn(mockEntity);

    when(mockModelProvider.getEntity(BUSINESS_PARTNER)).thenReturn(mockEntity);
    when(mockModelProvider.getEntityByTableName(C_BPARTNER)).thenReturn(mockEntity);

    // Should not throw when entities match
    instance.checkIsValidValue(mockProperty, mockObj);
  }
  /** Check is valid value mismatched entity throws. */

  @Test
  public void testCheckIsValidValueMismatchedEntityThrows() {
    Property mockProperty = mock(Property.class);
    when(mockProperty.getColumnName()).thenReturn(C_BPARTNER_ID);

    Entity mockEntity1 = mock(Entity.class);
    when(mockEntity1.getName()).thenReturn(BUSINESS_PARTNER);

    Entity mockEntity2 = mock(Entity.class);

    BaseOBObjectDef mockObj = mock(BaseOBObjectDef.class);
    when(mockObj.getEntity()).thenReturn(mockEntity1);

    when(mockModelProvider.getEntity(BUSINESS_PARTNER)).thenReturn(mockEntity1);
    when(mockModelProvider.getEntityByTableName(C_BPARTNER)).thenReturn(mockEntity2);

    try {
      instance.checkIsValidValue(mockProperty, mockObj);
      fail("Expected ValidationException");
    } catch (ValidationException e) {
      // expected
    }
  }
  /**
   * Check is valid value null refered entity.
   * @throws Exception if an error occurs
   */

  @Test
  public void testCheckIsValidValueNullReferedEntity() throws Exception {
    Property mockProperty = mock(Property.class);
    when(mockProperty.getColumnName()).thenReturn(C_BPARTNER_ID);

    Entity mockEntity = mock(Entity.class);
    when(mockEntity.getName()).thenReturn(BUSINESS_PARTNER);

    BaseOBObjectDef mockObj = mock(BaseOBObjectDef.class);
    when(mockObj.getEntity()).thenReturn(mockEntity);

    when(mockModelProvider.getEntity(BUSINESS_PARTNER)).thenReturn(mockEntity);
    when(mockModelProvider.getEntityByTableName(C_BPARTNER)).thenReturn(null);

    // Should not throw when refered entity is null (TableDir case)
    instance.checkIsValidValue(mockProperty, mockObj);
  }

  // --- getForeignKeyColumn tests ---
  /**
   * Get foreign key column success.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetForeignKeyColumnSuccess() throws Exception {
    Table mockTable = mock(Table.class);
    Column mockColumn = mock(Column.class);

    when(mockModelProvider.getTable(C_BPARTNER)).thenReturn(mockTable);
    when(mockTable.getPrimaryKeyColumns()).thenReturn(java.util.Collections.singletonList(mockColumn));

    Column result = instance.getForeignKeyColumn(C_BPARTNER_ID);
    assertEquals(mockColumn, result);
  }
  /**
   * Get foreign key column not found friendly warnings.
   * @throws Exception if an error occurs
   */

  @Test(expected = IllegalArgumentException.class)
  public void testGetForeignKeyColumnNotFoundFriendlyWarnings() throws Exception {
    try (MockedStatic<OBPropertiesProvider> propStatic = mockStatic(OBPropertiesProvider.class)) {
      propStatic.when(OBPropertiesProvider::isFriendlyWarnings).thenReturn(true);
      when(mockModelProvider.getTable("NonExistent")).thenThrow(new RuntimeException("not found"));

      instance.getForeignKeyColumn("NonExistent_ID");
    }
  }
}
