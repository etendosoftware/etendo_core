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

@RunWith(MockitoJUnitRunner.class)
public class CreateWorkEffortTest {

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

  @After
  public void tearDown() {
    if (obDalStatic != null) obDalStatic.close();
    if (propsStatic != null) propsStatic.close();
    if (utilityStatic != null) utilityStatic.close();
  }

  @Test
  public void testExecuteWithNullWorkRequirementHandlesError() throws Exception {
    HashMap<String, Object> params = new HashMap<>();
    params.put("MA_Workrequirement_ID", "NONEXISTENT_ID");
    params.put("date", "15-01-2024");
    params.put("starttime", "08:00:00");
    params.put("endtime", "17:00:00");
    when(mockBundle.getParams()).thenReturn(params);

    lenient().when(mockOBDal.get(any(Class.class), anyString())).thenReturn(null);

    utilityStatic.when(() -> Utility.messageBD(any(ConnectionProvider.class), anyString(), anyString()))
        .thenReturn("Error");

    instance.execute(mockBundle);

    verify(mockBundle).setResult(any(OBError.class));
  }

  @Test
  public void testExecuteWithNullStartEndTimeDefaults() throws Exception {
    HashMap<String, Object> params = new HashMap<>();
    params.put("MA_Workrequirement_ID", "NONEXISTENT_ID");
    params.put("date", "15-01-2024");
    params.put("starttime", null);
    params.put("endtime", null);
    when(mockBundle.getParams()).thenReturn(params);

    lenient().when(mockOBDal.get(any(Class.class), anyString())).thenReturn(null);

    utilityStatic.when(() -> Utility.messageBD(any(ConnectionProvider.class), anyString(), anyString()))
        .thenReturn("Error");

    instance.execute(mockBundle);

    verify(mockBundle).setResult(any(OBError.class));
  }

  @Test
  public void testExecuteWithEmptyStartEndTimeDefaults() throws Exception {
    HashMap<String, Object> params = new HashMap<>();
    params.put("MA_Workrequirement_ID", "NONEXISTENT_ID");
    params.put("date", "15-01-2024");
    params.put("starttime", "");
    params.put("endtime", "");
    when(mockBundle.getParams()).thenReturn(params);

    lenient().when(mockOBDal.get(any(Class.class), anyString())).thenReturn(null);

    utilityStatic.when(() -> Utility.messageBD(any(ConnectionProvider.class), anyString(), anyString()))
        .thenReturn("Error");

    instance.execute(mockBundle);

    verify(mockBundle).setResult(any(OBError.class));
  }
}
