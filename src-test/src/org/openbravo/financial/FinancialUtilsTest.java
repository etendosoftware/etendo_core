package org.openbravo.financial;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.ConversionRate;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.test.base.TestConstants.Orgs;

/**
 * Integration tests for {@link FinancialUtils}.
 * <p>
 * The {@code getConversionRate} tests exercise the behaviour where the method considers BOTH the
 * client's own conversion rates AND the shared system ({@code '0'}) rates, with a client-specific
 * rate winning over the system rate for the same organization, currencies and date. The recursive
 * parent-organization fallback is preserved.
 * <p>
 * The conversion rate rows are created inside the current transaction (save + flush, NO commit) and
 * a rollback is forced at teardown via {@link SessionHandler#setDoRollback(boolean)}. This keeps the
 * rows visible to the query within the same transaction while guaranteeing that nothing is
 * persisted, so the {@code C_CONVERSION_RATE_TRG} delete trigger (error {@code @20506@}) is never
 * fired and no manual cleanup is required.
 * <p>
 * {@link WeldBaseTest} is a subclass of {@code OBBaseTest}, so all the {@code OBBaseTest} test
 * infrastructure (context helpers, well-known test ids, transaction handling) is available here.
 */
public class FinancialUtilsTest extends WeldBaseTest {
  protected static final String BP_HEALTHY_FOOD = "B3ABB0B4AFEA4541AC1E29891D496079";

  /** System / shared client id. */
  private static final String STAR_CLIENT_ID = "0";

  /** Euro currency id (see {@code OBBaseTest.EURO_ID}). */
  private static final String EURO_CURRENCY_ID = "102";

  /** US Dollar currency id (see {@code OBBaseTest.DOLLAR_ID}). */
  private static final String DOLLAR_CURRENCY_ID = "100";

  /** Pound Sterling currency id (as used by FundsTransferTest). */
  private static final String POUND_CURRENCY_ID = "114";

  private Client testClient;
  private Client starClient;
  private Currency euro;
  private Currency dollar;
  private Currency pound;
  private Organization espOrg;
  private Organization espNorteOrg;

  @Test
  public void testGetProductPriceWithNullProduct() {
    // Given
    Date date = new Date();
    BusinessPartner healthyFoodBP = OBDal.getInstance().get(BusinessPartner.class, BP_HEALTHY_FOOD);
    PriceList priceList = healthyFoodBP.getPurchasePricelist();
    // When
    try {
      FinancialUtils.getProductPrice(null, date, true, priceList);
      fail("Expected an OBException to be thrown");
    } catch (OBException e) {
      assertEquals("@ParameterMissing@ @Product@", e.getMessage());
    }
  }

  /**
   * Prepares the references used by the {@code getConversionRate} tests and guarantees the
   * transaction is rolled back at teardown.
   */
  @Before
  public void initConversionRateReferences() {
    // Use the system administrator context so cross-client conversion rate rows (including the
    // shared '0' client) can be created freely.
    setSystemAdministratorContext();
    // Guarantee the transaction is rolled back at teardown: rows created below are seen by the
    // query in the same transaction but are never committed, so the delete trigger cannot fire.
    SessionHandler.getInstance().setDoRollback(true);

    testClient = OBDal.getInstance().get(Client.class, TEST_CLIENT_ID);
    starClient = OBDal.getInstance().get(Client.class, STAR_CLIENT_ID);
    euro = OBDal.getInstance().get(Currency.class, EURO_CURRENCY_ID);
    dollar = OBDal.getInstance().get(Currency.class, DOLLAR_CURRENCY_ID);
    pound = OBDal.getInstance().get(Currency.class, POUND_CURRENCY_ID);
    // F&B España, S.A. (parent) and F&B España - Región Norte (child of ESP).
    espOrg = OBDal.getInstance().get(Organization.class, Orgs.ESP);
    espNorteOrg = OBDal.getInstance().get(Organization.class, Orgs.ESP_NORTE);
  }

  /**
   * A client-specific rate must win over the shared system ('0') rate for the same organization,
   * currencies and date.
   */
  @Test
  public void clientSpecificRateWinsOverSystemRate() {
    // Unique far-future validity window to avoid collisions with seed data.
    Date validFrom = date(2099, 1, 1);
    Date validTo = date(2099, 1, 31);
    Date queryDate = date(2099, 1, 15);

    BigDecimal clientRate = new BigDecimal("2.0000000");
    BigDecimal systemRate = new BigDecimal("3.0000000");
    createRate(testClient, espNorteOrg, euro, dollar, clientRate, validFrom, validTo);
    createRate(starClient, espNorteOrg, euro, dollar, systemRate, validFrom, validTo);

    ConversionRate result = FinancialUtils.getConversionRate(queryDate, euro, dollar, espNorteOrg,
        testClient);

    assertNotNull("A conversion rate should be found", result);
    assertEquals("The client-specific rate must win over the system rate", TEST_CLIENT_ID,
        result.getClient().getId());
    assertEquals("Wrong multiply rate returned", 0,
        result.getMultipleRateBy().compareTo(clientRate));
  }

