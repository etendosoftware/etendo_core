package org.openbravo.dal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class MatchModeTest {

  @Test
  public void testExactMatchString() {
    assertEquals("test", MatchMode.EXACT.toMatchString("test"));
  }

  @Test
  public void testStartMatchString() {
    assertEquals("test%", MatchMode.START.toMatchString("test"));
  }

  @Test
  public void testEndMatchString() {
    assertEquals("%test", MatchMode.END.toMatchString("test"));
  }

  @Test
  public void testAnywhereMatchString() {
    assertEquals("%test%", MatchMode.ANYWHERE.toMatchString("test"));
  }

  @Test
  public void testExactWithEmptyString() {
    assertEquals("", MatchMode.EXACT.toMatchString(""));
  }

  @Test
  public void testStartWithEmptyString() {
    assertEquals("%", MatchMode.START.toMatchString(""));
  }

  @Test
  public void testEndWithEmptyString() {
    assertEquals("%", MatchMode.END.toMatchString(""));
  }

  @Test
  public void testAnywhereWithEmptyString() {
    assertEquals("%%", MatchMode.ANYWHERE.toMatchString(""));
  }

  @Test
  public void testValuesCount() {
    assertEquals(4, MatchMode.values().length);
  }
}
