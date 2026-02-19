package org.openbravo.erpCommon.info;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Tests for {@link BusinessPartnerSelectorFilterExpression}.
 */
@RunWith(MockitoJUnitRunner.class)
public class BusinessPartnerSelectorFilterExpressionTest {

  private static final String TEST_ORG_ID = "1000001";
  private static final String TEST_CLIENT_ID = "1000000";

  private BusinessPartnerSelectorFilterExpression filterExpression;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private Organization mockOrganization;

  private MockedStatic<OBDal> obDalStatic;

  @Before
  public void setUp() {
    filterExpression = new BusinessPartnerSelectorFilterExpression();
    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);
    lenient().when(mockOBDal.getProxy(Organization.class, TEST_ORG_ID)).thenReturn(mockOrganization);
    lenient().when(mockOrganization.getId()).thenReturn(TEST_ORG_ID);
  }

  @After
  public void tearDown() {
    if (obDalStatic != null) {
      obDalStatic.close();
    }
  }

  @Test
  public void testGetExpressionReturnsFilterForCOrder() {
    Map<String, String> requestMap = createRequestMap("259");

    String result = filterExpression.getExpression(requestMap);

    assertFilterContainsOrgAndClient(result);
  }

  @Test
  public void testGetExpressionReturnsFilterForCOrderLine() {
    Map<String, String> requestMap = createRequestMap("260");

    String result = filterExpression.getExpression(requestMap);

    assertFilterContainsOrgAndClient(result);
  }

  @Test
  public void testGetExpressionReturnsFilterForMInOut() {
    Map<String, String> requestMap = createRequestMap("319");

    String result = filterExpression.getExpression(requestMap);

    assertFilterContainsOrgAndClient(result);
  }

  @Test
  public void testGetExpressionReturnsFilterForMInOutLine() {
    Map<String, String> requestMap = createRequestMap("320");

    String result = filterExpression.getExpression(requestMap);

    assertFilterContainsOrgAndClient(result);
  }

  @Test
  public void testGetExpressionReturnsFilterForCInvoice() {
    Map<String, String> requestMap = createRequestMap("318");

    String result = filterExpression.getExpression(requestMap);

    assertFilterContainsOrgAndClient(result);
  }

  @Test
  public void testGetExpressionReturnsFilterForCInvoiceLine() {
    Map<String, String> requestMap = createRequestMap("333");

    String result = filterExpression.getExpression(requestMap);

    assertFilterContainsOrgAndClient(result);
  }

  @Test
  public void testGetExpressionReturnsEmptyForUnknownTable() {
    Map<String, String> requestMap = createRequestMap("999");

    String result = filterExpression.getExpression(requestMap);

    assertEquals("", result);
  }

  @Test
  public void testGetExpressionReturnsEmptyWhenNoTableId() {
    Map<String, String> requestMap = new HashMap<>();

    String result = filterExpression.getExpression(requestMap);

    assertEquals("", result);
  }

  @Test
  public void testGetExpressionUsesDefaultOrgWhenMissing() {
    when(mockOBDal.getProxy(Organization.class, "0")).thenReturn(mockOrganization);
    when(mockOrganization.getId()).thenReturn("0");

    Map<String, String> requestMap = new HashMap<>();
    requestMap.put("inpTableId", "259");

    String result = filterExpression.getExpression(requestMap);

    assertTrue(result.contains("ad_isorgincluded"));
    assertTrue(result.contains("<> -1"));
  }

  private Map<String, String> createRequestMap(String tableId) {
    Map<String, String> requestMap = new HashMap<>();
    requestMap.put("inpTableId", tableId);
    requestMap.put("inpadOrgId", TEST_ORG_ID);
    requestMap.put("inpadClientId", TEST_CLIENT_ID);
    return requestMap;
  }

  private void assertFilterContainsOrgAndClient(String result) {
    assertTrue("Result should contain ad_isorgincluded", result.contains("ad_isorgincluded"));
    assertTrue("Result should contain org ID", result.contains(TEST_ORG_ID));
    assertTrue("Result should contain client ID", result.contains(TEST_CLIENT_ID));
    assertTrue("Result should contain <> -1", result.contains("<> -1"));
  }
}
