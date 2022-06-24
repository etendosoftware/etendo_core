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
 * All portions are Copyright (C) 2015-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.taxes;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.InvoiceLineTax;
import org.openbravo.model.ad.access.OrderLineTax;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceDiscount;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.invoice.InvoiceTax;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderDiscount;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.order.OrderTax;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.test.base.OBBaseTest;
import org.openbravo.test.taxes.data.*;

/**
 * Tests cases to check taxes computation
 * 
 * 
 */
@RunWith(Parameterized.class)
public class TaxesTest extends OBBaseTest {
  final static private Logger log = LogManager.getLogger();

  // User Openbravo
  private final String USER_ID = "100";
  // Client QA Testing
  private final String CLIENT_ID = "4028E6C72959682B01295A070852010D";
  // Organization Spain
  private final String ORGANIZATION_ID = "357947E87C284935AD1D783CF6F099A1";
  // Role QA Testing Admin
  private final String ROLE_ID = "4028E6C72959682B01295A071429011E";
  // Sales Invoice: Taxes Test Template
  private final String SALESINVOICE_ID = "F889F6E61CA6454EA50BDD6DD75582E3";
  // Purchase Invoice: 10000017
  private final String PURCHASEINVOICE_ID = "9D0F6E57E59247F6AB6D063951811F51";
  // Sales Order: 50012
  private final String SALESORDER_ID = "8B53B7E6CF3B4D8D9BCF3A49EED6FCB4";
  // Purchase Order: 800010
  private final String PURCHASEORDER_ID = "2C9CEDC0761A41DCB276A5124F8AAA90";
  // PriceList: Price Including Taxes Sales
  private final String PRICEINCLUDINGTAXES_PRICELIST_SALES = "62C67BFD306C4BEF9F2738C27353380B";
  // PriceList: Price Including Taxes Purchase
  private final String PRICEINCLUDINGTAXES_PRICELIST_PURCHASE = "83BD2A678D30447983755C4E46C6F69A";
  // PriceList: Sales
  private final String PRICEEXCLUDINGTAXES_PRICELIST_SALES = "4028E6C72959682B01295ADC1D55022B";
  // PriceList: Purchase
  private final String PRICEEXCLUDINGTAXES_PRICELIST_PURCHASE = "4028E6C72959682B01295ADC1769021B";

  private static final String INVOICE_COMPLETE_PROCEDURE_NAME = "c_invoice_post";
  private static final String ORDER_COMPLETE_PROCEDURE_NAME = "c_order_post1";

  private String testNumber;
  private String testDescription;
  private boolean isTaxDocumentLevel;
  private boolean isPriceIncludingTaxes;
  private HashMap<String, String[]> docTaxes;
  private String[] docAmounts;
  private TaxesLineTestData[] linesData;

  public TaxesTest(String testNumber, String testDescription, TaxesTestData data) {
    this.testNumber = testNumber;
    this.testDescription = testDescription;
    this.isTaxDocumentLevel = data.isTaxDocumentLevel();
    this.isPriceIncludingTaxes = data.isPriceIncludingTaxes();
    this.docTaxes = data.getDocTaxes();
    this.docAmounts = data.getDocAmounts();
    this.linesData = data.getLinesData();
  }

