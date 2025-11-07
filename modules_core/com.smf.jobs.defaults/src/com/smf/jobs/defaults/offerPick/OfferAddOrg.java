package com.smf.jobs.defaults.offerPick;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.pricing.priceadjustment.OrganizationFilter;
import org.openbravo.model.pricing.priceadjustment.PriceAdjustment;

import jakarta.enterprise.context.Dependent;

@Dependent
public class OfferAddOrg extends OfferBaseActionHandler {

  private static final Logger log = Logger.getLogger(OfferAddOrg.class);

  @Override
  protected void doPickAndExecute(PriceAdjustment register, JSONArray selectedLines)
      throws JSONException {
    for (int i = 0; i < selectedLines.length(); i++) {
      JSONObject orgJson = selectedLines.getJSONObject(i);
      Organization organization = (Organization) OBDal.getInstance()
          .getProxy(Organization.ENTITY_NAME, orgJson.getString("id"));
      OrganizationFilter item = OBProvider.getInstance().get(OrganizationFilter.class);
      item.setActive(true);
      item.setClient(register.getClient());
      item.setOrganization(organization);
      item.setPriceAdjustment(register);
      OBDal.getInstance().save(item);
      if ((i % 100) == 0) {
        OBDal.getInstance().flush();
        OBDal.getInstance().getSession().clear();
      }
    }
  }

  @Override
  protected String getJSONName() {
    return "Conforgprocess";
  }
}
