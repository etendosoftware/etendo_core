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
 * All portions are Copyright (C) 2014 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.advpaymentmngt.utility;

public class APRMConstants {
  public final static String SALES_ORDER_WINDOW_ID = "143";
  public final static String PURCHASE_ORDER_WINDOW_ID = "181";
  public final static String SALES_INVOICE_WINDOW_ID = "167";
  public final static String PURCHASE_INVOICE_WINDOW_ID = "183";
  public final static String PAYMENT_IN_WINDOW_ID = "E547CE89D4C04429B6340FFA44E70716";
  public final static String PAYMENT_OUT_WINDOW_ID = "6F8F913FA60F4CBD93DC1D3AA696E76E";
  public final static String TRANSACTION_WINDOW_ID = "94EAA455D2644E04AB25D93BE5157B6D";
  public final static String ADD_PAYMENT_MENU = "NULLWINDOWID";

  /* Transaction Type */
  public static final String TRXTYPE_BPDeposit = "BPD";
  public static final String TRXTYPE_BPWithdrawal = "BPW";
  public static final String TRXTYPE_BankFee = "BF";

  /* Payment Statuses */
  public static final String PAYMENT_STATUS_AWAITING_EXECUTION = "RPAE";
  public static final String PAYMENT_STATUS_CANCELED = "RPVOID";
  public static final String PAYMENT_STATUS_PAYMENT_CLEARED = "RPPC";
  public static final String PAYMENT_STATUS_DEPOSIT_NOT_CLEARED = "RDNC";
  public static final String PAYMENT_STATUS_PAYMENT_MADE = "PPM";
  public static final String PAYMENT_STATUS_AWAITING_PAYMENT = "RPAP";
  public static final String PAYMENT_STATUS_WITHDRAWAL_NOT_CLEARED = "PWNC";
  public static final String PAYMENT_STATUS_PAYMENT_RECEIVED = "RPR";
}