  /** parameterized possible combinations for taxes computation */
  @Parameters(name = "idx:{0} name:{1}")
  public static Collection<Object[]> params() {
    return Arrays.asList(new Object[][] {
        { "01", "PriceExcludingTaxes 01: Line Exempt positive", new TaxesTestData1() }, //
        { "02", "PriceExcludingTaxes 02: Line Exempt negative", new TaxesTestData2() }, //
        { "03", "PriceExcludingTaxes 03: Line 10% positive", new TaxesTestData3() }, //
        { "04", "PriceExcludingTaxes 04: Line 10% negative", new TaxesTestData4() }, //
        { "05", "PriceExcludingTaxes 05: Line 3% + Charge positive", new TaxesTestData5() }, //
        { "06", "PriceExcludingTaxes 06: Line 3% + Charge negative", new TaxesTestData6() }, //
        { "07", "PriceExcludingTaxes 07: Doc Exempt positive", new TaxesTestData7() }, //
        { "08", "PriceExcludingTaxes 08: Doc Exempt negative", new TaxesTestData8() }, //
        { "09", "PriceExcludingTaxes 09: Doc 10% positive", new TaxesTestData9() }, //
        { "10", "PriceExcludingTaxes 10: Doc 10% negative", new TaxesTestData10() }, //
        { "11", "PriceExcludingTaxes 11: Doc 3% + charge positive", new TaxesTestData11() }, //
        { "12", "PriceExcludingTaxes 12: Doc 3% + charge negative", new TaxesTestData12() }, //
        { "13", "PriceExcludingTaxes 13: Doc BOM Taxes positive", new TaxesTestData13() }, //
        { "14", "PriceExcludingTaxes 14: Doc BOM Taxes negative", new TaxesTestData14() }, //
        { "15", "PriceExcludingTaxes 15: Line BOM Taxes positive", new TaxesTestData15() }, //
        { "16", "PriceExcludingTaxes 16: Line BOM Taxes negative", new TaxesTestData16() }, //
        { "17", "PriceIncludingTaxes 17: Line Exempt positive", new TaxesTestData17() }, //
        { "18", "PriceIncludingTaxes 18: Line Exempt negative", new TaxesTestData18() }, //
        { "19", "PriceIncludingTaxes 19: Line 10% positive", new TaxesTestData19() }, //
        { "20", "PriceIncludingTaxes 20: Line 10% negative", new TaxesTestData20() }, //
        { "21", "PriceIncludingTaxes 21: Line 3% + charge positive", new TaxesTestData21() }, //
        { "22", "PriceIncludingTaxes 22: Line 3% + charge negative", new TaxesTestData22() }, //
        { "23", "PriceIncludingTaxes 23: Doc Exempt positive", new TaxesTestData23() }, //
        { "24", "PriceIncludingTaxes 24: Doc Exempt negative", new TaxesTestData24() }, //
        { "25", "PriceIncludingTaxes 25: Doc 10% positive", new TaxesTestData25() }, //
        { "26", "PriceIncludingTaxes 26: Doc 10% negative", new TaxesTestData26() }, //
        { "27", "PriceIncludingTaxes 27: Doc 3% + charge positive", new TaxesTestData27() }, //
        { "28", "PriceIncludingTaxes 28: Doc 3% + charge negative", new TaxesTestData28() }, //
        { "29", "PriceIncludingTaxes 29: Doc BOM Taxes positive", new TaxesTestData29() }, //
        { "30", "PriceIncludingTaxes 30: Doc BOM Taxes negative", new TaxesTestData30() }, //
        { "31", "PriceIncludingTaxes 31: Line BOM Taxes positive", new TaxesTestData31() }, //
        { "32", "PriceIncludingTaxes 32: Line BOM Taxes negative", new TaxesTestData32() }, //
        { "33", "PriceExcludingTaxes 33: Doc BOM+Exempt positive", new TaxesTestData33() }, //
        { "34", "PriceExcludingTaxes 34: Doc BOM+Exempt negative", new TaxesTestData34() }, //
        { "35", "PriceExcludingTaxes 35: Line BOM+Exempt positive", new TaxesTestData35() }, //
        { "36", "PriceExcludingTaxes 36: Line BOM+Exempt negative", new TaxesTestData36() }, //
        { "37", "PriceIncludingTaxes 37: Doc BOM+Exempt positive", new TaxesTestData37() }, //
        { "38", "PriceIncludingTaxes 38: Doc BOM+Exempt negative", new TaxesTestData38() }, //
        { "39", "PriceIncludingTaxes 39: Line BOM+Exempt positive", new TaxesTestData39() }, //
        { "40", "PriceIncludingTaxes 40: Line BOM+Exempt negative", new TaxesTestData40() }, //
        { "41", "PriceExcludingTaxes 41: Doc +1*10% +1*3% int", new TaxesTestData41() }, //
        { "42", "PriceExcludingTaxes 42: Doc +1*10% -1*3% int", new TaxesTestData42() }, //
        { "43", "PriceExcludingTaxes 43: Doc -1*10% +1*3% int", new TaxesTestData43() }, //
        { "44", "PriceExcludingTaxes 44: Doc -1*10% -1*3% int", new TaxesTestData44() }, //
        { "45", "PriceExcludingTaxes 45: Line +1*10% +1*3% int", new TaxesTestData45() }, //
        { "46", "PriceExcludingTaxes 46: Line +1*10% -1*3% int", new TaxesTestData46() }, //
        { "47", "PriceExcludingTaxes 47: Line -1*10% +1*3% int", new TaxesTestData47() }, //
        { "48", "PriceExcludingTaxes 48: Line -1*10% -1*3% int", new TaxesTestData48() }, //
        { "49", "PriceIncludingTaxes 49: Doc +1*10% +1*3% int", new TaxesTestData49() }, //
        { "50", "PriceIncludingTaxes 50: Doc +1*10% -1*3% int", new TaxesTestData50() }, //
        { "51", "PriceIncludingTaxes 51: Doc -1*10% +1*3% int", new TaxesTestData51() }, //
        { "52", "PriceIncludingTaxes 52: Doc -1*10% -1*3% int", new TaxesTestData52() }, //
        { "53", "PriceIncludingTaxes 53: Line +1*10% +1*3% int", new TaxesTestData53() }, //
        { "54", "PriceIncludingTaxes 54: Line +1*10% -1*3% int", new TaxesTestData54() }, //
        { "55", "PriceIncludingTaxes 55: Line -1*10% +1*3% int", new TaxesTestData55() }, //
        { "56", "PriceIncludingTaxes 56: Line -1*10% -1*3% int", new TaxesTestData56() }, //
        { "57", "PriceExcludingTaxes 57: Doc +1*10% +1*3% real", new TaxesTestData57() }, //
        { "58", "PriceExcludingTaxes 58: Doc +1*10% -1*3% real", new TaxesTestData58() }, //
        { "59", "PriceExcludingTaxes 59: Doc -1*10% +1*3% real", new TaxesTestData59() }, //
        { "60", "PriceExcludingTaxes 60: Doc -1*10% -1*3% real", new TaxesTestData60() }, //
        { "61", "PriceExcludingTaxes 61: Line +1*10% +1*3% real", new TaxesTestData61() }, //
        { "62", "PriceExcludingTaxes 62: Line +1*10% -1*3% real", new TaxesTestData62() }, //
        { "63", "PriceExcludingTaxes 63: Line -1*10% +1*3% real", new TaxesTestData63() }, //
        { "64", "PriceExcludingTaxes 64: Line -1*10% -1*3% real", new TaxesTestData64() }, //
        { "65", "PriceIncludingTaxes 65: Doc +1*10% +1*3% real", new TaxesTestData65() }, //
        { "66", "PriceIncludingTaxes 66: Doc +1*10% -1*3% real", new TaxesTestData66() }, //
        { "67", "PriceIncludingTaxes 67: Doc -1*10% +1*3% real", new TaxesTestData67() }, //
        { "68", "PriceIncludingTaxes 68: Doc -1*10% -1*3% real", new TaxesTestData68() }, //
        { "69", "PriceIncludingTaxes 69: Line +1*10% +1*3% real", new TaxesTestData69() }, //
        { "70", "PriceIncludingTaxes 70: Line +1*10% -1*3% real", new TaxesTestData70() }, //
        { "71", "PriceIncludingTaxes 71: Line -1*10% +1*3% real", new TaxesTestData71() }, //
        { "72", "PriceIncludingTaxes 72: Line -1*10% -1*3% real", new TaxesTestData72() }, //
        { "73", "PriceExcludingTaxes 73: Doc +1*20% +1*20%", new TaxesTestData73() }, //
        { "74", "PriceExcludingTaxes 74: Doc +1*20% -1*20%", new TaxesTestData74() }, //
        { "75", "PriceExcludingTaxes 75: Doc -1*20% +1*20%", new TaxesTestData75() }, //
        { "76", "PriceExcludingTaxes 76: Doc -1*20% -1*20%", new TaxesTestData76() }, //
        { "77", "PriceExcludingTaxes 77: Line +1*20% +1*20%", new TaxesTestData77() }, //
        { "78", "PriceExcludingTaxes 78: Line +1*20% -1*20%", new TaxesTestData78() }, //
        { "79", "PriceExcludingTaxes 79: Line -1*20% +1*20%", new TaxesTestData79() }, //
        { "80", "PriceExcludingTaxes 80: Line -1*20% -1*20%", new TaxesTestData80() }, //
        { "81", "PriceIncludingTaxes 81: Doc +1*20% +1*20%", new TaxesTestData81() }, //
        { "82", "PriceIncludingTaxes 82: Doc +1*20% -1*20%", new TaxesTestData82() }, //
        { "83", "PriceIncludingTaxes 83: Doc -1*20% +1*20%", new TaxesTestData83() }, //
        { "84", "PriceIncludingTaxes 84: Doc -1*20% -1*20%", new TaxesTestData84() }, //
        { "85", "PriceIncludingTaxes 85: Line +1*20% +1*20%", new TaxesTestData85() }, //
        { "86", "PriceIncludingTaxes 86: Line +1*20% -1*20%", new TaxesTestData86() }, //
        { "87", "PriceIncludingTaxes 87: Line -1*20% +1*20%", new TaxesTestData87() }, //
        { "88", "PriceIncludingTaxes 88: Line -1*20% -1*20%", new TaxesTestData88() }, //
        { "89", "PriceExcludingTaxes 89: Doc Normal +", new TaxesTestData89() }, //
        { "90", "PriceExcludingTaxes 90: Doc Normal -", new TaxesTestData90() }, //
        { "91", "PriceExcludingTaxes 91: Line Normal +", new TaxesTestData91() }, //
        { "92", "PriceExcludingTaxes 92: Line Normal -", new TaxesTestData92() }, //
        { "93", "PriceIncludingTaxes 93: Doc Normal +", new TaxesTestData93() }, //
        { "94", "PriceIncludingTaxes 94: Doc Normal -", new TaxesTestData94() }, //
        { "95", "PriceIncludingTaxes 95: Line Normal +", new TaxesTestData95() }, //
        { "96", "PriceIncludingTaxes 96: Line Normal -", new TaxesTestData96() }, //
        { "97", "PriceExcludingTaxes 97: Doc Cascade 1 +", new TaxesTestData97() }, //
        { "98", "PriceExcludingTaxes 98: Doc Cascade 1 -", new TaxesTestData98() }, //
        { "99", "PriceExcludingTaxes 99: Line Cascade 1 +", new TaxesTestData99() }, //
        { "100", "PriceExcludingTaxes 100: Line Cascade 1 -", new TaxesTestData100() }, //
        { "101", "PriceIncludingTaxes 101: Doc Cascade 1 +", new TaxesTestData101() }, //
        { "102", "PriceIncludingTaxes 102: Doc Cascade 1 -", new TaxesTestData102() }, //
        { "103", "PriceIncludingTaxes 103: Line Cascade 1 +", new TaxesTestData103() }, //
        { "104", "PriceIncludingTaxes 104: Line Cascade 1 -", new TaxesTestData104() }, //
        { "105", "PriceExcludingTaxes 105: Doc Cascade 2 +", new TaxesTestData105() }, //
        { "106", "PriceExcludingTaxes 106: Doc Cascade 2 -", new TaxesTestData106() }, //
        { "107", "PriceExcludingTaxes 107: Line Cascade 2 +", new TaxesTestData107() }, //
        { "108", "PriceExcludingTaxes 108: Line Cascade 2 -", new TaxesTestData108() }, //
        { "109", "PriceIncludingTaxes 109: Doc Cascade 2 +", new TaxesTestData109() }, //
        { "110", "PriceIncludingTaxes 110: Doc Cascade 2 -", new TaxesTestData110() }, //
        { "111", "PriceIncludingTaxes 111: Line Cascade 2 +", new TaxesTestData111() }, //
        { "112", "PriceIncludingTaxes 112: Line Cascade 2 -", new TaxesTestData112() }, //
        { "113", "PriceExcludingTaxes 113: Doc Cascade 3 +", new TaxesTestData113() }, //
        { "114", "PriceExcludingTaxes 114: Doc Cascade 3 -", new TaxesTestData114() }, //
        { "115", "PriceExcludingTaxes 115: Line Cascade 3 +", new TaxesTestData115() }, //
        { "116", "PriceExcludingTaxes 116: Line Cascade 3 -", new TaxesTestData116() }, //
        { "117", "PriceIncludingTaxes 117: Doc Cascade 3 +", new TaxesTestData117() }, //
        { "118", "PriceIncludingTaxes 118: Doc Cascade 3 -", new TaxesTestData118() }, //
        { "119", "PriceIncludingTaxes 119: Line Cascade 3 +", new TaxesTestData119() }, //
        { "120", "PriceIncludingTaxes 120: Line Cascade 3 -", new TaxesTestData120() }, //
        { "121", "PriceExcludingTaxes 121: Doc Dependant 1 +", new TaxesTestData121() }, //
        { "122", "PriceExcludingTaxes 122: Doc Dependant 1 -", new TaxesTestData122() }, //
        { "123", "PriceExcludingTaxes 123: Line Dependant 1 +", new TaxesTestData123() }, //
        { "124", "PriceExcludingTaxes 124: Line Dependant 1 -", new TaxesTestData124() }, //
        { "125", "PriceIncludingTaxes 125: Doc Dependant 1 +", new TaxesTestData125() }, //
        { "126", "PriceIncludingTaxes 126: Doc Dependant 1 -", new TaxesTestData126() }, //
        { "127", "PriceIncludingTaxes 127: Line Dependant 1 +", new TaxesTestData127() }, //
        { "128", "PriceIncludingTaxes 128: Line Dependant 1 -", new TaxesTestData128() }, //
        { "129", "PriceExcludingTaxes 129: Doc Dependant 2 +", new TaxesTestData129() }, //
        { "130", "PriceExcludingTaxes 130: Doc Dependant 2 -", new TaxesTestData130() }, //
        { "131", "PriceExcludingTaxes 131: Line Dependant 2 +", new TaxesTestData131() }, //
        { "132", "PriceExcludingTaxes 132: Line Dependant 2 -", new TaxesTestData132() }, //
        { "133", "PriceIncludingTaxes 133: Doc Dependant 2 +", new TaxesTestData133() }, //
        { "134", "PriceIncludingTaxes 134: Doc Dependant 2 -", new TaxesTestData134() }, //
        { "135", "PriceIncludingTaxes 135: Line Dependant 2 +", new TaxesTestData135() }, //
        { "136", "PriceIncludingTaxes 136: Line Dependant 2 -", new TaxesTestData136() }, //
        { "137", "PriceExcludingTaxes 137: Doc Cascade+Dependant 1 +", new TaxesTestData137() }, //
        { "138", "PriceExcludingTaxes 138: Doc Cascade+Dependant 1 -", new TaxesTestData138() }, //
        { "139", "PriceExcludingTaxes 139: Line Cascade+Dependant 1 +", new TaxesTestData139() }, //
        { "140", "PriceExcludingTaxes 140: Line Cascade+Dependant 1 -", new TaxesTestData140() }, //
        { "141", "PriceIncludingTaxes 141: Doc Cascade+Dependant 1 +", new TaxesTestData141() }, //
        { "142", "PriceIncludingTaxes 142: Doc Cascade+Dependant 1 -", new TaxesTestData142() }, //
        { "143", "PriceIncludingTaxes 143: Line Cascade+Dependant 1 +", new TaxesTestData143() }, //
        { "144", "PriceIncludingTaxes 144: Line Cascade+Dependant 1 -", new TaxesTestData144() }, //
        { "145", "PriceExcludingTaxes 145: Doc Cascade+Dependant 2 +", new TaxesTestData145() }, //
        { "146", "PriceExcludingTaxes 146: Doc Cascade+Dependant 2 -", new TaxesTestData146() }, //
        { "147", "PriceExcludingTaxes 147: Line Cascade+Dependant 2 +", new TaxesTestData147() }, //
        { "148", "PriceExcludingTaxes 148: Line Cascade+Dependant 2 -", new TaxesTestData148() }, //
        { "149", "PriceIncludingTaxes 149: Doc Cascade+Dependant 2 +", new TaxesTestData149() }, //
        { "150", "PriceIncludingTaxes 150: Doc Cascade+Dependant 2 -", new TaxesTestData150() }, //
        { "151", "PriceIncludingTaxes 151: Line Cascade+Dependant 2 +", new TaxesTestData151() }, //
        { "152", "PriceIncludingTaxes 152: Line Cascade+Dependant 2 -", new TaxesTestData152() }, //
        { "153", "PriceExcludingTaxes 153: Doc Cascade+Dependant 3 +", new TaxesTestData153() }, //
        { "154", "PriceExcludingTaxes 154: Doc Cascade+Dependant 3 -", new TaxesTestData154() }, //
        { "155", "PriceExcludingTaxes 155: Line Cascade+Dependant 3 +", new TaxesTestData155() }, //
        { "156", "PriceExcludingTaxes 156: Line Cascade+Dependant 3 -", new TaxesTestData156() }, //
        { "157", "PriceIncludingTaxes 157: Doc Cascade+Dependant 3 +", new TaxesTestData157() }, //
        { "158", "PriceIncludingTaxes 158: Doc Cascade+Dependant 3 -", new TaxesTestData158() }, //
        { "159", "PriceIncludingTaxes 159: Line Cascade+Dependant 3 +", new TaxesTestData159() }, //
        { "160", "PriceIncludingTaxes 160: Line Cascade+Dependant 3 -", new TaxesTestData160() }, //
        { "161", "PriceExcludingTaxes 161: Doc 20% positive", new TaxesTestData161() }, //
        { "162", "PriceExcludingTaxes 162: Doc 20% negative", new TaxesTestData162() }, //
        { "163", "PriceExcludingTaxes 163: Line 20% positive", new TaxesTestData163() }, //
        { "164", "PriceExcludingTaxes 164: Line 20% negative", new TaxesTestData164() }, //
        { "165", "PriceIncludingTaxes 165: Doc 20% positive", new TaxesTestData165() }, //
        { "166", "PriceIncludingTaxes 166: Doc 20% negative", new TaxesTestData166() }, //
        { "167", "PriceIncludingTaxes 167: Line 20% positive", new TaxesTestData167() }, //
        { "168", "PriceIncludingTaxes 168: Line 20% negative", new TaxesTestData168() }, //
        { "169", "PriceExcludingTaxes 169: Doc Small 10% positive", new TaxesTestData169() }, //
        { "170", "PriceExcludingTaxes 170: Doc Small 10% negative", new TaxesTestData170() }, //
        { "171", "PriceExcludingTaxes 171: Line Small 10% positive", new TaxesTestData171() }, //
        { "172", "PriceExcludingTaxes 172: Line Small 10% negative", new TaxesTestData172() }, //
        { "173", "PriceIncludingTaxes 173: Doc Small 10% positive", new TaxesTestData173() }, //
        { "174", "PriceIncludingTaxes 174: Doc Small 10% negative", new TaxesTestData174() }, //
        { "175", "PriceIncludingTaxes 175: Line Small 10% positive", new TaxesTestData175() }, //
        { "176", "PriceIncludingTaxes 176: Line Small 10% negative", new TaxesTestData176() }, //
        { "177", "PriceExcludingTaxes 177: Doc Big 10% positive", new TaxesTestData177() }, //
        { "178", "PriceExcludingTaxes 178: Doc Big 10% negative", new TaxesTestData178() }, //
        { "179", "PriceExcludingTaxes 179: Line Big 10% positive", new TaxesTestData179() }, //
        { "180", "PriceExcludingTaxes 180: Line Big 10% negative", new TaxesTestData180() }, //
        { "181", "PriceIncludingTaxes 181: Doc Big 10% positive", new TaxesTestData181() }, //
        { "182", "PriceIncludingTaxes 182: Doc Big 10% negative", new TaxesTestData182() }, //
        { "183", "PriceIncludingTaxes 183: Line Big 10% positive", new TaxesTestData183() }, //
        { "184", "PriceIncludingTaxes 184: Line Big 10% negative", new TaxesTestData184() }, //
        { "185", "PriceExcludingTaxes 185: Doc Small Discount positive", new TaxesTestData185() }, //
        { "186", "PriceExcludingTaxes 186: Doc Small Discount negative", new TaxesTestData186() }, //
        { "187", "PriceExcludingTaxes 187: Line Small Discount positive", new TaxesTestData187() }, //
        { "188", "PriceExcludingTaxes 188: Line Small Discount negative", new TaxesTestData188() }, //
        { "189", "PriceIncludingTaxes 189: Doc Small Discount positive", new TaxesTestData189() }, //
        { "190", "PriceIncludingTaxes 190: Doc Small Discount negative", new TaxesTestData190() }, //
        { "191", "PriceIncludingTaxes 191: Line Small Discount positive", new TaxesTestData191() }, //
        { "192", "PriceIncludingTaxes 192: Line Small Discount negative", new TaxesTestData192() }, //
        { "193", "PriceExcludingTaxes 193: Doc Big Discount positive", new TaxesTestData193() }, //
        { "194", "PriceExcludingTaxes 194: Doc Big Discount negative", new TaxesTestData194() }, //
        { "195", "PriceExcludingTaxes 195: Line Big Discount positive", new TaxesTestData195() }, //
        { "196", "PriceExcludingTaxes 196: Line Big Discount negative", new TaxesTestData196() }, //
        { "197", "PriceIncludingTaxes 197: Doc Big Discount positive", new TaxesTestData197() }, //
        { "198", "PriceIncludingTaxes 198: Doc Big Discount negative", new TaxesTestData198() }, //
        { "199", "PriceIncludingTaxes 199: Line Big Discount positive", new TaxesTestData199() }, //
        { "200", "PriceIncludingTaxes 200: Line Big Discount negative", new TaxesTestData200() }, //
    });
  }

