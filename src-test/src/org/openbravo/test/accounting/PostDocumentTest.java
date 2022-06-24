/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2015-2016 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.accounting;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Collection;

import javax.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_forms.AcctServer;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.financial.ResetAccounting;
import org.openbravo.model.financialmgmt.accounting.AccountingFact;
import org.openbravo.test.base.OBBaseTest;

/**
 * Tests cases to check taxes computation
 * 
 * 
 */
@RunWith(Parameterized.class)
public class PostDocumentTest extends OBBaseTest {
  final static private Logger log = LogManager.getLogger();
  // User Openbravo
  private final String USER_ID = "100";
  // Client QA Testing
  private static final String CLIENT_ID = "4028E6C72959682B01295A070852010D";
  // Organization Spain
  private static final String ORGANIZATION_SPAIN_ID = "357947E87C284935AD1D783CF6F099A1";
  // Organization USA
  private static final String ORGANIZATION_USA_ID = "5EFF95EB540740A3B10510D9814EFAD5";
  // Table INVOICE
  private static final String TABLE_INVOICE = "318";
  // Table GL_JOURNAL
  private static final String TABLE_GLJOURNAL = "224";
  // Accounting Schemas
  private static final String MAIN_EURO_LEGDER = "9A68A0F8D72D4580B3EC3CAA00A5E1F0";
  private static final String USA_DOLLAR_LEGDER = "432EAC71E1B8451E97C7F54718C4A06B";
  // Sales Invoice with documentNo: I/29
  private static final String INVOICE_TEST1 = "66AE9202889D47DE910DECD72A427DAC";
  // Purchase Invoice with documentNo: Issue 29266
  private static final String INVOICE_TEST2 = "F5D55960B2704B86903740BF35779DB6";
  // Sales Invoice with documentNo: Issue 29403
  private static final String INVOICE_TEST3 = "E6C9AAC2D2384521AFD4716CE970587C";
  // Sales Invoice with documentNo: Issue 29505
  private static final String INVOICE_TEST4 = "5E9C3D827CF54E94BDE088DC8977ECCC";
  // G/L Journal Test with documentNo: 1000
  private static final String GLJOURNAL_TEST5 = "572E1C2ED680447987B234C78980158E";
  // G/L Journal Test with documentNo: 1001
  private static final String GLJOURNAL_TEST6 = "B6E47F412BB74C94B676921858D3AAED";
  // G/L Journal Test with documentNo: 1002
  private static final String GLJOURNAL_TEST7 = "F284DBAF46F244EB8CCA145A0CC47ACD";
  // G/L Journal Test with documentNo: 1003
  private static final String GLJOURNAL_TEST8 = "12857A9C03AD49D795F05A889CBEC0F2";
  // ACCOUNTS USED FOR TEST RESULTS
  private static String TAX_RECEIVABLES = "FABD8D6CF3F04EE7A0389C2BAA1D620E";
  private static String SERVICE_COST = "F7B96292FB5842FBB51143BA659008B0";
  private static String VENDOR_PREPAYMENT = "40285C81312D85B901312DBB9BB40096";

  private static String CLIENTES = "44D112E5F0CF4D1B8D4652EE282076F2";
  private static String ANTICIPOS_PROVEEDORES = "9D63B01A0359422185545CB7713652DC";
  private static String VENTA_MERCADERIAS = "861F04DD57084B9882064312E7AC1EEF";
  private static String COMPRA_MERCADERIAS = "FB2A7CD68876462BAED0D3FB2840E182";
  private static String IVA_REPERCUTIDO = "3544375BB8414739B93813F54246B2E1";
  private static String IVA_SOPORTADO = "D04185C47CCA43B1A59DF318A6921E2B";
  private static String CAPITAL_SOCIAL = "E35DCFD53E064A2DAAEB35D80DFEDB74";
  private static String FONDO_SOCIAL = "B1C14BA52F7D4E73B3F4D950A04CDD52";
  private static String PRETTY_CASH = "627A1291274F4BCF838588BB2F2102AD";
  private static String BANK_ACCOUNT = "58D66F384F7549D995523BF116F29BA0";

