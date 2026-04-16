package org.openbravo.common.actionhandler.createlinesfromprocess;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.model.common.order.OrderLine;

/**
 * Tests for {@link CreateInvoiceLinesFromOrderLines}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CreateInvoiceLinesFromOrderLinesTest {

  private CreateInvoiceLinesFromOrderLines handler;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    handler = objenesis.newInstance(CreateInvoiceLinesFromOrderLines.class);
  }
  /** Get from class returns order line class. */

  @Test
  public void testGetFromClassReturnsOrderLineClass() {
    assertEquals(OrderLine.class, handler.getFromClass());
  }
}
