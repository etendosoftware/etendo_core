package org.openbravo.client.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.model.ad.alert.Alert;

/**
 * Tests for {@link AlertManagementActionHandler}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class AlertManagementActionHandlerTest {

  private static final String ALERT_ID_1 = "ABC123";
  private static final String ALERT_ID_2 = "DEF456";

  private AlertManagementActionHandler handler;

  @Mock
  private OBDal mockOBDal;

  private MockedStatic<OBContext> obContextStatic;
  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<OBDao> obDaoStatic;

  @Before
  public void setUp() {
    handler = new AlertManagementActionHandler();
    obContextStatic = mockStatic(OBContext.class);
    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);
    obDaoStatic = mockStatic(OBDao.class);
  }

  @After
  public void tearDown() {
    if (obContextStatic != null) obContextStatic.close();
    if (obDalStatic != null) obDalStatic.close();
    if (obDaoStatic != null) obDaoStatic.close();
  }

  private JSONObject invokeExecute(Map<String, Object> parameters, String content) throws Exception {
    Method method = AlertManagementActionHandler.class.getDeclaredMethod("execute", Map.class, String.class);
    method.setAccessible(true);
    return (JSONObject) method.invoke(handler, parameters, content);
  }

  @Test
  public void testMoveToStatusChangesAlertStatus() throws Exception {
    JSONObject input = new JSONObject();
    input.put("eventType", "moveToStatus");
    input.put("alertIDs", ALERT_ID_1);
    input.put("oldStatus", "NEW");
    input.put("newStatus", "acknowledged");

    Alert mockAlert = mock(Alert.class);
    List<Alert> alertList = new ArrayList<>();
    alertList.add(mockAlert);

    obDaoStatic.when(() -> OBDao.getOBObjectListFromString(eq(Alert.class), eq(ALERT_ID_1)))
        .thenReturn(alertList);

    JSONObject result = invokeExecute(new HashMap<>(), input.toString());

    assertNotNull(result);
    assertEquals("NEW", result.getString("oldStatus"));
    assertEquals("acknowledged", result.getString("newStatus"));
    verify(mockAlert).setAlertStatus("ACKNOWLEDGED");
    verify(mockOBDal).save(mockAlert);
    verify(mockOBDal).flush();
  }

  @Test
  public void testMoveToStatusWithMultipleAlerts() throws Exception {
    JSONObject input = new JSONObject();
    input.put("eventType", "moveToStatus");
    input.put("alertIDs", ALERT_ID_1 + "," + ALERT_ID_2);
    input.put("oldStatus", "NEW");
    input.put("newStatus", "solved");

    Alert mockAlert1 = mock(Alert.class);
    Alert mockAlert2 = mock(Alert.class);
    List<Alert> alertList = new ArrayList<>();
    alertList.add(mockAlert1);
    alertList.add(mockAlert2);

    String alertIds = ALERT_ID_1 + "," + ALERT_ID_2;
    obDaoStatic.when(() -> OBDao.getOBObjectListFromString(eq(Alert.class), eq(alertIds)))
        .thenReturn(alertList);

    JSONObject result = invokeExecute(new HashMap<>(), input.toString());

    verify(mockAlert1).setAlertStatus("SOLVED");
    verify(mockAlert2).setAlertStatus("SOLVED");
    verify(mockOBDal, times(2)).save(any(Alert.class));
    verify(mockOBDal).flush();
  }

  @Test
  public void testMoveToStatusWithEmptyAlertIds() throws Exception {
    JSONObject input = new JSONObject();
    input.put("eventType", "moveToStatus");
    input.put("alertIDs", "");
    input.put("oldStatus", "NEW");
    input.put("newStatus", "solved");

    JSONObject result = invokeExecute(new HashMap<>(), input.toString());

    assertNotNull(result);
    verify(mockOBDal, never()).save(any());
    verify(mockOBDal, never()).flush();
  }

  @Test
  public void testUnsupportedEventTypeReturnsEmptyObject() throws Exception {
    JSONObject input = new JSONObject();
    input.put("eventType", "unsupportedEvent");

    JSONObject result = invokeExecute(new HashMap<>(), input.toString());

    assertNotNull(result);
    assertFalse(result.has("oldStatus"));
    assertFalse(result.has("newStatus"));
  }

  @Test
  public void testInvalidJsonReturnsEmptyObject() throws Exception {
    JSONObject result = invokeExecute(new HashMap<>(), "not valid json");

    assertNotNull(result);
    assertFalse(result.has("oldStatus"));
  }
}
