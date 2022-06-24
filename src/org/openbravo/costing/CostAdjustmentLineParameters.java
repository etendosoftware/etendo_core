/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2014-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.costing;

import java.math.BigDecimal;

import org.openbravo.base.exception.OBException;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.materialmgmt.cost.CostAdjustment;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;

/**
 * This class contains the necessary information to create a Cost Adjustment Line. Based on the
 * information stored in the class, a process will be able to create a Cost Adjustment Line
 * afterwards.
 * 
 * It controls that it is not possible to create a Cost Adjustment Line with an empty Transaction or
 * an empty Cost Adjustment Header.
 * 
 * Also, it is not possible to create an adjustment that is both Unit and Negative Correction. When
 * the Negative Correction flag is true, the Unit Cost flag is set to false automatically.
 *
 */
public class CostAdjustmentLineParameters {
  private MaterialTransaction transaction;
  private CostAdjustment costAdjustmentHeader;
  private BigDecimal adjustmentAmt;
  private Currency currency;
  private boolean isSource;
  private boolean isUnitCost;
  private boolean isBackdatedTransaction;
  private boolean isNegativeCorrection;
  private boolean isNeedPosting;
  private boolean isRelatedTransactionAdjusted;

  /**
   * It creates a new object based on the given parameters. It also defaults the values of some
   * other variables:
   * <ol>
   * <li>isSource: false</il>
   * <li>isUnitCost: true></il>
   * <li>isBackdatedTransaction: false</il>
   * <li>isNegativeCorrection: false</il>
   * <li>isNeedsPosting: true</il>
   * <li>isRelatedTransactionAdjusted: false</il>
   * </ol>
   * 
   * @param transaction
   *          The Transaction for which the adjustment is going to be made
   * @param adjustmentAmt
   *          The amount that will be adjusted against the Transaction
   * @param costAdjustmentHeader
   *          The Cost Adjustment Document that will contain the adjustment line
   */
  public CostAdjustmentLineParameters(final MaterialTransaction transaction,
      final BigDecimal adjustmentAmt, final CostAdjustment costAdjustmentHeader) {
    this(transaction, adjustmentAmt, costAdjustmentHeader, transaction.getCurrency());
  }

  /**
   * It creates a new object based on the given parameters. It also defaults the values of some
   * other variables:
   * <ol>
   * <li>isSource: false</il>
   * <li>isUnitCost: true></il>
   * <li>isBackdatedTransaction: false</il>
   * <li>isNegativeCorrection: false</il>
   * <li>isNeedsPosting: true</il>
   * <li>isRelatedTransactionAdjusted: false</il>
   * </ol>
   * 
   * @param transaction
   *          The Transaction for which the adjustment is going to be made
   * @param adjustmentAmt
   *          The amount that will be adjusted against the Transaction
   * @param costAdjustmentHeader
   *          The Cost Adjustment Document that will contain the adjustment line
   * @param currency
   *          The Currency for which the adjustment amount is done
   */
  public CostAdjustmentLineParameters(final MaterialTransaction transaction,
      final BigDecimal adjustmentAmt, final CostAdjustment costAdjustmentHeader,
      final Currency currency) {
    if (transaction == null || costAdjustmentHeader == null) {
      throw new OBException(OBMessageUtils.messageBD("CostAdjustmentCalculationError"));
    }

    this.transaction = transaction;
    this.costAdjustmentHeader = costAdjustmentHeader;
    this.adjustmentAmt = adjustmentAmt;
    this.currency = currency;
    this.isSource = false;
    this.isUnitCost = true;
    this.isBackdatedTransaction = false;
    this.isNegativeCorrection = false;
    this.isNeedPosting = true;
    this.isRelatedTransactionAdjusted = false;
  }

  /**
   * 
   * @return the value of the is Source flag. An adjustment that is source can create several other
   *         adjustments in cascade.
   */
  public boolean isSource() {
    return isSource;
  }

  /**
   * Set the is Source flag value. An adjustment that is source can create several other adjustments
   * in cascade.
   */
  public void setSource(final boolean isSource) {
    this.isSource = isSource;
  }

