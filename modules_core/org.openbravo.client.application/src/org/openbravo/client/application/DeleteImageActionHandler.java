/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2014 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.CsrfUtil;
import org.openbravo.model.ad.utility.Image;
import org.openbravo.service.json.JsonUtils;

/**
 * Action handler which can delete an image by its id. Used on create a new record, upload an image
 * and without saving the record, remove the image.
 * 
 * @author GuillermoGil
 */
@ApplicationScoped
public class DeleteImageActionHandler extends BaseActionHandler {

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String image) {

    try {
      // Get image id and remove the image
      JSONObject dataObject = new JSONObject(image);
      String imageId = dataObject.getString("img");
      Image imageInstance = OBDal.getInstance().get(Image.class, imageId);
      String csrfToken = "";
      if (dataObject.has("csrfToken")) {
        csrfToken = dataObject.getString("csrfToken");
      }
      CsrfUtil.checkCsrfToken(csrfToken, RequestContext.get().getRequest());
      if (imageInstance != null) {
        OBContext.setAdminMode(true);
        try {
          OBDal.getInstance().remove(imageInstance);
        } finally {
          OBContext.restorePreviousMode();
        }
      }

      // just return an empty message
      return new JSONObject();
    } catch (Exception e) {
      try {
        return new JSONObject(JsonUtils.convertExceptionToJson(e));
      } catch (JSONException t) {
        throw new OBException(t);
      }
    }
  }
}
