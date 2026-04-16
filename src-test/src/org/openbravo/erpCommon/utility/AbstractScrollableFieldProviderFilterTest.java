/*
 * Copyright (C) 2026 Openbravo SLU
 * All Rights Reserved.
 */
package org.openbravo.erpCommon.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.ServletException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.data.FieldProvider;
import org.openbravo.data.ScrollableFieldProvider;

/**
 * Tests for {@link AbstractScrollableFieldProviderFilter}.
 */
@SuppressWarnings({"java:S120"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class AbstractScrollableFieldProviderFilterTest {

  @Mock
  private ScrollableFieldProvider mockInput;

  @Mock
  private FieldProvider mockFieldProvider;

  private AbstractScrollableFieldProviderFilter filter;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    filter = new AbstractScrollableFieldProviderFilter(mockInput);
  }
  /**
   * Constructor sets input.
   * @throws Exception if an error occurs
   */

  @Test
  public void testConstructorSetsInput() throws Exception {
    java.lang.reflect.Field inputField = AbstractScrollableFieldProviderFilter.class
        .getDeclaredField("input");
    inputField.setAccessible(true);
    assertEquals(mockInput, inputField.get(filter));
  }
  /** Has data delegates to input. */

  @Test
  public void testHasDataDelegatesToInput() {
    when(mockInput.hasData()).thenReturn(true);
    assertTrue(filter.hasData());
    verify(mockInput).hasData();
  }
  /** Has data returns false when input has no data. */

  @Test
  public void testHasDataReturnsFalseWhenInputHasNoData() {
    when(mockInput.hasData()).thenReturn(false);
    assertFalse(filter.hasData());
  }
  /**
   * Next delegates to input.
   * @throws ServletException if an error occurs
   */

  @Test
  public void testNextDelegatesToInput() throws ServletException {
    when(mockInput.next()).thenReturn(true);
    assertTrue(filter.next());
    verify(mockInput).next();
  }
  /**
   * Next returns false when input exhausted.
   * @throws ServletException if an error occurs
   */

  @Test
  public void testNextReturnsFalseWhenInputExhausted() throws ServletException {
    when(mockInput.next()).thenReturn(false);
    assertFalse(filter.next());
  }
  /**
   * Get delegates to input.
   * @throws ServletException if an error occurs
   */

  @Test
  public void testGetDelegatesToInput() throws ServletException {
    when(mockInput.get()).thenReturn(mockFieldProvider);
    FieldProvider result = filter.get();
    assertNotNull(result);
    assertEquals(mockFieldProvider, result);
    verify(mockInput).get();
  }
  /** Close delegates to input. */

  @Test
  public void testCloseDelegatesToInput() {
    filter.close();
    verify(mockInput).close();
  }
  /**
   * Next called multiple times.
   * @throws ServletException if an error occurs
   */

  @Test
  public void testNextCalledMultipleTimes() throws ServletException {
    when(mockInput.next()).thenReturn(true, true, false);

    assertTrue(filter.next());
    assertTrue(filter.next());
    assertFalse(filter.next());
  }
}
