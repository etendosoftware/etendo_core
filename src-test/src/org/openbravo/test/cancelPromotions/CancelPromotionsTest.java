package org.openbravo.test.cancelPromotions;

import static org.junit.Assert.assertEquals;
import static org.openbravo.test.costing.utils.TestCostingUtils.reactivateInvoice;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.financial.ResetAccounting;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.pricing.priceadjustment.PriceAdjustment;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.test.base.TestConstants;

public class CancelPromotionsTest extends WeldBaseTest {

  /**
   * Sets up the test environment, initializing the OBContext and VariablesSecureApp.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    OBContext.setOBContext(TestConstants.Users.ADMIN, TestConstants.Roles.FB_GRP_ADMIN, TestConstants.Clients.FB_GRP,
        TestConstants.Orgs.ESP);
    OBContext currentContext = OBContext.getOBContext();
    VariablesSecureApp vsa = new VariablesSecureApp(currentContext.getUser().getId(),
        currentContext.getCurrentClient().getId(), currentContext.getCurrentOrganization().getId(),
        currentContext.getRole().getId());
    RequestContext.get().setVariableSecureApp(vsa);
    CancelPromotionsUtils.createPriceAdjustment();
  }

  /**
   * Tests the creation and processing of a sales invoice with cancel promotions.
   * Verifies that the grand total amount and summed line amount are as expected.
   */
  @Test
  public void salesInvoiceWithCancelPromotions() {
    Invoice salesInvoice = null;

    try {
      salesInvoice = CancelPromotionsUtils.createInvoice(true);
      processInvoice(salesInvoice);

      assertEquals(0, BigDecimal.valueOf(24.68).compareTo(salesInvoice.getGrandTotalAmount()));
      assertEquals(0, BigDecimal.valueOf(20.40).compareTo(salesInvoice.getSummedLineAmount()));
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    } finally {
      reactivateAndDeleteInvoice(salesInvoice);
    }
  }

  /**
   * Tests the creation and processing of a sales invoice without cancel promotions.
   * Verifies that the grand total amount and summed line amount are as expected.
   */
  @Test
  public void salesInvoiceWithoutCancelPromotions() {
    Invoice salesInvoice = null;

    try {
      salesInvoice = CancelPromotionsUtils.createInvoice(false);
      processInvoice(salesInvoice);

      assertEquals(0, BigDecimal.valueOf(22.26).compareTo(salesInvoice.getGrandTotalAmount()));
      assertEquals(0, BigDecimal.valueOf(18.40).compareTo(salesInvoice.getSummedLineAmount()));
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    } finally {
      reactivateAndDeleteInvoice(salesInvoice);
    }
  }

  /**
   * Clean up the test environment after each test case has been executed.
   */
  @After
  public void cleanUp() {
    PriceAdjustment pa = (PriceAdjustment) OBDal.getInstance().createCriteria(PriceAdjustment.class).add(
        Restrictions.eq(PriceAdjustment.PROPERTY_NAME, "10% DISCOUNT TEST")).uniqueResult();

    OBDal.getInstance().remove(pa);
    OBDal.getInstance().flush();
    OBDal.getInstance().commitAndClose();
    OBDal.getInstance().rollbackAndClose();
  }

  /**
   * Reactivates and deletes the given invoice.
   *
   * @param salesInvoice
   *     the invoice to reactivate and delete
   */
  private void reactivateAndDeleteInvoice(Invoice salesInvoice) {
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
}
