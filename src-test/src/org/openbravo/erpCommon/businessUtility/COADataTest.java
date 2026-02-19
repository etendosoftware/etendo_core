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
@RunWith(MockitoJUnitRunner.class)
public class COADataTest {

  private COAData coaData;

  @Before
  public void setUp() {
    coaData = new COAData();
  }

  @Test
  public void testGetFieldReturnsNullForNullFieldName() {
    assertNull(coaData.getField(null));
  }

  @Test
  public void testGetFieldReturnsNullForUnknownFieldName() {
    assertNull(coaData.getField("nonExistentField"));
  }

  @Test
  public void testGetFieldAccountValueByCamelCase() {
    coaData.setAccountValue("1000");
    assertEquals("1000", coaData.getField("accountValue"));
  }

  @Test
  public void testGetFieldAccountValueByUpperCase() {
    coaData.setAccountValue("1000");
    assertEquals("1000", coaData.getField("ACCOUNT_VALUE"));
  }

  @Test
  public void testGetFieldAccountName() {
    coaData.setAccountName("Cash");
    assertEquals("Cash", coaData.getField("accountName"));
  }

  @Test
  public void testGetFieldAccountDescription() {
    coaData.setAccountDescription("Cash account");
    assertEquals("Cash account", coaData.getField("ACCOUNT_DESCRIPTION"));
  }

  @Test
  public void testGetFieldAccountType() {
    coaData.setAccountType("A");
    assertEquals("A", coaData.getField("accountType"));
  }

  @Test
  public void testGetFieldAccountSign() {
    coaData.setAccountSign("D");
    assertEquals("D", coaData.getField("ACCOUNT_SIGN"));
  }

  @Test
  public void testGetFieldAccountDocument() {
    coaData.setAccountDocument("Y");
    assertEquals("Y", coaData.getField("accountDocument"));
  }

  @Test
  public void testGetFieldAccountSummary() {
    coaData.setAccountSummary("N");
    assertEquals("N", coaData.getField("ACCOUNT_SUMMARY"));
  }

  @Test
  public void testGetFieldDefaultAccount() {
    coaData.setDefaultAccount("DEFAULT_ACCT");
    assertEquals("DEFAULT_ACCT", coaData.getField("defaultAccount"));
  }

  @Test
  public void testGetFieldAccountParent() {
    coaData.setAccountParent("PARENT001");
    assertEquals("PARENT001", coaData.getField("accountParent"));
  }

  @Test
  public void testGetFieldElementLevel() {
    coaData.setElementLevel("S");
    assertEquals("S", coaData.getField("ELEMENT_LEVEL"));
  }

  @Test
  public void testGetFieldOperandsTrimsWhitespace() {
    coaData.setOperands("  +1000  ");
    assertEquals("+1000", coaData.getField("operands"));
  }

  @Test
  public void testGetFieldBalanceSheet() {
    coaData.setBalanceSheet("BS001");
    assertEquals("BS001", coaData.getField("balanceSheet"));
  }

  @Test
  public void testGetFieldShowValueCond() {
    coaData.setShowValueCond("Y");
    assertEquals("Y", coaData.getField("showValueCond"));
  }

  @Test
  public void testGetFieldTitleNode() {
    coaData.setTitleNode("Root");
    assertEquals("Root", coaData.getField("titleNode"));
  }

  @Test
  public void testLineSeparatorFormatedReturnsNullForNull() {
    assertNull(coaData.lineSeparatorFormated(null));
  }

  @Test
  public void testLineSeparatorFormatedReturnsNullForEmptyString() {
    assertNull(coaData.lineSeparatorFormated(""));
  }

  @Test
  public void testLineSeparatorFormatedParsesCSVLine() {
    String csvLine = "1000,Cash,Cash Account,A,D,Y,N,DEFAULT,PARENT,S,+100,ShowCond,Title";

    FieldProvider result = coaData.lineSeparatorFormated(csvLine);

    assertNotNull(result);
    assertEquals("1000", result.getField("accountValue"));
    assertEquals("Cash", result.getField("accountName"));
    assertEquals("Cash Account", result.getField("accountDescription"));
    assertEquals("A", result.getField("accountType"));
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

  @Test
  public void testLineSeparatorFormatedHandlesQuotedValues() {
    String csvLine = "\"1000\",\"Cash Account\",\"Description, with comma\"";

    FieldProvider result = coaData.lineSeparatorFormated(csvLine);

    assertNotNull(result);
    assertEquals("1000", result.getField("accountValue"));
    assertEquals("Cash Account", result.getField("accountName"));
  }

  @Test
  public void testLineSeparatorFormatedHandlesPartialLine() {
    String csvLine = "1000,Cash";

    FieldProvider result = coaData.lineSeparatorFormated(csvLine);

    assertNotNull(result);
    assertEquals("1000", result.getField("accountValue"));
    assertEquals("Cash", result.getField("accountName"));
  }

  @Test
  public void testLineFixedSizeReturnsNull() {
    assertNull(coaData.lineFixedSize("anything"));
  }

  @Test
  public void testGetFieldProfitAndLoss() {
    coaData.setProfitAndLoss("PL001");
    assertEquals("PL001", coaData.getField("profitAndLoss"));
  }

  @Test
  public void testGetFieldCashFlow() {
    coaData.setCashFlow("CF001");
    assertEquals("CF001", coaData.getField("cashFlow"));
  }

  @Test
  public void testDefaultFieldValuesAreEmpty() {
    assertEquals("", coaData.getField("accountValue"));
    assertEquals("", coaData.getField("accountName"));
    assertEquals("", coaData.getField("accountType"));
  }
}
