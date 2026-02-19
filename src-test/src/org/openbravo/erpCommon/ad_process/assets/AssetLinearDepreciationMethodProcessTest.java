package org.openbravo.erpCommon.ad_process.assets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.assetmgmt.Amortization;
import org.openbravo.model.financialmgmt.assetmgmt.AmortizationLine;
import org.openbravo.model.financialmgmt.assetmgmt.Asset;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DbUtility;
/** Tests for {@link AssetLinearDepreciationMethodProcess}. */
@SuppressWarnings({"java:S120"})

@RunWith(MockitoJUnitRunner.Silent.class)
public class AssetLinearDepreciationMethodProcessTest {

  private static final String ERROR = "Error";
  private static final String VAL_10000 = "10000";


  private AssetLinearDepreciationMethodProcess instance;

  @Mock
  private OBDal mockOBDal;
  @Mock
  private OBContext mockOBContext;
  @Mock
  private OBProvider mockOBProvider;
  @Mock
  private Asset mockAsset;
  @Mock
  private Currency mockCurrency;
  @Mock
  private Organization mockOrg;
  @Mock
  private ProcessBundle mockBundle;

  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<OBContext> obContextStatic;
  private MockedStatic<OBMessageUtils> obMessageUtilsStatic;
  private MockedStatic<OBProvider> obProviderStatic;
  private MockedStatic<DbUtility> dbUtilityStatic;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(AssetLinearDepreciationMethodProcess.class);

    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

    obContextStatic = mockStatic(OBContext.class);
    lenient().when(OBContext.getOBContext()).thenReturn(mockOBContext);

    obMessageUtilsStatic = mockStatic(OBMessageUtils.class);
    lenient().when(OBMessageUtils.messageBD(anyString())).thenAnswer(inv -> inv.getArgument(0));

    obProviderStatic = mockStatic(OBProvider.class);
    lenient().when(OBProvider.getInstance()).thenReturn(mockOBProvider);