  @Test
  public void testPurchaseOrderTaxes() {
    testOrderTaxes(false);
  }

  @Test
  public void testSalesOrderTaxes() {
    testOrderTaxes(true);
  }

  @Test
  public void testPurchaseInvoiceTaxes() {
    testInvoiceTaxes(false);
  }

  @Test
  public void testSalesInvoiceTaxes() {
    testInvoiceTaxes(true);
  }

  private void testOrderTaxes(boolean isSales) {
    // Set QA context
    OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
    try {
      updateTax(true);

      Order testOrder = createOrder(isSales);
      testOrder(testOrder, false, false);

      testOrder = completeOrder(testOrder);
      testOrder(testOrder, true, false);

      testOrder = reactivateOrder(testOrder);
      testOrder(testOrder, false, false);

      testOrder = updateOrder(testOrder);
      testOrder(testOrder, false, true);

      testOrder = completeOrder(testOrder);
      testOrder(testOrder, true, true);

      testOrder = reactivateOrder(testOrder);
      testOrder(testOrder, false, true);

      deleteOrder(testOrder);
      log.info("Test Completed successfully");
    }

    catch (Exception e) {
      log.error("Error when executing testOrderTaxes", e);
      assertFalse(true);
    }

    finally {
      updateTax(false);
    }
  }

