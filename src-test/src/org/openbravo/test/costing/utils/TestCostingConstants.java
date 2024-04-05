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
 * All portions are Copyright (C) 2017-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.costing.utils;

import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;

public class TestCostingConstants {

  // User System
  public static final String USERADMIN_ID = "0";
  // public QA Testing Admin
  public static final String QATESTING_ROLE_ID = "4028E6C72959682B01295A071429011E";
  public static final String EURO_ID = "102";
  public static final String DOLLAR_ID = "100";
  // Product with name: costing Product 1
  public static final String COSTING_PRODUCT_ID = "A8B10A097DBD4BF5865BA3C844A2299C";
  public static final String SPAIN_WAREHOUSE_ID = "4028E6C72959682B01295ECFEF4502A0";
  // Warehouse with name: Spain East warehouse
  public static final String SPAIN_EAST_WAREHOUSE_ID = "4D7B97565A024DB7B4C61650FA2B9560";
  // Process with name: Verify BOM
  public static final String VERIFYBOM_PROCESS_ID = "136";
  // Client QA Testing
  public static final String QATESTING_CLIENT_ID = "4028E6C72959682B01295A070852010D";
  // Organization Spain
  public static final String SPAIN_ORGANIZATION_ID = "357947E87C284935AD1D783CF6F099A1";
  // User Openbravo
  public static final String ADMIN_USER_ID = "100";
  // Storage Bin with name: L01
  public static final String LOCATOR_L01_ID = "193476BDD14E4A11B651B4E3E8D767C8";
  // Process request with name: Process Movements
  public static final String PROCESSMOVEMENT_PROCESS_ID = "122";
  // Process request with name: Process Internal Consumption
  public static final String PROCESSCONSUMPTION_PROCESS_ID = "800131";
  // G/L Item with name: Fees
  public static final String LANDEDCOSTTYPE_FEES_ID = "1DA4C24347EA4494BBA4466FF23ECAA5";
  // Product with name: Transportation Cost
  public static final String LANDEDCOSTTYPE_TRANSPORTATION_COST_ID = "5557E7C0FD064FD7A1CCB8C0E824DEE6";
  // Product with name: USD Cost
  public static final String LANDEDCOSTTYPE_USD_COST_ID = "CB473A64934B4D1583008D52DD0FBC49";
  // Business partner with name: Vendor USA
  public static final String BUSINESSPARTNER_VENDOR_USA_ID = "C8AD0EAF3052415BB1E15EFDEFBFD4AF";
  // Purchase Order with documentNo: 800010
  public static final String ORDERIN_ID = "2C9CEDC0761A41DCB276A5124F8AAA90";
  // Sales Order with documentNo: 50012
  public static final String ORDEROUT_ID = "8B53B7E6CF3B4D8D9BCF3A49EED6FCB4";
  // Return from customer with documentNo: RFC/2
  public static final String RETURNFROMCUSTOMER_ID = "C4CD2F1E708B44CB8796521C73D925CB";
  // Purchase Invoice with documentNo: 10000017
  public static final String INVOICEIN_ID = "9D0F6E57E59247F6AB6D063951811F51";
  // Document Sequence with name: DocumentNo_C_Invoice
  public static final String INVOICEIN_SEQUENCE_ID = "766DC632FDCE485B88F7535CF2A3422E";
  // Goods Receipt with documentNo: 10000012
  public static final String MOVEMENTIN_ID = "0450583047434254835B2B36B2E5B018";
  // Goods Shipment with documentNo: 500014
  public static final String MOVEMENTOUT_ID = "2BCCC64DA82A48C3976B4D007315C2C9";
  // Document Sequence with name: DocumentNo_M_InOut
  public static final String SHIPMENTIN_SEQUENCE_ID = "910E14E8BA4A419B92DF9973ACDB8A8F";
  // UOM with name: Unit
  public static final String UOM_ID = "100";
  // Document Sequence with name: DocumentNo_M_Movement
  public static final String MOVEMENT_SEQUENCE_ID = "07FD646511E14BADB5C1BB5CF2FCAC57";
  // Process with name: Validate Costing Rule
  public static final String VALIDATECOSTINGRULE_PROCESS_ID = "A269DCA4DE114E438695B66E166999B4";
  // Process with name: Create/Process Production
  public static final String PROCESSPRODUCTION_PROCESS_ID = "137";
  // Landed Cost Distribution Algorithm with name: Distribution by Amount
  public static final String LANDEDCOSTCOST_ALGORITHM_ID = "CF9B55BD159B474A9F79849C48715540";
  // Document type with name: Landed Cost Cost
  public static final String LANDEDCOSTCOST_DOCUMENTTYPE_ID = "F66B960D26C64215B1F4A09C3417FB16";
  // Document type with name: Landed Cost
  public static final String LANDEDCOST_DOCUMENTTYPE_ID = "38E131FE95F949CA97C95A9B03B3D6A8";
  // Document type with name: RFC Receipt
  public static final String RFCRECEIPT_DOCUMENTTYPE_ID = "4683C39FF3B242CD8A5B5825550C4472";
  // Document type with name: RFC Order
  public static final String RFCORDER_DOCUMENTTYPE_ID = "C789FE062AA8480BAD91543A0C6B41AB";
  // Document sequence with name: DocumentNo_M_Production
  public static final String PRODUCTION_DOCUMENTSEQUENCE_ID = "617CDE87DFC24C2FBFF278F7B8D22B82";

  // General Ledger Configuration with name: Main US/A/Euro
  public static final String GENERALLEDGER_ID = "9A68A0F8D72D4580B3EC3CAA00A5E1F0";
  // Table with name: MaterialMgmtInternalConsumption
  public static final String TABLE_INTERNAL_CONSUMPTION_ID = "800168";
  // Table with name: MaterialMgmtInternalMovement
  public static final String TABLE_INTERNAL_MOVEMENT_ID = "323";
  // Table with name: MaterialMgmtInventoryCount
  public static final String TABLE_INVENTORY_COUNT_ID = "321";
  // Table with name: MaterialMgmtShipmentInOut
  public static final String TABLE_INOUT_ID = "319";
  // Table with name: MaterialMgmtProductionTransaction
  public static final String TABLE_PRODUCTION_ID = "325";
  // Table with name: ProcurementReceiptInvoiceMatch
  public static final String TABLE_MATCH_INVOICE_ID = "472";
  // Costing Algorithm with name: Average Algorithm
  public static final String AVERAGE_COSTINGALGORITHM_ID = "B069080A0AE149A79CF1FA0E24F16AB6";
  // Storage Bin with name: M01
  public static final String LOCATOR_M01_ID = "96DEDCC179504711A81497DE68900F49";

  public static final String ENABLE_AUTOMATIC_PRICE_CORRECTION_TRXS = "enableAutomaticPriceCorrectionTrxs";
  // Negative Stock Correction Preference
  public static final String ENABLE_NEGATIVE_STOCK_CORRECTION_PREFERENCE = "enableNegativeStockCorrections";
  public static final String GL_CAT_STANDARD_ID = "FF8080812C2ABFC6012C2B3BE4980098";

  public static final String MAT_INT_CONSUMPTION_DOC_CAT = "MIC";
  public static final String INTERNAL_CONSUMPTION = "Internal Consumption";
  public static final String INTERNAL_CONSUMPTION_TABLE_ID = "800168";
  public static final Organization ALL_ORGANIZATIONS = OBDal.getInstance()
      .get(Organization.class, "0");
  public static boolean runBefore = true;
}
