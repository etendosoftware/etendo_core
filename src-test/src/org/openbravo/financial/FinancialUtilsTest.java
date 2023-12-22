package org.openbravo.financial;

import java.util.Date;

import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.junit.Test;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.model.pricing.pricelist.PriceList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


public class FinancialUtilsTest extends WeldBaseTest {
  protected static final String BP_HEALTHY_FOOD = "B3ABB0B4AFEA4541AC1E29891D496079";


  @Test
  public void testGetProductPriceWithNullProduct() {
    // Given
    Date date = new Date();
    BusinessPartner healthyFoodBP = OBDal.getInstance().get(BusinessPartner.class, BP_HEALTHY_FOOD);
    PriceList priceList = healthyFoodBP.getPurchasePricelist();
    // When
    try {
      FinancialUtils.getProductPrice(null, date, true, priceList);
      fail("Expected an OBException to be thrown");
    } catch (OBException e) {
      assertEquals("@ParameterMissing@ @Product@", e.getMessage());
    }
  }
}
