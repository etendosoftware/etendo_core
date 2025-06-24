package com.smf.securewebservices.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.erpCommon.utility.OBError;

/**
 * Unit tests for Result class.
 */
@RunWith(MockitoJUnitRunner.class)
public class ResultTest {

  private Result result;

  /**
   * Sets up the test environment before each test.
   */
  @Before
  public void setUp() {
    result = new Result();
  }

  /**
   * Tests the getType and setType methods.
   */
  @Test
  public void testGetSetType() {
    // GIVEN
    Result.Type expectedType = Result.Type.SUCCESS;

    // WHEN
    result.setType(expectedType);
    Result.Type actualType = result.getType();

    // THEN
    assertEquals(expectedType, actualType);
  }

  /**
   * Tests the getMessage and setMessage methods.
   */
  @Test
  public void testGetSetMessage() {
    // GIVEN
    String expectedMessage = "Test message";

    // WHEN
    result.setMessage(expectedMessage);
    String actualMessage = result.getMessage();

    // THEN
    assertEquals(expectedMessage, actualMessage);
  }

  /**
   * Tests the toOBError method for SUCCESS type.
   */
  @Test
  public void testToOBErrorSuccess() {
    // GIVEN
    result.setType(Result.Type.SUCCESS);
    result.setMessage("Success message");

    // WHEN
    OBError error = result.toOBError();

    // THEN
    assertEquals("SUCCESS", error.getType());
    assertEquals("Success", error.getTitle());
    assertEquals("Success message", error.getMessage());
  }

  /**
   * Tests the toOBError method for ERROR type.
   */
  @Test
  public void testToOBErrorError() {
    // GIVEN
    result.setType(Result.Type.ERROR);
    result.setMessage("Error message");

    // WHEN
    OBError error = result.toOBError();

    // THEN
    assertEquals("ERROR", error.getType());
    assertEquals("Error", error.getTitle());
    assertEquals("Error message", error.getMessage());
  }

  /**
   * Tests the toOBError method for WARNING type.
   */
  @Test
  public void testToOBErrorWarning() {
    // GIVEN
    result.setType(Result.Type.WARNING);
    result.setMessage("Warning message");

    // WHEN
    OBError error = result.toOBError();

    // THEN
    assertEquals("WARNING", error.getType());
    assertEquals("Warning", error.getTitle());
    assertEquals("Warning message", error.getMessage());
  }

  /**
   * Tests the toOBError method for INFO type.
   */
  @Test
  public void testToOBErrorInfo() {
    // GIVEN
    result.setType(Result.Type.INFO);
    result.setMessage("Info message");

    // WHEN
    OBError error = result.toOBError();

    // THEN
    assertEquals("INFO", error.getType());
    assertEquals("Info", error.getTitle());
    assertEquals("Info message", error.getMessage());
  }

  /**
   * Tests the fromOBError method with a valid type.
   */
  @Test
  public void testFromOBErrorValidType() {
    // GIVEN
    OBError error = new OBError();
    error.setType("SUCCESS");
    error.setTitle("Success");
    error.setMessage("Success message");

    // WHEN
    Result convertedResult = Result.fromOBError(error);

    // THEN
    assertEquals(Result.Type.SUCCESS, convertedResult.getType());
    assertEquals("Success message", convertedResult.getMessage());
  }
}
