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
@RunWith(MockitoJUnitRunner.Silent.class)
public class BasicUtilityTest {

  @Mock
  private ConnectionProvider mockConn;

  private MockedStatic<MessageBDData> messageBDDataStatic;

  @Before
  public void setUp() {
    messageBDDataStatic = mockStatic(MessageBDData.class);
  }

  @After
  public void tearDown() {
    if (messageBDDataStatic != null) {
      messageBDDataStatic.close();
    }
  }

  // --- messageBD tests ---

  @Test
  public void testMessageBDReturnsTranslatedMessage() throws Exception {
    messageBDDataStatic.when(() -> MessageBDData.message(mockConn, "en_US", "MY_CODE"))
        .thenReturn("My translated message");

    String result = BasicUtility.messageBD(mockConn, "MY_CODE", "en_US");
    assertEquals("My translated message", result);
  }

  @Test
  public void testMessageBDDefaultsToEnUSWhenLanguageNull() throws Exception {
    messageBDDataStatic.when(() -> MessageBDData.message(mockConn, "en_US", "CODE"))
        .thenReturn("English message");

    String result = BasicUtility.messageBD(mockConn, "CODE", null);
    assertEquals("English message", result);
  }

  @Test
  public void testMessageBDDefaultsToEnUSWhenLanguageEmpty() throws Exception {
    messageBDDataStatic.when(() -> MessageBDData.message(mockConn, "en_US", "CODE"))
        .thenReturn("English message");

    String result = BasicUtility.messageBD(mockConn, "CODE", "");
    assertEquals("English message", result);
  }

  @Test
  public void testMessageBDFallsBackToColumnName() throws Exception {
    messageBDDataStatic.when(() -> MessageBDData.message(mockConn, "en_US", "COL_CODE"))
        .thenReturn("");
    messageBDDataStatic.when(() -> MessageBDData.columnname(mockConn, "en_US", "COL_CODE"))
        .thenReturn("Column Name");

    String result = BasicUtility.messageBD(mockConn, "COL_CODE", "en_US");
    assertEquals("Column Name", result);
  }

  @Test
  public void testMessageBDReturnsCodeWhenNoTranslationFound() throws Exception {
    messageBDDataStatic.when(() -> MessageBDData.message(mockConn, "en_US", "UNKNOWN"))
        .thenReturn("");
    messageBDDataStatic.when(() -> MessageBDData.columnname(mockConn, "en_US", "UNKNOWN"))
        .thenReturn("");

    String result = BasicUtility.messageBD(mockConn, "UNKNOWN", "en_US");
    assertEquals("UNKNOWN", result);
  }

  @Test
  public void testMessageBDEscapesNewlinesAndQuotes() throws Exception {
    messageBDDataStatic.when(() -> MessageBDData.message(mockConn, "en_US", "CODE"))
        .thenReturn("line1\nline2 \"quoted\"");

    String result = BasicUtility.messageBD(mockConn, "CODE", "en_US", true);
    assertEquals("line1\\nline2 &quot;quoted&quot;", result);
  }

  @Test
  public void testMessageBDNoEscapeWhenFalse() throws Exception {
    messageBDDataStatic.when(() -> MessageBDData.message(mockConn, "en_US", "CODE"))
        .thenReturn("line1\nline2 \"quoted\"");

    String result = BasicUtility.messageBD(mockConn, "CODE", "en_US", false);
    assertEquals("line1\nline2 \"quoted\"", result);
  }

  // --- formatMessageBDToHtml tests ---

  @Test
  public void testFormatMessageBDToHtmlEscapesAmpersand() {
    String result = BasicUtility.formatMessageBDToHtml("A & B");
    assertEquals("A &amp; B", result);
  }

  @Test
  public void testFormatMessageBDToHtmlEscapesLessThan() {
    String result = BasicUtility.formatMessageBDToHtml("A < B");
    assertEquals("A &lt; B", result);
  }

  @Test
  public void testFormatMessageBDToHtmlEscapesGreaterThan() {
    String result = BasicUtility.formatMessageBDToHtml("A > B");
    assertEquals("A &gt; B", result);
  }

  @Test
  public void testFormatMessageBDToHtmlConvertsNewlineToBreak() {
    // formatMessageBDToHtml first reverts \\n to \n, then converts \n to <br/>
    String result = BasicUtility.formatMessageBDToHtml("line1\\nline2");
    assertEquals("line1<br/>line2", result);
  }

  @Test
  public void testFormatMessageBDToHtmlConvertsCarriageReturnToSpace() {
    String result = BasicUtility.formatMessageBDToHtml("line1\rline2");
    assertEquals("line1 line2", result);
  }

  @Test
  public void testFormatMessageBDToHtmlConvertsRegisteredTrademark() {
    String result = BasicUtility.formatMessageBDToHtml("Brand\u00AE");
    assertEquals("Brand&reg;", result);
  }

  @Test
  public void testFormatMessageBDToHtmlPreservesCDATA() {
    String result = BasicUtility.formatMessageBDToHtml("<![CDATA[content]]>");
    assertEquals("<![CDATA[content]]>", result);
  }

  @Test
  public void testFormatMessageBDToHtmlRevertsQuotEncoding() {
    // Input has &quot which gets reverted to " then re-escaped to &quot;
    String result = BasicUtility.formatMessageBDToHtml("say &quot hello");
    assertEquals("say &quot; hello", result);
  }

  // --- wikifiedName tests ---

  @Test
  public void testWikifiedNameSingleWord() throws FileNotFoundException {
    String result = BasicUtility.wikifiedName("hello");
    assertEquals("Hello", result);
  }

  @Test
  public void testWikifiedNameMultipleWords() throws FileNotFoundException {
    String result = BasicUtility.wikifiedName("hello world test");
    assertEquals("Hello_world_test", result);
  }

  @Test
  public void testWikifiedNameCapitalizesFirstChar() throws FileNotFoundException {
    String result = BasicUtility.wikifiedName("myName");
    assertEquals("MyName", result);
  }

  @Test
  public void testWikifiedNameReturnsNullForNull() throws FileNotFoundException {
    String result = BasicUtility.wikifiedName(null);
    assertEquals(null, result);
  }

  @Test
  public void testWikifiedNameReturnsEmptyForEmpty() throws FileNotFoundException {
    String result = BasicUtility.wikifiedName("");
    assertEquals("", result);
  }

  @Test
  public void testWikifiedNameTrimsWhitespace() throws FileNotFoundException {
    String result = BasicUtility.wikifiedName("  hello  ");
    assertEquals("Hello", result);
  }

  @Test
  public void testWikifiedNameReturnsEmptyForOnlyWhitespace() throws FileNotFoundException {
    String result = BasicUtility.wikifiedName("   ");
    assertEquals("", result);
  }
}
