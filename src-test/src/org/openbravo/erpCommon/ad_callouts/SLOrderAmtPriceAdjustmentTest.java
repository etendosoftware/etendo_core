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
 * All portions are Copyright © 2021-2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.erpCommon.ad_callouts;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.PriceAdjustment;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.pricing.pricelist.PriceList;

/**
 * Tests for {@link SL_Order_Amt} when rounded price adjustments are applied.
 */
@RunWith(MockitoJUnitRunner.class)
public class SLOrderAmtPriceAdjustmentTest {

  private static final String ORDER_ID = "ORDER_ID";
  private static final String PRODUCT_ID = "PRODUCT_ID";
  private static final String PRICE_LIST_ID = "PRICE_LIST_ID";
  private static final String PRICE_ACTUAL_PARAMETER = "inppriceactual";
  private static final String CANCEL_PRICE_AD_PARAMETER = "inpcancelpricead";
  private static final String ORDER_PARAMETER = "inpcOrderId";
  private static final String PRODUCT_PARAMETER = "inpmProductId";
  private static final String UOM_PARAMETER = "inpcUomId";
  private static final String ATTRIBUTE_SET_INSTANCE_PARAMETER = "inpmAttributesetinstanceId";
  private static final String TAX_PARAMETER = "inpcTaxId";
  private static final String QTY_ORDERED_PARAMETER = "inpqtyordered";
  private static final String PRICE_LIMIT_PARAMETER = "inppricelimit";
  private static final String PRICE_LIST_PARAMETER = "inppricelist";
  private static final String PRICE_STD_PARAMETER = "inppricestd";
  private static final String LINE_NET_AMT_PARAMETER = "inplinenetamt";
  private static final String TAX_BASE_AMT_PARAMETER = "inptaxbaseamt";
  private static final String GROSS_UNIT_PRICE_PARAMETER = "inpgrossUnitPrice";
  private static final String GROSS_PRICE_LIST_PARAMETER = "inpgrosspricelist";
  private static final String GROSS_PRICE_STD_PARAMETER = "inpgrosspricestd";
  private static final String DISCOUNT_PARAMETER = "inpdiscount";
  private static final String NOT_CANCEL_PRICE_ADJUSTMENT = "N";
  private static final String EMPTY_VALUE = "";
  private static final String STANDARD_PRECISION = "2";
  private static final BigDecimal LIST_PRICE = new BigDecimal("2.04");
  private static final BigDecimal ZERO_MANUAL_DISCOUNT = new BigDecimal("0.00");
  private static final BigDecimal ROUNDED_PROMOTION_PRICE = new BigDecimal("0.82");
  private final Map<String, Object> results = new HashMap<>();

  @Mock
  private SimpleCallout.CalloutInfo info;
  @Mock
  private OBDal obDal;
  @Mock
  private Order order;
  @Mock
  private Product product;
  @Mock
  private PriceList priceList;
  private SL_Order_Amt callout;

  /**
   * Initializes the callout instance and captures every result written during each test.
   */
  @Before
  public void setUp() {
    callout = new SL_Order_Amt();
    results.clear();
    doAnswer(invocation -> {
      results.put(invocation.getArgument(0), invocation.getArgument(1));
      return null;
    }).when(info).addResult(anyString(), nullable(Object.class));
  }

  /**
   * Tests that the callout keeps a zero manual discount when the final unit price is only the
   * rounded result of the applied promotion.
   *
   * @throws ServletException
   *     if the callout execution fails
   */
  @Test
  public void testKeepsZeroManualDiscountWhenRoundedPromotionAlreadyExplainsFinalPrice() throws ServletException {
    BigDecimal qty = BigDecimal.ONE;
    BigDecimal listPrice = LIST_PRICE;
    BigDecimal basePrice = LIST_PRICE;
    BigDecimal finalPrice = ROUNDED_PROMOTION_PRICE;
    BigDecimal manualDiscount = ZERO_MANUAL_DISCOUNT;

    stubCalloutParameters(finalPrice, listPrice, basePrice, manualDiscount, qty);

    try (MockedStatic<OBDal> obDalMock = mockStatic(
        OBDal.class); MockedStatic<SLOrderAmtData> orderAmtDataMock = mockStatic(
        SLOrderAmtData.class); MockedStatic<SLOrderStockData> orderStockDataMock = mockStatic(
        SLOrderStockData.class); MockedStatic<PriceAdjustment> priceAdjustmentMock = mockStatic(
        PriceAdjustment.class)) {
      stubOrderData(obDalMock, orderAmtDataMock, orderStockDataMock);
      priceAdjustmentMock.when(() -> PriceAdjustment.calculatePriceActual(order, product, qty, basePrice)).thenReturn(
          finalPrice);

      callout.execute(info);

      assertEquals(basePrice, results.get(PRICE_STD_PARAMETER));
      assertEquals(manualDiscount, results.get(DISCOUNT_PARAMETER));
      priceAdjustmentMock.verify(() -> PriceAdjustment.calculatePriceStd(order, product, qty, finalPrice), never());
    }
  }