  private void testInvoiceTaxes(boolean isSales) {
    // Set QA context
    OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
    try {
      updateTax(true);

      Invoice testInvoice = createInvoice(isSales);
      testInvoice(testInvoice, false, false);

      testInvoice = completeInvoice(testInvoice);
      testInvoice(testInvoice, true, false);

      testInvoice = reactivateInvoice(testInvoice);
      testInvoice(testInvoice, false, false);

      testInvoice = updateInvoice(testInvoice);
      testInvoice(testInvoice, false, true);

      testInvoice = completeInvoice(testInvoice);
      testInvoice(testInvoice, true, true);

      testInvoice = reactivateInvoice(testInvoice);
      testInvoice(testInvoice, false, true);

      deleteInvoice(testInvoice);
      log.info("Test Completed successfully");
    }

    catch (Exception e) {
      log.error("Error when executing testInvoiceTaxes", e);
      assertFalse(true);
    }

    finally {
      updateTax(false);
    }
  }

  private Order createOrder(boolean isSales) {
    Order order = OBDal.getInstance().get(Order.class, isSales ? SALESORDER_ID : PURCHASEORDER_ID);
    Order testOrder = (Order) DalUtil.copy(order, false);
    String documentNo = isPriceIncludingTaxes ? "PriceIncludingTaxes" : "PriceExcludingTaxes";
    testOrder.setDocumentNo(documentNo + " " + testNumber);
    testOrder.setBusinessPartner(OBDal.getInstance()
        .getProxy(BusinessPartner.class,
            isSales ? BPartnerDataConstants.CUSTOMER_A : BPartnerDataConstants.VENDOR_A));
    testOrder.setSummedLineAmount(BigDecimal.ZERO);
    testOrder.setGrandTotalAmount(BigDecimal.ZERO);
    testOrder.setPriceIncludesTax(isPriceIncludingTaxes);
    testOrder.setPriceList(OBDal.getInstance()
        .getProxy(PriceList.class,
            isPriceIncludingTaxes
                ? isSales ? PRICEINCLUDINGTAXES_PRICELIST_SALES
                    : PRICEINCLUDINGTAXES_PRICELIST_PURCHASE
                : isSales ? PRICEEXCLUDINGTAXES_PRICELIST_SALES
                    : PRICEEXCLUDINGTAXES_PRICELIST_PURCHASE));
    OBDal.getInstance().save(testOrder);

    OrderLine orderLine = order.getOrderLineList().get(0);
    OrderDiscount orderDiscount = order.getOrderDiscountList().get(0);
    for (int i = 0; i < linesData.length; i++) {
      Product product = OBDal.getInstance().get(Product.class, linesData[i].getProductId());

      if (product.getPricingDiscountList().isEmpty()) {
        OrderLine testOrderLine = (OrderLine) DalUtil.copy(orderLine, false);
        testOrderLine.setLineNo((i + 1) * 10L);
        testOrderLine.setBusinessPartner(OBDal.getInstance()
            .getProxy(BusinessPartner.class,
                isSales ? BPartnerDataConstants.CUSTOMER_A : BPartnerDataConstants.VENDOR_A));
        testOrderLine.setProduct(product);
        testOrderLine.setUOM(product.getUOM());
        testOrderLine.setOrderedQuantity(linesData[i].getQuantity());

        if (isPriceIncludingTaxes) {
          testOrderLine.setGrossUnitPrice(linesData[i].getPrice());
          testOrderLine.setGrossListPrice(linesData[i].getPrice());
          testOrderLine.setBaseGrossUnitPrice(linesData[i].getPrice());
        } else {
          testOrderLine.setUnitPrice(linesData[i].getPrice());
          testOrderLine.setListPrice(linesData[i].getPrice());
          testOrderLine.setStandardPrice(linesData[i].getPrice());
        }
        testOrderLine.setTax(OBDal.getInstance().getProxy(TaxRate.class, linesData[i].getTaxid()));
        testOrderLine
            .setLineGrossAmount(linesData[i].getQuantity().multiply(linesData[i].getPrice()));
        testOrderLine
            .setLineNetAmount(linesData[i].getQuantity().multiply(linesData[i].getPrice()));

        testOrderLine.setSalesOrder(testOrder);
        testOrder.getOrderLineList().add(testOrderLine);
        OBDal.getInstance().save(testOrderLine);
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(testOrderLine);
      }

      else {
        OrderDiscount testOrderDiscount = (OrderDiscount) DalUtil.copy(orderDiscount, false);
        testOrderDiscount.setDiscount(product.getPricingDiscountList().get(0));
        orderDiscount.setLineNo((i + 1) * 10L);

        testOrderDiscount.setSalesOrder(testOrder);
        testOrder.getOrderDiscountList().add(testOrderDiscount);
        OBDal.getInstance().save(testOrderDiscount);
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(testOrderDiscount);
      }
    }

    OBDal.getInstance().save(testOrder);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(testOrder);

    log.debug("Order Created:" + testOrder.getDocumentNo());
    log.debug(testDescription);

    return testOrder;
  }

