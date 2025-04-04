package com.etendoerp.format.mask;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for the MaskFormatter class.
 * Tests various aspects of the class including mask validation, formatting,
 * and character manipulation based on mask patterns.
 * p
 * The tests set allowsInvalid to true for valueToString tests to handle characters
 * that might not match the mask's validation rules.
 */
@RunWith(MockitoJUnitRunner.class)
public class MaskFormatterTest {

  private static final String MASK_PATTERN = "###-####";
  private static final String SAMPLE_INPUT = "123-4567";

  /**
   * Rule for expecting exceptions in test methods.
   * Allows verification of exception types thrown during test execution.
   */
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MaskFormatter maskFormatter;
  private AutoCloseable mocks;

  /**
   * Sets up the test environment before each test.
   * Initialize mocks and the MaskFormatter instance.
   */
  @Before
  public void setUp() {
    mocks = MockitoAnnotations.openMocks(this);
    maskFormatter = new MaskFormatter();
  }

  /**
   * Cleans up resources after each test.
   * Closes the mocks to prevent memory leaks.
   *
   * @throws Exception
   *     if an error occurs during teardown
   */
  @After
  public void tearDown() throws Exception {
    mocks.close();
  }

  /**
   * Tests the constructor with a mask.
   * Verifies that the mask is correctly set.
   */
  @Test
  public void testConstructorWithMaskSetsCorrectMask() {
    // GIVEN
    String mask = MASK_PATTERN;

    // WHEN
    MaskFormatter formatter = new MaskFormatter(mask);

    // THEN
    assertEquals("The mask should be correctly set", mask, formatter.getMask());
    assertFalse("By default, allows invalid should be false", formatter.getAllowsInvalid());
    assertTrue("By default, should contain literal characters", formatter.getValueContainsLiteralCharacters());
    assertEquals("Default placeholder character should be space", ' ', formatter.getPlaceholderCharacter());
  }

  /**
   * Tests updating the mask.
   * Verifies that the mask is updated correctly.
   */
  @Test
  public void testSetMaskUpdatesMaskCorrectly() {
    // GIVEN
    String initialMask = MASK_PATTERN;
    String newMask = "(###) ###-####";
    MaskFormatter formatter = new MaskFormatter(initialMask);

    // WHEN
    formatter.setMask(newMask);

    // THEN
    assertEquals("The mask should be updated", newMask, formatter.getMask());
  }

  /**
   * Tests updating the valueContainsLiteralCharacters property.
   * Verifies that the property is updated correctly.
   */
  @Test
  public void testSetValueContainsLiteralCharactersUpdatesCorrectly() {
    // GIVEN
    MaskFormatter formatter = new MaskFormatter(MASK_PATTERN);

    // WHEN
    formatter.setValueContainsLiteralCharacters(false);

    // THEN
    assertFalse("Value should not contain literal characters", formatter.getValueContainsLiteralCharacters());
  }

  /**
   * Tests updating the placeholder character.
   * Verifies that the placeholder character is updated correctly.
   */
  @Test
  public void testSetPlaceholderCharacterUpdatesCorrectly() {
    // GIVEN
    MaskFormatter formatter = new MaskFormatter(MASK_PATTERN);

    // WHEN
    formatter.setPlaceholderCharacter('_');

    // THEN
    assertEquals("Placeholder character should be updated", '_', formatter.getPlaceholderCharacter());
  }

  /**
   * Tests updating the placeholder string.
   * Verifies that the placeholder string is updated correctly.
   */
  @Test
  public void testSetPlaceholderUpdatesCorrectly() {
    // GIVEN
    MaskFormatter formatter = new MaskFormatter(MASK_PATTERN);
    String placeholder = "555-1212";

    // WHEN
    formatter.setPlaceholder(placeholder);

    // THEN
    assertEquals("Placeholder string should be updated", placeholder, formatter.getPlaceholder());
  }

  /**
   * Tests updating the valid characters.
   * Verifies that the valid characters are updated correctly.
   */
  @Test
  public void testSetValidCharactersUpdatesCorrectly() {
    // GIVEN
    MaskFormatter formatter = new MaskFormatter(MASK_PATTERN);
    String validChars = "0123456789";

    // WHEN
    formatter.setValidCharacters(validChars);

    // THEN
    assertEquals("Valid characters should be updated", validChars, formatter.getValidCharacters());
  }

