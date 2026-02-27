package org.openbravo.test.base.mock;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.exceptions.base.MockitoException;

/**
 * Utilities to create static mocks in a resilient way when previous tests leave stale inline mocks.
 */
public final class MockitoStaticMockUtils {

  private MockitoStaticMockUtils() {
  }

  public static <T> MockedStatic<T> mockStaticSafely(Class<T> classToMock) {
    try {
      return Mockito.mockStatic(classToMock);
    } catch (MockitoException e) {
      if (e.getMessage() != null
          && e.getMessage().contains("static mocking is already registered in the current thread")) {
        Mockito.framework().clearInlineMocks();
        return Mockito.mockStatic(classToMock);
      }
      throw e;
    }
  }
}
