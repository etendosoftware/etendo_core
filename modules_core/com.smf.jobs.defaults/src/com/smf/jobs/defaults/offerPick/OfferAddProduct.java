package com.smf.jobs.defaults.offerPick;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.pricing.priceadjustment.PriceAdjustment;

import jakarta.enterprise.context.Dependent;

@Dependent
public class OfferAddProduct extends OfferBaseActionHandler {

  private static final Logger log = Logger.getLogger(OfferAddProduct.class);

  @Override
  protected void doPickAndExecute(PriceAdjustment register, JSONArray selectedLines)
      throws JSONException {
    for (int i = 0; i < selectedLines.length(); i++) {
      JSONObject product = selectedLines.getJSONObject(i);
      Product prd = (Product) OBDal.getInstance()
          .getProxy(Product.ENTITY_NAME, product.getString("id"));
      org.openbravo.model.pricing.priceadjustment.Product item = OBProvider.getInstance()
          .get(org.openbravo.model.pricing.priceadjustment.Product.class);
      item.setActive(true);
      item.setClient(register.getClient());
      item.setOrganization(register.getOrganization());
      item.setPriceAdjustment(register);
      item.setProduct(prd);
      OBDal.getInstance().save(item);
      if ((i % 100) == 0) {
        OBDal.getInstance().flush();
        OBDal.getInstance().getSession().clear();
      }
    }
  }

  @Override
  protected String getJSONName() {
    return "Confprodprocess";
  }

}