  /**
   * Tests that the callout preserves the manual discount entered by the user when the current
   * final price matches that discount plus the active promotion.
   *
   * @throws ServletException
   *     if the callout execution fails
   */
  @Test
  public void testKeepsManualDiscountWhenFinalPriceMatchesDiscountAndPromotion() throws ServletException {
    BigDecimal qty = BigDecimal.ONE;
    BigDecimal listPrice = LIST_PRICE;
    BigDecimal basePrice = new BigDecimal("1.94");
    BigDecimal finalPrice = new BigDecimal("0.78");
    BigDecimal manualDiscount = new BigDecimal("5.00");

    stubCalloutParameters(finalPrice, listPrice, basePrice, manualDiscount, qty);

    try (MockedStatic<OBDal> obDalMock = mockStatic(
        OBDal.class); MockedStatic<SLOrderAmtData> orderAmtDataMock = mockStatic(
        SLOrderAmtData.class); MockedStatic<SLOrderStockData> orderStockDataMock = mockStatic(
        SLOrderStockData.class); MockedStatic<PriceAdjustment> priceAdjustmentMock = mockStatic(
        PriceAdjustment.class)) {
      stubOrderData(obDalMock, orderAmtDataMock, orderStockDataMock);
      priceAdjustmentMock.when(() -> PriceAdjustment.calculatePriceActual(order, product, qty, basePrice)).thenReturn(
          finalPrice);

      callout.execute(info);

      assertEquals(basePrice, results.get(PRICE_STD_PARAMETER));
      assertEquals(manualDiscount, results.get(DISCOUNT_PARAMETER));
      priceAdjustmentMock.verify(() -> PriceAdjustment.calculatePriceStd(order, product, qty, finalPrice), never());
    }
  }

  /**
   * Tests that the callout recalculates the manual discount when the user changes the final unit
   * price to a value that is no longer explained by the current promotion.
   *
   * @throws ServletException
   *     if the callout execution fails
   */
  @Test
  public void testRecalculatesManualDiscountWhenUserChangesFinalPrice() throws ServletException {
    BigDecimal qty = BigDecimal.ONE;
    BigDecimal listPrice = LIST_PRICE;
    BigDecimal currentBasePrice = LIST_PRICE;
    BigDecimal editedFinalPrice = new BigDecimal("0.79");
    BigDecimal recalculatedBasePrice = new BigDecimal("1.98");
    BigDecimal manualDiscount = ZERO_MANUAL_DISCOUNT;

    stubCalloutParameters(editedFinalPrice, listPrice, currentBasePrice, manualDiscount, qty);

    try (MockedStatic<OBDal> obDalMock = mockStatic(
        OBDal.class); MockedStatic<SLOrderAmtData> orderAmtDataMock = mockStatic(
        SLOrderAmtData.class); MockedStatic<SLOrderStockData> orderStockDataMock = mockStatic(
        SLOrderStockData.class); MockedStatic<PriceAdjustment> priceAdjustmentMock = mockStatic(
        PriceAdjustment.class)) {
      stubOrderData(obDalMock, orderAmtDataMock, orderStockDataMock);
      priceAdjustmentMock.when(
          () -> PriceAdjustment.calculatePriceActual(order, product, qty, currentBasePrice)).thenReturn(
          ROUNDED_PROMOTION_PRICE);
      priceAdjustmentMock.when(
          () -> PriceAdjustment.calculatePriceStd(order, product, qty, editedFinalPrice)).thenReturn(
          recalculatedBasePrice);

      callout.execute(info);

      assertEquals(recalculatedBasePrice, results.get(PRICE_STD_PARAMETER));
      assertEquals(new BigDecimal("2.94"), results.get(DISCOUNT_PARAMETER));
    }
  }

