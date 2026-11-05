/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance with
 * the License.
 * You may obtain a copy of the License at
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright (C) 2021-2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.costing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.exception.OBException;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.materialmgmt.cost.CostAdjustment;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;

/**
 * Unit tests for {@link CostAdjustmentLineParameters}. Covers both constructors (including the
 * mandatory transaction/header validation), the default flag values, and the mutual exclusion
 * between the Unit Cost and Negative Correction flags.
 */
@RunWith(MockitoJUnitRunner.class)
public class CostAdjustmentLineParametersTest {

  private static final String ERROR_KEY = "CostAdjustmentCalculationError";
  private static final String ERROR_MSG = "Cost adjustment calculation error";
  private static final BigDecimal AMOUNT = new BigDecimal("12.50");

  @Mock
  private MaterialTransaction transaction;
  @Mock
  private CostAdjustment costAdjustment;
  @Mock
  private Currency currency;
  @Mock
  private Currency otherCurrency;

  private MockedStatic<OBMessageUtils> messageUtils;

  /** Opens the static mock for {@link OBMessageUtils} used by the validation branch. */
  @Before
  public void setUp() {
    messageUtils = mockStatic(OBMessageUtils.class);
  }

  /** Closes the static mock. */
  @After
  public void tearDown() {
    messageUtils.close();
  }

  /** The three-argument constructor takes the currency from the transaction. */
  @Test
  public void threeArgConstructorUsesTransactionCurrency() {
    when(transaction.getCurrency()).thenReturn(currency);

    CostAdjustmentLineParameters params = new CostAdjustmentLineParameters(transaction, AMOUNT,
        costAdjustment);

    assertSame(transaction, params.getTransaction());
    assertSame(costAdjustment, params.getCostAdjustmentHeader());
    assertEquals(AMOUNT, params.getAdjustmentAmount());
    assertSame(currency, params.getCurrency());
  }

  /** The four-argument constructor keeps the explicit currency and sets all default flags. */
  @Test
  public void fourArgConstructorSetsValuesAndDefaults() {
    CostAdjustmentLineParameters params = new CostAdjustmentLineParameters(transaction, AMOUNT,
        costAdjustment, otherCurrency);

    assertSame(transaction, params.getTransaction());
    assertSame(costAdjustment, params.getCostAdjustmentHeader());
    assertEquals(AMOUNT, params.getAdjustmentAmount());
    assertSame(otherCurrency, params.getCurrency());
    assertFalse(params.isSource());
    assertTrue(params.isUnitCost());
    assertFalse(params.isBackdatedTransaction());
    assertFalse(params.isNegativeCorrection());
    assertTrue(params.isNeedPosting());
    assertFalse(params.isRelatedTransactionAdjusted());
    verify(transaction, never()).getCurrency();
  }

  /** Null amount and null currency are accepted as-is (only transaction/header are mandatory). */
  @Test
  public void nullAmountAndCurrencyAreAllowed() {
    CostAdjustmentLineParameters params = new CostAdjustmentLineParameters(transaction, null,
        costAdjustment, null);

    assertNull(params.getAdjustmentAmount());
    assertNull(params.getCurrency());
  }

  /** A null transaction raises an {@link OBException} with the translated message. */
  @Test
  public void nullTransactionThrows() {
    messageUtils.when(() -> OBMessageUtils.messageBD(ERROR_KEY)).thenReturn(ERROR_MSG);

    OBException ex = assertThrows(OBException.class,
        () -> new CostAdjustmentLineParameters(null, AMOUNT, costAdjustment, currency));

    assertEquals(ERROR_MSG, ex.getMessage());
  }

  /** A null cost adjustment header raises an {@link OBException}. */
  @Test
  public void nullHeaderThrows() {
    messageUtils.when(() -> OBMessageUtils.messageBD(ERROR_KEY)).thenReturn(ERROR_MSG);

    OBException ex = assertThrows(OBException.class,
        () -> new CostAdjustmentLineParameters(transaction, AMOUNT, null, currency));

    assertEquals(ERROR_MSG, ex.getMessage());
  }

  /** The three-argument constructor also validates a null header. */
  @Test
  public void threeArgConstructorNullHeaderThrows() {
    messageUtils.when(() -> OBMessageUtils.messageBD(ERROR_KEY)).thenReturn(ERROR_MSG);
    when(transaction.getCurrency()).thenReturn(currency);

    assertThrows(OBException.class,
        () -> new CostAdjustmentLineParameters(transaction, AMOUNT, null));
  }

  /** The Source flag can be toggled on and off. */
  @Test
  public void setSourceToggles() {
    CostAdjustmentLineParameters params = newParams();
    params.setSource(true);
    assertTrue(params.isSource());
    params.setSource(false);
    assertFalse(params.isSource());
  }

  /** The Backdated Transaction flag can be toggled on and off. */
  @Test
  public void setBackdatedTransactionToggles() {
    CostAdjustmentLineParameters params = newParams();
    params.setBackdatedTransaction(true);
    assertTrue(params.isBackdatedTransaction());
    params.setBackdatedTransaction(false);
    assertFalse(params.isBackdatedTransaction());
  }

  /** The Need Posting flag can be toggled off and on. */
  @Test
  public void setNeedPostingToggles() {
    CostAdjustmentLineParameters params = newParams();
    params.setNeedPosting(false);
    assertFalse(params.isNeedPosting());
    params.setNeedPosting(true);
    assertTrue(params.isNeedPosting());
  }

  /** The Related Transaction Adjusted flag can be toggled on and off. */
  @Test
  public void setRelatedTransactionAdjustedToggles() {
    CostAdjustmentLineParameters params = newParams();
    params.setRelatedTransactionAdjusted(true);
    assertTrue(params.isRelatedTransactionAdjusted());
    params.setRelatedTransactionAdjusted(false);
    assertFalse(params.isRelatedTransactionAdjusted());
  }

  /** Setting Negative Correction to true forces Unit Cost to false. */
  @Test
  public void negativeCorrectionTrueClearsUnitCost() {
    CostAdjustmentLineParameters params = newParams();
    params.setNegativeCorrection(true);
    assertTrue(params.isNegativeCorrection());
    assertFalse(params.isUnitCost());
  }

  /** Setting Negative Correction to false leaves Unit Cost untouched. */
  @Test
  public void negativeCorrectionFalseKeepsUnitCost() {
    CostAdjustmentLineParameters params = newParams();
    params.setNegativeCorrection(false);
    assertFalse(params.isNegativeCorrection());
    assertTrue(params.isUnitCost());

    params.setUnitCost(false);
    params.setNegativeCorrection(false);
    assertFalse(params.isUnitCost());
  }

  /** Setting Unit Cost to true forces Negative Correction to false. */
  @Test
  public void unitCostTrueClearsNegativeCorrection() {
    CostAdjustmentLineParameters params = newParams();
    params.setNegativeCorrection(true);
    params.setUnitCost(true);
    assertTrue(params.isUnitCost());
    assertFalse(params.isNegativeCorrection());
  }

  /** Setting Unit Cost to false leaves Negative Correction untouched. */
  @Test
  public void unitCostFalseKeepsNegativeCorrection() {
    CostAdjustmentLineParameters params = newParams();
    params.setNegativeCorrection(true);
    params.setUnitCost(false);
    assertFalse(params.isUnitCost());
    assertTrue(params.isNegativeCorrection());
  }

  private CostAdjustmentLineParameters newParams() {
    return new CostAdjustmentLineParameters(transaction, AMOUNT, costAdjustment, currency);
  }
}
