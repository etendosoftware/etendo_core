package com.smf.jobs.defaults.offerPick;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.pricing.priceadjustment.PriceAdjustment;

import com.smf.jobs.Data;
import com.smf.jobs.Result;
import com.smf.jobs.defaults.Utility;

import jakarta.enterprise.context.Dependent;

/**
 * Unit tests for the OfferBaseActionHandler class.
 * Tests the functionality of the action handler for offer pick actions.
 */
@RunWith(MockitoJUnitRunner.class)
public class OfferBaseActionHandlerTest {

  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;

  @Mock
  private PriceAdjustment mockPriceAdjustment;

  @Mock
  private OBDal mockOBDal;

  private TestableOfferBaseActionHandler actionHandler;

  /**
   * Sets up the test environment before each test.
   * Initialize mocks and configures common behavior.
   */
  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    // Mock static methods
    mockedOBContext = mockStatic(OBContext.class);
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);

    // Mock OBDal.getInstance()
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

    // Initialize the concrete implementation of the abstract class
    actionHandler = new TestableOfferBaseActionHandler();
  }

  /**
   * Cleans up resources after each test.
   * Closes the static mocks to prevent memory leaks.
   */
  @After
  public void tearDown() {
    mockedOBContext.close();
    mockedOBDal.close();
    mockedOBMessageUtils.close();
  }

  /**
   * Tests the action method when no lines are selected.
   * Verifies that an error result is returned.
   *
   * @throws Exception
   *     if an error occurs during test execution
   */
  @Test
  public void testActionNoSelectedLines() throws Exception {
    // Given
    JSONObject parameters = new JSONObject();
    parameters.put(Utility.TEST_JSON, new JSONObject().put(Utility.SELECTION, new JSONArray()));

    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD(Utility.NOT_SELECTED)).thenReturn(Utility.NOT_SELECTED);

    // When
    var result = actionHandler.action(parameters, new MutableBoolean(false));

    // Then
    assertEquals(Result.Type.ERROR, result.getType());
    assertEquals(Utility.NOT_SELECTED, result.getMessage());
  }

  /**
   * Tests the action method with valid input.
   * Verifies that a success result is returned.
   *
   * @throws Exception
   *     if an error occurs during test execution
   */
  @Test
  public void testActionValidInput() throws Exception {
    // Given
    JSONObject parameters = new JSONObject();
    JSONArray selection = new JSONArray();
    selection.put(new JSONObject().put("id", "testId"));
    parameters.put(Utility.TEST_JSON, new JSONObject().put(Utility.SELECTION, selection));

    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD(Utility.SUCCESS_CAPITALIZED)).thenReturn(Utility.SUCCESS_CAPITALIZED);

    // When
    var result = actionHandler.action(parameters, new MutableBoolean(false));

    // Then
    assertEquals(Result.Type.SUCCESS, result.getType());
    assertEquals(Utility.SUCCESS_CAPITALIZED, result.getMessage());
  }

  /**
   * Tests the action method's exception handling.
   * Verifies that an error result is returned when an exception occurs.
   *
   * @throws Exception
   *     if an error occurs during test execution
   */
  @Test
  public void testActionExceptionHandling() throws Exception {
    // Given
    JSONObject parameters = new JSONObject();
    parameters.put(Utility.TEST_JSON, new JSONObject().put(Utility.SELECTION, new JSONArray()));

    OBError mockError = mock(OBError.class);
    mockedOBMessageUtils.when(() -> OBMessageUtils.translateError(anyString())).thenReturn(mockError);

    // When
    var result = actionHandler.action(parameters, new MutableBoolean(false));

    // Then
    assertEquals(Result.Type.ERROR, result.getType());
  }

  /**
   * Tests the getInputClass method.
   * Verifies that the correct input class is returned.
   */
  @Test
  public void testGetInputClass() {
    // When
    var inputClass = actionHandler.getInputClass();

    // Then
    assertEquals(PriceAdjustment.class, inputClass);
  }

  /**
   * A testable version of the abstract OfferBaseActionHandler class.
   * Allows for controlled testing of the abstract class's behavior.
   */
  @Dependent
  private class TestableOfferBaseActionHandler extends OfferBaseActionHandler {

    public TestableOfferBaseActionHandler() {
      // Initialize with test data
      List<PriceAdjustment> testData = new ArrayList<>();
      testData.add(mockPriceAdjustment);

      // Set the input field using reflection
      try {
        java.lang.reflect.Field inputField = getClass().getSuperclass().getSuperclass().getDeclaredField("input");
        inputField.setAccessible(true);

        // Create a mock Data object that will return our test data
        Data mockData = mock(Data.class);
        when(mockData.getContents(PriceAdjustment.class)).thenReturn(testData);

        inputField.set(this, mockData);
      } catch (Exception e) {
        throw new RuntimeException("Failed to initialize test data", e);
      }
    }

    @Override
    protected void doPickAndExecute(PriceAdjustment register, JSONArray selectedLines) {
    }

    @Override
    protected String getJSONName() {
      return Utility.TEST_JSON;
    }

  }
}
