package org.openbravo.test.cancelpromotions;

import static org.openbravo.test.costing.utils.TestCostingUtils.reactivateInvoice;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.financial.ResetAccounting;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.pricing.priceadjustment.PriceAdjustment;
import org.openbravo.model.pricing.priceadjustment.PromotionType;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.test.costing.utils.TestCostingConstants;
import org.openbravo.test.stockReservation.StockReservationTestUtils;

/**
 * Utility class to create invoices with a specified option to cancel promotion and price
 * adjustments.
 */
public class CancelPromotionsUtils {

  public static final String DOCTYPE_ID = "7FCD49652E104E6BB06C3A0D787412E3"; // Transaction Document: AR Invoice
  public static final String PRODUCT_PRICE = "316F95A165914A538D923F3CA815E4D4"; // Product Price: Cerveza Ale 0,5L
  public static final String ORGANIZATION_ID = "E443A31992CB4635AFCAEABE7183CE85"; // Organization: ESP_NORTE
  public static final String DISCOUNT_TYPE_ID = "5D4BAF6BB86D4D2C9ED3D5A6FC051579"; // Discount/Promotion Type: Price Adjustment
  public static final String YES = "Y";

  /**
   * Private constructor to prevent instantiation of the utility class.
   */
  private CancelPromotionsUtils(){

  }

  /**
   * Creates an invoice with a specified option to cancel promotion.
   *
   * @param cancelPromotion
   *     indicates whether to cancel the promotion on the invoice line
   * @return the created Invoice object
   */
  protected static Invoice createInvoice(Boolean cancelPromotion) {
    Invoice invoice = OBProvider.getInstance().get(Invoice.class);
    InvoiceLine invoiceLine = OBProvider.getInstance().get(InvoiceLine.class);
    Client client = OBContext.getOBContext().getCurrentClient();
    Organization org = OBDal.getInstance().get(Organization.class, ORGANIZATION_ID);
    BusinessPartner bp = OBDal.getInstance().get(BusinessPartner.class, StockReservationTestUtils.BPARTNER_ID);
    FIN_PaymentMethod paymentMethod = OBDal.getInstance().get(FIN_PaymentMethod.class,
        StockReservationTestUtils.PAYMENT_METHOD_ID);
    Location location = OBDal.getInstance().get(Location.class, StockReservationTestUtils.LOCATION_ID);
    PriceList priceList = OBDal.getInstance().get(PriceList.class, StockReservationTestUtils.PRICELIST);
    PaymentTerm paymentTerm = OBDal.getInstance().get(PaymentTerm.class, StockReservationTestUtils.PAYMENT_TERM);
    Currency currency = OBDal.getInstance().get(Currency.class, TestCostingConstants.EURO_ID);
    DocumentType docType = OBDal.getInstance().get(DocumentType.class, DOCTYPE_ID);
    TaxRate taxRate = OBDal.getInstance().get(TaxRate.class, StockReservationTestUtils.TAX_ID);
    ProductPrice productPrice = OBDal.getInstance().get(ProductPrice.class, PRODUCT_PRICE);

    // Header
    invoice.setClient(client);
    invoice.setOrganization(org);
    invoice.setTransactionDocument(docType);
    invoice.setDocumentNo("SI-" + System.currentTimeMillis());
    invoice.setDocumentStatus(StockReservationTestUtils.DRAFT);
    invoice.setSalesTransaction(true);
    invoice.setAccountingDate(new Date());
    invoice.setInvoiceDate(new Date());
    invoice.setBusinessPartner(bp);
    invoice.setPartnerAddress(location);
    invoice.setPriceList(priceList);
    invoice.setPaymentMethod(paymentMethod);
    invoice.setPaymentTerms(paymentTerm);
    invoice.setDocumentAction(StockReservationTestUtils.COMPLETED);
    invoice.setDocumentType(docType);
    invoice.setCurrency(currency);

    OBDal.getInstance().save(invoice);
    OBDal.getInstance().flush();

    // Line
    invoiceLine.setClient(invoice.getClient());
    invoiceLine.setOrganization(invoice.getOrganization());
    invoiceLine.setProduct(productPrice.getProduct());
    invoiceLine.setLineNo(10L);
    invoiceLine.setUOM(productPrice.getProduct().getUOM());
    invoiceLine.setTax(taxRate);

    invoiceLine.setGrossUnitPrice(BigDecimal.valueOf(2.04));
    invoiceLine.setGrossListPrice(BigDecimal.valueOf(2.04));
    invoiceLine.setStandardPrice(BigDecimal.valueOf(2.04));
    invoiceLine.setBaseGrossUnitPrice(BigDecimal.valueOf(2.04));
    invoiceLine.setGrossAmount(BigDecimal.valueOf(2.04));

    invoiceLine.setInvoicedQuantity(BigDecimal.valueOf(10));
    invoiceLine.setUnitPrice(BigDecimal.valueOf(2.04));
    invoiceLine.setListPrice(BigDecimal.valueOf(2.04));
    invoiceLine.setLineNetAmount(BigDecimal.valueOf(20.40));

    invoiceLine.setCancelPriceAdjustment(cancelPromotion);
    invoiceLine.setInvoice(invoice);

    OBDal.getInstance().save(invoiceLine);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(invoiceLine);

    return processInvoice(invoice);
  }