  @SuppressWarnings("unused")
  private String testNumber; // It is used to show the test number when the test is run.
  @SuppressWarnings("unused")
  private String testDescription; // It is used to show the test description when the test is run.
  private String keyId;
  private String tableId;
  private String orgId;
  private String[][] resultTest;

  public PostDocumentTest(String testNumber, String testDescription, String keyId, String tableId,
      String orgId, String[][] resultTest) {
    this.testDescription = testDescription;
    this.testNumber = testNumber;
    this.keyId = keyId;
    this.tableId = tableId;
    this.orgId = orgId;
    this.resultTest = resultTest;
  }

  /** parameterized: Documents to be posted together with results expected */
  @Parameters(name = "idx:{0} name:{1}")
  public static Collection<Object[]> params() {
    // { "Ledger", "Account", "date", "currency", "amtsourcedr", "amtsourcecr", "amtacctdr",
    // "amtacctcr"
    // },
    String[][] resultTest1 = {
        { MAIN_EURO_LEGDER, IVA_REPERCUTIDO, "18-07-2014", "102", "0.00", "1.00", "0.00", "1.00" },
        { MAIN_EURO_LEGDER, IVA_REPERCUTIDO, "18-07-2014", "102", "0.00", "6.00", "0.00", "6.00" },
        { MAIN_EURO_LEGDER, CLIENTES, "18-07-2014", "102", "207.00", "0.00", "207.00", "0.00" },
        { MAIN_EURO_LEGDER, VENTA_MERCADERIAS, "18-07-2014", "102", "0.00", "200.00", "0.00",
            "200.00" } };
    String[][] resultTest2 = {
        { MAIN_EURO_LEGDER, IVA_SOPORTADO, "04-05-2015", "102", "-10.00", "0.00", "-10.00",
            "0.00" },
        { MAIN_EURO_LEGDER, IVA_SOPORTADO, "04-05-2015", "102", "10.00", "0.00", "10.00",
            "0.00" } };
    String[][] resultTest3 = {
        // Dollar Ledger
        { USA_DOLLAR_LEGDER, VENDOR_PREPAYMENT, "05-05-2015", "130", "0.00", "5.50", "0.00",
            "16.50" },
        { USA_DOLLAR_LEGDER, SERVICE_COST, "05-05-2015", "130", "5.00", "0.00", "15.00", "0.00" },
        { USA_DOLLAR_LEGDER, TAX_RECEIVABLES, "05-05-2015", "130", "0.50", "0.00", "1.50", "0.00" },
        // EURO Ledger
        { MAIN_EURO_LEGDER, ANTICIPOS_PROVEEDORES, "05-05-2015", "130", "0.00", "5.50", "0.00",
            "11.00" },
        { MAIN_EURO_LEGDER, IVA_SOPORTADO, "05-05-2015", "130", "0.50", "0.00", "1.00", "0.00" },
        { MAIN_EURO_LEGDER, COMPRA_MERCADERIAS, "05-05-2015", "130", "5.00", "0.00", "10.00",
            "0.00" } };
    String[][] resultTest4 = {
        { MAIN_EURO_LEGDER, IVA_REPERCUTIDO, "05-05-2015", "102", "0.00", "1.50", "0.00", "1.50" },
        { MAIN_EURO_LEGDER, IVA_REPERCUTIDO, "05-05-2015", "102", "0.00", "9.00", "0.00", "9.00" },
        { MAIN_EURO_LEGDER, CLIENTES, "05-05-2015", "102", "310.50", "0.00", "310.50", "0.00" },
        { MAIN_EURO_LEGDER, VENTA_MERCADERIAS, "05-05-2015", "102", "0.00", "300.00", "0.00",
            "300.00" } };
    String[][] resultTest5 = {
        { USA_DOLLAR_LEGDER, BANK_ACCOUNT, "02-06-2015", "100", "0.00", "100.00", "0.00",
            "100.00" },
        { USA_DOLLAR_LEGDER, PRETTY_CASH, "02-06-2015", "100", "100.00", "0.00", "100.00", "0.00" },
        { MAIN_EURO_LEGDER, FONDO_SOCIAL, "02-06-2015", "100", "0.00", "100.00", "0.00", "200.00" },
        { MAIN_EURO_LEGDER, CAPITAL_SOCIAL, "02-06-2015", "100", "100.00", "0.00", "200.00",
            "0.00" } };
    String[][] resultTest6 = {
        { USA_DOLLAR_LEGDER, BANK_ACCOUNT, "03-06-2015", "100", "0.00", "100.00", "0.00",
            "100.00" },
        { USA_DOLLAR_LEGDER, PRETTY_CASH, "03-06-2015", "100", "100.00", "0.00", "100.00", "0.00" },
        { MAIN_EURO_LEGDER, FONDO_SOCIAL, "03-06-2015", "100", "0.00", "100.00", "0.00", "40.00" },
        { MAIN_EURO_LEGDER, CAPITAL_SOCIAL, "03-06-2015", "100", "100.00", "0.00", "40.00",
            "0.00" } };
    String[][] resultTest7 = {
        { MAIN_EURO_LEGDER, FONDO_SOCIAL, "03-06-2015", "100", "0.00", "100.00", "0.00", "40.00" },
        { MAIN_EURO_LEGDER, CAPITAL_SOCIAL, "03-06-2015", "100", "100.00", "0.00", "40.00",
            "0.00" } };
    String[][] resultTest8 = {
        { USA_DOLLAR_LEGDER, BANK_ACCOUNT, "03-06-2015", "100", "0.00", "100.00", "0.00",
            "100.00" },
        { USA_DOLLAR_LEGDER, PRETTY_CASH, "03-06-2015", "100", "100.00", "0.00", "100.00", "0.00" },
        { MAIN_EURO_LEGDER, FONDO_SOCIAL, "03-06-2015", "100", "0.00", "100.00", "0.00", "200.00" },
        { MAIN_EURO_LEGDER, CAPITAL_SOCIAL, "03-06-2015", "100", "100.00", "0.00", "200.00",
            "0.00" } };

    return Arrays.asList(new Object[][] {
        { "1", "Sales invoice I/29", INVOICE_TEST1, TABLE_INVOICE, ORGANIZATION_SPAIN_ID,
            resultTest1 },
        // Purchase invoice of zero amount but with taxes: Issue 29266
        { "2", "Purchase Invoice Issue 29266", INVOICE_TEST2, TABLE_INVOICE, ORGANIZATION_SPAIN_ID,
            resultTest2 },
        // Purchase invoice with prepayment in other currency but with taxes: Issue 29403
        { "3", "Purchase Invoice Issue 29403", INVOICE_TEST3, TABLE_INVOICE, ORGANIZATION_USA_ID,
            resultTest3 },
        // Sales invoice with canceled payment: Issue 29505
        { "4", "Sales Invoice Issue 29505", INVOICE_TEST4, TABLE_INVOICE, ORGANIZATION_SPAIN_ID,
            resultTest4 },
        { "5", "Simple GL Journal with multi general ledge and exchange rates", GLJOURNAL_TEST5,
            TABLE_GLJOURNAL, ORGANIZATION_USA_ID, resultTest5 },
        { "6", "Simple GL Journal with multi general ledge and without exchange rates",
            GLJOURNAL_TEST6, TABLE_GLJOURNAL, ORGANIZATION_USA_ID, resultTest6 },
        { "7", "Simple GL Journal without multi general ledge", GLJOURNAL_TEST7, TABLE_GLJOURNAL,
            ORGANIZATION_USA_ID, resultTest7 },
        { "8", "Simple GL Journal with the same exchange rate than test 5", GLJOURNAL_TEST8,
            TABLE_GLJOURNAL, ORGANIZATION_USA_ID, resultTest8 }
        /*
         * , { "9", "Simple GL Journal with a currency without global conversion" , GLJOURNAL_TEST9,
         * TABLE_GLJOURNAL, ORGANIZATION_USA_ID, resultTest9 }
         */
    });
  }

