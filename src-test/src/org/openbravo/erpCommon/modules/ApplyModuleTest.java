package org.openbravo.erpCommon.modules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.reference.PInstanceProcessData;
import org.openbravo.erpCommon.utility.OBError;
/** Tests for {@link ApplyModule}. */
@SuppressWarnings({"java:S120"})

@RunWith(MockitoJUnitRunner.Silent.class)
public class ApplyModuleTest {

  private static final String GET_PROCESS_INSTANCE_MESSAGE_SIMPLE = "getProcessInstanceMessageSimple";

  private static final String TEST_DIR = "/test/ob/dir";
  private static final String CORE_MODULE_ID = "0";

  private ApplyModule instance;
  private ConnectionProvider mockPool;

  private MockedStatic<PInstanceProcessData> pInstanceStatic;
  /**
   * Sets up test fixtures.
   * @throws Exception if an error occurs
   */

  @Before
  public void setUp() throws Exception {
    mockPool = mock(ConnectionProvider.class);
    instance = new ApplyModule(mockPool, TEST_DIR);
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (pInstanceStatic != null) pInstanceStatic.close();
  }
  /**
   * Constructor sets fields.
   * @throws Exception if an error occurs
   */

  @Test
  public void testConstructorSetsFields() throws Exception {
    ApplyModule am = new ApplyModule(mockPool, TEST_DIR);

    Field obDirField = ApplyModule.class.getDeclaredField("obDir");
    obDirField.setAccessible(true);
    assertEquals(TEST_DIR, obDirField.get(am));

    Field forceField = ApplyModule.class.getDeclaredField("forceRefData");
    forceField.setAccessible(true);
    assertEquals(false, forceField.get(am));
  }
  /**
   * Constructor with force ref data.
   * @throws Exception if an error occurs
   */

  @Test
  public void testConstructorWithForceRefData() throws Exception {
    ApplyModule am = new ApplyModule(mockPool, TEST_DIR, true);

    Field forceField = ApplyModule.class.getDeclaredField("forceRefData");
    forceField.setAccessible(true);
    assertEquals(true, forceField.get(am));
  }
  /**
   * Get process instance message simple success.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetProcessInstanceMessageSimpleSuccess() throws Exception {
    OBError result = invokeGetProcessInstanceMessage("PINSTANCE1", "1", "Process completed successfully");

    assertEquals("Success", result.getType());
  }
  /**
   * Get process instance message simple error.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetProcessInstanceMessageSimpleError() throws Exception {
    OBError result = invokeGetProcessInstanceMessage("PINSTANCE2", "0", "Something went wrong");

    assertEquals("Error", result.getType());
  }
  /**
   * Get process instance message simple with error marker.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetProcessInstanceMessageSimpleWithErrorMarker() throws Exception {
    OBError result = invokeGetProcessInstanceMessage("PINSTANCE3", "0", "prefix @ERROR=Actual error message");

    assertEquals("Error", result.getType());
    assertEquals("Actual error message", result.getMessage());
  }
  /**
   * Get process instance message simple empty data.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetProcessInstanceMessageSimpleEmptyData() throws Exception {
    OBError result = invokeGetProcessInstanceMessageWithRawData("PINSTANCE_EMPTY",
        new PInstanceProcessData[0]);

    // When no data, OBError is returned with defaults (no type set explicitly)
    assertTrue(result != null);
  }
  /**
   * Data set2import filename for core module.
   * @throws Exception if an error occurs
   */

  @Test
  public void testDataSet2ImportFilenameForCoreModule() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    ApplyModuleData ds = objenesis.newInstance(ApplyModuleData.class);
    ds.adModuleId = CORE_MODULE_ID;
    ds.dsName = "Test Dataset";

    Method method = ApplyModule.class.getDeclaredMethod("dataSet2ImportFilename",
        ApplyModuleData.class);
    method.setAccessible(true);

    String result = (String) method.invoke(instance, ds);

    assertEquals(TEST_DIR + "/referencedata/standard/Test_Dataset.xml", result);
  }
  /**
   * Get process instance message simple null data.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetProcessInstanceMessageSimpleNullData() throws Exception {
    OBError result = invokeGetProcessInstanceMessageWithRawData("PINSTANCE_NULL", null);

    // When null data, OBError is returned with no type set
    assertTrue(result != null);
  }

  // --- Helper methods ---

  private OBError invokeGetProcessInstanceMessage(String pinstanceId, String resultCode,
      String errorMsg) throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    PInstanceProcessData data = objenesis.newInstance(PInstanceProcessData.class);
    data.result = resultCode;
    data.errormsg = errorMsg;

    return invokeGetProcessInstanceMessageWithRawData(pinstanceId,
        new PInstanceProcessData[] { data });
  }

  private OBError invokeGetProcessInstanceMessageWithRawData(String pinstanceId,
      PInstanceProcessData[] dataArray) throws Exception {
    pInstanceStatic = mockStatic(PInstanceProcessData.class);

    pInstanceStatic.when(() -> PInstanceProcessData.select(mockPool, pinstanceId))
        .thenReturn(dataArray);

    Field poolField = ApplyModule.class.getDeclaredField("pool");
    poolField.setAccessible(true);
    poolField.set(null, mockPool);

    Method method = ApplyModule.class.getDeclaredMethod(GET_PROCESS_INSTANCE_MESSAGE_SIMPLE,
        ConnectionProvider.class, String.class);
    method.setAccessible(true);

    return (OBError) method.invoke(null, mockPool, pinstanceId);
  }
}
