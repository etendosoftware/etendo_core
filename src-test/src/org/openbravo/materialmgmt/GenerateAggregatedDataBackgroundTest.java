package org.openbravo.materialmgmt;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

/**
 * Test class for {@link GenerateAggregatedDataBackground}.
 * <p>
 * This class includes unit tests for the behavior of the
 * GenerateAggregatedDataBackground process in different scenarios.
 */
@RunWith(MockitoJUnitRunner.class)
public class GenerateAggregatedDataBackgroundTest {

  private static final String TEST_ORG_ID = "TestOrgId";
  private static final String SUCCESS = "Success";
  private static final String ERROR = "Error";
  private static final String NO_LEGAL_ENTITY_FOUND = "No Legal Entity Found";
  private static final String DO_EXECUTE = "doExecute";

  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;
  private MockedStatic<ResetValuedStockAggregated> mockedResetValuedStockAggregated;

  @Mock
  private ProcessBundle mockBundle;

  @Mock
  private ProcessLogger mockLogger;

  @Mock
  private OBContext mockOBContext;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private Client mockClient;

  @Mock
  private Organization mockOrganization;

  @Mock
  private OrganizationStructureProvider mockOSP;


  private GenerateAggregatedDataBackground processUnderTest;

  /**
   * Initializes mocks and static classes before each test.
   */
  @Before
  public void setUp() {
    processUnderTest = new GenerateAggregatedDataBackground();

    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
    if (mockedOBMessageUtils != null) {
      mockedOBMessageUtils.close();
    }
    if (mockedResetValuedStockAggregated != null) {
      mockedResetValuedStockAggregated.close();
    }

    mockedOBDal = mockStatic(OBDal.class);
    mockedOBContext = mockStatic(OBContext.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);
    mockedResetValuedStockAggregated = mockStatic(ResetValuedStockAggregated.class);

    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
    mockedOBContext.when(OBContext::getOBContext).thenReturn(mockOBContext);

    when(mockBundle.getLogger()).thenReturn(mockLogger);

    when(mockOBContext.getCurrentOrganization()).thenReturn(mockOrganization);
    when(mockOBContext.getCurrentClient()).thenReturn(mockClient);
    when(mockOBContext.getOrganizationStructureProvider(anyString())).thenReturn(mockOSP);

    when(mockClient.getId()).thenReturn("TestClientId");
    when(mockOrganization.getId()).thenReturn(TEST_ORG_ID);

    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD(SUCCESS)).thenReturn(SUCCESS);
    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD(ERROR)).thenReturn(ERROR);
    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("NoLegalEntityFound")).thenReturn(NO_LEGAL_ENTITY_FOUND);

    mockedResetValuedStockAggregated.when(
        () -> ResetValuedStockAggregated.insertValuesIntoValuedStockAggregated(any(Organization.class),
            any(Period.class), any(Date.class))).thenAnswer(invocation -> null);
  }

  /**
   * Closes mocked static instances after each test.
   */
  @After
  public void tearDown() {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
    if (mockedOBMessageUtils != null) {
      mockedOBMessageUtils.close();
    }
    if (mockedResetValuedStockAggregated != null) {
      mockedResetValuedStockAggregated.close();
    }
  }

  /**
   * Tests the scenario where no legal entity is found.
   * <p>
   * Verifies that the process logs an error and does not attempt to insert aggregated data.
   *
   * @throws Exception
   *     if there is an error invoking the method using reflection.
   */
  @Test
  public void testDoExecuteNoLegalEntityFound() throws Exception {
    // GIVEN
    when(mockOSP.getLegalEntity(mockOrganization)).thenReturn(null);
    when(mockOSP.getChildLegalEntitesList(mockOrganization)).thenReturn(new ArrayList<>());
    when(mockOrganization.getId()).thenReturn(TEST_ORG_ID); // No es "0"

    // WHEN
    Method doExecuteMethod = DalBaseProcess.class.getDeclaredMethod(DO_EXECUTE, ProcessBundle.class);
    doExecuteMethod.setAccessible(true);
    doExecuteMethod.invoke(processUnderTest, mockBundle);

    // THEN
    verify(mockBundle).setResult(argThat(result -> result instanceof OBError && StringUtils.equals(ERROR,
        ((OBError) result).getType()) && StringUtils.equals(NO_LEGAL_ENTITY_FOUND, ((OBError) result).getMessage())));

    verify(mockLogger).logln(NO_LEGAL_ENTITY_FOUND);

    mockedResetValuedStockAggregated.verify(
        () -> ResetValuedStockAggregated.insertValuesIntoValuedStockAggregated(any(Organization.class),
            any(Period.class), any(Date.class)), never());
  }

  /**
   * Tests the scenario where the organization is the "*" (root) organization.
   * <p>
   * Verifies that the process inserts aggregated data for all legal entities.
   *
   * @throws Exception
   *     if there is an error invoking the method using reflection.
   */
  @Test
  public void testDoExecuteWithStarOrganization() throws Exception {
    // GIVEN
    when(mockOSP.getLegalEntity(mockOrganization)).thenReturn(null);
    when(mockOrganization.getId()).thenReturn("0");

    List<Organization> legalEntities = new ArrayList<>();
    Organization mockLegalEntity1 = mock(Organization.class);
    when(mockLegalEntity1.getId()).thenReturn("LegalEntity1");
    when(mockLegalEntity1.getClient()).thenReturn(mockClient);
    legalEntities.add(mockLegalEntity1);

    when(mockOSP.getLegalEntitiesListForSelectedClient(anyString())).thenReturn(legalEntities);

    List<Period> periodList = new ArrayList<>();
    Period mockPeriod = mock(Period.class);
    when(mockPeriod.getEndingDate()).thenReturn(new Date());
    periodList.add(mockPeriod);

    mockedResetValuedStockAggregated.when(
        () -> ResetValuedStockAggregated.getClosedPeriodsToAggregate(any(Date.class), anyString(),
            anyString())).thenReturn(periodList);

    mockedResetValuedStockAggregated.when(
        () -> ResetValuedStockAggregated.noAggregatedDataForPeriod(any(Period.class))).thenReturn(true);

    mockedResetValuedStockAggregated.when(
        () -> ResetValuedStockAggregated.costingRuleDefindedForPeriod(any(Organization.class),
            any(Period.class))).thenReturn(true);

    // WHEN
    Method doExecuteMethod = DalBaseProcess.class.getDeclaredMethod(DO_EXECUTE, ProcessBundle.class);
    doExecuteMethod.setAccessible(true);
    doExecuteMethod.invoke(processUnderTest, mockBundle);

    // THEN
    verify(mockBundle).setResult(
        argThat(result -> result instanceof OBError && StringUtils.equals(SUCCESS, ((OBError) result).getType())));

    mockedResetValuedStockAggregated.verify(
        () -> ResetValuedStockAggregated.insertValuesIntoValuedStockAggregated(eq(mockLegalEntity1), eq(mockPeriod),
            any(Date.class)));
  }

  /**
   * Tests the scenario where multiple child legal entities exist.
   * <p>
   * Verifies that the process inserts aggregated data for all child legal entities.
   *
   * @throws Exception
   *     if there is an error invoking the method using reflection.
   */
  @Test
  public void testDoExecuteMultipleChildLegalEntities() throws Exception {
    // GIVEN
    when(mockOSP.getLegalEntity(mockOrganization)).thenReturn(null);
    when(mockOrganization.getId()).thenReturn(TEST_ORG_ID);

    List<Organization> childLegalEntities = new ArrayList<>();
    Organization mockLegalEntity1 = mock(Organization.class);
    Organization mockLegalEntity2 = mock(Organization.class);
    when(mockLegalEntity1.getId()).thenReturn("ChildLegalEntity1");
    when(mockLegalEntity2.getId()).thenReturn("ChildLegalEntity2");
    when(mockLegalEntity1.getClient()).thenReturn(mockClient);
    when(mockLegalEntity2.getClient()).thenReturn(mockClient);
    childLegalEntities.add(mockLegalEntity1);
    childLegalEntities.add(mockLegalEntity2);

    when(mockOSP.getChildLegalEntitesList(mockOrganization)).thenReturn(childLegalEntities);

    List<Period> periodList = new ArrayList<>();
    Period mockPeriod = mock(Period.class);
    when(mockPeriod.getEndingDate()).thenReturn(new Date());
    periodList.add(mockPeriod);

    mockedResetValuedStockAggregated.when(
        () -> ResetValuedStockAggregated.getClosedPeriodsToAggregate(any(Date.class), anyString(),
            anyString())).thenReturn(periodList);

    mockedResetValuedStockAggregated.when(
        () -> ResetValuedStockAggregated.noAggregatedDataForPeriod(any(Period.class))).thenReturn(true);

    mockedResetValuedStockAggregated.when(
        () -> ResetValuedStockAggregated.costingRuleDefindedForPeriod(any(Organization.class),
            any(Period.class))).thenReturn(true);

    // WHEN
    Method doExecuteMethod = DalBaseProcess.class.getDeclaredMethod(DO_EXECUTE, ProcessBundle.class);
    doExecuteMethod.setAccessible(true);
    doExecuteMethod.invoke(processUnderTest, mockBundle);

    // THEN
    verify(mockBundle).setResult(
        argThat(result -> result instanceof OBError && StringUtils.equals(SUCCESS, ((OBError) result).getType())));

    mockedResetValuedStockAggregated.verify(
        () -> ResetValuedStockAggregated.insertValuesIntoValuedStockAggregated(eq(mockLegalEntity1), eq(mockPeriod),
            any(Date.class)));

    mockedResetValuedStockAggregated.verify(
        () -> ResetValuedStockAggregated.insertValuesIntoValuedStockAggregated(eq(mockLegalEntity2), eq(mockPeriod),
            any(Date.class)));
  }
}
