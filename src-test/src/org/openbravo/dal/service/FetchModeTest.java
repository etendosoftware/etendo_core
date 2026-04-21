package org.openbravo.dal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

public class FetchModeTest {

  @Test
  public void testValuesCount() {
    assertEquals(3, FetchMode.values().length);
  }

  @Test
  public void testGetDefaultReturnsDefault() {
    assertSame(FetchMode.DEFAULT, FetchMode.getDefault());
  }

  @Test
  public void testValueOf() {
    assertEquals(FetchMode.DEFAULT, FetchMode.valueOf("DEFAULT"));
    assertEquals(FetchMode.JOIN, FetchMode.valueOf("JOIN"));
    assertEquals(FetchMode.SELECT, FetchMode.valueOf("SELECT"));
  }
}
