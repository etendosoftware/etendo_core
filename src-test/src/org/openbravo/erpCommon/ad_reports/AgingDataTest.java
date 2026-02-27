/*
 * Unit tests for AgingData.
 */
package org.openbravo.erpCommon.ad_reports;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.junit.Test;

/**
 * Tests for {@link AgingData}.
 */
@SuppressWarnings({"java:S120"})
public class AgingDataTest {

  private static final String BP_ID = "TEST_BP_ID";
  private static final String BP_NAME = "Test Business Partner";

  // --- Constructor tests (full constructor) ---
  /** Full constructor calculates total. */

  @Test
  public void testFullConstructorCalculatesTotal() {
    // Arrange
    BigDecimal current = new BigDecimal("100");
    BigDecimal a1 = new BigDecimal("200");
    BigDecimal a2 = new BigDecimal("300");
    BigDecimal a3 = new BigDecimal("400");
    BigDecimal a4 = new BigDecimal("500");
    BigDecimal a5 = new BigDecimal("600");
    BigDecimal credit = BigDecimal.ZERO;
    BigDecimal doubtful = BigDecimal.ZERO;

    // Act
    AgingData data = new AgingData(BP_ID, BP_NAME, current, a1, a2, a3, a4, a5, credit, doubtful);

    // Assert - total = sum of all amounts
    BigDecimal expectedTotal = new BigDecimal("2100");
    assertEquals(expectedTotal, data.getTotal());
  }
  /** Full constructor calculates net. */

  @Test
  public void testFullConstructorCalculatesNet() {
    // Arrange
    BigDecimal current = new BigDecimal("100");
    BigDecimal a1 = new BigDecimal("200");
    BigDecimal a2 = BigDecimal.ZERO;
    BigDecimal a3 = BigDecimal.ZERO;
    BigDecimal a4 = BigDecimal.ZERO;
    BigDecimal a5 = BigDecimal.ZERO;
    BigDecimal credit = new BigDecimal("50");
    BigDecimal doubtful = new BigDecimal("10");

    // Act
    AgingData data = new AgingData(BP_ID, BP_NAME, current, a1, a2, a3, a4, a5, credit, doubtful);

    // Assert - net = total - credit + doubtfulDebt = 300 - 50 + 10 = 260
    BigDecimal expectedNet = new BigDecimal("260");
    assertEquals(expectedNet, data.getNet());
  }
  /** Full constructor calculates percentage with doubtful debt. */

  @Test
  public void testFullConstructorCalculatesPercentageWithDoubtfulDebt() {
    // Arrange
    BigDecimal current = new BigDecimal("1000");
    BigDecimal a1 = BigDecimal.ZERO;
    BigDecimal a2 = BigDecimal.ZERO;
    BigDecimal a3 = BigDecimal.ZERO;
    BigDecimal a4 = BigDecimal.ZERO;
    BigDecimal a5 = BigDecimal.ZERO;
    BigDecimal credit = BigDecimal.ZERO;
    BigDecimal doubtful = new BigDecimal("200");

    // Act
    AgingData data = new AgingData(BP_ID, BP_NAME, current, a1, a2, a3, a4, a5, credit, doubtful);

    // Assert - percentage = (doubtful / (total - credit + doubtful)) * 100
    // = (200 / (1000 - 0 + 200)) * 100 = (200 / 1200) * 100
    BigDecimal expectedPercentage = new BigDecimal("200").divide(new BigDecimal("1200"), 5, RoundingMode.HALF_UP)
        .multiply(new BigDecimal("100"));
    assertEquals(expectedPercentage, data.getPercentage());
  }
  /** Full constructor zero doubtful debt percentage. */

  @Test
  public void testFullConstructorZeroDoubtfulDebtPercentage() {
    // Arrange
    AgingData data = new AgingData(BP_ID, BP_NAME,
        new BigDecimal("500"), BigDecimal.ZERO, BigDecimal.ZERO,
        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
        BigDecimal.ZERO, BigDecimal.ZERO);

    // Assert - zero doubtful debt means zero percentage
    assertEquals(BigDecimal.ZERO, data.getPercentage());
  }

