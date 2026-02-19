package org.openbravo.erpCommon.ad_actionButton;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
@RunWith(MockitoJUnitRunner.class)
public class CreateStandardsTest {

  private CreateStandards instance;

  @Before
  public void setUp() {
    instance = new CreateStandards();
  }

  // --- Tests for replace(String) ---

  @Test
  public void testReplaceRemovesSpaces() throws Exception {
    String result = invokeReplace("hello world");
    assertEquals("helloworld", result);
  }

  @Test
  public void testReplaceRemovesHash() throws Exception {
    String result = invokeReplace("item#1");
    assertEquals("item1", result);
  }

  @Test
  public void testReplaceRemovesAmpersand() throws Exception {
    String result = invokeReplace("A&B");
    assertEquals("AB", result);
  }

  @Test
  public void testReplaceRemovesComma() throws Exception {
    String result = invokeReplace("a,b,c");
    assertEquals("abc", result);
  }

  @Test
  public void testReplaceRemovesParentheses() throws Exception {
    String result = invokeReplace("test(value)");
    assertEquals("testvalue", result);
  }

  @Test
  public void testReplaceRemovesMultipleSpecialChars() throws Exception {
    String result = invokeReplace("Item #1 (A & B, C)");
    assertEquals("Item1ABC", result);
  }

  @Test
  public void testReplaceEmptyString() throws Exception {
    String result = invokeReplace("");
    assertEquals("", result);
  }

  @Test
  public void testReplaceNoSpecialChars() throws Exception {
    String result = invokeReplace("SimpleText123");
    assertEquals("SimpleText123", result);
  }

  // --- Tests for addDays(Date, int) ---

  @Test
  public void testAddDaysPositive() throws Exception {
    Calendar cal = Calendar.getInstance();
    cal.set(2025, Calendar.JANUARY, 1, 0, 0, 0);
    cal.set(Calendar.MILLISECOND, 0);
    Date baseDate = cal.getTime();

    Date result = invokeAddDays(baseDate, 10);

    cal.add(Calendar.DATE, 10);
    assertEquals(cal.getTime(), result);
  }

  @Test
  public void testAddDaysZero() throws Exception {
    Calendar cal = Calendar.getInstance();
    cal.set(2025, Calendar.JUNE, 15, 0, 0, 0);
    cal.set(Calendar.MILLISECOND, 0);
    Date baseDate = cal.getTime();

    Date result = invokeAddDays(baseDate, 0);

    assertEquals(baseDate, result);
  }

  @Test
  public void testAddDaysNegative() throws Exception {
    Calendar cal = Calendar.getInstance();
    cal.set(2025, Calendar.MARCH, 10, 0, 0, 0);
    cal.set(Calendar.MILLISECOND, 0);
    Date baseDate = cal.getTime();

    Date result = invokeAddDays(baseDate, -5);

    cal.add(Calendar.DATE, -5);
    assertEquals(cal.getTime(), result);
  }

  @Test
  public void testAddDaysCrossesMonth() throws Exception {
    Calendar cal = Calendar.getInstance();
    cal.set(2025, Calendar.JANUARY, 28, 0, 0, 0);
    cal.set(Calendar.MILLISECOND, 0);
    Date baseDate = cal.getTime();

    Date result = invokeAddDays(baseDate, 5);

    cal.add(Calendar.DATE, 5);
    assertEquals(cal.getTime(), result);
  }

  @Test
  public void testAddDaysReturnsNonNull() throws Exception {
    Date result = invokeAddDays(new Date(), 1);
    assertNotNull(result);
  }

  // --- Helper methods ---

  private String invokeReplace(String input) throws Exception {
    Method method = CreateStandards.class.getDeclaredMethod("replace", String.class);
    method.setAccessible(true);
    return (String) method.invoke(instance, input);
  }

  private Date invokeAddDays(Date date, int days) throws Exception {
    Method method = CreateStandards.class.getDeclaredMethod("addDays", Date.class, int.class);
    method.setAccessible(true);
    return (Date) method.invoke(instance, date, days);
  }
}
