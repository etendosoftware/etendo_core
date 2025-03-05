package org.openbravo.erpCommon.ad_callouts;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.ServletException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.materialmgmt.cost.LandedCostType;
import org.openbravo.model.materialmgmt.cost.LCDistributionAlgorithm;
import org.openbravo.test.base.OBBaseTest;

/**
 * Test class for {@link SL_LandedCost_Cost_Type}.
 */
public class LandedCostTypeTest extends OBBaseTest {

  private SL_LandedCost_Cost_Type callout;

  @Mock
  private SimpleCallout.CalloutInfo infoMock;
  @Mock
  private LandedCostType landedCostTypeMock;
  @Mock
  private LCDistributionAlgorithm distributionAlgorithmMock;
  @Mock
  private VariablesSecureApp varsMock;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    callout = new SL_LandedCost_Cost_Type();
  }

  /**
   * Test the {@link SL_LandedCost_Cost_Type} method. This method is expected to
   * add the distribution algorithm id to the callout result.
   *
   * @throws ServletException
   *     if the callout fails to execute
   */
  @Test
  public void testSL_LandedCost_Cost_Type() throws ServletException {
    String landedCostTypeId = "1000000";
    String expectedAlgorithmId = "2000000";

    when(infoMock.getStringParameter("inpmLcTypeId", null)).thenReturn(landedCostTypeId);

    infoMock.vars = varsMock;

    doNothing().when(varsMock).setSessionValue(anyString(), any());

    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class)) {
      OBDal mockOBDal = mock(OBDal.class);
      mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

      when(OBDal.getInstance().get(LandedCostType.class, landedCostTypeId)).thenReturn(landedCostTypeMock);
      when(landedCostTypeMock.getLandedCostDistributionAlgorithm()).thenReturn(distributionAlgorithmMock);
      when(distributionAlgorithmMock.getId()).thenReturn(expectedAlgorithmId);

      callout.execute(infoMock);

      verify(infoMock).addResult("inpmLcDistributionAlgId", expectedAlgorithmId);

      verify(varsMock, times(2)).setSessionValue(anyString(), isNull());
    }
  }
}
