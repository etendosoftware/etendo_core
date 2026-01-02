package org.openbravo.test.cancelpromotions;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.Restrictions;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.pricing.priceadjustment.PriceAdjustment;
import org.openbravo.test.base.TestConstants;

/**
 * Contains tests for the cancel promotions functionality.
 */
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

      assertEquals(0, BigDecimal.valueOf(24.68).compareTo(salesInvoice.getGrandTotalAmount()));
      assertEquals(0, BigDecimal.valueOf(20.40).compareTo(salesInvoice.getSummedLineAmount()));
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    } finally {
      CancelPromotionsUtils.reactivateAndDeleteInvoice(salesInvoice);
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

      assertEquals(0, BigDecimal.valueOf(22.26).compareTo(salesInvoice.getGrandTotalAmount()));
      assertEquals(0, BigDecimal.valueOf(18.40).compareTo(salesInvoice.getSummedLineAmount()));
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    } finally {
      CancelPromotionsUtils.reactivateAndDeleteInvoice(salesInvoice);
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
}
