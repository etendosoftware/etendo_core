package org.openbravo.erpCommon.ad_callouts;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

@RunWith(MockitoJUnitRunner.class)
public class SLOrderAmtPriceAdjustmentTest {

  private static final String ORDER_ID = "ORDER_ID";
  private static final String PRODUCT_ID = "PRODUCT_ID";
  private static final String PRICE_LIST_ID = "PRICE_LIST_ID";

  @Mock private SimpleCallout.CalloutInfo info;
  @Mock private OBDal obDal;
  @Mock private Order order;
  @Mock private Product product;
  @Mock private PriceList priceList;

  private final SL_Order_Amt callout = new SL_Order_Amt();
  private final Map<String, Object> results = new HashMap<>();

  @Before
  public void setUp() {
    results.clear();
    doAnswer(invocation -> {
      results.put(invocation.getArgument(0), invocation.getArgument(1));
      return null;
    }).when(info).addResult(anyString(), nullable(Object.class));
  }

  @Test
  public void keepsZeroManualDiscountWhenRoundedPromotionAlreadyExplainsFinalPrice()
      throws ServletException {
    BigDecimal qty = BigDecimal.ONE;
    BigDecimal listPrice = new BigDecimal("2.04");
    BigDecimal basePrice = new BigDecimal("2.04");
    BigDecimal finalPrice = new BigDecimal("0.82");
    BigDecimal manualDiscount = new BigDecimal("0.00");

    stubCalloutParameters(finalPrice, listPrice, basePrice, manualDiscount, qty);

    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class);
        MockedStatic<SLOrderAmtData> orderAmtDataMock = mockStatic(SLOrderAmtData.class);
        MockedStatic<SLOrderStockData> orderStockDataMock = mockStatic(SLOrderStockData.class);
        MockedStatic<PriceAdjustment> priceAdjustmentMock = mockStatic(PriceAdjustment.class)) {
      stubOrderData(obDalMock, orderAmtDataMock, orderStockDataMock);
      priceAdjustmentMock.when(() -> PriceAdjustment.calculatePriceActual(order, product, qty,
          basePrice)).thenReturn(finalPrice);

      callout.execute(info);

      assertEquals(basePrice, results.get("inppricestd"));
      assertEquals(manualDiscount, results.get("inpdiscount"));
      priceAdjustmentMock.verify(
          () -> PriceAdjustment.calculatePriceStd(order, product, qty, finalPrice), never());
    }
  }

  @Test
  public void keepsManualDiscountWhenFinalPriceMatchesDiscountAndPromotion()
      throws ServletException {
    BigDecimal qty = BigDecimal.ONE;
    BigDecimal listPrice = new BigDecimal("2.04");
    BigDecimal basePrice = new BigDecimal("1.94");
    BigDecimal finalPrice = new BigDecimal("0.78");
    BigDecimal manualDiscount = new BigDecimal("5.00");

    stubCalloutParameters(finalPrice, listPrice, basePrice, manualDiscount, qty);

    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class);
        MockedStatic<SLOrderAmtData> orderAmtDataMock = mockStatic(SLOrderAmtData.class);
        MockedStatic<SLOrderStockData> orderStockDataMock = mockStatic(SLOrderStockData.class);
        MockedStatic<PriceAdjustment> priceAdjustmentMock = mockStatic(PriceAdjustment.class)) {
      stubOrderData(obDalMock, orderAmtDataMock, orderStockDataMock);
      priceAdjustmentMock.when(() -> PriceAdjustment.calculatePriceActual(order, product, qty,
          basePrice)).thenReturn(finalPrice);

      callout.execute(info);

      assertEquals(basePrice, results.get("inppricestd"));
      assertEquals(manualDiscount, results.get("inpdiscount"));
      priceAdjustmentMock.verify(
          () -> PriceAdjustment.calculatePriceStd(order, product, qty, finalPrice), never());
    }
  }

  @Test
  public void recalculatesManualDiscountWhenUserChangesFinalPrice() throws ServletException {
    BigDecimal qty = BigDecimal.ONE;
    BigDecimal listPrice = new BigDecimal("2.04");
    BigDecimal currentBasePrice = new BigDecimal("2.04");
    BigDecimal editedFinalPrice = new BigDecimal("0.79");
    BigDecimal recalculatedBasePrice = new BigDecimal("1.98");
    BigDecimal manualDiscount = new BigDecimal("0.00");

    stubCalloutParameters(editedFinalPrice, listPrice, currentBasePrice, manualDiscount, qty);

    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class);
        MockedStatic<SLOrderAmtData> orderAmtDataMock = mockStatic(SLOrderAmtData.class);
        MockedStatic<SLOrderStockData> orderStockDataMock = mockStatic(SLOrderStockData.class);
        MockedStatic<PriceAdjustment> priceAdjustmentMock = mockStatic(PriceAdjustment.class)) {
      stubOrderData(obDalMock, orderAmtDataMock, orderStockDataMock);
      priceAdjustmentMock.when(() -> PriceAdjustment.calculatePriceActual(order, product, qty,
          currentBasePrice)).thenReturn(new BigDecimal("0.82"));
      priceAdjustmentMock.when(() -> PriceAdjustment.calculatePriceStd(order, product, qty,
          editedFinalPrice)).thenReturn(recalculatedBasePrice);

      callout.execute(info);

      assertEquals(recalculatedBasePrice, results.get("inppricestd"));
      assertEquals(new BigDecimal("2.94"), results.get("inpdiscount"));
    }
  }

  private void stubCalloutParameters(BigDecimal finalPrice, BigDecimal listPrice,
      BigDecimal basePrice, BigDecimal manualDiscount, BigDecimal qty) throws ServletException {
    when(info.getLastFieldChanged()).thenReturn("inppriceactual");
    when(info.getStringParameter("inpcancelpricead")).thenReturn("N");
    when(info.getStringParameter(org.mockito.ArgumentMatchers.eq("inpcOrderId"), any()))
        .thenReturn(ORDER_ID);
    when(info.getStringParameter(org.mockito.ArgumentMatchers.eq("inpmProductId"), any()))
        .thenReturn(PRODUCT_ID);
    when(info.getStringParameter(org.mockito.ArgumentMatchers.eq("inpcUomId"), any()))
        .thenReturn("");
    when(info.getStringParameter(org.mockito.ArgumentMatchers.eq("inpmAttributesetinstanceId"),
        any())).thenReturn("");
    when(info.getStringParameter(org.mockito.ArgumentMatchers.eq("inpcTaxId"), any()))
        .thenReturn("");

    when(info.getBigDecimalParameter("inpqtyordered")).thenReturn(qty);
    when(info.getBigDecimalParameter("inppriceactual")).thenReturn(finalPrice);
    when(info.getBigDecimalParameter("inppricelimit")).thenReturn(BigDecimal.ZERO);
    when(info.getBigDecimalParameter("inppricelist")).thenReturn(listPrice);
    when(info.getBigDecimalParameter("inppricestd")).thenReturn(basePrice);
    when(info.getBigDecimalParameter("inplinenetamt")).thenReturn(finalPrice);
    when(info.getBigDecimalParameter("inptaxbaseamt")).thenReturn(finalPrice);
    when(info.getBigDecimalParameter("inpgrossUnitPrice")).thenReturn(BigDecimal.ZERO);
    when(info.getBigDecimalParameter("inpgrosspricelist")).thenReturn(BigDecimal.ZERO);
    when(info.getBigDecimalParameter("inpgrosspricestd")).thenReturn(BigDecimal.ZERO);
    when(info.getBigDecimalParameter("inpdiscount")).thenReturn(manualDiscount);
  }

  private void stubOrderData(MockedStatic<OBDal> obDalMock,
      MockedStatic<SLOrderAmtData> orderAmtDataMock,
      MockedStatic<SLOrderStockData> orderStockDataMock) throws ServletException {
    obDalMock.when(OBDal::getInstance).thenReturn(obDal);
    when(obDal.get(Order.class, ORDER_ID)).thenReturn(order);
    when(obDal.get(Product.class, PRODUCT_ID)).thenReturn(product);
    when(obDal.get(PriceList.class, PRICE_LIST_ID)).thenReturn(priceList);
    when(order.isSalesTransaction()).thenReturn(false);
    when(priceList.isPriceIncludesTax()).thenReturn(false);
    when(priceList.getId()).thenReturn(PRICE_LIST_ID);

    SLOrderAmtData orderAmtData = new SLOrderAmtData();
    orderAmtData.stdprecision = "2";
    orderAmtData.priceprecision = "2";
    orderAmtData.mPricelistId = PRICE_LIST_ID;

    orderAmtDataMock.when(() -> SLOrderAmtData.select(callout, ORDER_ID))
        .thenReturn(new SLOrderAmtData[] { orderAmtData });
    orderAmtDataMock.when(() -> SLOrderAmtData.listPriceType(callout, PRICE_LIST_ID))
        .thenReturn(false);
    orderStockDataMock.when(() -> SLOrderStockData.select(callout, PRODUCT_ID))
        .thenReturn(new SLOrderStockData[0]);
  }
}
