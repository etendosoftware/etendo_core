package org.openbravo.erpCommon.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.database.ConnectionProvider;

/**
 * Tests for {@link BasicUtility}.
 */
@SuppressWarnings({"java:S120", "java:S112"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class BasicUtilityTest {

  private static final String EN_US = "en_US";
  private static final String ENGLISH_MESSAGE = "English message";
  private static final String COL_CODE = "COL_CODE";
  private static final String UNKNOWN = "UNKNOWN";

  @Mock
  private ConnectionProvider mockConn;

  private MockedStatic<MessageBDData> messageBDDataStatic;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    messageBDDataStatic = mockStatic(MessageBDData.class);
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (messageBDDataStatic != null) {
      messageBDDataStatic.close();
    }
  }

  // --- messageBD tests ---
  /**
   * Message bd returns translated message.
   * @throws Exception if an error occurs
   */
  @Test
  public void testMessageBDReturnsTranslatedMessage() throws Exception {
    messageBDDataStatic.when(() -> MessageBDData.message(mockConn, EN_US, "MY_CODE"))
        .thenReturn("My translated message");

    String result = BasicUtility.messageBD(mockConn, "MY_CODE", EN_US);
    assertEquals("My translated message", result);
  }
  /**
   * Message bd defaults to en us when language null.
   * @throws Exception if an error occurs
   */

  @Test
  public void testMessageBDDefaultsToEnUSWhenLanguageNull() throws Exception {
    messageBDDataStatic.when(() -> MessageBDData.message(mockConn, EN_US, "CODE"))
        .thenReturn(ENGLISH_MESSAGE);

    String result = BasicUtility.messageBD(mockConn, "CODE", null);
    assertEquals(ENGLISH_MESSAGE, result);
  }
  /**
   * Message bd defaults to en us when language empty.
   * @throws Exception if an error occurs
   */

  @Test
  public void testMessageBDDefaultsToEnUSWhenLanguageEmpty() throws Exception {
    messageBDDataStatic.when(() -> MessageBDData.message(mockConn, EN_US, "CODE"))
        .thenReturn(ENGLISH_MESSAGE);

    String result = BasicUtility.messageBD(mockConn, "CODE", "");
    assertEquals(ENGLISH_MESSAGE, result);
  }
  /**
   * Message bd falls back to column name.
   * @throws Exception if an error occurs
   */

  @Test
  public void testMessageBDFallsBackToColumnName() throws Exception {
    messageBDDataStatic.when(() -> MessageBDData.message(mockConn, EN_US, COL_CODE))
        .thenReturn("");
    messageBDDataStatic.when(() -> MessageBDData.columnname(mockConn, EN_US, COL_CODE))
        .thenReturn("Column Name");

    String result = BasicUtility.messageBD(mockConn, COL_CODE, EN_US);
    assertEquals("Column Name", result);
  }
  /**
   * Message bd returns code when no translation found.
   * @throws Exception if an error occurs
   */

  @Test
  public void testMessageBDReturnsCodeWhenNoTranslationFound() throws Exception {
    messageBDDataStatic.when(() -> MessageBDData.message(mockConn, EN_US, UNKNOWN))
        .thenReturn("");
    messageBDDataStatic.when(() -> MessageBDData.columnname(mockConn, EN_US, UNKNOWN))
        .thenReturn("");

    String result = BasicUtility.messageBD(mockConn, UNKNOWN, EN_US);
    assertEquals(UNKNOWN, result);
  }
  /**
   * Message bd escapes newlines and quotes.
   * @throws Exception if an error occurs
   */

  @Test
  public void testMessageBDEscapesNewlinesAndQuotes() throws Exception {
    messageBDDataStatic.when(() -> MessageBDData.message(mockConn, EN_US, "CODE"))
        .thenReturn("line1\nline2 \"quoted\"");

    String result = BasicUtility.messageBD(mockConn, "CODE", EN_US, true);
    assertEquals("line1\\nline2 &quot;quoted&quot;", result);
  }
  /**
   * Message bd no escape when false.
   * @throws Exception if an error occurs
   */

  @Test
  public void testMessageBDNoEscapeWhenFalse() throws Exception {
    messageBDDataStatic.when(() -> MessageBDData.message(mockConn, EN_US, "CODE"))
        .thenReturn("line1\nline2 \"quoted\"");

    String result = BasicUtility.messageBD(mockConn, "CODE", EN_US, false);
    assertEquals("line1\nline2 \"quoted\"", result);
  }

  // --- formatMessageBDToHtml tests ---
  /** Format message bd to html escapes ampersand. */

  @Test
  public void testFormatMessageBDToHtmlEscapesAmpersand() {
    String result = BasicUtility.formatMessageBDToHtml("A & B");
    assertEquals("A &amp; B", result);
  }
  /** Format message bd to html escapes less than. */

  @Test
  public void testFormatMessageBDToHtmlEscapesLessThan() {
    String result = BasicUtility.formatMessageBDToHtml("A < B");
    assertEquals("A &lt; B", result);
  }
  /** Format message bd to html escapes greater than. */

  @Test
  public void testFormatMessageBDToHtmlEscapesGreaterThan() {
    String result = BasicUtility.formatMessageBDToHtml("A > B");
    assertEquals("A &gt; B", result);
  }
  /** Format message bd to html converts newline to break. */

  @Test
  public void testFormatMessageBDToHtmlConvertsNewlineToBreak() {
    // formatMessageBDToHtml first reverts \\n to \n, then converts \n to <br/>
    String result = BasicUtility.formatMessageBDToHtml("line1\\nline2");
    assertEquals("line1<br/>line2", result);
  }
  /** Format message bd to html converts carriage return to space. */

  @Test
  public void testFormatMessageBDToHtmlConvertsCarriageReturnToSpace() {
    String result = BasicUtility.formatMessageBDToHtml("line1\rline2");
    assertEquals("line1 line2", result);
  }
  /** Format message bd to html converts registered trademark. */

  @Test
  public void testFormatMessageBDToHtmlConvertsRegisteredTrademark() {
    String result = BasicUtility.formatMessageBDToHtml("Brand\u00AE");
    assertEquals("Brand&reg;", result);
  }
  /** Format message bd to html preserves cdata. */

  @Test
  public void testFormatMessageBDToHtmlPreservesCDATA() {
    String result = BasicUtility.formatMessageBDToHtml("<![CDATA[content]]>");
    assertEquals("<![CDATA[content]]>", result);
  }
  /** Format message bd to html reverts quot encoding. */

  @Test
  public void testFormatMessageBDToHtmlRevertsQuotEncoding() {
    String result = BasicUtility.formatMessageBDToHtml("say &quot hello");
    assertEquals("say &quot; hello", result);
  }

  // --- wikifiedName tests ---
  /**
   * Wikified name single word.
   * @throws FileNotFoundException if an error occurs
   */

  @Test
  public void testWikifiedNameSingleWord() throws FileNotFoundException {
    String result = BasicUtility.wikifiedName("hello");
    assertEquals("Hello", result);
  }
  /**
   * Wikified name multiple words.
   * @throws FileNotFoundException if an error occurs
   */

  @Test
  public void testWikifiedNameMultipleWords() throws FileNotFoundException {
    String result = BasicUtility.wikifiedName("hello world test");
    assertEquals("Hello_world_test", result);
  }
  /**
   * Wikified name capitalizes first char.
   * @throws FileNotFoundException if an error occurs
   */

  @Test
  public void testWikifiedNameCapitalizesFirstChar() throws FileNotFoundException {
    String result = BasicUtility.wikifiedName("myName");
    assertEquals("MyName", result);
  }
  /**
   * Wikified name returns null for null.
   * @throws FileNotFoundException if an error occurs
   */

  @Test
  public void testWikifiedNameReturnsNullForNull() throws FileNotFoundException {
    String result = BasicUtility.wikifiedName(null);
    assertEquals(null, result);
  }
  /**
   * Wikified name returns empty for empty.
   * @throws FileNotFoundException if an error occurs
   */

  @Test
  public void testWikifiedNameReturnsEmptyForEmpty() throws FileNotFoundException {
    String result = BasicUtility.wikifiedName("");
    assertEquals("", result);
  }
  /**
   * Wikified name trims whitespace.
   * @throws FileNotFoundException if an error occurs
   */

  @Test
  public void testWikifiedNameTrimsWhitespace() throws FileNotFoundException {
    String result = BasicUtility.wikifiedName("  hello  ");
    assertEquals("Hello", result);
  }
  /**
   * Wikified name returns empty for only whitespace.
   * @throws FileNotFoundException if an error occurs
   */

  @Test
  public void testWikifiedNameReturnsEmptyForOnlyWhitespace() throws FileNotFoundException {
    String result = BasicUtility.wikifiedName("   ");
    assertEquals("", result);
  }
}