  /**
   * When only the shared system ('0') rate exists, it must be returned as the fallback.
   */
  @Test
  public void fallsBackToSystemRateWhenNoClientRate() {
    Date validFrom = date(2099, 2, 1);
    Date validTo = date(2099, 2, 28);
    Date queryDate = date(2099, 2, 15);

    BigDecimal systemRate = new BigDecimal("5.0000000");
    createRate(starClient, espNorteOrg, dollar, euro, systemRate, validFrom, validTo);

    ConversionRate result = FinancialUtils.getConversionRate(queryDate, dollar, euro, espNorteOrg,
        testClient);

    assertNotNull("The system ('0') conversion rate should be found as fallback", result);
    assertEquals("The returned rate must belong to the system ('0') client", STAR_CLIENT_ID,
        result.getClient().getId());
    assertEquals("Wrong multiply rate returned", 0,
        result.getMultipleRateBy().compareTo(systemRate));
  }

  /**
   * Backward compatibility: when only the client's own rate exists (no system rate), it must be
   * returned.
   */
  @Test
  public void returnsClientRateWhenNoSystemRate() {
    Date validFrom = date(2099, 3, 1);
    Date validTo = date(2099, 3, 31);
    Date queryDate = date(2099, 3, 15);

    BigDecimal clientRate = new BigDecimal("7.0000000");
    createRate(testClient, espNorteOrg, euro, pound, clientRate, validFrom, validTo);

    ConversionRate result = FinancialUtils.getConversionRate(queryDate, euro, pound, espNorteOrg,
        testClient);

    assertNotNull("The client conversion rate should be found", result);
    assertEquals("The returned rate must belong to the test client", TEST_CLIENT_ID,
        result.getClient().getId());
    assertEquals("Wrong multiply rate returned", 0,
        result.getMultipleRateBy().compareTo(clientRate));
  }

  /**
   * Parent-organization recursion: when the rate is defined on a parent organization and the query
   * is performed with a child organization, the rate must still be found.
   */
  @Test
  public void findsRateOnParentOrganization() {
    Date validFrom = date(2099, 4, 1);
    Date validTo = date(2099, 4, 30);
    Date queryDate = date(2099, 4, 15);

    BigDecimal parentRate = new BigDecimal("11.0000000");
    // Rate defined on the parent org (ESP); query performed with the child org (ESP_NORTE).
    createRate(testClient, espOrg, pound, euro, parentRate, validFrom, validTo);

    ConversionRate result = FinancialUtils.getConversionRate(queryDate, pound, euro, espNorteOrg,
        testClient);

    assertNotNull("The rate defined on the parent organization should be found", result);
    assertEquals("The returned rate must be the parent-organization one", Orgs.ESP,
        result.getOrganization().getId());
    assertEquals("Wrong multiply rate returned", 0,
        result.getMultipleRateBy().compareTo(parentRate));
  }

  /**
   * Creates and flushes (but never commits) a {@link ConversionRate} row inside admin mode so that
   * cross-client / cross-organization rows can be created regardless of the current context.
   */
  private ConversionRate createRate(Client client, Organization org, Currency from, Currency to,
      BigDecimal multiplyRate, Date validFrom, Date validTo) {
    OBContext.setAdminMode(true);
    try {
      ConversionRate rate = OBProvider.getInstance().get(ConversionRate.class);
      rate.setClient(client);
      rate.setOrganization(org);
      rate.setCurrency(from);
      rate.setToCurrency(to);
      rate.setValidFromDate(validFrom);
      rate.setValidToDate(validTo);
      rate.setMultipleRateBy(multiplyRate);
      rate.setDivideRateBy(BigDecimal.ONE.divide(multiplyRate, 7, RoundingMode.HALF_UP));
      rate.setActive(true);
      OBDal.getInstance().save(rate);
      // Flush (NOT commit) so the query in the method under test sees the row in this transaction.
      OBDal.getInstance().flush();
      return rate;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Builds a date with the time part set to midnight.
   *
   * @param year
   *          full year (e.g. 2099)
   * @param month
   *          1-based month (1 = January)
   * @param day
   *          day of month
   * @return the corresponding {@link Date} at 00:00:00
   */
  private Date date(int year, int month, int day) {
    Calendar cal = Calendar.getInstance();
    cal.clear();
    cal.set(year, month - 1, day, 0, 0, 0);
    return cal.getTime();
  }
}