  private Invoice createInvoice(boolean isSales) {
    Invoice invoice = OBDal.getInstance()
        .get(Invoice.class, isSales ? SALESINVOICE_ID : PURCHASEINVOICE_ID);
    Invoice testInvoice = (Invoice) DalUtil.copy(invoice, false);
    String documentNo = isPriceIncludingTaxes ? "PriceIncludingTaxes" : "PriceExcludingTaxes";
    testInvoice.setDocumentNo(documentNo + " " + testNumber);
    testInvoice.setDescription(testDescription);
    testInvoice.setBusinessPartner(OBDal.getInstance()
        .getProxy(BusinessPartner.class,
            isSales ? BPartnerDataConstants.CUSTOMER_A : BPartnerDataConstants.VENDOR_A));
    testInvoice.setSummedLineAmount(BigDecimal.ZERO);
    testInvoice.setGrandTotalAmount(BigDecimal.ZERO);
    testInvoice.setPriceIncludesTax(isPriceIncludingTaxes);
    testInvoice.setPriceList(OBDal.getInstance()
        .getProxy(PriceList.class,
            isPriceIncludingTaxes
                ? isSales ? PRICEINCLUDINGTAXES_PRICELIST_SALES
                    : PRICEINCLUDINGTAXES_PRICELIST_PURCHASE
                : isSales ? PRICEEXCLUDINGTAXES_PRICELIST_SALES
                    : PRICEEXCLUDINGTAXES_PRICELIST_PURCHASE));
    OBDal.getInstance().save(testInvoice);

    InvoiceLine invoiceLine = invoice.getInvoiceLineList().get(0);
    InvoiceDiscount invoiceDiscount = invoice.getInvoiceDiscountList().get(0);
    for (int i = 0; i < linesData.length; i++) {
      Product product = OBDal.getInstance().get(Product.class, linesData[i].getProductId());

      if (product.getPricingDiscountList().isEmpty()) {
        InvoiceLine testInvoiceLine = (InvoiceLine) DalUtil.copy(invoiceLine, false);
        testInvoiceLine.setLineNo((i + 1) * 10L);
        testInvoiceLine.setBusinessPartner(OBDal.getInstance()
            .getProxy(BusinessPartner.class,
                isSales ? BPartnerDataConstants.CUSTOMER_A : BPartnerDataConstants.VENDOR_A));
        testInvoiceLine.setProduct(product);
        testInvoiceLine.setUOM(product.getUOM());
        testInvoiceLine.setInvoicedQuantity(linesData[i].getQuantity());
        if (isPriceIncludingTaxes) {
          testInvoiceLine.setGrossUnitPrice(linesData[i].getPrice());
          testInvoiceLine.setGrossListPrice(linesData[i].getPrice());
          testInvoiceLine.setBaseGrossUnitPrice(linesData[i].getPrice());
        } else {
          testInvoiceLine.setUnitPrice(linesData[i].getPrice());
          testInvoiceLine.setListPrice(linesData[i].getPrice());
          testInvoiceLine.setStandardPrice(linesData[i].getPrice());
        }
        testInvoiceLine
            .setTax(OBDal.getInstance().getProxy(TaxRate.class, linesData[i].getTaxid()));
        testInvoiceLine
            .setGrossAmount(linesData[i].getQuantity().multiply(linesData[i].getPrice()));
        testInvoiceLine
            .setLineNetAmount(linesData[i].getQuantity().multiply(linesData[i].getPrice()));

        testInvoiceLine.setInvoice(testInvoice);
        testInvoice.getInvoiceLineList().add(testInvoiceLine);

        OBDal.getInstance().save(testInvoiceLine);
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(testInvoiceLine);
      }

      else {
        InvoiceDiscount testInvoiceDiscount = (InvoiceDiscount) DalUtil.copy(invoiceDiscount,
            false);
        testInvoiceDiscount.setDiscount(product.getPricingDiscountList().get(0));
        invoiceDiscount.setLineNo((i + 1) * 10L);

        testInvoiceDiscount.setInvoice(testInvoice);
        testInvoice.getInvoiceDiscountList().add(testInvoiceDiscount);
        OBDal.getInstance().save(testInvoiceDiscount);
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(testInvoiceDiscount);
      }
    }

    OBDal.getInstance().save(testInvoice);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(testInvoice);

    log.debug("Invoice Created: " + testInvoice.getDocumentNo());
    log.debug(testDescription);

    return testInvoice;
  }

  private Order updateOrder(Order testOrder) {

    OBCriteria<OrderLine> obc = OBDal.getInstance().createCriteria(OrderLine.class);
    obc.add(Restrictions.eq(OrderLine.PROPERTY_SALESORDER, testOrder));
    obc.addOrderBy(OrderLine.PROPERTY_LINENO, true);

    int i = 0;
    for (OrderLine testOrderLine : obc.list()) {
      testOrderLine.setOrderedQuantity(linesData[i].getQuantityUpdated());
      if (isPriceIncludingTaxes) {
        testOrderLine.setGrossUnitPrice(linesData[i].getPriceUpdated());
        testOrderLine.setGrossListPrice(linesData[i].getPriceUpdated());
        testOrderLine.setBaseGrossUnitPrice(linesData[i].getPriceUpdated());
      } else {
        testOrderLine.setUnitPrice(linesData[i].getPriceUpdated());
        testOrderLine.setListPrice(linesData[i].getPriceUpdated());
        testOrderLine.setStandardPrice(linesData[i].getPriceUpdated());
      }
      testOrderLine.setLineGrossAmount(
          linesData[i].getQuantityUpdated().multiply(linesData[i].getPriceUpdated()));
      testOrderLine.setLineNetAmount(
          linesData[i].getQuantityUpdated().multiply(linesData[i].getPriceUpdated()));

      OBDal.getInstance().save(testOrderLine);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(testOrderLine);

      i++;
    }

    OBDal.getInstance().save(testOrder);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(testOrder);

    return testOrder;
  }