    dbUtilityStatic = mockStatic(DbUtility.class);
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (obDalStatic != null) obDalStatic.close();
    if (obContextStatic != null) obContextStatic.close();
    if (obMessageUtilsStatic != null) obMessageUtilsStatic.close();
    if (obProviderStatic != null) obProviderStatic.close();
    if (dbUtilityStatic != null) dbUtilityStatic.close();
  }
  /**
   * Generate amortization plan null start date.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGenerateAmortizationPlanNullStartDate() throws Exception {
    when(mockAsset.getDepreciationStartDate()).thenReturn(null);

    OBError result = instance.generateAmortizationPlan(mockAsset);

    assertEquals(ERROR, result.getType());
  }
  /**
   * Generate amortization plan zero amount.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGenerateAmortizationPlanZeroAmount() throws Exception {
    Calendar cal = Calendar.getInstance();
    cal.set(2024, Calendar.JANUARY, 1);
    when(mockAsset.getDepreciationStartDate()).thenReturn(cal.getTime());
    when(mockAsset.getDepreciationType()).thenReturn("LI");
    when(mockAsset.getCalculateType()).thenReturn("TI");
    when(mockAsset.getAmortize()).thenReturn("MO");
    when(mockAsset.getUsableLifeMonths()).thenReturn(12L);
    when(mockAsset.isEveryMonthIs30Days()).thenReturn(false);
    when(mockAsset.getUsableLifeYears()).thenReturn(1L);
    when(mockAsset.getAnnualDepreciation()).thenReturn(BigDecimal.ZERO);
    when(mockAsset.getDepreciationAmt()).thenReturn(BigDecimal.ZERO);
    when(mockAsset.getPreviouslyDepreciatedAmt()).thenReturn(BigDecimal.ZERO);
    when(mockAsset.getFinancialMgmtAmortizationLineList()).thenReturn(new ArrayList<>());

    OBError result = instance.generateAmortizationPlan(mockAsset);

    assertEquals(ERROR, result.getType());
  }
  /**
   * Generate amortization plan negative amount.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGenerateAmortizationPlanNegativeAmount() throws Exception {
    Calendar cal = Calendar.getInstance();
    cal.set(2024, Calendar.JANUARY, 1);
    when(mockAsset.getDepreciationStartDate()).thenReturn(cal.getTime());
    when(mockAsset.getDepreciationType()).thenReturn("LI");
    when(mockAsset.getCalculateType()).thenReturn("TI");
    when(mockAsset.getAmortize()).thenReturn("MO");
    when(mockAsset.getUsableLifeMonths()).thenReturn(12L);
    when(mockAsset.isEveryMonthIs30Days()).thenReturn(false);
    when(mockAsset.getUsableLifeYears()).thenReturn(1L);
    when(mockAsset.getAnnualDepreciation()).thenReturn(BigDecimal.ZERO);
    when(mockAsset.getDepreciationAmt()).thenReturn(new BigDecimal("-100"));
    when(mockAsset.getPreviouslyDepreciatedAmt()).thenReturn(BigDecimal.ZERO);
    when(mockAsset.getFinancialMgmtAmortizationLineList()).thenReturn(new ArrayList<>());

    OBError result = instance.generateAmortizationPlan(mockAsset);

    assertEquals(ERROR, result.getType());
  }
  /**
   * Generate amortization plan percentage zero annual depreciation.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGenerateAmortizationPlanPercentageZeroAnnualDepreciation() throws Exception {
    Calendar cal = Calendar.getInstance();
    cal.set(2024, Calendar.JANUARY, 1);
    when(mockAsset.getDepreciationStartDate()).thenReturn(cal.getTime());
    when(mockAsset.getDepreciationType()).thenReturn("LI");
    when(mockAsset.getCalculateType()).thenReturn("PE");
    when(mockAsset.getAmortize()).thenReturn("YE");
    when(mockAsset.getUsableLifeMonths()).thenReturn(null);
    when(mockAsset.isEveryMonthIs30Days()).thenReturn(false);
    when(mockAsset.getUsableLifeYears()).thenReturn(null);
    when(mockAsset.getAnnualDepreciation()).thenReturn(BigDecimal.ZERO);
    when(mockAsset.getDepreciationAmt()).thenReturn(new BigDecimal(VAL_10000));
    when(mockAsset.getPreviouslyDepreciatedAmt()).thenReturn(BigDecimal.ZERO);
    when(mockAsset.getFinancialMgmtAmortizationLineList()).thenReturn(new ArrayList<>());

    OBError result = instance.generateAmortizationPlan(mockAsset);

    assertEquals(ERROR, result.getType());
  }
  /**
   * Generate amortization plan monthly zero usable life months.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGenerateAmortizationPlanMonthlyZeroUsableLifeMonths() throws Exception {
    Calendar cal = Calendar.getInstance();
    cal.set(2024, Calendar.JANUARY, 1);
    when(mockAsset.getDepreciationStartDate()).thenReturn(cal.getTime());
    when(mockAsset.getDepreciationType()).thenReturn("LI");
    when(mockAsset.getCalculateType()).thenReturn("TI");
    when(mockAsset.getAmortize()).thenReturn("MO");
    when(mockAsset.getUsableLifeMonths()).thenReturn(0L);
    when(mockAsset.isEveryMonthIs30Days()).thenReturn(false);
    when(mockAsset.getUsableLifeYears()).thenReturn(null);
    when(mockAsset.getAnnualDepreciation()).thenReturn(BigDecimal.ZERO);
    when(mockAsset.getDepreciationAmt()).thenReturn(new BigDecimal(VAL_10000));
    when(mockAsset.getPreviouslyDepreciatedAmt()).thenReturn(BigDecimal.ZERO);
    when(mockAsset.getFinancialMgmtAmortizationLineList()).thenReturn(new ArrayList<>());

    OBError result = instance.generateAmortizationPlan(mockAsset);

    assertEquals(ERROR, result.getType());
  }
  /**
   * Generate amortization plan yearly zero usable life years.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGenerateAmortizationPlanYearlyZeroUsableLifeYears() throws Exception {
    Calendar cal = Calendar.getInstance();
    cal.set(2024, Calendar.JANUARY, 1);
    when(mockAsset.getDepreciationStartDate()).thenReturn(cal.getTime());
    when(mockAsset.getDepreciationType()).thenReturn("LI");
    when(mockAsset.getCalculateType()).thenReturn("TI");
    when(mockAsset.getAmortize()).thenReturn("YE");
    when(mockAsset.getUsableLifeMonths()).thenReturn(null);
    when(mockAsset.isEveryMonthIs30Days()).thenReturn(false);
    when(mockAsset.getUsableLifeYears()).thenReturn(0L);
    when(mockAsset.getAnnualDepreciation()).thenReturn(BigDecimal.ZERO);
    when(mockAsset.getDepreciationAmt()).thenReturn(new BigDecimal(VAL_10000));
    when(mockAsset.getPreviouslyDepreciatedAmt()).thenReturn(BigDecimal.ZERO);
    when(mockAsset.getFinancialMgmtAmortizationLineList()).thenReturn(new ArrayList<>());

    OBError result = instance.generateAmortizationPlan(mockAsset);

    assertEquals(ERROR, result.getType());
  }
  /**
   * Generate amortization plan null currency.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGenerateAmortizationPlanNullCurrency() throws Exception {
    Calendar cal = Calendar.getInstance();
    cal.set(2024, Calendar.JANUARY, 1);
    when(mockAsset.getDepreciationStartDate()).thenReturn(cal.getTime());
    when(mockAsset.getDepreciationType()).thenReturn("LI");
    when(mockAsset.getCalculateType()).thenReturn("PE");
    when(mockAsset.getAmortize()).thenReturn("YE");
    when(mockAsset.getUsableLifeMonths()).thenReturn(null);
    when(mockAsset.isEveryMonthIs30Days()).thenReturn(false);
    when(mockAsset.getUsableLifeYears()).thenReturn(null);
    when(mockAsset.getAnnualDepreciation()).thenReturn(new BigDecimal("10"));
    when(mockAsset.getDepreciationAmt()).thenReturn(new BigDecimal(VAL_10000));
    when(mockAsset.getPreviouslyDepreciatedAmt()).thenReturn(BigDecimal.ZERO);
    when(mockAsset.getFinancialMgmtAmortizationLineList()).thenReturn(new ArrayList<>());
    when(mockAsset.getCurrency()).thenReturn(null);

    OBError result = instance.generateAmortizationPlan(mockAsset);

    assertEquals(ERROR, result.getType());
  }
  /**
   * Generate amortization plan fully depreciated.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGenerateAmortizationPlanFullyDepreciated() throws Exception {
    Calendar cal = Calendar.getInstance();
    cal.set(2024, Calendar.JANUARY, 1);
    when(mockAsset.getDepreciationStartDate()).thenReturn(cal.getTime());
    when(mockAsset.getDepreciationType()).thenReturn("LI");
    when(mockAsset.getCalculateType()).thenReturn("TI");
    when(mockAsset.getAmortize()).thenReturn("MO");
    when(mockAsset.getUsableLifeMonths()).thenReturn(12L);
    when(mockAsset.isEveryMonthIs30Days()).thenReturn(false);
    when(mockAsset.getUsableLifeYears()).thenReturn(1L);
    when(mockAsset.getAnnualDepreciation()).thenReturn(BigDecimal.ZERO);
    when(mockAsset.getDepreciationAmt()).thenReturn(new BigDecimal(VAL_10000));
    when(mockAsset.getPreviouslyDepreciatedAmt()).thenReturn(BigDecimal.ZERO);

    // Create amortization lines that sum up to full amount
    List<AmortizationLine> lines = new ArrayList<>();
    AmortizationLine mockLine = mock(AmortizationLine.class);
    when(mockLine.getAmortizationAmount()).thenReturn(new BigDecimal(VAL_10000));
    Amortization mockAmortization = mock(Amortization.class);
    when(mockAmortization.getProcessed()).thenReturn("Y");
    when(mockAmortization.getId()).thenReturn("AMORT_001");
    when(mockLine.getAmortization()).thenReturn(mockAmortization);
    when(mockOBDal.get(eq(Amortization.class), anyString())).thenReturn(mockAmortization);
    Calendar endCal = Calendar.getInstance();
    endCal.set(2024, Calendar.DECEMBER, 31);
    when(mockLine.getAmortization().getEndingDate()).thenReturn(endCal.getTime());
    lines.add(mockLine);
    when(mockAsset.getFinancialMgmtAmortizationLineList()).thenReturn(lines);

    OBError result = instance.generateAmortizationPlan(mockAsset);

    assertEquals("Warning", result.getType());
  }
  /**
   * Generate amortization plan unsupported calculate type.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGenerateAmortizationPlanUnsupportedCalculateType() throws Exception {
    Calendar cal = Calendar.getInstance();
    cal.set(2024, Calendar.JANUARY, 1);
    when(mockAsset.getDepreciationStartDate()).thenReturn(cal.getTime());
    when(mockAsset.getDepreciationType()).thenReturn("LI");
    when(mockAsset.getCalculateType()).thenReturn("INVALID");
    when(mockAsset.getAmortize()).thenReturn("MO");
    when(mockAsset.getUsableLifeMonths()).thenReturn(12L);
    when(mockAsset.isEveryMonthIs30Days()).thenReturn(false);
    when(mockAsset.getUsableLifeYears()).thenReturn(1L);
    when(mockAsset.getAnnualDepreciation()).thenReturn(BigDecimal.ZERO);
    when(mockAsset.getDepreciationAmt()).thenReturn(new BigDecimal(VAL_10000));
    when(mockAsset.getPreviouslyDepreciatedAmt()).thenReturn(BigDecimal.ZERO);
    when(mockAsset.getFinancialMgmtAmortizationLineList()).thenReturn(new ArrayList<>());

    OBError result = instance.generateAmortizationPlan(mockAsset);

    assertEquals(ERROR, result.getType());
  }
  /**
   * Generate amortization plan unsupported frequency.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGenerateAmortizationPlanUnsupportedFrequency() throws Exception {
    Calendar cal = Calendar.getInstance();
    cal.set(2024, Calendar.JANUARY, 1);
    when(mockAsset.getDepreciationStartDate()).thenReturn(cal.getTime());
    when(mockAsset.getDepreciationType()).thenReturn("LI");
    when(mockAsset.getCalculateType()).thenReturn("TI");
    when(mockAsset.getAmortize()).thenReturn("INVALID");
    when(mockAsset.getUsableLifeMonths()).thenReturn(12L);
    when(mockAsset.isEveryMonthIs30Days()).thenReturn(false);
    when(mockAsset.getUsableLifeYears()).thenReturn(1L);
    when(mockAsset.getAnnualDepreciation()).thenReturn(BigDecimal.ZERO);
    when(mockAsset.getDepreciationAmt()).thenReturn(new BigDecimal(VAL_10000));
    when(mockAsset.getPreviouslyDepreciatedAmt()).thenReturn(BigDecimal.ZERO);
    when(mockAsset.getFinancialMgmtAmortizationLineList()).thenReturn(new ArrayList<>());

    OBError result = instance.generateAmortizationPlan(mockAsset);

    assertEquals(ERROR, result.getType());
  }
  /**
   * Get days between proportional periods monthly.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetDaysBetweenProportionalPeriodsMonthly() throws Exception {
    Calendar start = Calendar.getInstance();
    start.set(2024, Calendar.JANUARY, 1, 0, 0, 0);
    start.set(Calendar.MILLISECOND, 0);

    Calendar end = Calendar.getInstance();
    end.set(2024, Calendar.APRIL, 1, 0, 0, 0);
    end.set(Calendar.MILLISECOND, 0);

    Method method = AssetLinearDepreciationMethodProcess.class.getDeclaredMethod(
        "getDaysBetweenProportionalPeriods", Calendar.class, Calendar.class, boolean.class,
        boolean.class);
    method.setAccessible(true);

    BigDecimal result = (BigDecimal) method.invoke(instance, start, end, false, true);

    assertNotNull(result);
    // 3 months of 30 days = 90 proportional days
    assertEquals(0, result.compareTo(new BigDecimal("90")));
  }
  /**
   * Get days between proportional periods yearly.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetDaysBetweenProportionalPeriodsYearly() throws Exception {
    Calendar start = Calendar.getInstance();
    start.set(2024, Calendar.JANUARY, 1, 0, 0, 0);
    start.set(Calendar.MILLISECOND, 0);

    Calendar end = Calendar.getInstance();
    end.set(2025, Calendar.JANUARY, 1, 0, 0, 0);
    end.set(Calendar.MILLISECOND, 0);

    Method method = AssetLinearDepreciationMethodProcess.class.getDeclaredMethod(
        "getDaysBetweenProportionalPeriods", Calendar.class, Calendar.class, boolean.class,
        boolean.class);
    method.setAccessible(true);

    BigDecimal result = (BigDecimal) method.invoke(instance, start, end, true, false);

    assertNotNull(result);
    // 1 year of 365 days
    assertEquals(0, result.compareTo(new BigDecimal("365")));
  }

}
