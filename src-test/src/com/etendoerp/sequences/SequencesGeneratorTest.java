package com.etendoerp.sequences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;

import com.etendoerp.sequences.transactional.TransactionalSequenceUtils;
import com.smf.jobs.ActionResult;
import com.smf.jobs.Result;

/**
 * Test class for SequencesGenerator.
 * <p>
 * This class tests the functionality of the SequencesGenerator class, which is responsible for
 * generating sequence combinations for different client/organization combinations based on columns
 * and document types. The tests cover the different scenarios and methods involved in sequence
 * generation, including checking for existing sequences, creating new ones, and handling
 * organization hierarchies.
 */
@RunWith(MockitoJUnitRunner.class)
public class SequencesGeneratorTest {

  private static final String TEST_ORG_ID = "testOrgId";
  private static final String TEST_SEQUENCE = "TestSequence";

  @Mock
  private Client mockClient;

  @Mock
  private OBContext mockOBContext;

  @Mock
  private Organization mockOrganization;

  @Mock
  private Reference mockReference;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private OBCriteria<Column> mockColumnCriteria;

  @Mock
  private OBCriteria<DocumentType> mockDocTypeCriteria;

  @Mock
  private OBCriteria<Sequence> mockSequenceCriteria;

  @Mock
  private Column mockColumn;

  @Mock
  private Table mockTable;

  @Mock
  private Entity mockEntity;

  @Mock
  private DocumentType mockDocumentType;

  @Mock
  private Sequence mockSequence;

  private SequencesGenerator sequencesGenerator;
  private JSONObject parameters;
  private Set<String> organizations;
  private List<Column> sequenceColumns;
  private List<DocumentType> documentTypes;

