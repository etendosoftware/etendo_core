package org.openbravo.base.secureApp;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the {@link VariablesHistory} class.
 * Verifies the behavior of session value management, history index management,
 * and accessor methods under various scenarios.
 */
@ExtendWith(MockitoExtension.class)
public class VariablesHistoryTest {
  protected static final String HISTORY_CURRENT = "REQHISTORY.CURRENT";
  protected static final String LANGUAGE = "en_US";
  protected static final String DB_SESSION = "12345";
  protected static final String TEST_ATTRIBUTE = "TEST_ATTRIBUTE";
  protected static final String TEST_VALUE = "test_value";
  protected static final String TEST_ATTRIBUTE_LOWERCASE = "test_attribute";
  protected static final String DEFAULT_PATH = "/default";

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpSession session;

  private VariablesHistory variablesHistory;

  /**
   * Sets up the test environment before each test.
   * Initializes the mocked request and session attributes.
   */
  @BeforeEach
  public void setUp() {
    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute(anyString())).thenReturn(null);

    when(session.getAttribute(HISTORY_CURRENT)).thenReturn("0");
    when(session.getAttribute("#AD_ROLE_ID")).thenReturn("100");
    when(session.getAttribute("#AD_LANGUAGE")).thenReturn(LANGUAGE);
    when(session.getAttribute("#AD_SESSION_ID")).thenReturn(DB_SESSION);
  }

  /**
   * Tests related to the constructor of the {@link VariablesHistory} class.
   */
  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    /**
     * Verifies that the constructor initializes the instance with session values.
     */
    @Test
    @DisplayName("Should initialize with session values")
    public void testConstructor() {
      variablesHistory = new VariablesHistory(request);

      assertEquals("0", variablesHistory.getCurrentHistoryIndex());
      assertEquals("100", variablesHistory.getRole());
      assertEquals(LANGUAGE, variablesHistory.getLanguage());
      assertEquals(DB_SESSION, variablesHistory.getDBSession());
    }

  }

  /**
   * Tests related to history index management.
   */
  @Nested
  @DisplayName("History Index Management Tests")
  class HistoryIndexTests {

    /**
     * Initializes the {@link VariablesHistory} instance before each test.
     */
    @BeforeEach
    public void initHistory() {
      variablesHistory = new VariablesHistory(request);
    }

    /**
     * Verifies that the current history index is retrieved properly.
     */
    @Test
    @DisplayName("Should get current history index properly")
    public void testGetCurrentHistoryIndex() {

      assertEquals("0", variablesHistory.getCurrentHistoryIndex());

      when(session.getAttribute(HISTORY_CURRENT)).thenReturn("15");
      variablesHistory = new VariablesHistory(request);

      assertEquals("5", variablesHistory.getCurrentHistoryIndex());
    }

    /**
     * Verifies that the history index is incremented correctly.
     */
    @Test
    @DisplayName("Should increment history index")
    public void testUpCurrentHistoryIndex() {
      variablesHistory.upCurrentHistoryIndex();

      ArgumentCaptor<String> attributeCaptor = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

      verify(session).setAttribute(attributeCaptor.capture(), valueCaptor.capture());

      assertEquals(HISTORY_CURRENT, attributeCaptor.getValue());
      assertEquals("1", valueCaptor.getValue());
    }

    /**
     * Verifies that the history index is decremented correctly.
     */
    @Test
    @DisplayName("Should decrement history index")
    public void testDownCurrentHistoryIndex() {
      when(session.getAttribute(HISTORY_CURRENT)).thenReturn("5");
      variablesHistory = new VariablesHistory(request);

      variablesHistory.downCurrentHistoryIndex();

      ArgumentCaptor<String> attributeCaptor = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

      verify(session).setAttribute(attributeCaptor.capture(), valueCaptor.capture());

      assertEquals(HISTORY_CURRENT, attributeCaptor.getValue());
      assertEquals("4", valueCaptor.getValue());
    }

    /**
     * Verifies that the history index handles wraparound correctly.
     */
    @Test
    @DisplayName("Should handle history wraparound in getCurrentHistoryIndex")
    public void testGetCurrentHistoryIndexWithWraparound() {
      when(session.getAttribute(HISTORY_CURRENT)).thenReturn("25");
      variablesHistory = new VariablesHistory(request);

      assertEquals("5", variablesHistory.getCurrentHistoryIndex()); // 25 % 10 = 5
    }
  }

  /**
   * Tests related to session value management.
   */
  @Nested
  @DisplayName("Session Value Management Tests")
  class SessionValueTests {

    /**
     * Initializes the {@link VariablesHistory} instance before each test.
     */
    @BeforeEach
    public void initVariablesHistory() {
      variablesHistory = new VariablesHistory(request);
    }

    /**
     * Verifies that session values are retrieved correctly with and without defaults.
     */
    @Test
    @DisplayName("Should get session value with default")
    public void testGetSessionValue() {
      when(session.getAttribute(TEST_ATTRIBUTE)).thenReturn(TEST_VALUE);

      assertEquals(TEST_VALUE, variablesHistory.getSessionValue(TEST_ATTRIBUTE_LOWERCASE));
      assertEquals(TEST_VALUE, variablesHistory.getSessionValue(TEST_ATTRIBUTE_LOWERCASE, "default"));

      assertEquals("default_value", variablesHistory.getSessionValue("nonexistent", "default_value"));

      assertEquals("", variablesHistory.getSessionValue("nonexistent"));
    }

    /**
     * Verifies that session values are set correctly.
     */
    @Test
    @DisplayName("Should set session value")
    public void testSetSessionValue() {
      variablesHistory.setSessionValue(TEST_ATTRIBUTE_LOWERCASE, TEST_VALUE);

      verify(session).setAttribute(TEST_ATTRIBUTE, TEST_VALUE);
    }

    /**
     * Verifies that exceptions during session value setting are handled gracefully.
     */
    @Test
    @DisplayName("Should handle exception when setting session value")
    public void testSetSessionValueWithException() {
      doThrow(new RuntimeException("Session error")).when(session).setAttribute(anyString(), anyString());

      assertDoesNotThrow(() -> variablesHistory.setSessionValue(TEST_ATTRIBUTE_LOWERCASE, TEST_VALUE));
    }

    /**
     * Verifies that session values are removed correctly.
     */
    @Test
    @DisplayName("Should remove session value")
    public void testRemoveSessionValue() {
      variablesHistory.removeSessionValue(TEST_ATTRIBUTE_LOWERCASE);

      verify(session).removeAttribute(TEST_ATTRIBUTE);
    }

    /**
     * Verifies that exceptions during session value removal are handled gracefully.
     */
    @Test
    @DisplayName("Should handle exception when removing session value")
    public void testRemoveSessionValueWithException() {
      doThrow(new RuntimeException("Session error")).when(session).removeAttribute(anyString());

      assertDoesNotThrow(() -> variablesHistory.removeSessionValue(TEST_ATTRIBUTE_LOWERCASE));
    }
  }

  /**
   * Tests related to current servlet information retrieval.
   */
  @Nested
  @DisplayName("Current Servlet Information Tests")
  class ServletInfoTests {

    /**
     * Initializes the {@link VariablesHistory} instance before each test.
     */
    @BeforeEach
    public void initVariablesHistory() {
      variablesHistory = new VariablesHistory(request);
    }

    /**
     * Verifies that the current servlet path is retrieved correctly.
     */
    @Test
    @DisplayName("Should get current servlet path")
    public void testGetCurrentServletPath() {
      when(session.getAttribute("REQHISTORY.PATH0")).thenReturn("/module/servlet");

      assertEquals("/module/servlet", variablesHistory.getCurrentServletPath(DEFAULT_PATH));

      when(session.getAttribute("REQHISTORY.PATH0")).thenReturn(null);
      assertEquals(DEFAULT_PATH, variablesHistory.getCurrentServletPath(DEFAULT_PATH));
    }

    /**
     * Verifies that the current servlet command is retrieved correctly.
     */
    @Test
    @DisplayName("Should get current servlet command")
    public void testGetCurrentServletCommand() {
      when(session.getAttribute("REQHISTORY.COMMAND0")).thenReturn("CREATE");

      assertEquals("CREATE", variablesHistory.getCurrentServletCommand());

      when(session.getAttribute("REQHISTORY.COMMAND0")).thenReturn(null);
      assertEquals("DEFAULT", variablesHistory.getCurrentServletCommand());
    }
  }

  /**
   * Tests related to accessor methods for role, language, and DB session.
   */
  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorTests {

    /**
     * Initializes the {@link VariablesHistory} instance before each test.
     */
    @BeforeEach
    public void initVariablesHistory() {
      variablesHistory = new VariablesHistory(request);
    }

    /**
     * Verifies that role, language, and DB session values are retrieved correctly.
     */
    @Test
    @DisplayName("Should get role, language and DB session properly")
    public void testAccessorMethods() {
      assertEquals("100", variablesHistory.getRole());
      assertEquals(LANGUAGE, variablesHistory.getLanguage());
      assertEquals(DB_SESSION, variablesHistory.getDBSession());
    }
  }
}
