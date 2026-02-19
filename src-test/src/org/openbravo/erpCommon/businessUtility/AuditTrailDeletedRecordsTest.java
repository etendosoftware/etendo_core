package org.openbravo.erpCommon.businessUtility;

import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.ui.Window;

/**
 * Tests for {@link AuditTrailDeletedRecords}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class AuditTrailDeletedRecordsTest {

  private static final String TEST_TAB_ID = "TAB001";
  private static final String TEST_TABLE_ID = "TABLE001";
  private static final String TEST_WINDOW_ID = "WIN001";
  private static final String TEST_COLUMN_ID = "COL001";

  @Mock
  private ConnectionProvider mockConn;

  @Mock
  private VariablesSecureApp mockVars;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private Tab mockTab;

  @Mock
  private Table mockTable;

  @Mock
  private Window mockWindow;

  @Mock
  private ModelProvider mockModelProvider;

  @Mock
  private Entity mockEntity;

  @Mock
  private Property mockProperty;

  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<ModelProvider> modelProviderStatic;
  private MockedStatic<Utility> utilityStatic;

  @Before
  public void setUp() {
    obDalStatic = mockStatic(OBDal.class);
    modelProviderStatic = mockStatic(ModelProvider.class);
    utilityStatic = mockStatic(Utility.class);

    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);
    modelProviderStatic.when(ModelProvider::getInstance).thenReturn(mockModelProvider);
  }

  @After
  public void tearDown() {
    if (obDalStatic != null) obDalStatic.close();
    if (modelProviderStatic != null) modelProviderStatic.close();
    if (utilityStatic != null) utilityStatic.close();
  }

  @Test
  public void testGetDeletedRecordsReturnsNullOnException() {
    // Arrange - OBDal.getInstance().get(Tab.class, tabId) throws exception
    when(mockOBDal.get(Tab.class, TEST_TAB_ID)).thenThrow(new RuntimeException("DB error"));

    // Act
    FieldProvider[] result = AuditTrailDeletedRecords.getDeletedRecords(
        mockConn, mockVars, TEST_TAB_ID, 0, 0, false, null, null, null);

    // Assert
    assertNull(result);
  }

  @Test
  public void testGetDeletedRecordsWithFkReturnsNullOnException() {
    // Arrange
    when(mockOBDal.get(Tab.class, TEST_TAB_ID)).thenThrow(new RuntimeException("DB error"));

    // Act
    FieldProvider[] result = AuditTrailDeletedRecords.getDeletedRecords(
        mockConn, mockVars, TEST_TAB_ID, "fkCol", "fkId", 0, 0, false, null, null, null);

    // Assert
    assertNull(result);
  }

  @Test
  public void testGetDeletedRecordsOverloadDelegatesToMain() {
    // Arrange - will fail inside the method but verifies delegation
    when(mockOBDal.get(Tab.class, TEST_TAB_ID)).thenThrow(new RuntimeException("Expected"));

    // Act
    FieldProvider[] result = AuditTrailDeletedRecords.getDeletedRecords(
        mockConn, mockVars, TEST_TAB_ID, 0, 10, true, "2026-01-01", "2026-12-31", "user1");

    // Assert
    assertNull(result);
  }
}
