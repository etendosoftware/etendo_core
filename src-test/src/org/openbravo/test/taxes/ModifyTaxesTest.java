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
 * All portions are Copyright (C) 2018-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.taxes;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.order.OrderlineServiceRelation;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.test.base.OBBaseTest;
import org.openbravo.test.taxes.data.OrderLineRelTestData;
import org.openbravo.test.taxes.data.OrderLineTestData;
import org.openbravo.test.taxes.data.OrderTestData;

@RunWith(Parameterized.class)
public class ModifyTaxesTest extends OBBaseTest {

  private final static DateFormat DF = new SimpleDateFormat("yyyy-MM-dd");

  private final static String USER_OPENBRAVO = "100"; // User Openbravo
  private final static String CLIENT_QA_TESTING = "4028E6C72959682B01295A070852010D"; // Client QA
                                                                                      // // Testing
  private final static String ORGANIZATION_SPAIN = "357947E87C284935AD1D783CF6F099A1"; // Organization
  private final static String ROLE_QA_ADMIN = "4028E6C72959682B01295A071429011E";
  private final static String PRICELIST_SALES = "4028E6C72959682B01295ADC1D55022B";
  private final static String PRICELIST_INCTAXES = "62C67BFD306C4BEF9F2738C27353380B";
  private final static String WAREHOUSE_SPAIN = "4D7B97565A024DB7B4C61650FA2B9560";
  private final static String DOCTYPE_STANDARD = "FF8080812C2ABFC6012C2B3BDF4D005A";

  private final static String CUSTOMER_A = "4028E6C72959682B01295F40C3CB02EC";
  private final static String LOCATION_CUSTOMER_A = "4028E6C72959682B01295F40C43802EE";
  private final static String PAYMENT_TERM = "7B308C5CB9674BB3A56E63D85887058A";

  private final static String PRODUCT_FGA = "4028E6C72959682B01295ADC1D07022A";
  private final static String PRODUCT_INST001 = "F8CEB6A3648D40EAB5476A73778CFF88";
  private final static String PRODUCT_INST002 = "7FD9B6C8D39F49D2BAF08142F4902D6C";
  private final static String TAX_VAT3 = "5A74E390B82747F9A5754C8EB1BDB47A";
  private final static String TAX_VAT3PLUS05 = "58A7B9D1DDDD442CAD08052C1B735AFC";
  private final static String TAX_VAT10 = "F9D9AF81F4FA459C9CE7A2D9697DF1E4";
  private final static String TAX_VAT21 = "DBFCCC14B64147168F0F516F82FAF38B";

  private final String testNumber;
  private final String testDescription;
  private final OrderTestData data;

  @Parameters(name = "idx:{0} name:{1}")
  public static Collection<Object[]> params() {
    return Arrays.asList(new Object[][] { //
        { "0001", "Modify Taxes. Test 1.", new OrderTestData(
            //
            CUSTOMER_A, LOCATION_CUSTOMER_A, PRICELIST_SALES, "13.00", "14.63", "13.00", "14.11", //
            Arrays.asList(
                //
                new OrderLineTestData(PRODUCT_FGA, TAX_VAT3, "1.00", "3.00", TAX_VAT21, "3.00",
                    "0.00", TAX_VAT3PLUS05, "3.00", "0.00"), //
                new OrderLineTestData(PRODUCT_INST001, TAX_VAT10, "1.00", "10.00", TAX_VAT10,
                    "10.00", "0.00", TAX_VAT10, "10.00", "0.00")), //
            Arrays.asList(new OrderLineRelTestData(1, 0))) }, //
        { "0002", "Modify Taxes. Test 2. Including taxes.", new OrderTestData(
            //
            CUSTOMER_A, LOCATION_CUSTOMER_A, PRICELIST_INCTAXES, "12.00", "13.52", "11.99", "13.00", //
            Arrays.asList(
                //
                new OrderLineTestData(PRODUCT_FGA, TAX_VAT3, "1.00", "3.00", TAX_VAT21, "2.91",
                    "3.52", TAX_VAT3PLUS05, "2.90", "3.00"),
                new OrderLineTestData(PRODUCT_INST001, TAX_VAT10, "1.00", "10.00", TAX_VAT10,
                    "9.09", "10.00", TAX_VAT10, "9.09", "10.00")),
            Arrays.asList(new OrderLineRelTestData(1, 0))) }, //
        { "0003", "Modify Taxes. Test 3.", new OrderTestData(
            //
            CUSTOMER_A, LOCATION_CUSTOMER_A, PRICELIST_SALES, "16.00", "17.18", "16.00", "17.18", //
            Arrays.asList(
                //
                new OrderLineTestData(PRODUCT_FGA, TAX_VAT3, "2.00", "3.00", TAX_VAT3, "6.00",
                    "0.00", TAX_VAT3, "6.00", "0.00"), //
                new OrderLineTestData(PRODUCT_INST002, TAX_VAT10, "1.00", "10.00", TAX_VAT10,
                    "10.00", "0.00", TAX_VAT10, "10.00", "0.00")), //
            Arrays.asList(new OrderLineRelTestData(1, 0))) }, //
        { "0004", "Modify Taxes. Test 4. Including taxes.", new OrderTestData(
            //
            CUSTOMER_A, LOCATION_CUSTOMER_A, PRICELIST_INCTAXES, "14.92", "16.00", "14.92", "16.00", //
            Arrays.asList(
                //
                new OrderLineTestData(PRODUCT_FGA, TAX_VAT3, "2.00", "3.00", TAX_VAT3, "5.83",
                    "6.00", TAX_VAT3, "5.83", "6.00"), //
                new OrderLineTestData(PRODUCT_INST002, TAX_VAT10, "1.00", "10.00", TAX_VAT10,
                    "9.09", "10.00", TAX_VAT10, "9.09", "10.00")), //
            Arrays.asList(new OrderLineRelTestData(1, 0))) }, //
    });
  }