  /**
   * Sets up the test environment before each test execution.
   * Initialize mocks, test data, and common mock behaviors.
   *
   * @throws Exception
   *     if any error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    // Initialize Mockito annotations
    MockitoAnnotations.openMocks(this);

    // Initialize the class to test
    sequencesGenerator = new SequencesGenerator();

    // Setup common test data
    parameters = new JSONObject();
    parameters.put("ad_org_id", TEST_ORG_ID);

    organizations = new HashSet<>();
    organizations.add(TEST_ORG_ID);

    // Setup column list
    sequenceColumns = new ArrayList<>();
    sequenceColumns.add(mockColumn);

    // Setup document types list
    documentTypes = new ArrayList<>();
    documentTypes.add(mockDocumentType);

    // Configure common mock behaviors
    when(mockColumn.getTable()).thenReturn(mockTable);
    when(mockTable.getName()).thenReturn("TestTable");
    when(mockColumn.getName()).thenReturn("TestColumn");
  }

  /**
   * Tests the generateSequenceCombination method with a single organization.
   * This test verifies that the method correctly processes sequence columns and
   * creates sequences when they don't exist.
   *
   * @throws Exception
   *     if any error occurs during test execution
   */
  @Test
  public void testGenerateSequenceCombination() throws Exception {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(
        OBDal.class); MockedConstruction<OrganizationStructureProvider> ignored = mockConstruction(
        OrganizationStructureProvider.class, (mock, context) -> {
          Set<String> parentOrgs = new HashSet<>();
          parentOrgs.add("parentOrgId");
          when(mock.getParentTree(anyString(), anyBoolean())).thenReturn(parentOrgs);
        })) {

      // GIVEN
      mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

      setupColumnCriteriaMocks();

      when(mockOBDal.get(Reference.class, TransactionalSequenceUtils.TRANSACTIONAL_SEQUENCE_ID)).thenReturn(
          mockReference);

      setupDocTypeCriteriaMocks();

      when(mockOBDal.get(Organization.class, TEST_ORG_ID)).thenReturn(mockOrganization);

      SequencesGenerator spyGenerator = spy(sequencesGenerator);
      doReturn(true).when(spyGenerator).hasDocType(mockTable);
      doReturn(1).when(spyGenerator).createSequence(eq(mockClient), eq(mockOrganization), anyString(), eq(mockColumn),
          eq(mockDocumentType));

      // WHEN
      int count = spyGenerator.generateSequenceCombination(mockClient, organizations, parameters);

      // THEN
      assertEquals(1, count);
      verify(spyGenerator).hasDocType(mockTable);
      verify(spyGenerator).createSequence(eq(mockClient), eq(mockOrganization), anyString(), eq(mockColumn),
          eq(mockDocumentType));
    }
  }

  /**
   * Tests the hasDocType method in different scenarios.
   * Verifies that the method correctly identifies tables that have document type properties.
   */
  @Test
  public void testHasDocType() {
    try (var mockedModelProvider = mockStatic(ModelProvider.class)) {
      // GIVEN
      setupModelProviderMock(mockedModelProvider);

      // Case 1: Has documentType property
      when(mockEntity.hasProperty(SequenceDatabaseUtils.PROPERTY_DOCUMENTTYPE)).thenReturn(true);
      when(mockEntity.hasProperty(SequenceDatabaseUtils.PROPERTY_DOCUMENTTYPE_TARGET)).thenReturn(false);

      // WHEN
      boolean result1 = sequencesGenerator.hasDocType(mockTable);

      // THEN
      assertTrue(result1);

      // Case 2: Has documentType_ID property
      when(mockEntity.hasProperty(SequenceDatabaseUtils.PROPERTY_DOCUMENTTYPE)).thenReturn(false);
      when(mockEntity.hasProperty(SequenceDatabaseUtils.PROPERTY_DOCUMENTTYPE_TARGET)).thenReturn(true);

      // WHEN
      boolean result2 = sequencesGenerator.hasDocType(mockTable);

      // THEN
      assertTrue(result2);

      // Case 3: Has neither property
      when(mockEntity.hasProperty(SequenceDatabaseUtils.PROPERTY_DOCUMENTTYPE)).thenReturn(false);
      when(mockEntity.hasProperty(SequenceDatabaseUtils.PROPERTY_DOCUMENTTYPE_TARGET)).thenReturn(false);

      // WHEN
      boolean result3 = sequencesGenerator.hasDocType(mockTable);

      // THEN
      assertFalse(result3);
    }
  }

  /**
   * Tests the createSequence method when a sequence does not already exist.
   * Verifies that a new sequence is created and properly initialized.
   */
  @Test
  public void testCreateSequenceWhenSequenceDoesNotExist() {
    // GIVEN
    SequencesGenerator spyGenerator = spy(sequencesGenerator);
    doReturn(false).when(spyGenerator).existsSequence(eq(mockColumn), eq(mockClient), eq(mockOrganization),
        eq(mockDocumentType));
    doReturn(mockSequence).when(spyGenerator).setSequenceValues(eq(mockClient), eq(mockOrganization), anyString(),
        eq(mockColumn), eq(mockDocumentType));

    // WHEN
    int result = spyGenerator.createSequence(mockClient, mockOrganization, TEST_SEQUENCE, mockColumn, mockDocumentType);

    // THEN
    assertEquals(1, result);
    verify(spyGenerator).existsSequence(eq(mockColumn), eq(mockClient), eq(mockOrganization), eq(mockDocumentType));
    verify(spyGenerator).setSequenceValues(eq(mockClient), eq(mockOrganization), eq(TEST_SEQUENCE), eq(mockColumn),
        eq(mockDocumentType));
  }

  /**
   * Tests the createSequence method when a sequence already exists.
   * Verifies that no new sequence is created when one already exists.
   */
  @Test
  public void testCreateSequenceWhenSequenceExists() {
    // GIVEN
    SequencesGenerator spyGenerator = spy(sequencesGenerator);
    doReturn(true).when(spyGenerator).existsSequence(eq(mockColumn), eq(mockClient), eq(mockOrganization),
        eq(mockDocumentType));

    // WHEN
    int result = spyGenerator.createSequence(mockClient, mockOrganization, TEST_SEQUENCE, mockColumn, mockDocumentType);

    // THEN
    assertEquals(0, result);
    verify(spyGenerator).existsSequence(eq(mockColumn), eq(mockClient), eq(mockOrganization), eq(mockDocumentType));
    verify(spyGenerator, never()).setSequenceValues(any(Client.class), any(Organization.class), anyString(),
        any(Column.class), any(DocumentType.class));
  }

  /**
   * Tests the setSequenceValues method.
   * Verifies that a new sequence is properly initialized with the correct values.
   */
  @Test
  public void testSetSequenceValues() {
    try (var mockedOBProvider = mockStatic(OBProvider.class); var mockedOBDal = mockStatic(OBDal.class)) {
      // GIVEN
      mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
      mockedOBProvider.when(OBProvider::getInstance).thenReturn(mock(OBProvider.class));
      mockedOBProvider.when(() -> OBProvider.getInstance().get(Sequence.class)).thenReturn(mockSequence);

      // WHEN
      Sequence result = sequencesGenerator.setSequenceValues(mockClient, mockOrganization, TEST_SEQUENCE, mockColumn,
          mockDocumentType);

      // THEN
      assertEquals(mockSequence, result);

      // Verify all properties were set correctly
      verify(mockSequence).setClient(mockClient);
      verify(mockSequence).setOrganization(mockOrganization);
      verify(mockSequence).setName(TEST_SEQUENCE);
      verify(mockSequence).setPrefix("");
      verify(mockSequence).setSuffix("");
      verify(mockSequence).setMask("#######");
      verify(mockSequence).setActive(true);
      verify(mockSequence).setColumn(mockColumn);
      verify(mockSequence).setTable(mockColumn.getTable());
      verify(mockSequence).setDocumentType(mockDocumentType);
      verify(mockSequence).setAutoNumbering(true);
      verify(mockSequence).setNextAssignedNumber(1000000L);
      verify(mockSequence).setIncrementBy(1L);

      // Verify sequence was saved
      verify(mockOBDal).save(mockSequence);
    }
  }

  /**
   * Tests the existsSequence method when a sequence exists.
   * Verifies that the method correctly identifies existing sequences.
   */
  @Test
  public void testExistsSequenceWhenSequenceExists() {
    try (var mockedOBDal = mockStatic(OBDal.class)) {
      // GIVEN
      setupExistsSequenceMocks(mockedOBDal, mockSequence);

      // WHEN
      boolean result = sequencesGenerator.existsSequence(mockColumn, mockClient, mockOrganization, mockDocumentType);

      // THEN
      assertTrue(result);
      verify(mockSequenceCriteria).setMaxResults(1);
      verify(mockSequenceCriteria).uniqueResult();
    }
  }

  /**
   * Tests the existsSequence method when a sequence does not exist.
   * Verifies that the method correctly identifies non-existing sequences.
   */
  @Test
  public void testExistsSequenceWhenSequenceDoesNotExist() {
    try (var mockedOBDal = mockStatic(OBDal.class)) {
      // GIVEN
      setupExistsSequenceMocks(mockedOBDal, null);

      // WHEN
      boolean result = sequencesGenerator.existsSequence(mockColumn, mockClient, mockOrganization, mockDocumentType);

      // THEN
      assertFalse(result);
      verify(mockSequenceCriteria).uniqueResult();
    }
  }

  /**
   * Tests the existsSequence method with a null document type.
   * Verifies that the method correctly handles null document types.
   */
  @Test
  public void testExistsSequenceWithNullDocumentType() {
    try (var mockedOBDal = mockStatic(OBDal.class)) {
      // GIVEN
      setupExistsSequenceMocks(mockedOBDal, mockSequence);

      // WHEN
      boolean result = sequencesGenerator.existsSequence(mockColumn, mockClient, mockOrganization, null);

      // THEN
      assertTrue(result);
      // Verify that we don't add a restriction for document type when it's null
      verify(mockSequenceCriteria, times(3)).add(any()); // Only 3 restrictions, not 4
    }
  }

  /**
   * Tests the generateSequenceCombination method with multiple organizations.
   * Verifies that the method correctly processes sequences for multiple organizations.
   *
   * @throws Exception
   *     if any error occurs during test execution
   */
  @Test
  public void testGenerateSequenceCombinationWithMultipleOrganizations() throws Exception {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(
        OBDal.class); MockedConstruction<OrganizationStructureProvider> ignored = mockConstruction(
        OrganizationStructureProvider.class, (mock, context) -> {
          Set<String> parentOrgs = new HashSet<>();
          parentOrgs.add("parentOrgId");
          when(mock.getParentTree(anyString(), anyBoolean())).thenReturn(parentOrgs);
        })) {

      // GIVEN
      mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

      // Setup multiple organizations
      Set<String> multipleOrgs = new HashSet<>();
      multipleOrgs.add("org1");
      multipleOrgs.add("org2");

      // Mock column criteria setup
      setupColumnCriteriaMocks();

      when(mockOBDal.get(Reference.class, TransactionalSequenceUtils.TRANSACTIONAL_SEQUENCE_ID)).thenReturn(
          mockReference);

      // Mock DocumentType criteria to return one document type
      setupDocTypeCriteriaMocks();

      // Mock Organization retrievals
      Organization mockOrg1 = mock(Organization.class);
      Organization mockOrg2 = mock(Organization.class);
      when(mockOBDal.get(Organization.class, "org1")).thenReturn(mockOrg1);
      when(mockOBDal.get(Organization.class, "org2")).thenReturn(mockOrg2);

      // Mock hasDocType and createSequence
      SequencesGenerator spyGenerator = spy(sequencesGenerator);
      doReturn(true).when(spyGenerator).hasDocType(mockTable);

      // Different return values for different org calls
      doReturn(1).when(spyGenerator).createSequence(eq(mockClient), eq(mockOrg1), anyString(), eq(mockColumn),
          eq(mockDocumentType));
      doReturn(0).when(spyGenerator).createSequence(eq(mockClient), eq(mockOrg2), anyString(), eq(mockColumn),
          eq(mockDocumentType));

      // WHEN
      int count = spyGenerator.generateSequenceCombination(mockClient, multipleOrgs, parameters);

      // THEN
      assertEquals(1, count); // Only 1 for org1, 0 for org2
      verify(spyGenerator, times(1)).createSequence(eq(mockClient), eq(mockOrg1), anyString(), eq(mockColumn),
          eq(mockDocumentType));
      verify(spyGenerator, times(1)).createSequence(eq(mockClient), eq(mockOrg2), anyString(), eq(mockColumn),
          eq(mockDocumentType));
    }
  }

  /**
   * Tests the action method when an exception occurs.
   * Verifies that the method correctly handles and reports exceptions.
   *
   * @throws Exception
   *     if any error occurs during test execution
   */
  @Test
  public void testActionWithException() throws Exception {
    try (MockedStatic<OBContext> mockedOBContext = mockStatic(
        OBContext.class); MockedStatic<OBDal> mockedOBDal = mockStatic(
        OBDal.class); MockedConstruction<OrganizationStructureProvider> ignored = mockConstruction(
        OrganizationStructureProvider.class, (mock, context) -> {
          Set<String> orgSet = new HashSet<>();
          orgSet.add(TEST_ORG_ID);
          when(mock.getChildTree(anyString(), anyBoolean())).thenReturn(orgSet);
        })) {

      // GIVEN
      mockedOBContext.when(OBContext::getOBContext).thenReturn(mockOBContext);
      when(mockOBContext.getCurrentClient()).thenReturn(mockClient);

      // Mock para OBDal.getInstance()
      mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

      // Spy para SequencesGenerator
      SequencesGenerator spyGenerator = spy(sequencesGenerator);

      doThrow(new RuntimeException("Test error")).when(spyGenerator).generateSequenceCombination(any(Client.class),
          anySet(), any(JSONObject.class));

      // WHEN
      ActionResult result = spyGenerator.action(parameters, new MutableBoolean(false));

      // THEN
      assertEquals(Result.Type.ERROR, result.getType());
      assertEquals("Test error", result.getMessage());

      mockedOBContext.verify(OBContext::setAdminMode);
      mockedOBContext.verify(OBContext::restorePreviousMode);
    }
  }

  /**
   * Tests the getInputClass method.
   * Verifies that the method returns the correct class.
   */
  @Test
  public void testGetInputClass() {
    // WHEN
    Class<?> resultClass = sequencesGenerator.getInputClass();

    // THEN
    assertEquals(Sequence.class, resultClass);
  }

  /**
   * Helper method to set up column criteria mocks.
   * Extracts common mock setup for column criteria.
   */
  private void setupColumnCriteriaMocks() {
    when(mockOBDal.createCriteria(Column.class)).thenReturn(mockColumnCriteria);
    when(mockColumnCriteria.add(any())).thenReturn(mockColumnCriteria);
    when(mockColumnCriteria.list()).thenReturn(sequenceColumns);
  }

  /**
   * Helper method to set up document type criteria mocks.
   * Extracts common mock setup for document type criteria.
   */
  private void setupDocTypeCriteriaMocks() {
    when(mockOBDal.createCriteria(DocumentType.class)).thenReturn(mockDocTypeCriteria);
    when(mockDocTypeCriteria.add(any())).thenReturn(mockDocTypeCriteria);
    when(mockDocTypeCriteria.list()).thenReturn(documentTypes);
  }

  /**
   * Helper method to setup model provider mocks.
   * Extracts common mock setup for model provider.
   *
   * @param mockedModelProvider
   *     the mocked static ModelProvider
   */
  private void setupModelProviderMock(MockedStatic<ModelProvider> mockedModelProvider) {
    ModelProvider mockModelProviderInstance = mock(ModelProvider.class);
    mockedModelProvider.when(ModelProvider::getInstance).thenReturn(mockModelProviderInstance);

    when(mockModelProviderInstance.getEntity(anyString())).thenReturn(mockEntity);
    when(mockTable.getName()).thenReturn("TestTable");
  }

  /**
   * Helper method to set up exists sequence mocks.
   * Extracts common mock setup for the existsSequence method tests.
   *
   * @param mockedOBDal
   *     the mocked static OBDal
   * @param returnSequence
   *     the sequence to return or null
   */
  private void setupExistsSequenceMocks(MockedStatic<OBDal> mockedOBDal, Sequence returnSequence) {
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

    when(mockOBDal.createCriteria(Sequence.class)).thenReturn(mockSequenceCriteria);
    lenient().when(mockSequenceCriteria.add(any())).thenReturn(mockSequenceCriteria);
    when(mockSequenceCriteria.setMaxResults(anyInt())).thenReturn(mockSequenceCriteria);
    when(mockSequenceCriteria.uniqueResult()).thenReturn(returnSequence);
  }
}
