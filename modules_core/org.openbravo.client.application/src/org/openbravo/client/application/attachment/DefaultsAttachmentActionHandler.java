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
 * All portions are Copyright (C) 2015-2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application.attachment;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.client.application.Parameter;
import org.openbravo.client.application.ParameterUtils;
import org.openbravo.client.application.ParameterValue;
import org.openbravo.client.application.window.ApplicationDictionaryCachedStructures;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.KernelConstants;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.model.ad.utility.Attachment;

/**
 * This ActionHandler is invoked when opening a Attachment window. It is in charge of computing
 * default values for the parameters in the window.
 */
public class DefaultsAttachmentActionHandler extends BaseActionHandler {

  private static final Logger log = LogManager.getLogger();
  @Inject
  private ApplicationDictionaryCachedStructures adcs;

  @Override
  protected final JSONObject execute(Map<String, Object> parameters, String content) {
    try {
      OBContext.setAdminMode(true);
      JSONObject defaults = new JSONObject();

      final String attMethodID = (String) parameters.get("attachmentMethod");
      final String tabId = (String) parameters.get("tabId");
      final String attachmentId = (String) parameters.get("attachmentId");
      final String action = (String) parameters.get("action");
      final String keyId = (String) parameters.get("keyId");
      final Attachment attachment = OBDal.getInstance().get(Attachment.class, attachmentId);
      JSONObject context = new JSONObject(content);
      final Map<String, String> fixedParameters = fixRequestMap(parameters, context);

      // The parameter list is sorted so the fixed parameters are evaluated before. This is needed
      // to be able to define parameters with default values based on the fixed parameters.
      for (Parameter param : adcs.getMethodMetadataParameters(attMethodID, tabId)) {
        if (param.isFixed()) {
          Object value = null;
          if (param.getPropertyPath() != null) {
            value = AttachmentUtils.getPropertyPathValue(param, tabId, keyId);
          } else if (param.isEvaluateFixedValue()) {
            value = ParameterUtils.getParameterFixedValue(fixedParameters, param);
          } else {
            value = param.getFixedValue();
          }
          parameters.put(param.getDBColumnName(), value);
          // Add the value as a String in the fixedParameters so they can be used in Default Values
          // expressions of other parameters.
          fixedParameters.put(param.getDBColumnName(), ObjectUtils.toString(value, null));
          continue;
        }

        if ("edit".equals(action)) {
          // Calculate stored value.
          OBCriteria<ParameterValue> parameterValueCriteria = OBDal.getInstance()
              .createCriteria(ParameterValue.class);
          parameterValueCriteria.add(Restrictions.eq(ParameterValue.PROPERTY_FILE, attachment));
          parameterValueCriteria.add(Restrictions.eq(ParameterValue.PROPERTY_PARAMETER, param));
          ParameterValue parameterValue = (ParameterValue) parameterValueCriteria.uniqueResult();
          if (parameterValue != null) {
            // If the parameter has a previous value set it on the defaults map and continue with
            // next parameter.
            Object baseValue = ParameterUtils.getParameterValue(parameterValue);
            Object parsedValue = "";
            if (baseValue == null) {
              parsedValue = "";
            } else if (baseValue instanceof Date) {
              parsedValue = OBDateUtils.formatDate((Date) baseValue);
            } else if (baseValue instanceof BigDecimal) {
              parsedValue = ((BigDecimal) baseValue).toPlainString();
            } else if (baseValue instanceof Boolean) {
              parsedValue = baseValue;
            } else {
              parsedValue = baseValue.toString();
            }

            defaults.put(param.getDBColumnName(), parsedValue);
            continue;
          }
        }
        if (param.getDefaultValue() != null) {
          Object defValue = ParameterUtils.getParameterDefaultValue(fixedParameters, param,
              (HttpSession) parameters.get(KernelConstants.HTTP_SESSION), context);
          defaults.put(param.getDBColumnName(), defValue);
        }

      }

      log.debug("Defaults for tab {} \n {}", tabId, defaults);
      JSONObject results = new JSONObject();
      results.put("defaults", defaults);

      return results;
    } catch (Exception e) {
      log.error("Error trying getting defaults for process: " + e.getMessage(), e);
      return new JSONObject();
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