  /**
   * Tests updating the invalid characters.
   * Verifies that the invalid characters are updated correctly.
   */
  @Test
  public void testSetInvalidCharactersUpdatesCorrectly() {
    // GIVEN
    MaskFormatter formatter = new MaskFormatter(MASK_PATTERN);
    String invalidChars = "abcdefghijklmnopqrstuvwxyz";

    // WHEN
    formatter.setInvalidCharacters(invalidChars);

    // THEN
    assertEquals("Invalid characters should be updated", invalidChars, formatter.getInvalidCharacters());
  }

  /**
   * Tests valueToString with a partial numeric value.
   * Verifies that the value is formatted with placeholders.
   *
   * @throws ParseException
   *     if an error occurs during parsing
   */
  @Test
  public void testValueToStringWithPartialNumericValue() throws ParseException {
    // GIVEN
    MaskFormatter formatter = new MaskFormatter(MASK_PATTERN);
    formatter.setPlaceholderCharacter('_');
    formatter.setAllowsInvalid(true); // Allow invalid characters for this test
    String value = "123";

    // WHEN
    String result = formatter.valueToString(value);

    // THEN
    assertEquals("Value should be formatted with placeholders", "123-____", result);
  }

  /**
   * Tests valueToString with a placeholder string.
   * Verifies that the value uses the placeholder string for missing characters.
   *
   * @throws ParseException
   *     if an error occurs during parsing
   */
  @Test
  public void testValueToStringWithPlaceholderString() throws ParseException {
    // GIVEN
    MaskFormatter formatter = new MaskFormatter(MASK_PATTERN);
    formatter.setPlaceholder("555-1212");
    formatter.setPlaceholderCharacter('_');
    formatter.setAllowsInvalid(true); // Allow invalid characters for this test
    String value = "123";

    // WHEN
    String result = formatter.valueToString(value);

    // THEN
    // Should use placeholder string for missing characters
    assertEquals("Value should use placeholder string for missing characters", "123-1212", result);
  }

  /**
   * Tests stringToValue with valid input.
   * Verifies that the string value is correctly parsed with literals.
   *
   * @throws ParseException
   *     if an error occurs during parsing
   */
  @Test
  public void testStringToValueWithValidInput() throws ParseException {
    // GIVEN
    MaskFormatter formatter = new MaskFormatter(MASK_PATTERN);
    formatter.setValueContainsLiteralCharacters(true);
    String value = SAMPLE_INPUT;

    // WHEN
    Object result = formatter.stringToValue(value);

    // THEN
    assertEquals("String value should be correctly parsed with literals", SAMPLE_INPUT, result);
  }

  /**
   * Tests stringToValue without literal characters.
   * Verifies that the string value is correctly parsed without literals.
   *
   * @throws ParseException
   *     if an error occurs during parsing
   */
  @Test
  public void testStringToValueWithoutLiteralCharacters() throws ParseException {
    // GIVEN
    MaskFormatter formatter = new MaskFormatter(MASK_PATTERN);
    formatter.setValueContainsLiteralCharacters(false);
    String value = SAMPLE_INPUT;

    // WHEN
    Object result = formatter.stringToValue(value);

    // THEN
    assertEquals("String value should be correctly parsed without literals", "1234567", result);
  }

  /**
   * Tests stringToValue with invalid input.
   * Verifies that a ParseException is thrown for invalid input.
   *
   * @throws ParseException
   *     if an error occurs during parsing
   */
  @Test
  public void testStringToValueWithInvalidInput() throws ParseException {
    // GIVEN
    MaskFormatter formatter = new MaskFormatter(MASK_PATTERN);
    String invalidValue = "abc-defg"; // Letters don't match numeric mask

    // WHEN & THEN
    expectedException.expect(ParseException.class);
    formatter.stringToValue(invalidValue);
  }

  /**
   * Tests stringToValue with incomplete input.
   * Verifies that a ParseException is thrown for incomplete input.
   *
   * @throws ParseException
   *     if an error occurs during parsing
   */
  @Test
  public void testStringToValueWithIncompleteInput() throws ParseException {
    // GIVEN
    MaskFormatter formatter = new MaskFormatter(MASK_PATTERN);
    String incompleteValue = "123-456"; // One digit short

    // WHEN & THEN
    expectedException.expect(ParseException.class);
    formatter.stringToValue(incompleteValue);
  }

