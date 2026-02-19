package org.openbravo.client.application.window;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.HashMap;
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
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchema;

@RunWith(MockitoJUnitRunner.Silent.class)
public class GetNameGeneralLedgerTest {

  private GetNameGeneralLedger instance;
  private MockedStatic<OBContext> obContextStatic;
  private MockedStatic<OBDal> obDalStatic;

  @Mock
  private OBDal mockOBDal;

  @Before
  public void setUp() {
    instance = new GetNameGeneralLedger();
    obContextStatic = mockStatic(OBContext.class);
    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);
  }

  @After
  public void tearDown() {
    if (obContextStatic != null) {
      obContextStatic.close();
    }
    if (obDalStatic != null) {
      obDalStatic.close();
    }
  }

  @Test
  public void testExecuteReturnsEmptyJsonForNonGetNameCommand() throws Exception {
    // Arrange
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("Command", "OTHER");

    // Act
    Method executeMethod = GetNameGeneralLedger.class.getDeclaredMethod("execute", Map.class, String.class);
    executeMethod.setAccessible(true);
    JSONObject result = (JSONObject) executeMethod.invoke(instance, parameters, null);

    // Assert
    assertNotNull(result);
    assertFalse(result.has("id"));
    assertFalse(result.has("name"));
  }

  @Test
  public void testExecuteReturnsSchemaDataForGetNameCommand() throws Exception {
    // Arrange
    String glId = "TEST_GL_001";
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("Command", "GETNAME");
    parameters.put("glId", glId);

    AcctSchema mockSchema = mock(AcctSchema.class);
    when(mockOBDal.get(AcctSchema.class, glId)).thenReturn(mockSchema);
    when(mockSchema.getId()).thenReturn(glId);
    when(mockSchema.getName()).thenReturn("Test General Ledger");

    // Act
    Method executeMethod = GetNameGeneralLedger.class.getDeclaredMethod("execute", Map.class, String.class);
    executeMethod.setAccessible(true);
    JSONObject result = (JSONObject) executeMethod.invoke(instance, parameters, null);

    // Assert
    assertNotNull(result);
    assertEquals(glId, result.getString("id"));
    assertEquals("Test General Ledger", result.getString("name"));
  }
}
