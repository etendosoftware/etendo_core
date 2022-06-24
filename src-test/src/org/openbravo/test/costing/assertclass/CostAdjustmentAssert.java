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
 * All portions are Copyright (C) 2018 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.costing.assertclass;

import java.math.BigDecimal;

import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.test.costing.utils.TestCostingConstants;

public class CostAdjustmentAssert {

  final private MaterialTransaction materialTransaction;
  final private Currency currency;
  final private String type;
  final private BigDecimal amount;
  final private int day;
  final private boolean source;
  final private boolean unit;
  final private String status;
  final private boolean isNeedPosting;

  public CostAdjustmentAssert(MaterialTransaction materialTransaction, String type,
      BigDecimal amount, int day, boolean source) {
    this(materialTransaction, type, amount, day, source, true);
  }

  public CostAdjustmentAssert(MaterialTransaction materialTransaction, String currencyId,
      String type, BigDecimal amount, int day, boolean source) {
    this(materialTransaction, currencyId, type, amount, day, source, true);
  }

  public CostAdjustmentAssert(MaterialTransaction materialTransaction, String type,
      BigDecimal amount, int day, boolean source, boolean unit) {
    this(materialTransaction, TestCostingConstants.EURO_ID, type, amount, day, source, unit);
  }

  public CostAdjustmentAssert(MaterialTransaction materialTransaction, String type,
      BigDecimal amount, int day, boolean source, boolean unit, boolean isNeedPosting) {
    this(materialTransaction, TestCostingConstants.EURO_ID, type, amount, day, source, unit,
        isNeedPosting);
  }

  public CostAdjustmentAssert(MaterialTransaction materialTransaction, String currencyId,
      String type, BigDecimal amount, int day, boolean source, boolean unit) {
    this(materialTransaction, currencyId, type, amount, day, source, unit, "CO", false);
  }

  public CostAdjustmentAssert(MaterialTransaction materialTransaction, String currencyId,
      String type, BigDecimal amount, int day, boolean source, boolean unit,
      boolean isNeedPosting) {
    this(materialTransaction, currencyId, type, amount, day, source, unit, "CO", isNeedPosting);
  }

  public CostAdjustmentAssert(MaterialTransaction materialTransaction, String type,
      BigDecimal amount, int day, boolean source, String status) {
    this(materialTransaction, type, amount, day, source, true, status);
  }

  public CostAdjustmentAssert(MaterialTransaction materialTransaction, String type,
      BigDecimal amount, int day, boolean source, boolean unit, String status) {
    this(materialTransaction, TestCostingConstants.EURO_ID, type, amount, day, source, unit, status,
        false);
  }

  public CostAdjustmentAssert(MaterialTransaction materialTransaction, String currencyId,
      String type, BigDecimal amount, int day, boolean source, boolean unit, String status,
      boolean isNeedPosting) {
    this.materialTransaction = materialTransaction;
    this.currency = OBDal.getInstance().get(Currency.class, currencyId);
    this.type = type;
    this.amount = amount;
    this.day = day;
    this.source = source;
    this.unit = unit;
    this.status = status;
    this.isNeedPosting = isNeedPosting;
  }

  public MaterialTransaction getMaterialTransaction() {
    return materialTransaction;
  }

  public Currency getCurrency() {
    return currency;
  }

  public String getType() {
    return type;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public int getDay() {
    return day;
  }

  public boolean isSource() {
    return source;
  }

  public boolean isUnit() {
    return unit;
  }

  public String getStatus() {
    return status;
  }

  public boolean isNeedPosting() {
    return isNeedPosting;
  }

}
