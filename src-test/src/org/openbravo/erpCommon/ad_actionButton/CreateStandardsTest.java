package org.openbravo.erpCommon.ad_actionButton;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.lang.reflect.InvocationTargetException;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for {@link CreateStandards}.
 * Tests the private utility methods: replace and addDays.
 */
@SuppressWarnings({"java:S120", "java:S112"})
@RunWith(MockitoJUnitRunner.class)
public class CreateStandardsTest {

  private CreateStandards instance;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    instance = new CreateStandards();
  }

  // --- Tests for replace(String) ---
  /**
   * Replace removes spaces.
   * @throws Exception if an error occurs
   */

  @Test
  public void testReplaceRemovesSpaces() throws Exception {
    String result = invokeReplace("hello world");
    assertEquals("helloworld", result);
  }
  /**
   * Replace removes hash.
   * @throws Exception if an error occurs
   */

  @Test
  public void testReplaceRemovesHash() throws Exception {
    String result = invokeReplace("item#1");
    assertEquals("item1", result);
  }
  /**
   * Replace removes ampersand.
   * @throws Exception if an error occurs
   */

  @Test
  public void testReplaceRemovesAmpersand() throws Exception {
    String result = invokeReplace("A&B");
    assertEquals("AB", result);
  }
  /**
   * Replace removes comma.
   * @throws Exception if an error occurs
   */

  @Test
  public void testReplaceRemovesComma() throws Exception {
    String result = invokeReplace("a,b,c");
    assertEquals("abc", result);
  }
  /**
   * Replace removes parentheses.
   * @throws Exception if an error occurs
   */

  @Test
  public void testReplaceRemovesParentheses() throws Exception {
    String result = invokeReplace("test(value)");
    assertEquals("testvalue", result);
  }
  /**
   * Replace removes multiple special chars.
   * @throws Exception if an error occurs
   */

  @Test
  public void testReplaceRemovesMultipleSpecialChars() throws Exception {
    String result = invokeReplace("Item #1 (A & B, C)");
    assertEquals("Item1ABC", result);
  }
  /**
   * Replace empty string.
   * @throws Exception if an error occurs
   */

  @Test
  public void testReplaceEmptyString() throws Exception {
    String result = invokeReplace("");
    assertEquals("", result);
  }
  /**
   * Replace no special chars.
   * @throws Exception if an error occurs
   */

  @Test
  public void testReplaceNoSpecialChars() throws Exception {
    String result = invokeReplace("SimpleText123");
    assertEquals("SimpleText123", result);
  }

  // --- Tests for addDays(Date, int) ---
  /**
   * Add days positive.
   * @throws Exception if an error occurs
   */

  @Test
  public void testAddDaysPositive() throws Exception {
    Calendar cal = createCalendar(2025, Calendar.JANUARY, 1);
    Date result = invokeAddDays(cal.getTime(), 10);

    cal.add(Calendar.DATE, 10);
    assertEquals(cal.getTime(), result);
  }
  /**
   * Add days zero.
   * @throws Exception if an error occurs
   */

  @Test
  public void testAddDaysZero() throws Exception {
    Calendar cal = createCalendar(2025, Calendar.JUNE, 15);
    Date baseDate = cal.getTime();

    Date result = invokeAddDays(baseDate, 0);

    assertEquals(baseDate, result);
  }
  /**
   * Add days negative.
   * @throws Exception if an error occurs
   */

  @Test
  public void testAddDaysNegative() throws Exception {
    Calendar cal = createCalendar(2025, Calendar.MARCH, 10);
    Date result = invokeAddDays(cal.getTime(), -5);

    cal.add(Calendar.DATE, -5);
    assertEquals(cal.getTime(), result);
  }
  /**
   * Add days crosses month.
   * @throws Exception if an error occurs
   */

  @Test
  public void testAddDaysCrossesMonth() throws Exception {
    Calendar cal = createCalendar(2025, Calendar.JANUARY, 28);
    Date result = invokeAddDays(cal.getTime(), 5);

    cal.add(Calendar.DATE, 5);
    assertEquals(cal.getTime(), result);
  }
  /**
   * Add days returns non null.
   * @throws Exception if an error occurs
   */

  @Test
  public void testAddDaysReturnsNonNull() throws Exception {
    Date result = invokeAddDays(new Date(), 1);
    assertNotNull(result);
  }

  // --- Helper methods ---

  private Calendar createCalendar(int year, int month, int day) {
    Calendar cal = Calendar.getInstance();
    cal.set(year, month, day, 0, 0, 0);
    cal.set(Calendar.MILLISECOND, 0);
    return cal;
  }

  private String invokeReplace(String input) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method method = CreateStandards.class.getDeclaredMethod("replace", String.class);
    method.setAccessible(true);
    return (String) method.invoke(instance, input);
  }

  private Date invokeAddDays(Date date, int days) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method method = CreateStandards.class.getDeclaredMethod("addDays", Date.class, int.class);
    method.setAccessible(true);
    return (Date) method.invoke(instance, date, days);
  }
}
