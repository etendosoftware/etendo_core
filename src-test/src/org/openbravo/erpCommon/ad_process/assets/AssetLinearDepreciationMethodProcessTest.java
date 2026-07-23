package org.openbravo.erpCommon.ad_process.assets; // NOSONAR - existing Etendo package naming convention

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.assetmgmt.AmortizationLine;
import org.openbravo.model.financialmgmt.assetmgmt.Asset;

/**
 * Regression tests for ETP-4654 on {@link AssetLinearDepreciationMethodProcess}.
 */
@SuppressWarnings({"java:S120"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class AssetLinearDepreciationMethodProcessTest {

  private AssetLinearDepreciationMethodProcess instance;

  @Mock
  private OBDal mockOBDal;
  @Mock
  private Asset mockAsset;
  @Mock
  private Session mockSession;
  @Mock
  private Query mockQuery;
  @Mock
  private OBCriteria<AmortizationLine> mockAmortizationLineCriteria;

  private MockedStatic<OBDal> obDalStatic;

  /** Sets up test fixtures. */
  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(AssetLinearDepreciationMethodProcess.class);

    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);
  }

  /** Tears down test fixtures. */
  @After
  public void tearDown() {
    if (obDalStatic != null) obDalStatic.close();
  }

  /**
   * Regression test for ETP-4654: DepreciatedPlan must be persisted through a direct bulk UPDATE,
   * never through the entity setter, since Hibernate's dirty-check silently drops the setter when
   * the A_AMORTIZATIONLINE_TRG trigger already wrote the same value out-of-band, causing the
   * field to toggle between 0 and the correct total on every recalculation.
   * @throws Exception if an error occurs
   */

  @Test
  public void testUpdateDepreciatedPlanFromLinesPersistsViaBulkUpdate() throws Exception {
    final BigDecimal expectedTotal = new BigDecimal("1500.00");
    when(mockAsset.getId()).thenReturn("ASSET_001");
    when(mockOBDal.createCriteria(AmortizationLine.class)).thenReturn(mockAmortizationLineCriteria);
    when(mockAmortizationLineCriteria.add(any())).thenReturn(mockAmortizationLineCriteria);
    when(mockAmortizationLineCriteria.uniqueResult()).thenReturn(expectedTotal);
    when(mockOBDal.getSession()).thenReturn(mockSession);
    when(mockSession.createQuery(anyString())).thenReturn(mockQuery);
    when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);

    final Method method = AssetLinearDepreciationMethodProcess.class
        .getDeclaredMethod("updateDepreciatedPlanFromLines", Asset.class);
    method.setAccessible(true);
    method.invoke(instance, mockAsset);

    verify(mockAmortizationLineCriteria).setFilterOnReadableOrganization(false);
    verify(mockQuery).setParameter("plannedAmount", expectedTotal);
    verify(mockQuery).setParameter("assetId", "ASSET_001");
    verify(mockQuery).executeUpdate();
    verify(mockSession).refresh(mockAsset);
    verify(mockAsset, never()).setDepreciatedPlan(any());
  }

  /**
   * Get active amortization lines total zero when no active lines remain.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetActiveAmortizationLinesTotalReturnsZeroWhenNoLines() throws Exception {
    when(mockOBDal.createCriteria(AmortizationLine.class)).thenReturn(mockAmortizationLineCriteria);
    when(mockAmortizationLineCriteria.add(any())).thenReturn(mockAmortizationLineCriteria);
    when(mockAmortizationLineCriteria.uniqueResult()).thenReturn(null);

    final Method method = AssetLinearDepreciationMethodProcess.class
        .getDeclaredMethod("getActiveAmortizationLinesTotal", Asset.class);
    method.setAccessible(true);
    final BigDecimal result = (BigDecimal) method.invoke(instance, mockAsset);

    assertEquals(0, result.compareTo(BigDecimal.ZERO));
  }
}