  /*
   * Posts a document and verifies entries are correct
   */
  @Test
  public void testPostDocument() {
    try {
      OBContext.setAdminMode(false);
      ResetAccounting.delete(CLIENT_ID, orgId, tableId, keyId, "", "");
      postDocument();
      checkResults();
    } catch (ServletException e) {
      log.error("Error posting document", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void checkResults() {
    OBCriteria<AccountingFact> obc = OBDal.getInstance().createCriteria(AccountingFact.class);
    obc.add(Restrictions.eq(AccountingFact.PROPERTY_RECORDID, keyId));
    obc.add(Restrictions.eq(AccountingFact.PROPERTY_TABLE + ".id", tableId));
    obc.setFilterOnReadableClients(false);
    obc.setFilterOnReadableOrganization(false);
    obc.addOrder(Order.asc(AccountingFact.PROPERTY_ACCOUNTINGSCHEMA));
    obc.addOrder(Order.asc(AccountingFact.PROPERTY_ACCOUNT));
    obc.addOrder(Order.asc(AccountingFact.PROPERTY_DEBIT));
    obc.addOrder(Order.asc(AccountingFact.PROPERTY_CREDIT));
    int counter = 0;
    for (AccountingFact fact : obc.list()) {
      String[] result = resultTest[counter];
      counter++;
      String ledger = result[0];
      String accountId = result[1];
      String date = result[2];
      String currencyId = result[3];
      String amtSourceDr = result[4];
      String amtSourceCr = result[5];
      String amtAcctDr = result[6];
      String amtAcctCr = result[7];
      BigDecimal amtAcctCrObtained = fact.getCredit();
      BigDecimal amtAcctDrObtained = fact.getDebit();
      BigDecimal amtSourceCrObtained = fact.getForeignCurrencyCredit();
      BigDecimal amtSourceDrObtained = fact.getForeignCurrencyDebit();
      log.debug("****************  NEW ENTRY ***************");
      log.debug("SeqNo: " + fact.getSequenceNumber());
      log.debug("CurrencyId: " + fact.getCurrency().getId());
      log.debug(OBDateUtils.formatDate(fact.getAccountingDate()));
      log.debug(fact.getAccount().getIdentifier());
      log.debug("AccountId: " + fact.getAccount().getId());
      log.debug("Foreign Debit:" + fact.getForeignCurrencyDebit().toString());
      log.debug("Foreign Credit:" + fact.getForeignCurrencyCredit().toString());
      log.debug("Debit:" + fact.getDebit().toString());
      log.debug("Credit:" + fact.getCredit().toString());
      assertThat("Wrong amtSourceDr", new BigDecimal(amtSourceDr),
          closeTo(amtSourceDrObtained, BigDecimal.ZERO));
      assertThat("Wrong amtSourceCr", new BigDecimal(amtSourceCr),
          closeTo(amtSourceCrObtained, BigDecimal.ZERO));
      assertThat("Wrong amtAcctDr", new BigDecimal(amtAcctDr),
          closeTo(amtAcctDrObtained, BigDecimal.ZERO));
      assertThat("Wrong amtAcctCr", new BigDecimal(amtAcctCr),
          closeTo(amtAcctCrObtained, BigDecimal.ZERO));
      assertThat("Wrong Date", date, equalTo(OBDateUtils.formatDate(fact.getAccountingDate())));
      assertThat("Wrong Leger", ledger, equalTo(fact.getAccountingSchema().getId()));
      assertThat("Wrong Account", accountId, equalTo(fact.getAccount().getId()));
      assertThat("Wrong Currency", currencyId, equalTo(fact.getCurrency().getId()));
    }
    assertTrue("Wrong number of entries. Expected: " + resultTest.length + " obtained: " + counter,
        resultTest.length == counter);
  }

  private void postDocument() throws ServletException {
    ConnectionProvider conn = getConnectionProvider();
    Connection con = null;
    try {
      con = conn.getTransactionConnection();
      AcctServer acct = AcctServer.get(tableId, CLIENT_ID, orgId, conn);
      if (acct == null) {
        conn.releaseRollbackConnection(con);
        return;
      } else if (!acct.post(keyId, false, new VariablesSecureApp(USER_ID, CLIENT_ID, orgId), conn,
          con) || acct.errors != 0) {
        conn.releaseRollbackConnection(con);
        return;
      }
      conn.releaseCommitConnection(con);
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      try {
        conn.releaseRollbackConnection(con);
      } catch (Exception ignored) {
      }
    }
    return;
  }
}
