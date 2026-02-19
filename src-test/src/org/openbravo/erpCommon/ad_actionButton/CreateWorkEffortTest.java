package org.openbravo.erpCommon.ad_actionButton;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessContext;
/** Tests for {@link CreateWorkEffort}. */
@SuppressWarnings({"java:S120"})

@RunWith(MockitoJUnitRunner.class)
public class CreateWorkEffortTest {

  private static final String MA_WORKREQUIREMENT_ID = "MA_Workrequirement_ID";
  private static final String NONEXISTENT_ID = "NONEXISTENT_ID";
  private static final String VAL_15_01_2024 = "15-01-2024";
  private static final String STARTTIME = "starttime";
  private static final String ENDTIME = "endtime";
  private static final String ERROR = "Error";

  private CreateWorkEffort instance;

  @Mock
  private ProcessBundle mockBundle;

  @Mock
  private ConnectionProvider mockConn;

  @Mock
  private ProcessContext mockProcessContext;

  @Mock
  private VariablesSecureApp mockVars;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private OBPropertiesProvider mockPropsProvider;

  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<OBPropertiesProvider> propsStatic;
  private MockedStatic<Utility> utilityStatic;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    instance = new CreateWorkEffort();

    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

    propsStatic = mockStatic(OBPropertiesProvider.class);
    propsStatic.when(OBPropertiesProvider::getInstance).thenReturn(mockPropsProvider);

    Properties props = new Properties();
    props.setProperty("dateFormat.java", "dd-MM-yyyy");
    lenient().when(mockPropsProvider.getOpenbravoProperties()).thenReturn(props);

    utilityStatic = mockStatic(Utility.class);

    lenient().when(mockBundle.getConnection()).thenReturn(mockConn);
    lenient().when(mockBundle.getContext()).thenReturn(mockProcessContext);
    lenient().when(mockProcessContext.toVars()).thenReturn(mockVars);
    lenient().when(mockProcessContext.getLanguage()).thenReturn("en_US");
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (obDalStatic != null) obDalStatic.close();
    if (propsStatic != null) propsStatic.close();
    if (utilityStatic != null) utilityStatic.close();
  }
  /**
   * Execute with null work requirement handles error.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteWithNullWorkRequirementHandlesError() throws Exception {
    setupBundleAndExecute("08:00:00", "17:00:00");

    verify(mockBundle).setResult(any(OBError.class));
  }
  /**
   * Execute with null start end time defaults.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteWithNullStartEndTimeDefaults() throws Exception {
    setupBundleAndExecute(null, null);

    verify(mockBundle).setResult(any(OBError.class));
  }
  /**
   * Execute with empty start end time defaults.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteWithEmptyStartEndTimeDefaults() throws Exception {
    setupBundleAndExecute("", "");

    verify(mockBundle).setResult(any(OBError.class));
  }

  private void setupBundleAndExecute(String startTime, String endTime) throws Exception {
    HashMap<String, Object> params = new HashMap<>();
    params.put(MA_WORKREQUIREMENT_ID, NONEXISTENT_ID);
    params.put("date", VAL_15_01_2024);
    params.put(STARTTIME, startTime);
    params.put(ENDTIME, endTime);
    when(mockBundle.getParams()).thenReturn(params);

    lenient().when(mockOBDal.get(any(Class.class), anyString())).thenReturn(null);

    utilityStatic.when(() -> Utility.messageBD(any(ConnectionProvider.class), anyString(), anyString()))
        .thenReturn(ERROR);

    instance.execute(mockBundle);
  }
}