  public ModifyTaxesTest(final String testNumber, final String testDescription,
      final OrderTestData data) {
    this.testNumber = testNumber;
    this.testDescription = testDescription;
    this.data = data;
  }

  @Test
  public void testModifyTaxes() throws ParseException {

    OBContext.setOBContext(USER_OPENBRAVO, ROLE_QA_ADMIN, CLIENT_QA_TESTING, ORGANIZATION_SPAIN);

    final Order order = OBProvider.getInstance().get(Order.class);
    order.setDocumentNo("MTTEST/" + testNumber);
    order.setBusinessPartner(
        OBDal.getInstance().getProxy(BusinessPartner.class, data.getCustomer()));
    order.setPartnerAddress(OBDal.getInstance().getProxy(Location.class, data.getLocation()));
    order.setCurrency(OBDal.getInstance().getProxy(Currency.class, EURO_ID));
    order.setPaymentTerms(OBDal.getInstance().getProxy(PaymentTerm.class, PAYMENT_TERM));
    order.setWarehouse(OBDal.getInstance().getProxy(Warehouse.class, WAREHOUSE_SPAIN));
    order.setSalesTransaction(true);
    order.setDocumentType(OBDal.getInstance().getProxy(DocumentType.class, DOCTYPE_STANDARD));
    order
        .setTransactionDocument(OBDal.getInstance().getProxy(DocumentType.class, DOCTYPE_STANDARD));
    order.setOrderDate(DF.parse("2018-01-01"));
    order.setAccountingDate(DF.parse("2018-01-01"));

    order.setSummedLineAmount(BigDecimal.ZERO);
    order.setGrandTotalAmount(BigDecimal.ZERO);
    order.setDescription(testDescription);

    PriceList pricelist = OBDal.getInstance().get(PriceList.class, data.getPriceList());
    order.setPriceList(pricelist);
    order.setPriceIncludesTax(pricelist.isPriceIncludesTax());

    OBDal.getInstance().save(order);

    int index = 0;
    List<OrderLine> alllines = new ArrayList<>();
    for (OrderLineTestData linedata : data.getLines()) {
      OrderLine orderLine = OBProvider.getInstance().get(OrderLine.class);
      Product product1 = OBDal.getInstance().get(Product.class, linedata.getProduct());
      orderLine.setLineNo(10L * (long) ++index);
      orderLine.setBusinessPartner(
          OBDal.getInstance().getProxy(BusinessPartner.class, data.getCustomer()));
      orderLine.setPartnerAddress(OBDal.getInstance().getProxy(Location.class, data.getLocation()));
      orderLine.setWarehouse(OBDal.getInstance().getProxy(Warehouse.class, WAREHOUSE_SPAIN));
      orderLine.setOrderDate(DF.parse("2018-01-01"));
      orderLine.setCurrency(OBDal.getInstance().getProxy(Currency.class, EURO_ID));
      orderLine.setTax(OBDal.getInstance().getProxy(TaxRate.class, linedata.getTax()));
      orderLine.setProduct(product1);
      orderLine.setUOM(product1.getUOM());
      orderLine.setOrderedQuantity(new BigDecimal(linedata.getQuantity()));

      if (pricelist.isPriceIncludesTax()) {
        orderLine.setGrossUnitPrice(new BigDecimal(linedata.getPrice()));
        orderLine.setGrossListPrice(new BigDecimal(linedata.getPrice()));
        orderLine.setBaseGrossUnitPrice(new BigDecimal(linedata.getPrice()));
      }

      orderLine.setUnitPrice(new BigDecimal(linedata.getPrice()));
      orderLine.setListPrice(new BigDecimal(linedata.getPrice()));
      orderLine.setStandardPrice(new BigDecimal(linedata.getPrice()));
      orderLine.setSalesOrder(order);
      OBDal.getInstance().save(orderLine);
      alllines.add(orderLine);
    }

    List<OrderlineServiceRelation> allrelations = new ArrayList<>();
    for (OrderLineRelTestData rel : data.getRelations()) {
      OrderlineServiceRelation relation = OBProvider.getInstance()
          .get(OrderlineServiceRelation.class);
      relation.setQuantity(new BigDecimal(data.getLines().get(rel.getLineRelated()).getQuantity()));
      relation
          .setAmount(new BigDecimal(data.getLines().get(rel.getLineRelated()).getExpectedNet()));
      relation.setOrderlineRelated(alllines.get(rel.getLineRelated()));
      relation.setSalesOrderLine(alllines.get(rel.getLine()));
      OBDal.getInstance().save(relation);
      allrelations.add(relation);
    }

    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(order);
    for (OrderLine l : alllines) {
      OBDal.getInstance().refresh(l);
    }

    // Verify order after adding relation
    assertThat(testDescription + " Order SummedLineAmmount", order.getSummedLineAmount(),
        comparesEqualTo(new BigDecimal(data.getExpectedNet())));
    assertThat(testDescription + " Order GrandTotalAmmount", order.getGrandTotalAmount(),
        comparesEqualTo(new BigDecimal(data.getExpectedGross())));

    index = 0;
    for (OrderLineTestData linedata : data.getLines()) {
      assertThat(testDescription + " Line " + Long.toString(index) + " Tax",
          alllines.get(index).getTax().getId(), comparesEqualTo(linedata.getExpectedTax()));
      assertThat(testDescription + " Line " + Long.toString(index) + " LineNetAmount",
          alllines.get(index).getLineNetAmount(),
          comparesEqualTo(new BigDecimal(linedata.getExpectedNet())));
      assertThat(testDescription + " Line " + Long.toString(index) + " LineGrossAmount",
          alllines.get(index).getLineGrossAmount(),
          comparesEqualTo(new BigDecimal(linedata.getExpectedGross())));
      index++;
    }

    for (OrderlineServiceRelation r : allrelations) {
      OBDal.getInstance().remove(r);
    }
    OBDal.getInstance().flush();

    OBDal.getInstance().refresh(order);
    for (OrderLine l : alllines) {
      OBDal.getInstance().refresh(l);
    }

    // Verify order after removing relation
    assertThat(testDescription + " Order SummedLineAmmount", order.getSummedLineAmount(),
        comparesEqualTo(new BigDecimal(data.getExpectedNet2())));
    assertThat(testDescription + " Order GrandTotalAmmount", order.getGrandTotalAmount(),
        comparesEqualTo(new BigDecimal(data.getExpectedGross2())));

    index = 0;
    for (OrderLineTestData linedata : data.getLines()) {
      assertThat(testDescription + " Line " + Long.toString(index) + " Tax",
          alllines.get(index).getTax().getId(), comparesEqualTo(linedata.getExpectedTax2()));
      assertThat(testDescription + " Line " + Long.toString(index) + " LineNetAmount",
          alllines.get(index).getLineNetAmount(),
          comparesEqualTo(new BigDecimal(linedata.getExpectedNet2())));
      assertThat(testDescription + " Line " + Long.toString(index) + " LineGrossAmount",
          alllines.get(index).getLineGrossAmount(),
          comparesEqualTo(new BigDecimal(linedata.getExpectedGross2())));
      index++;
    }

    for (OrderLine l : alllines) {
      OBDal.getInstance().remove(l);
    }
    OBDal.getInstance().flush();
    OBDal.getInstance().remove(order);
    OBDal.getInstance().flush();
  }
}
