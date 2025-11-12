package org.openbravo.materialmgmt;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.openbravo.materialmgmt.actionhandler.AddProductsToChValueTest;
import org.openbravo.materialmgmt.actionhandler.ManageVariantsTest;
import org.openbravo.test.materialMgmt.invoiceFromShipment.InvoiceFromGoodsShipmentDefaultValueFilterExpressionTest;

/**
 * Test suite for the Material Management module.
 * <p>
 * This suite consolidates multiple test classes related to the Material Management module,
 * ensuring they are executed together.
 * </p>
 * <p>
 * Test classes included in this suite cover:
 * <ul>
 *   <li>Variant management</li>
 *   <li>Price list and default value filter expressions for invoices</li>
 *   <li>Background data aggregation</li>
 *   <li>Characteristic and variant processing</li>
 *   <li>Stock utilities</li>
 * </ul>
 * </p>
 */
@Suite
@SelectClasses({ ManageVariantsCustomProductCharacteristicWhereClauseTest.class,
    InvoiceFromGoodsShipmentPriceListFilterExpressionTest.class,
    InvoiceFromGoodsShipmentDefaultValueFilterExpressionTest.class,
    GenerateAggregatedDataBackgroundTest.class,
    CharacteristicsUtilsTest.class,
    AddProductsToChValueTest.class,
    ManageVariantsDSTest.class,
    VariantChDescUpdateProcessorTest.class,
    VariantChDescUpdateProcessTest.class,
    AddProductsToChValueTest.class,
    ManageVariantsTest.class,
    StockUtilsTest.class,
    ServiceDeliverUtilityTest.class,
    ResetValuedStockAggregatedTest.class,
    UOMUtilTest.class,
    ProductCharacteristicsDSTest.class,
    VariantAutomaticGenerationProcessAdditionalTest.class,

})
public class MaterialMgmtTestSuite {
}
