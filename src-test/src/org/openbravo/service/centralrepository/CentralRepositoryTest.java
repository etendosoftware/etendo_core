package org.openbravo.service.centralrepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.exception.OBException;

/**
 * Unit tests for the CentralRepository class.
 * Verifies the behavior of the methods responsible for communication with the Central Repository.
 */
@ExtendWith(MockitoExtension.class)
public class CentralRepositoryTest {

  private MockedStatic<CentralRepository> centralRepositoryStatic;

  /**
   * Sets up the test environment before each test.
   * Mocks the static methods of the CentralRepository class.
   */
  @BeforeEach
  public void setUp() {
    centralRepositoryStatic = mockStatic(CentralRepository.class);
  }

  /**
   * Cleans up the test environment after each test.
   * Closes the mocked static methods to release resources.
   */
  @AfterEach
  public void tearDown() {
    if (centralRepositoryStatic != null) {
      centralRepositoryStatic.close();
    }
  }

  /**
   * Tests the executeRequest method with a valid service and response.
   * Verifies that the response is successful and contains the expected data.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testExecuteRequestGivenValidServiceAndResponseShouldReturnSuccess() throws Exception {
    CentralRepository.Service service = CentralRepository.Service.VERSION_INFO;
    JSONObject expectedResponse = new JSONObject();
    expectedResponse.put(CentralRepositoryTestConstants.SUCCESS, true);
    expectedResponse.put("responseCode", 200);
    expectedResponse.put(CentralRepositoryTestConstants.RESPONSE, new JSONObject("{\"msg\":\"success\"}"));

    centralRepositoryStatic.when(() -> CentralRepository.executeRequest(service)).thenReturn(expectedResponse);

    JSONObject actualResponse = CentralRepository.executeRequest(service);

    assertTrue(actualResponse.getBoolean(CentralRepositoryTestConstants.SUCCESS));
    assertEquals(200, actualResponse.getInt("responseCode"));
    assertEquals(CentralRepositoryTestConstants.SUCCESS, actualResponse.getJSONObject(CentralRepositoryTestConstants.RESPONSE).getString("msg"));
  }

  /**
   * Tests the executeRequest method with an invalid service URI.
   * Verifies that an OBException is thrown with the expected message.
   */
  @Test
  public void testExecuteRequestGivenInvalidServiceUriShouldThrowOBException() {
    CentralRepository.Service service = CentralRepository.Service.MODULE_INFO;

    centralRepositoryStatic.when(() -> CentralRepository.executeRequest(service)).thenThrow(
        new OBException("Invalid URI"));

    Exception exception = assertThrows(OBException.class, () -> CentralRepository.executeRequest(service));

    assertEquals("Invalid URI", exception.getMessage());
  }

  /**
   * Tests the executeRequest method when the server returns an HTTP error.
   * Verifies that the response contains the appropriate error information.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testExecuteRequestGivenHttpServerErrorShouldReturnJsonWithError() throws Exception {
    CentralRepository.Service service = CentralRepository.Service.SEARCH_MODULES;
    JSONObject expectedError = new JSONObject();
    expectedError.put(CentralRepositoryTestConstants.SUCCESS, false);
    expectedError.put(CentralRepositoryTestConstants.RESPONSE_CODE, 500);
    expectedError.put(CentralRepositoryTestConstants.RESPONSE, new JSONObject("{\"msg\":\"Internal Server Error\"}"));

    centralRepositoryStatic.when(() -> CentralRepository.executeRequest(service)).thenReturn(expectedError);

    JSONObject actualResponse = CentralRepository.executeRequest(service);

    assertFalse(actualResponse.getBoolean(CentralRepositoryTestConstants.SUCCESS));
    assertEquals(500, actualResponse.getInt(CentralRepositoryTestConstants.RESPONSE_CODE));
  }

  /**
   * Tests the executeRequest method with a null payload.
   * Verifies that the method handles the null payload gracefully and returns a valid response.
   *
   * @throws JSONException
   *     if an error occurs during JSON processing
   */
  @Test
  public void testExecuteRequestGivenNullPayloadShouldHandleGracefully() throws JSONException {
    CentralRepository.Service service = CentralRepository.Service.REGISTER_MODULE;
    JSONObject expectedResponse = new JSONObject();
    expectedResponse.put(CentralRepositoryTestConstants.SUCCESS, true);
    expectedResponse.put(CentralRepositoryTestConstants.RESPONSE_CODE, 200);

    centralRepositoryStatic.when(() -> CentralRepository.executeRequest(service, (JSONObject) null)).thenReturn(
        expectedResponse);

    JSONObject actualResponse = CentralRepository.executeRequest(service, (JSONObject) null);

    assertNotNull(actualResponse);
    assertTrue(actualResponse.getBoolean(CentralRepositoryTestConstants.SUCCESS));
  }
}