  /**
   * Create a price adjustment of the given type with the given discount.
   *
   * @return the created price adjustment
   */
  protected static PriceAdjustment createPriceAdjustment() {
    PriceAdjustment pd = OBProvider.getInstance().get(PriceAdjustment.class);
    PromotionType promotionType = OBDal.getInstance().get(PromotionType.class, DISCOUNT_TYPE_ID);

    pd.setDiscountType(promotionType);
    pd.setName("10% DISCOUNT TEST");
    pd.setCharacteristicsExclSelection("A");
    pd.setDiscount(BigDecimal.valueOf(10));
    pd.setStartingDate(new Date());
    pd.setIncludedBPCategories(YES);
    pd.setSetSelection(YES);
    pd.setIncludedBusinessPartners(YES);
    pd.setIncludedProductCategories(YES);
    pd.setIncludedProducts(YES);
    pd.setIncludePriceLists(YES);
    pd.setIncludedOrganizations(YES);
    pd.setIncludedCharacteristics(YES);
    pd.setBpartnerExtrefSelection(YES);
    pd.setDiscountAmount(BigDecimal.ZERO);

    OBDal.getInstance().save(pd);
    OBDal.getInstance().flush();
    OBDal.getInstance().commitAndClose();

    return pd;
  }

  /**
   * Posts the given invoice and returns the posted invoice.
   *
   * @param invoice
   *     the invoice to post
   * @return the posted invoice
   */
  private static Invoice processInvoice(Invoice invoice) {
    final List<Object> params = new ArrayList<>();
    params.add(null);
    params.add(invoice.getId());

    CallStoredProcedure.getInstance().call("C_INVOICE_POST", params, null, true, false);

    OBDal.getInstance().refresh(invoice);
    return invoice;
  }

  /**
   * Reactivates and deletes the given invoice.
   *
   * @param salesInvoice
   *     the invoice to reactivate and delete
   */
  public static void reactivateAndDeleteInvoice(Invoice salesInvoice) {
    try {
      if (salesInvoice == null) {
        return;
      }

      salesInvoice = OBDal.getInstance().get(Invoice.class, salesInvoice.getId());

      ResetAccounting.delete(salesInvoice.getClient().getId(), salesInvoice.getOrganization().getId(),
          salesInvoice.getEntity().getTableId(), salesInvoice.getId(),
          OBDateUtils.formatDate(salesInvoice.getAccountingDate()), null);

      OBDal.getInstance().refresh(salesInvoice);

      if (salesInvoice.isProcessed()) {
        reactivateInvoice(salesInvoice);
      }

      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();

      salesInvoice = OBDal.getInstance().get(Invoice.class, salesInvoice.getId());
      OBDal.getInstance().remove(salesInvoice);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      Assert.fail(e.getMessage());
    }
  }
}
