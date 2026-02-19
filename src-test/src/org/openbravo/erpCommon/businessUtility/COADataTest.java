package org.openbravo.erpCommon.businessUtility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.data.FieldProvider;

/**
 * Tests for {@link COAData}.
 */
@SuppressWarnings({"java:S120"})
@RunWith(MockitoJUnitRunner.class)
public class COADataTest {

  private static final String ACCOUNT_VALUE = "accountValue";
  private static final String ACCOUNT_NAME = "accountName";
  private static final String ACCOUNT_TYPE = "accountType";

  private COAData coaData;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    coaData = new COAData();
  }
  /** Get field returns null for null field name. */

  @Test
  public void testGetFieldReturnsNullForNullFieldName() {
    assertNull(coaData.getField(null));
  }
  /** Get field returns null for unknown field name. */

  @Test
  public void testGetFieldReturnsNullForUnknownFieldName() {
    assertNull(coaData.getField("nonExistentField"));
  }
  /** Get field account value by camel case. */

  @Test
  public void testGetFieldAccountValueByCamelCase() {
    coaData.setAccountValue("1000");
    assertEquals("1000", coaData.getField(ACCOUNT_VALUE));
  }
  /** Get field account value by upper case. */

  @Test
  public void testGetFieldAccountValueByUpperCase() {
    coaData.setAccountValue("1000");
    assertEquals("1000", coaData.getField("ACCOUNT_VALUE"));
  }
  /** Get field account name. */

  @Test
  public void testGetFieldAccountName() {
    coaData.setAccountName("Cash");
    assertEquals("Cash", coaData.getField(ACCOUNT_NAME));
  }
  /** Get field account description. */

  @Test
  public void testGetFieldAccountDescription() {
    coaData.setAccountDescription("Cash account");
    assertEquals("Cash account", coaData.getField("ACCOUNT_DESCRIPTION"));
  }
  /** Get field account type. */

  @Test
  public void testGetFieldAccountType() {
    coaData.setAccountType("A");
    assertEquals("A", coaData.getField(ACCOUNT_TYPE));
  }
  /** Get field account sign. */

  @Test
  public void testGetFieldAccountSign() {
    coaData.setAccountSign("D");
    assertEquals("D", coaData.getField("ACCOUNT_SIGN"));
  }
  /** Get field account document. */

  @Test
  public void testGetFieldAccountDocument() {
    coaData.setAccountDocument("Y");
    assertEquals("Y", coaData.getField("accountDocument"));
  }
  /** Get field account summary. */

  @Test
  public void testGetFieldAccountSummary() {
    coaData.setAccountSummary("N");
    assertEquals("N", coaData.getField("ACCOUNT_SUMMARY"));
  }
  /** Get field default account. */

  @Test
  public void testGetFieldDefaultAccount() {
    coaData.setDefaultAccount("DEFAULT_ACCT");
    assertEquals("DEFAULT_ACCT", coaData.getField("defaultAccount"));
  }
  /** Get field account parent. */

  @Test
  public void testGetFieldAccountParent() {
    coaData.setAccountParent("PARENT001");
    assertEquals("PARENT001", coaData.getField("accountParent"));
  }
  /** Get field element level. */

  @Test
  public void testGetFieldElementLevel() {
    coaData.setElementLevel("S");
    assertEquals("S", coaData.getField("ELEMENT_LEVEL"));
  }
  /** Get field operands trims whitespace. */

  @Test
  public void testGetFieldOperandsTrimsWhitespace() {
    coaData.setOperands("  +1000  ");
    assertEquals("+1000", coaData.getField("operands"));
  }
  /** Get field balance sheet. */

  @Test
  public void testGetFieldBalanceSheet() {
    coaData.setBalanceSheet("BS001");
    assertEquals("BS001", coaData.getField("balanceSheet"));
  }
  /** Get field show value cond. */

  @Test
  public void testGetFieldShowValueCond() {
    coaData.setShowValueCond("Y");
    assertEquals("Y", coaData.getField("showValueCond"));
  }
  /** Get field title node. */

  @Test
  public void testGetFieldTitleNode() {
    coaData.setTitleNode("Root");
    assertEquals("Root", coaData.getField("titleNode"));
  }
  /** Line separator formated returns null for null. */

  @Test
  public void testLineSeparatorFormatedReturnsNullForNull() {
    assertNull(coaData.lineSeparatorFormated(null));
  }
  /** Line separator formated returns null for empty string. */

  @Test
  public void testLineSeparatorFormatedReturnsNullForEmptyString() {
    assertNull(coaData.lineSeparatorFormated(""));
  }
  /** Line separator formated parses csv line. */

  @Test
  public void testLineSeparatorFormatedParsesCSVLine() {
    String csvLine = "1000,Cash,Cash Account,A,D,Y,N,DEFAULT,PARENT,S,+100,ShowCond,Title";

    FieldProvider result = coaData.lineSeparatorFormated(csvLine);

    assertNotNull(result);
    assertEquals("1000", result.getField(ACCOUNT_VALUE));
    assertEquals("Cash", result.getField(ACCOUNT_NAME));
    assertEquals("Cash Account", result.getField("accountDescription"));
    assertEquals("A", result.getField(ACCOUNT_TYPE));
    assertEquals("D", result.getField("ACCOUNT_SIGN"));
    assertEquals("Y", result.getField("accountDocument"));
    assertEquals("N", result.getField("accountSummary"));
    assertEquals("DEFAULT", result.getField("defaultAccount"));
    assertEquals("PARENT", result.getField("accountParent"));
    assertEquals("S", result.getField("elementLevel"));
    assertEquals("+100", result.getField("operands"));
    assertEquals("ShowCond", result.getField("showValueCond"));
    assertEquals("Title", result.getField("titleNode"));
  }
  /** Line separator formated handles quoted values. */

  @Test
  public void testLineSeparatorFormatedHandlesQuotedValues() {
    String csvLine = "\"1000\",\"Cash Account\",\"Description, with comma\"";

    FieldProvider result = coaData.lineSeparatorFormated(csvLine);

    assertNotNull(result);
    assertEquals("1000", result.getField(ACCOUNT_VALUE));
    assertEquals("Cash Account", result.getField(ACCOUNT_NAME));
  }
  /** Line separator formated handles partial line. */

  @Test
  public void testLineSeparatorFormatedHandlesPartialLine() {
    String csvLine = "1000,Cash";

    FieldProvider result = coaData.lineSeparatorFormated(csvLine);

    assertNotNull(result);
    assertEquals("1000", result.getField(ACCOUNT_VALUE));
    assertEquals("Cash", result.getField(ACCOUNT_NAME));
  }
  /** Line fixed size returns null. */

  @Test
  public void testLineFixedSizeReturnsNull() {
    assertNull(coaData.lineFixedSize("anything"));
  }
  /** Get field profit and loss. */

  @Test
  public void testGetFieldProfitAndLoss() {
    coaData.setProfitAndLoss("PL001");
    assertEquals("PL001", coaData.getField("profitAndLoss"));
  }
  /** Get field cash flow. */

  @Test
  public void testGetFieldCashFlow() {
    coaData.setCashFlow("CF001");
    assertEquals("CF001", coaData.getField("cashFlow"));
  }
  /** Default field values are empty. */

  @Test
  public void testDefaultFieldValuesAreEmpty() {
    assertEquals("", coaData.getField(ACCOUNT_VALUE));
    assertEquals("", coaData.getField(ACCOUNT_NAME));
    assertEquals("", coaData.getField(ACCOUNT_TYPE));
  }
}