  /**
   * 
   * @return the value of the is Unit Cost flag. An adjustment that is Unit Cost will modify the
   *         unitary cost of the Transaction (the intrinsic cost, not taking additional costs like
   *         Landed Costs into account) A Unit Cost can not be a Negative Correction at the same
   *         time.
   */
  public boolean isUnitCost() {
    return isUnitCost;
  }

  /**
   * Sets the value of the Unit Cost flag. An adjustment that is Unit Cost will modify the unitary
   * cost of the Transaction (the intrinsic cost, not taking additional costs like Landed Costs into
   * account) A Unit Cost can not be a Negative Correction at the same time.
   * 
   * If the value of the Unit Cost is true, then the value of the Negative Correction is set to
   * false automatically.
   */
  public void setUnitCost(final boolean isUnitCost) {
    this.isUnitCost = isUnitCost;
    if (isUnitCost) {
      this.isNegativeCorrection = false;
    }
  }

  /**
   * 
   * @return the value of the is Backdated Transaction flag. A Backdated Transaction adjustment
   *         means that it will take place in the past, and it will affect transactions between the
   *         past and the present.
   */
  public boolean isBackdatedTransaction() {
    return isBackdatedTransaction;
  }

  /**
   * Sets the value of the Backdated Transaction flag. A Backdated Transaction adjustment means that
   * it will take place in the past, and it will affect transactions between the past and the
   * present.
   */
  public void setBackdatedTransaction(final boolean isBackdatedTransaction) {
    this.isBackdatedTransaction = isBackdatedTransaction;
  }

  /**
   * 
   * @return the value of the Negative Correction flag. A Negative Correction Adjustment happens
   *         when there is negative stock and the related transaction is going to increase the stock
   *         quantity. A Negative Correction can not be a Unit Cost adjustment at the same time.
   */
  public boolean isNegativeCorrection() {
    return isNegativeCorrection;
  }

  /**
   * Sets the value of the Negative Correction flag. A Negative Correction Adjustment happens when
   * there is negative stock and the related transaction is going to increase the stock quantity. A
   * Negative Correction can not be a Unit Cost adjustment at the same time.
   * 
   * If the value of the Negative Correction is true, then the value of the Unit Cost is set to
   * false automatically.
   */
  public void setNegativeCorrection(final boolean isNegativeCorrection) {
    this.isNegativeCorrection = isNegativeCorrection;
    if (isNegativeCorrection) {
      this.isUnitCost = false;
    }
  }

  /**
   * 
   * @return the value of the is Need Posting flag. It is used to differentiate the adjustments that
   *         needs to be posted to the ledger to the ones that are not needed
   */
  public boolean isNeedPosting() {
    return isNeedPosting;
  }

  /**
   * Sets the value of the is Need Posting flag. It is used to differentiate the adjustments that
   * needs to be posted to the ledger to the ones that are not needed
   */
  public void setNeedPosting(boolean isNeedPosting) {
    this.isNeedPosting = isNeedPosting;
  }

  /**
   * 
   * @return the value of the is Related Transaction Adjusted flag. It is true when the related
   *         transaction has been taken into account already in the adjustment process
   */
  public boolean isRelatedTransactionAdjusted() {
    return isRelatedTransactionAdjusted;
  }

  /**
   * Sets the is Related Transaction Adjusted flag. It is true when the related transaction has been
   * taken into account already in the adjustment process
   */
  public void setRelatedTransactionAdjusted(boolean isRelatedTransactionAdjusted) {
    this.isRelatedTransactionAdjusted = isRelatedTransactionAdjusted;
  }

  /**
   * 
   * @return The transaction that is being adjusted
   */
  public MaterialTransaction getTransaction() {
    return transaction;
  }

  /**
   * 
   * @return The Cost Adjustment document that is going to contain the cost adjustment line
   */
  public CostAdjustment getCostAdjustmentHeader() {
    return costAdjustmentHeader;
  }

  /**
   * 
   * @return The amount that is going to be adjusted to the transaction
   */
  public BigDecimal getAdjustmentAmount() {
    return adjustmentAmt;
  }

  /**
   * 
   * @return The currency for which the adjustment is made
   */
  public Currency getCurrency() {
    return currency;
  }
}