  private Invoice updateInvoice(Invoice testInvoice) {

    OBCriteria<InvoiceLine> obc = OBDal.getInstance().createCriteria(InvoiceLine.class);
    obc.add(Restrictions.eq(InvoiceLine.PROPERTY_INVOICE, testInvoice));
    obc.addOrderBy(InvoiceLine.PROPERTY_LINENO, true);

    int i = 0;
    for (InvoiceLine testInvoiceLine : obc.list()) {
      testInvoiceLine.setInvoicedQuantity(linesData[i].getQuantityUpdated());
      if (isPriceIncludingTaxes) {
        testInvoiceLine.setGrossUnitPrice(linesData[i].getPriceUpdated());
        testInvoiceLine.setGrossListPrice(linesData[i].getPriceUpdated());
        testInvoiceLine.setBaseGrossUnitPrice(linesData[i].getPriceUpdated());
      } else {
        testInvoiceLine.setUnitPrice(linesData[i].getPriceUpdated());
        testInvoiceLine.setListPrice(linesData[i].getPriceUpdated());
        testInvoiceLine.setStandardPrice(linesData[i].getPriceUpdated());
      }
      testInvoiceLine.setGrossAmount(
          linesData[i].getQuantityUpdated().multiply(linesData[i].getPriceUpdated()));
      testInvoiceLine.setLineNetAmount(
          linesData[i].getQuantityUpdated().multiply(linesData[i].getPriceUpdated()));

      OBDal.getInstance().save(testInvoiceLine);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(testInvoiceLine);

      i++;
    }

    OBDal.getInstance().save(testInvoice);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(testInvoice);

    return testInvoice;
  }

  private void deleteOrder(Order testOrder) {

    for (OrderLine testOrderLine : testOrder.getOrderLineList()) {
      OBDal.getInstance().remove(testOrderLine);
    }
    testOrder.getOrderLineList().clear();
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(testOrder);

    if (testOrder.getOrderTaxList().size() > 0) {
      assertTrue("Document Taxes not properly removed", false);
    }

    assertThat("GrandTotal holds an amount when order has no lines",
        testOrder.getGrandTotalAmount(), comparesEqualTo(BigDecimal.ZERO));
    assertThat("TotalLines holds an amount when order has no lines",
        testOrder.getSummedLineAmount(), comparesEqualTo(BigDecimal.ZERO));

    OBDal.getInstance().remove(testOrder);
    OBDal.getInstance().flush();

    log.debug("Order Deleted:" + testOrder.getDocumentNo());
  }

  private void deleteInvoice(Invoice testInvoice) {

    for (InvoiceLine testInvoiceLine : testInvoice.getInvoiceLineList()) {
      OBDal.getInstance().remove(testInvoiceLine);
    }
    testInvoice.getInvoiceLineList().clear();
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(testInvoice);

    if (testInvoice.getInvoiceTaxList().size() > 0) {
      assertTrue("Document Taxes not properly removed", false);
    }

    assertThat("GrandTotal holds an amount when invoice has no lines",
        testInvoice.getGrandTotalAmount(), comparesEqualTo(BigDecimal.ZERO));
    assertThat("TotalLines holds an amount when invoice has no lines",
        testInvoice.getSummedLineAmount(), comparesEqualTo(BigDecimal.ZERO));

    OBDal.getInstance().remove(testInvoice);
    OBDal.getInstance().flush();

    log.debug("Invoice Deleted:" + testInvoice.getDocumentNo());
  }

  private void updateTax(boolean isStart) {
    if (!isTaxDocumentLevel) {
      StringBuffer update = new StringBuffer();
      update.append(" update " + TaxRate.ENTITY_NAME);
      update.append(" set " + TaxRate.PROPERTY_DOCTAXAMOUNT + " = :docTax");
      update.append(" where " + TaxRate.PROPERTY_CLIENT + ".id = :clientId");
      @SuppressWarnings("rawtypes")
      Query updateQry = OBDal.getInstance().getSession().createQuery(update.toString());
      updateQry.setParameter("docTax", isStart ? "L" : "D");
      updateQry.setParameter("clientId", CLIENT_ID);
      updateQry.executeUpdate();
      OBDal.getInstance().flush();
    }
  }

  private Order completeOrder(Order testOrder) {
    testOrder.setDocumentAction("CO");
    return processOrder(testOrder);
  }

  private Invoice completeInvoice(Invoice testInvoice) {
    testInvoice.setDocumentAction("CO");
    return processInvoice(testInvoice);
  }

  private Order reactivateOrder(Order testOrder) {
    testOrder.setDocumentAction("RE");
    return processOrder(testOrder);
  }

  private Invoice reactivateInvoice(Invoice testInvoice) {
    testInvoice.setDocumentAction("RE");
    return processInvoice(testInvoice);
  }

  private Order processOrder(Order testOrder) {
    final List<Object> params = new ArrayList<Object>();
    params.add(null);
    params.add(testOrder.getId());
    CallStoredProcedure.getInstance()
        .call(ORDER_COMPLETE_PROCEDURE_NAME, params, null, true, false);
    OBDal.getInstance().refresh(testOrder);
    return testOrder;
  }

  private Invoice processInvoice(Invoice testInvoice) {
    final List<Object> params = new ArrayList<Object>();
    params.add(null);
    params.add(testInvoice.getId());
    CallStoredProcedure.getInstance()
        .call(INVOICE_COMPLETE_PROCEDURE_NAME, params, null, true, false);
    OBDal.getInstance().refresh(testInvoice);
    return testInvoice;
  }

