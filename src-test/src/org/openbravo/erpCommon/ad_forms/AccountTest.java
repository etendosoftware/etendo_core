package org.openbravo.erpCommon.ad_forms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;

import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.database.ConnectionProvider;

/**
 * Tests for {@link Account}.
 */
@SuppressWarnings({"java:S100", "java:S120"})
@RunWith(MockitoJUnitRunner.class)
public class AccountTest {

  private static final String TEST_SCHEMA_ID = "TEST_SCHEMA_001";
  private static final String TEST_ORG_ID = "ORG_001";
  private static final String TEST_ACCOUNT_ID = "ACCT_001";
  private static final String TEST_BPARTNER_ID = "BP_001";
  private static final String TEST_PRODUCT_ID = "PROD_001";
  private static final String TEST_COMBO_ID = "COMBO_001";
  private static final String TEST_CLIENT_ID = "CLIENT_001";

  @Mock
  private ConnectionProvider mockConn;

  private MockedStatic<AccountData> accountDataStatic;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    accountDataStatic = mockStatic(AccountData.class);
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (accountDataStatic != null) {
      accountDataStatic.close();
    }
  }
  /** Default constructor initializes fields. */

  @Test
  public void testDefaultConstructorInitializesFields() {
    Account account = new Account();

    assertEquals("", account.C_ValidCombination_ID);
    assertEquals("", account.C_AcctSchema_ID);
    assertEquals("", account.AD_Client_ID);
    assertEquals("", account.AD_Org_ID);
    assertEquals("", account.Account_ID);
    assertEquals("", account.M_Product_ID);
    assertEquals("", account.C_BPartner_ID);
    assertEquals("", account.AD_OrgTrx_ID);
    assertEquals("", account.C_LocFrom_ID);
    assertEquals("", account.C_LocTo_ID);
    assertEquals("", account.C_SalesRegion_ID);
    assertEquals("", account.C_Project_ID);
    assertEquals("", account.C_Channel_ID);
    assertEquals("", account.C_Campaign_ID);
    assertEquals("", account.C_Activity_ID);
    assertEquals("", account.User1_ID);
    assertEquals("", account.User2_ID);
    assertEquals("", account.alias);
    assertEquals("", account.combination);
    assertEquals("", account.description);
    assertEquals("Y", account.active);
    assertEquals("", account.updatedBy);
    assertEquals("F", account.fullyQualified);
    assertEquals(' ', account.m_AcctType);
    assertEquals(null, account.m_IsValid);
    assertEquals("", account.m_OldAccount_ID);
    assertEquals("Y", account.m_changed);
  }
  /**
   * Constructor with empty id returns early.
   * @throws ServletException if an error occurs
   */

  @Test
  public void testConstructorWithEmptyIdReturnsEarly() throws ServletException {
    Account account = new Account(mockConn, "");

    assertEquals("", account.C_ValidCombination_ID);
    assertEquals("N", account.fullyQualified);
  }
  /**
   * Constructor with null id calls load.
   * @throws ServletException if an error occurs
   */

  @Test
  public void testConstructorWithNullIdCallsLoad() throws ServletException {
    accountDataStatic.when(() -> AccountData.select(any(ConnectionProvider.class), any()))
        .thenReturn(new AccountData[] {});

    Account account = new Account(mockConn, null);

    assertEquals("", account.C_ValidCombination_ID);
    assertEquals("N", account.fullyQualified);
  }
  /**
   * Constructor with valid id calls load.
   * @throws ServletException if an error occurs
   */

  @Test
  public void testConstructorWithValidIdCallsLoad() throws ServletException {
    ObjenesisStd objenesis = new ObjenesisStd();
    AccountData data = objenesis.newInstance(AccountData.class);
    data.adClientId = TEST_CLIENT_ID;
    data.adOrgId = TEST_ORG_ID;
    data.isactive = "Y";
    data.updatedby = "ADMIN";
    data.alias = "TEST_ALIAS";
    data.combination = "TEST_COMBO";
    data.description = "Test Description";
    data.isfullyqualified = "Y";
    data.cAcctschemaId = TEST_SCHEMA_ID;
    data.accountId = TEST_ACCOUNT_ID;
    data.mProductId = TEST_PRODUCT_ID;
    data.cBpartnerId = TEST_BPARTNER_ID;
    data.adOrgtrxId = "";
    data.cLocfromId = "";
    data.cLoctoId = "";
    data.cSalesregionId = "";
    data.cProjectId = "";
    data.cCampaignId = "";
    data.cActivityId = "";
    data.user1Id = "";
    data.user2Id = "";

    accountDataStatic.when(() -> AccountData.select(any(ConnectionProvider.class), anyString()))
        .thenReturn(new AccountData[] { data });

    Account account = new Account(mockConn, TEST_COMBO_ID);

    assertEquals(TEST_CLIENT_ID, account.AD_Client_ID);
    assertEquals(TEST_ORG_ID, account.AD_Org_ID);
    assertEquals(TEST_ACCOUNT_ID, account.Account_ID);
    assertEquals(TEST_COMBO_ID, account.C_ValidCombination_ID);
    assertEquals("N", account.m_changed);
  }
  /**
   * Load with no data does not set fields.
   * @throws ServletException if an error occurs
   */

  @Test
  public void testLoadWithNoDataDoesNotSetFields() throws ServletException {
    accountDataStatic.when(() -> AccountData.select(any(ConnectionProvider.class), anyString()))
        .thenReturn(new AccountData[] {});

    Account account = new Account();
    account.load(null, mockConn, TEST_COMBO_ID);

    assertEquals("", account.AD_Client_ID);
    assertEquals("Y", account.m_changed);
  }
  /**
   * Load rethrows servlet exception.
   * @throws ServletException if an error occurs
   */

  @Test(expected = ServletException.class)
  public void testLoadRethrowsServletException() throws ServletException {
    accountDataStatic.when(() -> AccountData.select(any(ConnectionProvider.class), anyString()))
        .thenThrow(new ServletException("DB Error"));

    Account account = new Account();
    account.load(null, mockConn, TEST_COMBO_ID);
  }
  /** Is balance sheet with asset type. */

  @Test
  public void testIsBalanceSheetWithAssetType() {
    Account account = new Account();
    account.m_AcctType = 'A';
    assertTrue(account.isBalanceSheet());
  }
  /** Is balance sheet with liability type. */

  @Test
  public void testIsBalanceSheetWithLiabilityType() {
    Account account = new Account();
    account.m_AcctType = 'L';
    assertTrue(account.isBalanceSheet());
  }
  /** Is balance sheet with owner equity type. */

  @Test
  public void testIsBalanceSheetWithOwnerEquityType() {
    Account account = new Account();
    account.m_AcctType = 'O';
    assertTrue(account.isBalanceSheet());
  }
  /** Is balance sheet with revenue type. */

  @Test
  public void testIsBalanceSheetWithRevenueType() {
    Account account = new Account();
    account.m_AcctType = 'R';
    assertFalse(account.isBalanceSheet());
  }
  /** Is balance sheet with expense type. */

  @Test
  public void testIsBalanceSheetWithExpenseType() {
    Account account = new Account();
    account.m_AcctType = 'E';
    assertFalse(account.isBalanceSheet());
  }
  /** Get ad_org_id. */

  @Test
  public void testGetAD_Org_ID() {
    Account account = new Account();
    account.AD_Org_ID = TEST_ORG_ID;
    assertEquals(TEST_ORG_ID, account.getAD_Org_ID());
  }
  /** Get account_id. */

  @Test
  public void testGetAccount_ID() {
    Account account = new Account();
    account.Account_ID = TEST_ACCOUNT_ID;
    assertEquals(TEST_ACCOUNT_ID, account.getAccount_ID());
  }
  /**
   * Get account static method.
   * @throws ServletException if an error occurs
   */

  @Test
  public void testGetAccountStaticMethod() throws ServletException {
    accountDataStatic.when(() -> AccountData.select(any(ConnectionProvider.class), anyString()))
        .thenReturn(new AccountData[] {});

    Account account = Account.getAccount(mockConn, TEST_COMBO_ID);

    assertNotNull(account);
  }
  /**
   * Get default with mandatory elements.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetDefaultWithMandatoryElements() throws Exception {
    AcctSchema acctSchema = createAcctSchemaWithElements(true);

    Account account = Account.getDefault(acctSchema, false);

    assertNotNull(account);
    assertEquals(TEST_SCHEMA_ID, account.C_AcctSchema_ID);
    assertEquals(TEST_ORG_ID, account.AD_Org_ID);
    assertEquals(TEST_ACCOUNT_ID, account.Account_ID);
    assertEquals(TEST_BPARTNER_ID, account.C_BPartner_ID);
    assertEquals(TEST_PRODUCT_ID, account.M_Product_ID);
  }
  /**
   * Get default with optional null skips non mandatory.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetDefaultWithOptionalNullSkipsNonMandatory() throws Exception {
    AcctSchema acctSchema = createAcctSchemaWithElements(false);

    Account account = Account.getDefault(acctSchema, true);

    assertNotNull(account);
    assertEquals(TEST_ORG_ID, account.AD_Org_ID);
    assertEquals(TEST_ACCOUNT_ID, account.Account_ID);
    // Non-mandatory fields should remain empty when optionalNull=true
    assertEquals("", account.C_BPartner_ID);
    assertEquals("", account.M_Product_ID);
  }
  /**
   * Get default with optional null false sets non mandatory.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetDefaultWithOptionalNullFalseSetsNonMandatory() throws Exception {
    AcctSchema acctSchema = createAcctSchemaWithElements(false);

    Account account = Account.getDefault(acctSchema, false);

    assertNotNull(account);
    assertEquals(TEST_BPARTNER_ID, account.C_BPartner_ID);
    assertEquals(TEST_PRODUCT_ID, account.M_Product_ID);
  }

  private AcctSchema createAcctSchemaWithElements(boolean allMandatory) throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    AcctSchema acctSchema = objenesis.newInstance(AcctSchema.class);

    Field schemaIdField = AcctSchema.class.getDeclaredField("m_C_AcctSchema_ID");
    schemaIdField.setAccessible(true);
    schemaIdField.set(acctSchema, TEST_SCHEMA_ID);

    String mandatory = allMandatory ? "Y" : "N";

    ArrayList<Object> elements = new ArrayList<>();
    elements.add(new AcctSchemaElement("1", "10", "Org", "OO", "", TEST_ORG_ID, "Y", "N"));
    elements.add(new AcctSchemaElement("2", "20", "Account", "AC", "", TEST_ACCOUNT_ID, "Y", "N"));
    elements.add(
        new AcctSchemaElement("3", "30", "BPartner", "BP", "", TEST_BPARTNER_ID, mandatory, "N"));
    elements.add(
        new AcctSchemaElement("4", "40", "Product", "PR", "", TEST_PRODUCT_ID, mandatory, "N"));
    elements.add(
        new AcctSchemaElement("5", "50", "Activity", "AY", "", "ACT_001", mandatory, "N"));
    elements.add(
        new AcctSchemaElement("6", "60", "Campaign", "MC", "", "CAMP_001", mandatory, "N"));
    elements.add(
        new AcctSchemaElement("7", "70", "Project", "PJ", "", "PROJ_001", mandatory, "N"));
    elements.add(
        new AcctSchemaElement("8", "80", "SalesRegion", "SR", "", "SR_001", mandatory, "N"));

    Field elementListField = AcctSchema.class.getDeclaredField("m_elementList");
    elementListField.setAccessible(true);
    elementListField.set(acctSchema, elements);

    return acctSchema;
  }
}
