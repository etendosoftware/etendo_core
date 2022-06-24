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

package org.openbravo.test.webservice;

import static org.junit.Assert.assertEquals;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.openbravo.test.base.Issue;

/**
 * Tests that ensure XML and JSON DAL REST web services are able to create records that include a
 * computed column.
 * 
 * @author alostale
 * 
 */
@Issue("25773")
public class WSAddRecordWithComputedColumns extends BaseWSTest {

  /**
   * Creates a Shipment using XML REST web service including <code>invoiceStatus</code> computed
   * column.
   * 
   */
  @Test
  public void testShipmentXML() {
    String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" //
        + "<ob:Openbravo xmlns:ob=\"http://www.openbravo.com\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" //
        + "  <MaterialMgmtShipmentInOut>" //
        + "    <client id=\"23C59575B9CF467C9620760EB255B389\" entity-name=\"ADClient\" />" //
        + "    <organization id=\"DC206C91AA6A4897B44DA897936E0EC3\" entity-name=\"Organization\" />" //
        + "    <active>true</active>" //
        + "    <salesTransaction>false</salesTransaction>" //
        + "    <documentNo>NUEVO1</documentNo>" //
        + "    <documentAction>CO</documentAction>" //
        + "    <documentStatus>DR</documentStatus>" //
        + "    <posted>N</posted>" //
        + "    <processNow>false</processNow>" //
        + "    <processed>false</processed>" //
        + "    <documentType id=\"2030AD7DD4284E2B936E261662EF735A\" entity-name=\"DocumentType\" identifier=\"MM Receipt\" />" //
        + "    <description xsi:nil=\"true\" />" //
        + "    <salesOrder xsi:nil=\"true\" />" //
        + "    <orderDate xsi:nil=\"true\" />" //
        + "    <print>false</print>" //
        + "    <movementType>V+</movementType>" //
        + "    <movementDate>2013-07-17T00:00:00.0Z</movementDate>" //
        + "    <accountingDate>2013-07-17T00:00:00.0Z</accountingDate>" //
        + "    <businessPartner id=\"9C91AE200EFA4A61836D79A2E99E29DB\" entity-name=\"BusinessPartner\" identifier=\"La Fruta es la Vida, S.L.\" />" //
        + "    <partnerAddress id=\"84405848239443D190AF63BCABCE9656\" entity-name=\"BusinessPartnerLocation\" identifier=\".Madrid, Pso. de la Castellana, 154\" />" //
        + "    <warehouse id=\"5848641D712545C7AE0FE9634A163648\" entity-name=\"Warehouse\" identifier=\"España Región Sur\" />" //
        + "    <orderReference xsi:nil=\"true\" />" //
        + "    <deliveryTerms>A</deliveryTerms>" //
        + "    <freightCostRule>I</freightCostRule>" //
        + "    <freightAmount>0</freightAmount>" //
        + "    <deliveryMethod>P</deliveryMethod>" //
        + "    <shippingCompany xsi:nil=\"true\" />" //
        + "    <charge xsi:nil=\"true\" />" //
        + "    <chargeAmount>0</chargeAmount>" //
        + "    <priority>5</priority>" //
        + "    <datePrinted xsi:nil=\"true\" />" //
        + "    <invoice xsi:nil=\"true\" />" //
        + "    <createLinesFrom>false</createLinesFrom>" //
        + "    <generateTo>false</generateTo>" //
        + "    <userContact xsi:nil=\"true\" />" //
        + "    <salesRepresentative xsi:nil=\"true\" />" //
        + "    <numberOfPackages xsi:nil=\"true\" />" //
        + "    <pickDate xsi:nil=\"true\" />" //
        + "    <shipDate xsi:nil=\"true\" />" //
        + "    <trackingNo xsi:nil=\"true\" />" //
        + "    <trxOrganization xsi:nil=\"true\" />" //
        + "    <project xsi:nil=\"true\" />" //
        + "    <salesCampaign xsi:nil=\"true\" />" //
        + "    <activity xsi:nil=\"true\" />" //
        + "    <stDimension xsi:nil=\"true\" />" //
        + "    <ndDimension xsi:nil=\"true\" />" //
        + "    <updateLines>false</updateLines>" //
        + "    <logistic>false</logistic>" //
        + "    <calculateFreight>false</calculateFreight>" //
        + "    <deliveryLocation xsi:nil=\"true\" />" //
        + "    <freightCategory xsi:nil=\"true\" />" //
        + "    <freightCurrency xsi:nil=\"true\" />" //
        + "    <receiveMaterials>false</receiveMaterials>" //
        + "    <sendMaterials>false</sendMaterials>" //
        + "    <conditionGoods xsi:nil=\"true\" />" //
        + "    <asset xsi:nil=\"true\" />" //
        + "    <costcenter xsi:nil=\"true\" />" //
        + "    <processGoodsJava>CO</processGoodsJava>" //
        + "    <invoiceStatus>0</invoiceStatus>" // Computed column
        + "  </MaterialMgmtShipmentInOut>" //
        + "</ob:Openbravo>";

    doContentRequest("/ws/dal/MaterialMgmtShipmentInOut", content, 200,
        "Inserted 1 business object", "POST");
  }