  /**
   * Stubs the callout input parameters required by the sales order amount calculation.
   *
   * @param finalPrice
   *     final unit price currently present in the order line
   * @param listPrice
   *     list price used as the base reference for the discount
   * @param basePrice
   *     current base price before applying promotions
   * @param manualDiscount
   *     manual discount currently entered by the user
   * @param qty
   *     ordered quantity used during price adjustment evaluation
   * @throws ServletException
   *     if the mocked callout parameter retrieval fails
   */
  private void stubCalloutParameters(BigDecimal finalPrice, BigDecimal listPrice, BigDecimal basePrice,
      BigDecimal manualDiscount, BigDecimal qty) throws ServletException {
    when(info.getLastFieldChanged()).thenReturn(PRICE_ACTUAL_PARAMETER);
    when(info.getStringParameter(CANCEL_PRICE_AD_PARAMETER)).thenReturn(NOT_CANCEL_PRICE_ADJUSTMENT);
    when(info.getStringParameter(eq(ORDER_PARAMETER), any())).thenReturn(ORDER_ID);
    when(info.getStringParameter(eq(PRODUCT_PARAMETER), any())).thenReturn(PRODUCT_ID);
    when(info.getStringParameter(eq(UOM_PARAMETER), any())).thenReturn(EMPTY_VALUE);
    when(info.getStringParameter(eq(ATTRIBUTE_SET_INSTANCE_PARAMETER), any())).thenReturn(EMPTY_VALUE);
    when(info.getStringParameter(eq(TAX_PARAMETER), any())).thenReturn(EMPTY_VALUE);

    when(info.getBigDecimalParameter(QTY_ORDERED_PARAMETER)).thenReturn(qty);
    when(info.getBigDecimalParameter(PRICE_ACTUAL_PARAMETER)).thenReturn(finalPrice);
    when(info.getBigDecimalParameter(PRICE_LIMIT_PARAMETER)).thenReturn(BigDecimal.ZERO);
    when(info.getBigDecimalParameter(PRICE_LIST_PARAMETER)).thenReturn(listPrice);
    when(info.getBigDecimalParameter(PRICE_STD_PARAMETER)).thenReturn(basePrice);
    when(info.getBigDecimalParameter(LINE_NET_AMT_PARAMETER)).thenReturn(finalPrice);
    when(info.getBigDecimalParameter(TAX_BASE_AMT_PARAMETER)).thenReturn(finalPrice);
    when(info.getBigDecimalParameter(GROSS_UNIT_PRICE_PARAMETER)).thenReturn(BigDecimal.ZERO);
    when(info.getBigDecimalParameter(GROSS_PRICE_LIST_PARAMETER)).thenReturn(BigDecimal.ZERO);
    when(info.getBigDecimalParameter(GROSS_PRICE_STD_PARAMETER)).thenReturn(BigDecimal.ZERO);
    when(info.getBigDecimalParameter(DISCOUNT_PARAMETER)).thenReturn(manualDiscount);
  }

  /**
   * Stubs the DAL and generated data access collaborators used by the callout to resolve order
   * context and precision metadata.
   *
   * @param obDalMock
   *     static DAL mock used to resolve order, product and price list entities
   * @param orderAmtDataMock
   *     static generated data mock used to return order precision data
   * @param orderStockDataMock
   *     static generated data mock used to avoid stock related side effects
   */
  private void stubOrderData(MockedStatic<OBDal> obDalMock, MockedStatic<SLOrderAmtData> orderAmtDataMock,
      MockedStatic<SLOrderStockData> orderStockDataMock) {
    obDalMock.when(OBDal::getInstance).thenReturn(obDal);
    when(obDal.get(Order.class, ORDER_ID)).thenReturn(order);
    when(obDal.get(Product.class, PRODUCT_ID)).thenReturn(product);
    when(obDal.get(PriceList.class, PRICE_LIST_ID)).thenReturn(priceList);
    when(order.isSalesTransaction()).thenReturn(false);
    when(priceList.isPriceIncludesTax()).thenReturn(false);
    when(priceList.getId()).thenReturn(PRICE_LIST_ID);

    SLOrderAmtData orderAmtData = new SLOrderAmtData();
    orderAmtData.stdprecision = STANDARD_PRECISION;
    orderAmtData.priceprecision = STANDARD_PRECISION;
    orderAmtData.mPricelistId = PRICE_LIST_ID;

    orderAmtDataMock.when(() -> SLOrderAmtData.select(callout, ORDER_ID)).thenReturn(
        new SLOrderAmtData[]{ orderAmtData });
    orderAmtDataMock.when(() -> SLOrderAmtData.listPriceType(callout, PRICE_LIST_ID)).thenReturn(false);
    orderStockDataMock.when(() -> SLOrderStockData.select(callout, PRODUCT_ID)).thenReturn(new SLOrderStockData[0]);
  }
}
