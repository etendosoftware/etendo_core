package org.openbravo.erpCommon.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.calendar.Calendar;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.model.financialmgmt.calendar.Year;

/**
 * Tests for {@link AccDefUtility}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AccDefUtilityTest {

  @Mock
  private OBDal obDal;

  @Mock
  private OBCriteria<Period> periodCriteria;

  @Mock
  private OBCriteria<Year> yearCriteria;

  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<OrganizationStructureProvider> ospStatic;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(obDal);
    lenient().when(obDal.createCriteria(Period.class)).thenReturn(periodCriteria);
    lenient().when(obDal.createCriteria(Year.class)).thenReturn((OBCriteria) yearCriteria);

    lenient().when(periodCriteria.createAlias(any(), any())).thenReturn(periodCriteria);
    lenient().when(periodCriteria.add(any())).thenReturn(periodCriteria);
  }

  @After
  public void tearDown() {
    if (obDalStatic != null) {
      obDalStatic.close();
    }
    if (ospStatic != null) {
      ospStatic.close();
    }
  }

  @Test
  public void testGetCalendarWithZeroOrg() {
    Organization org = mock(Organization.class);
    when(org.getId()).thenReturn("0");
    Calendar result = AccDefUtility.getCalendar(org);
    assertNull(result);
  }

  @Test
  public void testGetCalendarReturnsOrgCalendar() {
    Organization org = mock(Organization.class);
    Calendar calendar = mock(Calendar.class);
    when(org.getId()).thenReturn("100");
    when(org.getCalendar()).thenReturn(calendar);
    Calendar result = AccDefUtility.getCalendar(org);
    assertEquals(calendar, result);
  }

  @Test
  public void testGetCurrentPeriodReturnsPeriod() {
    Date date = new Date();
    Calendar calendar = mock(Calendar.class);
    Period period = mock(Period.class);
    List<Period> periods = new ArrayList<>();
    periods.add(period);
    when(periodCriteria.list()).thenReturn(periods);

    Period result = AccDefUtility.getCurrentPeriod(date, calendar);
    assertNotNull(result);
    assertEquals(period, result);
  }

  @Test
  public void testGetCurrentPeriodReturnsNullWhenNoPeriods() {
    Date date = new Date();
    Calendar calendar = mock(Calendar.class);
    when(periodCriteria.list()).thenReturn(Collections.emptyList());

    Period result = AccDefUtility.getCurrentPeriod(date, calendar);
    assertNull(result);
  }

  @Test
  public void testGetDeferredPlanWithEmptyInvoiceId() {
    HashMap<String, String> result = AccDefUtility.getDeferredPlanForInvoiceProduct("", "prod1");
    assertEquals("", result.get("planType"));
    assertEquals("", result.get("periodNumber"));
    assertEquals("", result.get("startingPeriodId"));
  }

  @Test
  public void testGetDeferredPlanWithEmptyProductId() {
    HashMap<String, String> result = AccDefUtility.getDeferredPlanForInvoiceProduct("inv1", "");
    assertEquals("", result.get("planType"));
    assertEquals("", result.get("periodNumber"));
    assertEquals("", result.get("startingPeriodId"));
  }

  @Test
  public void testGetDeferredPlanWithBothEmpty() {
    HashMap<String, String> result = AccDefUtility.getDeferredPlanForInvoiceProduct("", "");
    assertEquals("", result.get("planType"));
    assertEquals("", result.get("periodNumber"));
    assertEquals("", result.get("startingPeriodId"));
  }

  @Test
  public void testGetDeferredPlanResultContainsAllKeys() {
    HashMap<String, String> result = AccDefUtility.getDeferredPlanForInvoiceProduct("", "");
    assertNotNull(result.get("planType"));
    assertNotNull(result.get("periodNumber"));
    assertNotNull(result.get("startingPeriodId"));
  }

  @Test
  public void testGetDeferredPlanNonDeferredProduct() {
    Invoice invoice = mock(Invoice.class);
    Product product = mock(Product.class);
    when(obDal.get(Invoice.class, "inv1")).thenReturn(invoice);
    when(obDal.get(Product.class, "prod1")).thenReturn(product);
    when(invoice.isSalesTransaction()).thenReturn(true);
    when(product.isDeferredRevenue()).thenReturn(false);

    HashMap<String, String> result = AccDefUtility.getDeferredPlanForInvoiceProduct("inv1",
        "prod1");
    assertEquals("", result.get("planType"));
    assertEquals("", result.get("periodNumber"));
  }
}