  // --- Constructor tests (index constructor) ---
  /** Index constructor sets current for index0. */

  @Test
  public void testIndexConstructorSetsCurrentForIndex0() {
    // Arrange & Act
    BigDecimal amount = new BigDecimal("150");
    AgingData data = new AgingData(BP_ID, BP_NAME, amount, 0);

    // Assert
    assertEquals(amount, data.getcurrent());
    assertEquals(BigDecimal.ZERO, data.getamount1());
    assertEquals(amount, data.getTotal());
    assertEquals(amount, data.getNet());
  }
  /** Index constructor sets amount1for index1. */

  @Test
  public void testIndexConstructorSetsAmount1ForIndex1() {
    BigDecimal amount = new BigDecimal("250");
    AgingData data = new AgingData(BP_ID, BP_NAME, amount, 1);

    assertEquals(BigDecimal.ZERO, data.getcurrent());
    assertEquals(amount, data.getamount1());
    assertEquals(BigDecimal.ZERO, data.getamount2());
  }
  /** Index constructor sets amount5for index5. */

  @Test
  public void testIndexConstructorSetsAmount5ForIndex5() {
    BigDecimal amount = new BigDecimal("350");
    AgingData data = new AgingData(BP_ID, BP_NAME, amount, 5);

    assertEquals(amount, data.getamount5());
    assertEquals(BigDecimal.ZERO, data.getamount4());
  }
  /** Index constructor default index does not set amount. */

  @Test
  public void testIndexConstructorDefaultIndexDoesNotSetAmount() {
    BigDecimal amount = new BigDecimal("100");
    AgingData data = new AgingData(BP_ID, BP_NAME, amount, 99);

    assertEquals(BigDecimal.ZERO, data.getcurrent());
    assertEquals(BigDecimal.ZERO, data.getamount1());
    assertEquals(BigDecimal.ZERO, data.getamount2());
    assertEquals(BigDecimal.ZERO, data.getamount3());
    assertEquals(BigDecimal.ZERO, data.getamount4());
    assertEquals(BigDecimal.ZERO, data.getamount5());
    // total is still set to amount regardless of index
    assertEquals(amount, data.getTotal());
  }

  // --- addAmount tests ---
  /** Add amount to index0. */

  @Test
  public void testAddAmountToIndex0() {
    AgingData data = new AgingData(BP_ID, BP_NAME, new BigDecimal("100"), 0);

    data.addAmount(new BigDecimal("50"), 0);

    assertEquals(new BigDecimal("150"), data.getcurrent());
    assertEquals(new BigDecimal("150"), data.getTotal());
    assertEquals(new BigDecimal("150"), data.getNet());
  }
  /** Add amount to index3. */

  @Test
  public void testAddAmountToIndex3() {
    AgingData data = new AgingData(BP_ID, BP_NAME, BigDecimal.ZERO, 0);

    data.addAmount(new BigDecimal("75"), 3);

    assertEquals(new BigDecimal("75"), data.getamount3());
    assertEquals(new BigDecimal("75"), data.getTotal());
  }
  /** Add amount default index does nothing. */

  @Test
  public void testAddAmountDefaultIndexDoesNothing() {
    AgingData data = new AgingData(BP_ID, BP_NAME, new BigDecimal("100"), 0);

    data.addAmount(new BigDecimal("50"), 99);

    // Amount slots unchanged, but total and net still increase
    assertEquals(new BigDecimal("100"), data.getcurrent());
    assertEquals(new BigDecimal("150"), data.getTotal());
  }

  // --- addCredit tests ---
  /** Add credit reduces net. */

  @Test
  public void testAddCreditReducesNet() {
    AgingData data = new AgingData(BP_ID, BP_NAME, new BigDecimal("1000"), 0);

    data.addCredit(new BigDecimal("200"));

    assertEquals(new BigDecimal("200"), data.getCredit());
    assertEquals(new BigDecimal("800"), data.getNet());
  }

