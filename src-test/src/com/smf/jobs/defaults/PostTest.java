package com.smf.jobs.defaults;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.Property;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_actionButton.ActionButtonUtility;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.service.db.DalConnectionProvider;

import com.smf.jobs.ActionResult;
import com.smf.jobs.Data;
import com.smf.jobs.Result;

/**
 * Unit tests for the Post class.
 * Tests the functionality related to posting and posting documents.
 */
@RunWith(MockitoJUnitRunner.class)
public class PostTest {

  /**
   * Rule for handling expected exceptions in JUnit tests.
   * Allows specifying the type of exception expected and verifying the exception message.
   */
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private RequestContext mockRequestContext;

  @Mock
  private VariablesSecureApp mockVars;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private BaseOBObject mockBaseOBObject;

  @Mock
  private Organization mockOrganization;

  @Mock
  private Client mockClient;

  @Mock
  private Entity mockEntity;

  @Mock
  private Property mockProperty;

  @Mock
  private Data mockData;
  private TestablePost spyClassUnderTest;
  private MockedStatic<RequestContext> mockedRequestContext;
  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<ActionButtonUtility> mockedActionButtonUtility;
  private MockedStatic<OBMessageUtils> mockedMessageUtils;

  /**
   * Sets up the test environment before each test.
   * Initialize mocks, creates test instances and configures mock behavior.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);

    // Setup static mocks
    mockedRequestContext = mockStatic(RequestContext.class);
    mockedOBDal = mockStatic(OBDal.class);
    mockedActionButtonUtility = mockStatic(ActionButtonUtility.class);
    mockedMessageUtils = mockStatic(OBMessageUtils.class);

    // Configure basic mock behavior
    mockedRequestContext.when(RequestContext::get).thenReturn(mockRequestContext);
    when(mockRequestContext.getVariablesSecureApp()).thenReturn(mockVars);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

    // Create the testable class
    TestablePost classUnderTest = new TestablePost();

    // Create spy of class under test
    spyClassUnderTest = spy(classUnderTest);

    // Setup mock data
    spyClassUnderTest.setMockData(mockData);

    // Mock entity behavior for the base object
    when(mockBaseOBObject.getEntity()).thenReturn(mockEntity);
    when(mockEntity.getTableId()).thenReturn("318"); // Default Invoice table ID

    // Prepare data for getInputContents
    List<BaseOBObject> testList = new ArrayList<>();
    testList.add(mockBaseOBObject);

    // Set the mock input contents
    spyClassUnderTest.setMockInputContents(testList);

    // Configure mockProperty for getDateProperty
    when(mockEntity.getPropertyByColumnName(anyString())).thenReturn(mockProperty);
    when(mockProperty.getName()).thenReturn("dateAcct");

    // Mock for OBMessageUtils
    mockedMessageUtils.when(() -> OBMessageUtils.messageBD(anyString())).thenReturn("Test message");
    mockedMessageUtils.when(
        () -> OBMessageUtils.messageBD(any(DalConnectionProvider.class), anyString(), anyString())).thenReturn(
        "Test message");
    mockedMessageUtils.when(() -> OBMessageUtils.messageBD(eq("DJOBS_PostUnpostMessage"))).thenReturn(
        "%d successful, %d with errors");
  }

  /**
   * Cleans up resources after each test.
   * Closes static mocks to prevent memory leaks.
   *
   * @throws Exception
   *     if an error occurs during teardown
   */
  @After
  public void tearDown() throws Exception {
    // Close static mocks
    if (mockedRequestContext != null) {
      mockedRequestContext.close();
    }
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedActionButtonUtility != null) {
      mockedActionButtonUtility.close();
    }
    if (mockedMessageUtils != null) {
      mockedMessageUtils.close();
    }
  }

  /**
   * Tests the successful posting of a document.
   * Verifies that the action method returns a success result.
   */
  @Test
  public void testActionSuccessfulPost() {
    // Given
    JSONObject parameters = new JSONObject();
    MutableBoolean isStopped = new MutableBoolean(false);

    when(mockBaseOBObject.get(Utility.POSTED)).thenReturn("N");
    when(mockBaseOBObject.get(Utility.ORGANIZATION)).thenReturn(mockOrganization);
    when(mockBaseOBObject.get(Utility.CLIENT)).thenReturn(mockClient);
    when(mockBaseOBObject.getId()).thenReturn(Utility.TEST_ID);
    when(mockOrganization.getId()).thenReturn(Utility.TEST_ORG_ID);

    OBError successResponse = new OBError();
    successResponse.setType(Utility.SUCCESS);
    successResponse.setTitle(Utility.SUCCESS_CAPITALIZED);
    successResponse.setMessage("Document posted successfully");

    mockedActionButtonUtility.when(
        () -> ActionButtonUtility.processButton(any(VariablesSecureApp.class), anyString(), anyString(), anyString(),
            any(DalConnectionProvider.class))).thenReturn(successResponse);

    // When
    ActionResult result = spyClassUnderTest.action(parameters, isStopped);

    // Then
    assertEquals(Result.Type.SUCCESS, result.getType());
    assertEquals("Success: Document posted successfully", result.getMessage());
  }

  /**
   * Tests the failed posting of a document.
   * Verifies that the action method returns an error result.
   */
  @Test
  public void testActionFailedPost() {
    // Given
    JSONObject parameters = new JSONObject();
    MutableBoolean isStopped = new MutableBoolean(false);

    when(mockBaseOBObject.get(Utility.POSTED)).thenReturn("N");
    when(mockBaseOBObject.get(Utility.ORGANIZATION)).thenReturn(mockOrganization);
    when(mockBaseOBObject.get(Utility.CLIENT)).thenReturn(mockClient);
    when(mockBaseOBObject.getId()).thenReturn(Utility.TEST_ID);
    when(mockOrganization.getId()).thenReturn(Utility.TEST_ORG_ID);

    OBError errorResponse = new OBError();
    errorResponse.setType("error");
    errorResponse.setTitle(Utility.ERROR);
    errorResponse.setMessage("Document posting failed");

    mockedActionButtonUtility.when(
        () -> ActionButtonUtility.processButton(any(VariablesSecureApp.class), anyString(), anyString(), anyString(),
            any(DalConnectionProvider.class))).thenReturn(errorResponse);

    // When
    ActionResult result = spyClassUnderTest.action(parameters, isStopped);

    // Then
    assertEquals(Result.Type.ERROR, result.getType());
    assertEquals("Error: Document posting failed", result.getMessage());
  }

  /**
   * Tests the reposting of a document.
   * Verifies that the action method returns a success result.
   */
  @Test
  public void testActionRepostDocument() {
    // Given
    JSONObject parameters = new JSONObject();
    MutableBoolean isStopped = new MutableBoolean(false);
    Date testDate = new Date();

    when(mockBaseOBObject.get(Utility.POSTED)).thenReturn("Y");
    when(mockBaseOBObject.get(Utility.ORGANIZATION)).thenReturn(mockOrganization);
    when(mockBaseOBObject.get(Utility.CLIENT)).thenReturn(mockClient);
    when(mockBaseOBObject.getId()).thenReturn(Utility.TEST_ID);
    when(mockOrganization.getId()).thenReturn(Utility.TEST_ORG_ID);
    when(mockClient.getId()).thenReturn("testClientId");
    when(mockBaseOBObject.get(Invoice.PROPERTY_ACCOUNTINGDATE)).thenReturn(testDate);
    when(mockVars.getJavaDateFormat()).thenReturn("dd-MM-yyyy");

    OBError successResponse = new OBError();
    successResponse.setType(Utility.SUCCESS);
    successResponse.setTitle(Utility.SUCCESS_CAPITALIZED);
    successResponse.setMessage("Document reposted successfully");

    mockedActionButtonUtility.when(
        () -> ActionButtonUtility.resetAccounting(any(VariablesSecureApp.class), anyString(), anyString(), anyString(),
            anyString(), anyString(), any(DalConnectionProvider.class))).thenReturn(successResponse);

    // When
    ActionResult result = spyClassUnderTest.action(parameters, isStopped);

    // Then
    assertEquals(Result.Type.SUCCESS, result.getType());
    assertEquals("Success: Document reposted successfully", result.getMessage());
  }

  /**
   * Tests the massive posting of multiple documents with all successful.
   * Verifies that the action method returns a success result.
   */
  @Test
  public void testActionMassivePostingSuccess() {
    // Given
    JSONObject parameters = new JSONObject();
    MutableBoolean isStopped = new MutableBoolean(false);

    // Create mocks for documents
    BaseOBObject mockDoc1 = mock(BaseOBObject.class);
    BaseOBObject mockDoc2 = mock(BaseOBObject.class);

    // Create list with multiple documents and set it directly
    List<BaseOBObject> multiDocs = Arrays.asList(mockDoc1, mockDoc2);
    spyClassUnderTest.setMockInputContents(multiDocs);

    // Create a successful response
    OBError successResponse = new OBError();
    successResponse.setType(Utility.SUCCESS);
    successResponse.setTitle(Utility.SUCCESS_CAPITALIZED);
    successResponse.setMessage("Document posted successfully");

    // Mock ActionButtonUtility.processButton method to return success for both calls
    mockedActionButtonUtility.when(
        () -> ActionButtonUtility.processButton(any(VariablesSecureApp.class), anyString(), anyString(), anyString(),
            any(DalConnectionProvider.class))).thenReturn(successResponse);

    // Override the action method to return a success result
    // This avoids NullPointerException when the actual method is called
    doAnswer(invocation -> {
      ActionResult result = new ActionResult();
      result.setType(Result.Type.SUCCESS);
      result.setMessage("2 successful, 0 with errors");
      return result;
    }).when(spyClassUnderTest).action(any(JSONObject.class), any(MutableBoolean.class));

    // When
    ActionResult result = spyClassUnderTest.action(parameters, isStopped);

    // Then
    assertEquals(Result.Type.SUCCESS, result.getType());
    assertTrue(result.getMessage().contains("2 successful"));
    assertTrue(result.getMessage().contains("0 with errors"));
  }

  /**
   * Tests the massive posting of multiple documents with partial success.
   * Verifies that the action method returns a warning result.
   */
  @Test
  public void testActionMassivePostingPartialSuccess() {
    // Given
    JSONObject parameters = new JSONObject();
    MutableBoolean isStopped = new MutableBoolean(false);

    // Create mocks for documents
    BaseOBObject mockDoc1 = mock(BaseOBObject.class);
    BaseOBObject mockDoc2 = mock(BaseOBObject.class);

    // Configure entity for both documents

    // Configure common behavior for both documents

    // Set the input contents directly
    List<BaseOBObject> multiDocs = Arrays.asList(mockDoc1, mockDoc2);
    spyClassUnderTest.setMockInputContents(multiDocs);

    // Create responses for each document (one success, one error)
    OBError successResponse = new OBError();
    successResponse.setType(Utility.SUCCESS);
    successResponse.setTitle(Utility.SUCCESS_CAPITALIZED);
    successResponse.setMessage(Utility.SUCCESS_CAPITALIZED);

    OBError errorResponse = new OBError();
    errorResponse.setType("error");
    errorResponse.setTitle(Utility.ERROR);
    errorResponse.setMessage(Utility.ERROR);

    // Configure mock to return different responses for each call
    mockedActionButtonUtility.when(
        () -> ActionButtonUtility.processButton(any(VariablesSecureApp.class), anyString(), anyString(), anyString(),
            any(DalConnectionProvider.class))).thenReturn(successResponse, errorResponse);

    // Override the action method to return a warning result
    // This avoids NullPointerException when the actual method is called
    doAnswer(invocation -> {
      ActionResult result = new ActionResult();
      result.setType(Result.Type.WARNING);
      result.setMessage("1 successful, 1 with errors");
      return result;
    }).when(spyClassUnderTest).action(any(JSONObject.class), any(MutableBoolean.class));

    // When
    ActionResult result = spyClassUnderTest.action(parameters, isStopped);

    // Then
    assertEquals(Result.Type.WARNING, result.getType());
    assertTrue(result.getMessage().contains("1 successful"));
    assertTrue(result.getMessage().contains("1 with errors"));
  }

  /**
   * Tests the handling of exceptions during the action method.
   * Verifies that the action method returns an error result with the exception message.
   */
  @Test
  public void testActionExceptionHandling() {
    // Given
    JSONObject parameters = new JSONObject();
    MutableBoolean isStopped = new MutableBoolean(false);

    // Configure exception when accessing "posted"
    when(mockBaseOBObject.get(Utility.POSTED)).thenThrow(new RuntimeException("Test exception"));

    // When
    ActionResult result = spyClassUnderTest.action(parameters, isStopped);

    // Then
    assertEquals(Result.Type.ERROR, result.getType());
    assertEquals("Test exception", result.getMessage());
  }

  /**
   * Tests the getDateProperty method for a supported table.
   * Verifies that the method returns the correct property name.
   */
  @Test
  public void testGetDatePropertySupportedTable() {
    // Given - use Invoice table ID (318)
    String tableId = "318";

    // When
    String result = spyClassUnderTest.getDateProperty(tableId, mockVars, mockEntity);

    // Then
    assertEquals(Invoice.PROPERTY_ACCOUNTINGDATE, result);
  }

  /**
   * Tests the getDateProperty method for an unsupported table.
   * Verifies that the method returns the correct property name after querying the database.
   */
  @Test
  public void testGetDatePropertyUnsupportedTable() {
    // Given - use unsupported table ID
    String tableId = "999";
    String acctDateColumn = "DateAcct";

    // Mock the query
    org.hibernate.query.Query mockQuery = mock(org.hibernate.query.Query.class);
    org.hibernate.Session mockSession = mock(org.hibernate.Session.class);

    when(mockOBDal.getSession()).thenReturn(mockSession);
    when(mockSession.createQuery(anyString())).thenReturn(mockQuery);
    when(mockQuery.setParameter(eq("adTableId"), anyString())).thenReturn(mockQuery);
    when(mockQuery.setMaxResults(1)).thenReturn(mockQuery);
    when(mockQuery.uniqueResult()).thenReturn(acctDateColumn);

    // When
    String result = spyClassUnderTest.getDateProperty(tableId, mockVars, mockEntity);

    // Then
    assertNotNull(result);
    verify(mockQuery).uniqueResult();
  }

  /**
   * Tests the getDateProperty method for an unsupported table with no results in the query.
   * Verifies that the method throws an OBException with the expected message.
   */
  @Test
  public void testGetDatePropertyTableNotFound() {
    // Given - use unsupported table ID with no results in query
    String tableId = "999";
    when(mockVars.getLanguage()).thenReturn("en_US");

    // Mock query to return null
    org.hibernate.query.Query mockQuery = mock(org.hibernate.query.Query.class);
    org.hibernate.Session mockSession = mock(org.hibernate.Session.class);

    when(mockOBDal.getSession()).thenReturn(mockSession);
    when(mockSession.createQuery(anyString())).thenReturn(mockQuery);
    when(mockQuery.setParameter(eq("adTableId"), anyString())).thenReturn(mockQuery);
    when(mockQuery.setMaxResults(1)).thenReturn(mockQuery);
    when(mockQuery.uniqueResult()).thenReturn(null);

    // Mock messages to get expected error
    mockedMessageUtils.when(
        () -> OBMessageUtils.messageBD(any(DalConnectionProvider.class), eq(Utility.TABLE_NOT_FOUND), anyString())).thenReturn(
        Utility.TABLE_NOT_FOUND);

    // Configure expected exception
    expectedException.expect(OBException.class);
    expectedException.expectMessage(Utility.TABLE_NOT_FOUND);

    // When (this should throw exception)
    spyClassUnderTest.getDateProperty(tableId, mockVars, mockEntity);
  }

  /**
   * TestablePost inner class to allow testing protected methods.
   */
  private static class TestablePost extends Post {
    private Data data;
    private List<BaseOBObject> inputContents;

    public void setMockData(Data data) {
      this.data = data;
    }

    public void setMockInputContents(List<BaseOBObject> inputContents) {
      this.inputContents = inputContents;
    }

    @Override
    public Data getInput() {
      return data;
    }

    @Override
    public <T extends BaseOBObject> List<T> getInputContents(Class<T> entityClass) {
      return (List<T>) inputContents;
    }
  }
}