  private void testOrder(Order testOrder, boolean isCompleted, boolean isUpdated) {

    boolean round = isPriceIncludingTaxes && isTaxDocumentLevel && !isCompleted;
    int stdPrecision = testOrder.getCurrency().getStandardPrecision().intValue();

    // Assert line taxes and amounts
    int i = 0;
    OBCriteria<OrderLine> obc1 = OBDal.getInstance().createCriteria(OrderLine.class);
    obc1.add(Restrictions.eq(OrderLine.PROPERTY_SALESORDER, testOrder));
    obc1.addOrderBy(OrderLine.PROPERTY_LINENO, true);
    for (OrderLine testOrderLine : obc1.list()) {
      OBDal.getInstance().refresh(testOrderLine);

      int n = 0;
      OBCriteria<OrderLineTax> obc2 = OBDal.getInstance().createCriteria(OrderLineTax.class);
      obc2.add(Restrictions.eq(OrderLineTax.PROPERTY_SALESORDERLINE, testOrderLine));
      obc2.addOrderBy(OrderLineTax.PROPERTY_LINENO, true);
      for (OrderLineTax linetax : obc2.list()) {
        OBDal.getInstance().refresh(linetax);

        log.debug(linetax.getTax().getIdentifier());
        log.debug(linetax.getTax().getId());
        log.debug(linetax.getTaxableAmount().toString());
        log.debug(linetax.getTaxAmount().toString());

        if (!linesData[i].getLineTaxes().containsKey(linetax.getTax().getId())) {
          assertTrue(
              testDescription + ". Tax Should not be present: " + linetax.getTax().getIdentifier(),
              false);
        }

        // Assert line taxes
        BigDecimal expectedTaxableAmount = new BigDecimal(isUpdated
            ? (isCompleted ? linesData[i].getLineTaxes().get(linetax.getTax().getId())[6]
                : linesData[i].getLineTaxes().get(linetax.getTax().getId())[4])
            : (isCompleted ? linesData[i].getLineTaxes().get(linetax.getTax().getId())[2]
                : linesData[i].getLineTaxes().get(linetax.getTax().getId())[0]));
        BigDecimal expectedTaxAmount = new BigDecimal(isUpdated
            ? (isCompleted ? linesData[i].getLineTaxes().get(linetax.getTax().getId())[7]
                : linesData[i].getLineTaxes().get(linetax.getTax().getId())[5])
            : (isCompleted ? linesData[i].getLineTaxes().get(linetax.getTax().getId())[3]
                : linesData[i].getLineTaxes().get(linetax.getTax().getId())[1]));
        assertThat("Wrong taxable amount for line in document",
            round(linetax.getTaxableAmount(), round, stdPrecision),
            comparesEqualTo(expectedTaxableAmount));
        assertThat("Wrong tax amount for line in document", linetax.getTaxAmount(),
            comparesEqualTo(expectedTaxAmount));

        n++;
      }

      if (linesData[i].getLineTaxes().size() != n) {
        assertTrue(testDescription + ". Number of lines obtained("
            + linesData[i].getLineTaxes().size() + ") different than expected (" + n + ")", false);
      }

      // Assert line amounts
      BigDecimal expectedGrossAmount = new BigDecimal(isUpdated
          ? (isCompleted ? linesData[i].getLineAmounts()[6] : linesData[i].getLineAmounts()[4])
          : (isCompleted ? linesData[i].getLineAmounts()[2] : linesData[i].getLineAmounts()[0]));
      BigDecimal expectedNetAmount = new BigDecimal(isUpdated
          ? (isCompleted ? linesData[i].getLineAmounts()[7] : linesData[i].getLineAmounts()[5])
          : (isCompleted ? linesData[i].getLineAmounts()[3] : linesData[i].getLineAmounts()[1]));
      assertThat("Wrong Order line Gross Amount", testOrderLine.getLineGrossAmount(),
          comparesEqualTo(expectedGrossAmount));
      assertThat("Wrong Order line Net Amount", testOrderLine.getLineNetAmount(),
          comparesEqualTo(expectedNetAmount));

      i++;
    }

    // Assert header taxes and amounts
    int n = 0;
    OBCriteria<OrderTax> obc3 = OBDal.getInstance().createCriteria(OrderTax.class);
    obc3.add(Restrictions.eq(OrderTax.PROPERTY_SALESORDER, testOrder));
    obc3.addOrderBy(OrderLineTax.PROPERTY_LINENO, true);

    for (OrderTax tax : obc3.list()) {
      OBDal.getInstance().refresh(tax);

      log.debug(tax.getTax().getIdentifier());
      log.debug(tax.getTax().getId());
      log.debug(tax.getTaxableAmount().toString());
      log.debug(tax.getTaxAmount().toString());

      if (!docTaxes.containsKey(tax.getTax().getId())) {
        assertTrue(testDescription + ". Tax Should not be present: " + tax.getTax().getIdentifier(),
            false);
      }

      // Assert header taxes
      BigDecimal expectedTaxableAmount = new BigDecimal(isUpdated
          ? (isCompleted ? docTaxes.get(tax.getTax().getId())[6]
              : docTaxes.get(tax.getTax().getId())[4])
          : (isCompleted ? docTaxes.get(tax.getTax().getId())[2]
              : docTaxes.get(tax.getTax().getId())[0]));
      BigDecimal expectedTaxAmount = new BigDecimal(isUpdated
          ? (isCompleted ? docTaxes.get(tax.getTax().getId())[7]
              : docTaxes.get(tax.getTax().getId())[5])
          : (isCompleted ? docTaxes.get(tax.getTax().getId())[3]
              : docTaxes.get(tax.getTax().getId())[1]));
      assertThat("Wrong taxable amount for document",
          round(tax.getTaxableAmount(), round, stdPrecision),
          comparesEqualTo(expectedTaxableAmount));
      assertThat("Wrong tax amount for document", tax.getTaxAmount(),
          comparesEqualTo(expectedTaxAmount));

      n++;
    }

    if (docTaxes.size() != n) {
      assertTrue(testDescription + ". Number of lines obtained(" + docTaxes.size()
          + ") different than expected (" + n + ")", false);
    }

    // Assert header amounts
    BigDecimal expectedGrossAmount = new BigDecimal(
        isUpdated ? (isCompleted ? docAmounts[6] : docAmounts[4])
            : (isCompleted ? docAmounts[2] : docAmounts[0]));
    BigDecimal expectedNetAmount = new BigDecimal(
        isUpdated ? (isCompleted ? docAmounts[7] : docAmounts[5])
            : (isCompleted ? docAmounts[3] : docAmounts[1]));
    assertThat("Wrong Order GrandTotal", testOrder.getGrandTotalAmount(),
        comparesEqualTo(expectedGrossAmount));
    assertThat("Wrong Order TotalLines", testOrder.getSummedLineAmount(),
        comparesEqualTo(expectedNetAmount));

    // Final Asserts
    BigDecimal taxSum = BigDecimal.ZERO;
    BigDecimal netSum = BigDecimal.ZERO;
    BigDecimal grossSum = BigDecimal.ZERO;

    for (OrderLine testOrderLine : obc1.list()) {
      BigDecimal netAmount = testOrderLine.getLineNetAmount();
      BigDecimal grossAmount = testOrderLine.getLineGrossAmount();

      if (isPriceIncludingTaxes) {
        assertThat("Line Gross Price * Line Quantity <> Line Gross Amount", grossAmount,
            comparesEqualTo(
                testOrderLine.getGrossUnitPrice().multiply(testOrderLine.getOrderedQuantity())));
      } else {
        assertThat("Line Net Price * Line Quantity <> Line Net Amount", netAmount, comparesEqualTo(
            testOrderLine.getUnitPrice().multiply(testOrderLine.getOrderedQuantity())));
      }

      netSum = netSum.add(netAmount);
      grossSum = grossSum.add(grossAmount);
    }

    for (OrderTax tax : obc3.list()) {
      BigDecimal taxAmount = tax.getTaxAmount();
      taxSum = taxSum.add(taxAmount);
    }

    if (isCompleted) {
      assertThat("Line Net Amount Sum <> Document Net Amount", testOrder.getSummedLineAmount(),
          comparesEqualTo(netSum));
    }
    if (isPriceIncludingTaxes) {
      assertThat("Line Gross Amount Sum <> Document Gross Amount", testOrder.getGrandTotalAmount(),
          comparesEqualTo(grossSum));
    }
    if (isCompleted) {
      assertThat("Line Net Amount Sum + Tax Sum <> Document Gross Amount",
          testOrder.getGrandTotalAmount(), comparesEqualTo(netSum.add(taxSum)));
    }
  }