  /**
   * Creates a Shipment using JSON REST web service including <code>invoiceStatus</code> computed
   * column.
   * 
   */
  @Test
  public void testShipmentJSON() throws JSONException {
    String content = "{" //
        + "  \"data\": {" //
        + "    \"_entityName\": \"MaterialMgmtShipmentInOut\"," //
        + "    \"_new\": true," //
        + "    \"client\": \"23C59575B9CF467C9620760EB255B389\"," //
        + "    \"client$_identifier\": \"F&B International Group\"," //
        + "    \"organization\": \"DC206C91AA6A4897B44DA897936E0EC3\"," //
        + "    \"organization$_identifier\": \"F&B España - Región Sur\"," //
        + "    \"active\": true," //
        + "    \"salesTransaction\": false," //
        + "    \"documentNo\": \"NUEVO1\"," //
        + "    \"documentAction\": \"CO\"," //
        + "    \"documentStatus\": \"DR\"," //
        + "    \"posted\": \"N\"," //
        + "    \"processNow\": false," //
        + "    \"processed\": false," //
        + "    \"documentType\": \"BBC17DD5FD25470BB2752D096B72D412\"," //
        + "    \"documentType$_identifier\": \"MM Receipt\"," //
        + "    \"description\": null," //
        + "    \"salesOrder\": null," //
        + "    \"orderDate\": null," //
        + "    \"print\": false," //
        + "    \"movementType\": \"V+\"," //
        + "    \"movementDate\": \"2013-07-17\"," //
        + "    \"accountingDate\": \"2013-07-17\"," //
        + "    \"businessPartner\": \"9C91AE200EFA4A61836D79A2E99E29DB\"," //
        + "    \"businessPartner$_identifier\": \"La Fruta es la Vida, S.L.\"," //
        + "    \"partnerAddress\": \"84405848239443D190AF63BCABCE9656\"," //
        + "    \"partnerAddress$_identifier\": \".Madrid, Pso. de la Castellana, 154\"," //
        + "    \"warehouse\": \"5848641D712545C7AE0FE9634A163648\"," //
        + "    \"warehouse$_identifier\": \"España Región Sur\"," //
        + "    \"orderReference\": null," //
        + "    \"deliveryTerms\": \"A\"," //
        + "    \"freightCostRule\": \"I\"," //
        + "    \"freightAmount\": 0," //
        + "    \"deliveryMethod\": \"P\"," //
        + "    \"shippingCompany\": null," //
        + "    \"charge\": null," //
        + "    \"chargeAmount\": 0," //
        + "    \"priority\": \"5\"," //
        + "    \"datePrinted\": null," //
        + "    \"invoice\": null," //
        + "    \"createLinesFrom\": false," //
        + "    \"generateTo\": false," //
        + "    \"userContact\": null," //
        + "    \"salesRepresentative\": null," //
        + "    \"numberOfPackages\": null," //
        + "    \"pickDate\": null," //
        + "    \"shipDate\": null," //
        + "    \"trackingNo\": null," //
        + "    \"trxOrganization\": null," //
        + "    \"project\": null," //
        + "    \"salesCampaign\": null," //
        + "    \"activity\": null," //
        + "    \"stDimension\": null," //
        + "    \"ndDimension\": null," //
        + "    \"updateLines\": false," //
        + "    \"logistic\": false," //
        + "    \"calculateFreight\": false," //
        + "    \"deliveryLocation\": null," //
        + "    \"freightCategory\": null," //
        + "    \"freightCurrency\": null," //
        + "    \"receiveMaterials\": false," //
        + "    \"sendMaterials\": false," //
        + "    \"conditionGoods\": null," //
        + "    \"asset\": null," //
        + "    \"costcenter\": null," //
        + "    \"processGoodsJava\": \"CO\"," //
        + "    \"invoiceStatus\": 0" // Computed column
        + "  }" //
        + "}";

    String res = doContentRequest("/org.openbravo.service.json.jsonrest/MaterialMgmtShipmentInOut",
        content, 200, "", "POST", false);

    JSONObject json = new JSONObject(res);
    JSONObject jsonRes = json.getJSONObject("response");
    assertEquals("JSON response status", 0, jsonRes.getInt("status"));
    assertEquals("JSON response data length", 1, jsonRes.getJSONArray("data").length());
  }
}
