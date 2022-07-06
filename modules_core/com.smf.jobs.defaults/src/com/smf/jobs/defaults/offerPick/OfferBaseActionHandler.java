package com.smf.jobs.defaults.offerPick;

import com.smf.jobs.Action;
import com.smf.jobs.ActionResult;
import com.smf.jobs.Result;
import org.apache.log4j.Logger;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.pricing.priceadjustment.PriceAdjustment;
import org.openbravo.service.db.DbUtility;

abstract public class OfferBaseActionHandler extends Action {

  private static final Logger log = Logger.getLogger(OfferBaseActionHandler.class);

  @Override
  protected ActionResult action(JSONObject parameters, MutableBoolean isStopped) {
    ActionResult result = new ActionResult();
    var registers = getInputContents(getInputClass());
    OBContext.setAdminMode(true);
    try {
      JSONArray selectedLines = parameters.getJSONObject(getJSONName())
          .getJSONArray("_selection");
      if (selectedLines.length() == 0) {
        result.setType(Result.Type.ERROR);
        result.setMessage(OBMessageUtils.messageBD("NotSelected"));
        return result;
      }
      for (PriceAdjustment register : registers) {
        doPickAndExecute(register, selectedLines);
      }
      result.setType(Result.Type.SUCCESS);
      result.setMessage(OBMessageUtils.messageBD("Success"));
      return result;

    } catch (Exception e) {
      log.error("Error in RelateProductsToServiceProduct Action Handler", e);
      OBDal.getInstance().rollbackAndClose();
      try {
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        result.setType(Result.Type.ERROR);
        result.setMessage(message);
      } catch (Exception e2) {
        log.error(e.getMessage(), e2);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  abstract protected void doPickAndExecute(PriceAdjustment register, JSONArray selectedLines) throws JSONException;

  abstract protected String getJSONName();

  @Override
  protected Class<PriceAdjustment> getInputClass() {
    return PriceAdjustment.class;
  }

}