  /**
   * Tests different types of mask characters.
   * Verifies that the mask characters behave as expected.
   *
   * @throws ParseException
   *     if an error occurs during parsing
   */
  @Test
  public void testDifferentMaskTypes() throws ParseException {
    // Test different types of mask characters

    // GIVEN - Test uppercase mask 'U'
    MaskFormatter uppercaseFormatter = new MaskFormatter("UUU-UUU");
    uppercaseFormatter.setAllowsInvalid(true);
    String uppercaseValue = "abc-def";

    // WHEN
    String uppercaseResult = uppercaseFormatter.valueToString(uppercaseValue);

    // THEN
    assertEquals("Uppercase mask should convert letters to uppercase", "ABC-DEF", uppercaseResult);

    // GIVEN - Test lowercase mask 'L'
    MaskFormatter lowercaseFormatter = new MaskFormatter("LLL-LLL");
    lowercaseFormatter.setAllowsInvalid(true);
    String lowercaseValue = "ABC-DEF";

    // WHEN
    String lowercaseResult = lowercaseFormatter.valueToString(lowercaseValue);

    // THEN
    assertEquals("Lowercase mask should convert letters to lowercase", "abc-def", lowercaseResult);

    // GIVEN - Test alphanumeric mask 'A'
    MaskFormatter alphaNumFormatter = new MaskFormatter("AAA-AAA");
    alphaNumFormatter.setAllowsInvalid(true);
    String alphaNumValue = "ab3-d4f";

    // WHEN
    String alphaNumResult = alphaNumFormatter.valueToString(alphaNumValue);

    // THEN
    assertEquals("Alphanumeric mask should allow both letters and numbers", "ab3-d4f", alphaNumResult);

    // GIVEN - Test hex mask 'H'
    MaskFormatter hexFormatter = new MaskFormatter("HHH-HHH");
    hexFormatter.setAllowsInvalid(true);
    String hexValue = "abc-d4f";

    // WHEN
    String hexResult = hexFormatter.valueToString(hexValue);

    // THEN
    assertEquals("Hex mask should allow hex characters and capitalize letters", "ABC-D4F", hexResult);
  }

  /**
   * Tests the MaskCharacter class's getChar method.
   * Verifies that the method behaves as expected for different mask characters.
   */
  @Test
  public void testMaskCharacterClassIsValidCharacter() {
    // GIVEN
    MaskFormatter.MaskCharacter maskChar = maskFormatter.new MaskCharacter();

    // WHEN & THEN
    assertTrue("Any character should be valid for base MaskCharacter", maskChar.isValidCharacter('a'));
    assertTrue("Any character should be valid for base MaskCharacter", maskChar.isValidCharacter('5'));

    // GIVEN
    MaskFormatter.DigitMaskCharacter digitChar = maskFormatter.new DigitMaskCharacter();

    // WHEN & THEN
    assertTrue("Digit character should be valid for DigitMaskCharacter", digitChar.isValidCharacter('5'));
    assertFalse("Letter character should not be valid for DigitMaskCharacter", digitChar.isValidCharacter('a'));

    // GIVEN
    MaskFormatter.HexCharacter hexChar = maskFormatter.new HexCharacter();

    // WHEN & THEN
    assertTrue("Hex character (digit) should be valid for HexCharacter", hexChar.isValidCharacter('5'));
    assertTrue("Hex character (a-f) should be valid for HexCharacter", hexChar.isValidCharacter('a'));
    assertTrue("Hex character (A-F) should be valid for HexCharacter", hexChar.isValidCharacter('F'));
    assertFalse("Non-hex character should not be valid for HexCharacter", hexChar.isValidCharacter('g'));
  }

  /**
   * Tests the MaskCharacter class's getChar method.
   * Verifies that the method behaves as expected for different mask characters.
   */
  @Test
  public void testMaskCharacterClassGetChar() {
    // GIVEN
    MaskFormatter.UpperCaseCharacter upperChar = maskFormatter.new UpperCaseCharacter();
    MaskFormatter.LowerCaseCharacter lowerChar = maskFormatter.new LowerCaseCharacter();
    MaskFormatter.HexCharacter hexChar = maskFormatter.new HexCharacter();

    // WHEN & THEN
    assertEquals("UpperCaseCharacter should convert to uppercase", 'A', upperChar.getChar('a'));
    assertEquals("LowerCaseCharacter should convert to lowercase", 'a', lowerChar.getChar('A'));
    assertEquals("HexCharacter should convert letters to uppercase", 'A', hexChar.getChar('a'));
    assertEquals("HexCharacter should keep digits as is", '5', hexChar.getChar('5'));
  }

  /**
   * Tests the LiteralCharacter class.
   * Verifies that the LiteralCharacter behaves as expected.
   */
  @Test
  public void testLiteralCharacter() {
    // GIVEN
    MaskFormatter.LiteralCharacter literalChar = maskFormatter.new LiteralCharacter('-');

    // WHEN & THEN
    assertTrue("LiteralCharacter should be identified as literal", literalChar.isLiteral());
    assertEquals("LiteralCharacter should return fixed char", '-', literalChar.getChar('a')); // Input char is ignored
  }
}
