package org.openbravo.advpaymentmngt.filterexpression;

import org.codehaus.jettison.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openbravo.client.kernel.ComponentProvider;

import javax.enterprise.inject.Instance;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MatchStatementFilterExpressionHandlerTest {

  private TestMatchStatementFilterExpressionHandler handlerUnderTest;

  @Mock
  private Instance<MatchStatementFilterExpressionHandler> matchStatementFilterExpressionHandlers;

  @Before
  public void setUp() throws NoSuchFieldException, IllegalAccessException {
    MockitoAnnotations.initMocks(this);

    handlerUnderTest = new TestMatchStatementFilterExpressionHandler();

    Field handlersField = MatchStatementFilterExpressionHandler.class.getDeclaredField("matchStatementFilterExpressionHandlers");
    handlersField.setAccessible(true);
    handlersField.set(handlerUnderTest, matchStatementFilterExpressionHandlers);
  }

  @Test
  public void testGetDefaultsHandlerReturnsNullWhenNoHandlersFound() {
    // Arrange
    String windowId = "123";
    @SuppressWarnings("unchecked")
    Instance<MatchStatementFilterExpressionHandler> mockInstance = mock(Instance.class);
    Iterator<MatchStatementFilterExpressionHandler> emptyIterator = mock(Iterator.class);
    when(emptyIterator.hasNext()).thenReturn(false);
    when(mockInstance.iterator()).thenReturn(emptyIterator);
    when(matchStatementFilterExpressionHandlers.select(any(ComponentProvider.Selector.class)))
        .thenReturn(mockInstance);

    // Act
    MatchStatementFilterExpressionHandler result = handlerUnderTest.getDefaultsHandler(windowId);

    // Assert
    assertNull(result);
  }

  @Test
  public void testGetDefaultsHandlerReturnsSingleHandlerWhenOneExists() {
    // Arrange
    String windowId = "123";
    MatchStatementFilterExpressionHandler mockHandler = mock(MatchStatementFilterExpressionHandler.class);
    when(mockHandler.getSeq()).thenReturn(10L);

    @SuppressWarnings("unchecked")
    Instance<MatchStatementFilterExpressionHandler> mockInstance = mock(Instance.class);
    Iterator<MatchStatementFilterExpressionHandler> iterator = mock(Iterator.class);
    when(iterator.hasNext()).thenReturn(true).thenReturn(false);
    when(iterator.next()).thenReturn(mockHandler);
    when(mockInstance.iterator()).thenReturn(iterator);
    when(matchStatementFilterExpressionHandlers.select(any(ComponentProvider.Selector.class)))
        .thenReturn(mockInstance);

    // Act
    MatchStatementFilterExpressionHandler result = handlerUnderTest.getDefaultsHandler(windowId);

    // Assert
    assertEquals(mockHandler, result);
  }

  @Test
  public void testGetDefaultsHandlerReturnsLowestSeqHandlerWhenMultipleExist() {
    // Arrange
    String windowId = "123";
    MatchStatementFilterExpressionHandler handler1 = mock(MatchStatementFilterExpressionHandler.class);
    MatchStatementFilterExpressionHandler handler2 = mock(MatchStatementFilterExpressionHandler.class);

    when(handler1.getSeq()).thenReturn(20L);
    when(handler2.getSeq()).thenReturn(10L);

    @SuppressWarnings("unchecked")
    Instance<MatchStatementFilterExpressionHandler> mockInstance = mock(Instance.class);
    Iterator<MatchStatementFilterExpressionHandler> iterator = mock(Iterator.class);
    when(iterator.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
    when(iterator.next()).thenReturn(handler1).thenReturn(handler2);
    when(mockInstance.iterator()).thenReturn(iterator);
    when(matchStatementFilterExpressionHandlers.select(any(ComponentProvider.Selector.class)))
        .thenReturn(mockInstance);

    // Act
    MatchStatementFilterExpressionHandler result = handlerUnderTest.getDefaultsHandler(windowId);

    // Assert
    assertEquals(handler2, result);
  }

  @Test
  public void testGetFilterExpressionReturnsDefaultValue() throws JSONException {
    // Arrange
    Map<String, String> requestMap = mock(Map.class);

    // Act
    String result = handlerUnderTest.getFilterExpression(requestMap);

    // Assert
    assertEquals("No", result);
  }

  private static class TestMatchStatementFilterExpressionHandler extends MatchStatementFilterExpressionHandler {
    @Override
    protected long getSeq() {
      return 0L;
    }
  }
}