  // --- addDoubtfulDebt tests ---
  /** Add doubtful debt increases net. */

  @Test
  public void testAddDoubtfulDebtIncreasesNet() {
    AgingData data = new AgingData(BP_ID, BP_NAME, new BigDecimal("1000"), 0);

    data.addDoubtfulDebt(new BigDecimal("100"));

    assertEquals(new BigDecimal("100"), data.getDoubtfulDebt());
    assertEquals(new BigDecimal("1100"), data.getNet());
  }
  /** Add doubtful debt recalculates percentage. */

  @Test
  public void testAddDoubtfulDebtRecalculatesPercentage() {
    AgingData data = new AgingData(BP_ID, BP_NAME, new BigDecimal("1000"), 0);

    data.addDoubtfulDebt(new BigDecimal("200"));

    // percentage = (200 / (1000 + 200)) * 100 = (200/1200) * 100
    BigDecimal expected = new BigDecimal("200").divide(new BigDecimal("1200"), 5, RoundingMode.HALF_UP)
        .multiply(new BigDecimal("100"));
    assertEquals(expected, data.getPercentage());
  }

  // --- getAmount array test ---
  /** Get amount returns all six amounts. */

  @Test
  public void testGetAmountReturnsAllSixAmounts() {
    AgingData data = new AgingData(BP_ID, BP_NAME,
        new BigDecimal("10"), new BigDecimal("20"), new BigDecimal("30"),
        new BigDecimal("40"), new BigDecimal("50"), new BigDecimal("60"),
        BigDecimal.ZERO, BigDecimal.ZERO);

    BigDecimal[] amounts = data.getAmount();

    assertEquals(6, amounts.length);
    assertEquals(new BigDecimal("10"), amounts[0]);
    assertEquals(new BigDecimal("20"), amounts[1]);
    assertEquals(new BigDecimal("30"), amounts[2]);
    assertEquals(new BigDecimal("40"), amounts[3]);
    assertEquals(new BigDecimal("50"), amounts[4]);
    assertEquals(new BigDecimal("60"), amounts[5]);
  }

  // --- compareTo tests ---
  /** Compare to sorts by partner name ignoring case. */

  @Test
  public void testCompareToSortsByPartnerNameIgnoringCase() {
    AgingData alpha = new AgingData("1", "Alpha", BigDecimal.ZERO, 0);
    AgingData beta = new AgingData("2", "beta", BigDecimal.ZERO, 0);

    assertTrue(alpha.compareTo(beta) < 0);
    assertTrue(beta.compareTo(alpha) > 0);
  }
  /** Compare to same name sorts by id. */

  @Test
  public void testCompareToSameNameSortsByID() {
    AgingData first = new AgingData("AAA", "Same Name", BigDecimal.ZERO, 0);
    AgingData second = new AgingData("BBB", "Same Name", BigDecimal.ZERO, 0);

    assertTrue(first.compareTo(second) < 0);
  }
  /** Compare to equal returns zero. */

  @Test
  public void testCompareToEqualReturnsZero() {
    AgingData a = new AgingData("ID1", "Partner", BigDecimal.ZERO, 0);
    AgingData b = new AgingData("ID1", "Partner", BigDecimal.ZERO, 0);

    assertEquals(0, a.compareTo(b));
  }

  // --- getter/setter tests ---
  /** Set b partner id. */

  @Test
  public void testSetBPartnerID() {
    AgingData data = new AgingData(BP_ID, BP_NAME, BigDecimal.ZERO, 0);
    data.setBPartnerID("NEW_ID");
    assertEquals("NEW_ID", data.getBPartnerID());
  }
  /** Set b partner. */

  @Test
  public void testSetBPartner() {
    AgingData data = new AgingData(BP_ID, BP_NAME, BigDecimal.ZERO, 0);
    data.setBPartner("New Name");
    assertEquals("New Name", data.getBPartner());
  }
}
