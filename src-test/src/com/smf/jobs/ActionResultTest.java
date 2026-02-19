package com.smf.jobs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.client.application.process.ResponseActionsBuilder;

/**
 * Unit tests for ActionResult.
 */
@RunWith(MockitoJUnitRunner.class)
public class ActionResultTest {

  private ActionResult actionResult;

  @Before
  public void setUp() {
    actionResult = new ActionResult();
  }

  @Test
  public void testGetOutputReturnsEmptyWhenNotSet() {
    Optional<Data> output = actionResult.getOutput();
    assertNotNull(output);
    assertFalse(output.isPresent());
  }

  @Test
  public void testSetAndGetOutput() {
    Data mockData = mock(Data.class);
    actionResult.setOutput(mockData);

    Optional<Data> output = actionResult.getOutput();
    assertTrue(output.isPresent());
    assertSame(mockData, output.get());
  }

  @Test
  public void testGetResponseActionsBuilderReturnsEmptyWhenNotSet() {
    Optional<ResponseActionsBuilder> builder = actionResult.getResponseActionsBuilder();
    assertNotNull(builder);
    assertFalse(builder.isPresent());
  }

  @Test
  public void testSetAndGetResponseActionsBuilder() {
    ResponseActionsBuilder mockBuilder = mock(ResponseActionsBuilder.class);
    actionResult.setResponseActionsBuilder(mockBuilder);

    Optional<ResponseActionsBuilder> builder = actionResult.getResponseActionsBuilder();
    assertTrue(builder.isPresent());
    assertSame(mockBuilder, builder.get());
  }

  @Test
  public void testGetResponseActionsReturnsEmptyWhenBuilderNotSet() {
    assertFalse(actionResult.getResponseActions().isPresent());
  }

  @Test
  public void testInheritedTypeAndMessage() {
    actionResult.setType(Result.Type.SUCCESS);
    actionResult.setMessage("Operation completed");

    assertEquals(Result.Type.SUCCESS, actionResult.getType());
    assertEquals("Operation completed", actionResult.getMessage());
  }

  @Test
  public void testSetOutputWithNull() {
    actionResult.setOutput(null);
    assertFalse(actionResult.getOutput().isPresent());
  }

  @Test
  public void testSetResponseActionsBuilderWithNull() {
    actionResult.setResponseActionsBuilder(null);
    assertFalse(actionResult.getResponseActionsBuilder().isPresent());
  }

  @Test
  public void testAllResultTypes() {
    for (Result.Type type : Result.Type.values()) {
      actionResult.setType(type);
      assertEquals(type, actionResult.getType());
    }
  }

  @Test
  public void testSetOutputOverwrite() {
    Data firstData = mock(Data.class);
    Data secondData = mock(Data.class);

    actionResult.setOutput(firstData);
    assertSame(firstData, actionResult.getOutput().get());

    actionResult.setOutput(secondData);
    assertSame(secondData, actionResult.getOutput().get());
  }
}