  private void testInvoice(Invoice testInvoice, boolean isCompleted, boolean isUpdated) {

    boolean round = isPriceIncludingTaxes && isTaxDocumentLevel && !isCompleted;
    int stdPrecision = testInvoice.getCurrency().getStandardPrecision().intValue();

    // Assert line taxes and amounts
    int i = 0;
    OBCriteria<InvoiceLine> obc1 = OBDal.getInstance().createCriteria(InvoiceLine.class);
    obc1.add(Restrictions.eq(InvoiceLine.PROPERTY_INVOICE, testInvoice));
    obc1.addOrderBy(InvoiceLine.PROPERTY_LINENO, true);
    for (InvoiceLine testInvoiceLine : obc1.list()) {
      OBDal.getInstance().refresh(testInvoiceLine);

      int n = 0;
      OBCriteria<InvoiceLineTax> obc2 = OBDal.getInstance().createCriteria(InvoiceLineTax.class);
      obc2.add(Restrictions.eq(InvoiceLineTax.PROPERTY_INVOICELINE, testInvoiceLine));
      obc2.addOrderBy(InvoiceLineTax.PROPERTY_LINENO, true);
      for (InvoiceLineTax linetax : obc2.list()) {
        OBDal.getInstance().refresh(linetax);

        log.debug(linetax.getTax().getIdentifier());
        log.debug(linetax.getTax().getId());
        log.debug(linetax.getTaxableAmount().toString());
        log.debug(linetax.getTaxAmount().toString());

        if (!linesData[i].getLineTaxes().containsKey(linetax.getTax().getId())) {
          assertTrue(
              testDescription + ". Tax Should not be present: " + linetax.getTax().getIdentifier(),
              false);
        }

        // Assert line taxes
        BigDecimal expectedTaxableAmount = new BigDecimal(isUpdated
            ? (isCompleted ? linesData[i].getLineTaxes().get(linetax.getTax().getId())[6]
                : linesData[i].getLineTaxes().get(linetax.getTax().getId())[4])
            : (isCompleted ? linesData[i].getLineTaxes().get(linetax.getTax().getId())[2]
                : linesData[i].getLineTaxes().get(linetax.getTax().getId())[0]));
        BigDecimal expectedTaxAmount = new BigDecimal(isUpdated
            ? (isCompleted ? linesData[i].getLineTaxes().get(linetax.getTax().getId())[7]
                : linesData[i].getLineTaxes().get(linetax.getTax().getId())[5])
            : (isCompleted ? linesData[i].getLineTaxes().get(linetax.getTax().getId())[3]
                : linesData[i].getLineTaxes().get(linetax.getTax().getId())[1]));
        assertThat("Wrong taxable amount for line in document",
            round(linetax.getTaxableAmount(), round, stdPrecision),
            comparesEqualTo(expectedTaxableAmount));
        assertThat("Wrong tax amount for line in document", linetax.getTaxAmount(),
            comparesEqualTo(expectedTaxAmount));

        n++;
      }

      if (linesData[i].getLineTaxes().size() != n) {
        assertTrue(testDescription + ". Number of lines obtained("
            + linesData[i].getLineTaxes().size() + ") different than expected (" + n + ")", false);
      }

      // Assert line amounts
      BigDecimal expectedGrossAmount = new BigDecimal(isUpdated
          ? (isCompleted ? linesData[i].getLineAmounts()[6] : linesData[i].getLineAmounts()[4])
          : (isCompleted ? linesData[i].getLineAmounts()[2] : linesData[i].getLineAmounts()[0]));
      BigDecimal expectedNetAmount = new BigDecimal(isUpdated
          ? (isCompleted ? linesData[i].getLineAmounts()[7] : linesData[i].getLineAmounts()[5])
          : (isCompleted ? linesData[i].getLineAmounts()[3] : linesData[i].getLineAmounts()[1]));
      assertThat("Wrong Invoice line Gross Amount", testInvoiceLine.getGrossAmount(),
          comparesEqualTo(expectedGrossAmount));
      assertThat("Wrong Invoice line Net Amount", testInvoiceLine.getLineNetAmount(),
          comparesEqualTo(expectedNetAmount));

      i++;
    }

    // Assert header taxes and amounts
    int n = 0;
    OBCriteria<InvoiceTax> obc3 = OBDal.getInstance().createCriteria(InvoiceTax.class);
    obc3.add(Restrictions.eq(InvoiceTax.PROPERTY_INVOICE, testInvoice));
    obc3.addOrderBy(InvoiceLineTax.PROPERTY_LINENO, true);

    for (InvoiceTax tax : obc3.list()) {
      OBDal.getInstance().refresh(tax);

      log.debug(tax.getTax().getIdentifier());
      log.debug(tax.getTax().getId());
      log.debug(tax.getTaxableAmount().toString());
      log.debug(tax.getTaxAmount().toString());

      if (!docTaxes.containsKey(tax.getTax().getId())) {
        assertTrue(testDescription + ". Tax Should not be present: " + tax.getTax().getIdentifier(),
            false);
      }

      // Assert header taxes
      BigDecimal expectedTaxableAmount = new BigDecimal(isUpdated
          ? (isCompleted ? docTaxes.get(tax.getTax().getId())[6]
              : docTaxes.get(tax.getTax().getId())[4])
          : (isCompleted ? docTaxes.get(tax.getTax().getId())[2]
              : docTaxes.get(tax.getTax().getId())[0]));
      BigDecimal expectedTaxAmount = new BigDecimal(isUpdated
          ? (isCompleted ? docTaxes.get(tax.getTax().getId())[7]
              : docTaxes.get(tax.getTax().getId())[5])
          : (isCompleted ? docTaxes.get(tax.getTax().getId())[3]
              : docTaxes.get(tax.getTax().getId())[1]));
      assertThat("Wrong taxable amount for document",
          round(tax.getTaxableAmount(), round, stdPrecision),
          comparesEqualTo(expectedTaxableAmount));
      assertThat("Wrong tax amount for document", tax.getTaxAmount(),
          comparesEqualTo(expectedTaxAmount));

      n++;
    }

    if (docTaxes.size() != n) {
      assertTrue(testDescription + ". Number of lines obtained(" + docTaxes.size()
          + ") different than expected (" + n + ")", false);
    }

    // Assert header amounts
    BigDecimal expectedGrossAmount = new BigDecimal(
        isUpdated ? (isCompleted ? docAmounts[6] : docAmounts[4])
            : (isCompleted ? docAmounts[2] : docAmounts[0]));
    BigDecimal expectedNetAmount = new BigDecimal(
        isUpdated ? (isCompleted ? docAmounts[7] : docAmounts[5])
            : (isCompleted ? docAmounts[3] : docAmounts[1]));
    assertThat("Wrong Invoice GrandTotal", testInvoice.getGrandTotalAmount(),
        comparesEqualTo(expectedGrossAmount));
    assertThat("Wrong Invoice TotalLines", testInvoice.getSummedLineAmount(),
        comparesEqualTo(expectedNetAmount));

    // Final Asserts
    BigDecimal taxSum = BigDecimal.ZERO;
    BigDecimal netSum = BigDecimal.ZERO;
    BigDecimal grossSum = BigDecimal.ZERO;

    for (InvoiceLine testInvoiceLine : obc1.list()) {
      BigDecimal netAmount = testInvoiceLine.getLineNetAmount();
      BigDecimal grossAmount = testInvoiceLine.getGrossAmount();

      if (isPriceIncludingTaxes) {
        assertThat("Line Gross Price * Line Quantity <> Line Gross Amount", grossAmount,
            comparesEqualTo(testInvoiceLine.getGrossUnitPrice()
                .multiply(testInvoiceLine.getInvoicedQuantity())));
      } else {
        assertThat("Line Net Price * Line Quantity <> Line Net Amount", netAmount, comparesEqualTo(
            testInvoiceLine.getUnitPrice().multiply(testInvoiceLine.getInvoicedQuantity())));
      }

      netSum = netSum.add(netAmount);
      grossSum = grossSum.add(grossAmount);
    }

    for (InvoiceTax tax : obc3.list()) {
      BigDecimal taxAmount = tax.getTaxAmount();
      taxSum = taxSum.add(taxAmount);
    }

    if (isCompleted) {
      assertThat("Line Net Amount Sum <> Document Net Amount", testInvoice.getSummedLineAmount(),
          comparesEqualTo(netSum));
    }
    if (isPriceIncludingTaxes) {
      assertThat("Line Gross Amount Sum <> Document Gross Amount",
          testInvoice.getGrandTotalAmount(), comparesEqualTo(grossSum));
    }
    if (isCompleted) {
      assertThat("Line Net Amount Sum + Tax Sum <> Document Gross Amount",
          testInvoice.getGrandTotalAmount(), comparesEqualTo(netSum.add(taxSum)));
    }
  }

  private BigDecimal round(BigDecimal value, boolean round, int precision) {
    if (round) {
      return value.setScale(precision, RoundingMode.HALF_UP);
    } else {
      return value;
    }
  }
}
