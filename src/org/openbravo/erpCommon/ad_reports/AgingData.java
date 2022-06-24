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
 * All portions are Copyright (C) 2012-2016 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 **/

package org.openbravo.erpCommon.ad_reports;

import java.math.BigDecimal;
import java.math.RoundingMode;

class AgingData implements Comparable<AgingData> {

  private String bPartnerID;
  private String bPartner;
  private BigDecimal current;
  private BigDecimal amount1;
  private BigDecimal amount2;
  private BigDecimal amount3;
  private BigDecimal amount4;
  private BigDecimal amount5;
  private BigDecimal total;
  private BigDecimal credit;
  private BigDecimal doubtfulDebt;
  private BigDecimal percentage;
  private BigDecimal net;

  public AgingData(String BPartnerID, String BPartner, BigDecimal current, BigDecimal amount1,
      BigDecimal amount2, BigDecimal amount3, BigDecimal amount4, BigDecimal amount5,
      BigDecimal credit, BigDecimal doubtfulDebt) {
    this.bPartnerID = BPartnerID;
    this.bPartner = BPartner;
    this.current = current;
    this.amount1 = amount1;
    this.amount2 = amount2;
    this.amount3 = amount3;
    this.amount4 = amount4;
    this.amount5 = amount5;
    this.total = current.add(amount1).add(amount2).add(amount3).add(amount4).add(amount5);
    this.credit = credit;
    this.doubtfulDebt = doubtfulDebt;
    this.percentage = calculatePercentage(total.subtract(credit).add(doubtfulDebt), doubtfulDebt);
    this.net = current.add(amount1)
        .add(amount2)
        .add(amount3)
        .add(amount4)
        .add(amount5)
        .subtract(credit)
        .add(doubtfulDebt);
  }

  public AgingData(String BPartnerID, String BPartner, BigDecimal amount, int index) {
    this.bPartnerID = BPartnerID;
    this.bPartner = BPartner;
    this.current = BigDecimal.ZERO;
    this.amount1 = BigDecimal.ZERO;
    this.amount2 = BigDecimal.ZERO;
    this.amount3 = BigDecimal.ZERO;
    this.amount4 = BigDecimal.ZERO;
    this.amount5 = BigDecimal.ZERO;
    switch (index) {
      case 0: {
        this.current = amount;
        break;
      }
      case 1: {
        this.amount1 = amount;
        break;
      }
      case 2: {
        this.amount2 = amount;
        break;
      }
      case 3: {
        this.amount3 = amount;
        break;
      }
      case 4: {
        this.amount4 = amount;
        break;
      }
      case 5: {
        this.amount5 = amount;
        break;
      }
      default: {
        break;
      }
    }
    this.total = amount;
    this.credit = BigDecimal.ZERO;
    this.doubtfulDebt = BigDecimal.ZERO;
    this.percentage = BigDecimal.ZERO;
    this.net = amount;
  }

  public void setBPartnerID(String BPartnerID) {
    this.bPartnerID = BPartnerID;
  }

  public void setBPartner(String BPartner) {
    this.bPartner = BPartner;
  }

  public void addAmount(BigDecimal amt, int index) {
    switch (index) {
      case 0: {
        this.current = this.current.add(amt);
        break;
      }
      case 1: {
        this.amount1 = this.amount1.add(amt);
        break;
      }
      case 2: {
        this.amount2 = this.amount2.add(amt);
        break;
      }
      case 3: {
        this.amount3 = this.amount3.add(amt);
        break;
      }
      case 4: {
        this.amount4 = this.amount4.add(amt);
        break;
      }
      case 5: {
        this.amount5 = this.amount5.add(amt);
        break;
      }
      default: {
        break;
      }
    }
    this.total = total.add(amt);
    this.net = net.add(amt);
  }

  public void addCredit(BigDecimal creditAmount) {
    this.credit = this.credit.add(creditAmount);
    this.net = net.subtract(creditAmount);
  }

  public void addDoubtfulDebt(BigDecimal doubtfulDebtAmount) {
    this.doubtfulDebt = this.doubtfulDebt.add(doubtfulDebtAmount);
    this.percentage = calculatePercentage(net.add(doubtfulDebtAmount), this.doubtfulDebt);
    this.net = net.add(doubtfulDebtAmount);
  }

  public String getBPartnerID() {
    return this.bPartnerID;
  }

  public String getBPartner() {
    return this.bPartner;
  }

  public BigDecimal[] getAmount() {
    BigDecimal[] auxBigDecimal = { this.current, this.amount1, this.amount2, this.amount3,
        this.amount4, this.amount5 };
    return auxBigDecimal;
  }

  public BigDecimal getcurrent() {
    return this.current;
  }

  public BigDecimal getamount1() {
    return this.amount1;
  }

  public BigDecimal getamount2() {
    return this.amount2;
  }

  public BigDecimal getamount3() {
    return this.amount3;
  }

  public BigDecimal getamount4() {
    return this.amount4;
  }

  public BigDecimal getamount5() {
    return this.amount5;
  }

  public BigDecimal getTotal() {
    return this.total;
  }

  public BigDecimal getCredit() {
    return this.credit;
  }

  public BigDecimal getDoubtfulDebt() {
    return this.doubtfulDebt;
  }

  public BigDecimal getPercentage() {
    return this.percentage;
  }

  public BigDecimal getNet() {
    return this.net;
  }

  private BigDecimal calculatePercentage(BigDecimal totalAmount, BigDecimal doubtfulDebtAmount) {
    if (doubtfulDebtAmount.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    }
    return doubtfulDebtAmount.divide(totalAmount, 5, RoundingMode.HALF_UP)
        .multiply(new BigDecimal("100"));
  }

  @Override
  public int compareTo(AgingData arg0) {
    int compare = bPartner.compareToIgnoreCase(arg0.getBPartner());
    if (compare == 0) {
      compare = bPartnerID.compareTo(arg0.getBPartnerID());
    }
    return compare;
  }
}
