package com.smf.jobs.defaults.invoices;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.model.common.order.Order;

import com.smf.jobs.ActionResult;
import com.smf.jobs.Result;

@RunWith(MockitoJUnitRunner.class)
public class CreateFromOrderTest {

  @Spy
  @InjectMocks
  private CreateFromOrder createFromOrder;

  
  @Before
  public void setUp() {
  }

  @Test
  public void testActionWithRefreshButton() throws Exception {
    JSONObject parameters = new JSONObject();
    parameters.put("_buttonValue", "REFRESH");
    MutableBoolean isStopped = new MutableBoolean(false);

    ActionResult result = createFromOrder.action(parameters, isStopped);

    assertNotNull("Result should not be null", result);
    assertEquals("Should return success type", Result.Type.SUCCESS, result.getType());
  }

  @Test
  public void testGetInputClass() {
    assertEquals("Should return Order.class", Order.class, createFromOrder.getInputClass());
  }
}
