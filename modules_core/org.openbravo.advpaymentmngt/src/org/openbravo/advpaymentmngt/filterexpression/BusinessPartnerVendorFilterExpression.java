package org.openbravo.advpaymentmngt.filterexpression;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.client.application.OBBindingsConstants;
import org.openbravo.client.application.ParameterUtils;
import org.openbravo.client.kernel.RequestContext;

public class BusinessPartnerVendorFilterExpression implements FilterExpression {
  private Logger log = LogManager.getLogger();
  private Map<String, String> requestMap;
  private String windowId;
  private String financialAccountWindowId = "94EAA455D2644E04AB25D93BE5157B6D";
  private String issotrx = null;
  String result = null;

  @Override
  public String getExpression(Map<String, String> _requestMap) {
    requestMap = _requestMap;
    windowId = requestMap.get(OBBindingsConstants.WINDOW_ID_PARAM);
    if (requestMap.get("issotrx") != null
        && (financialAccountWindowId.equals(windowId) || windowId == null)) {
      issotrx = requestMap.get("issotrx");
      if ("false".equals(issotrx)) {
        return "true";
      } else {
        return "";
      }
    } else if (requestMap.get("IsSOTrx") != null && financialAccountWindowId.equals(windowId)) {
      issotrx = requestMap.get("IsSOTrx");
      if ("N".equals(issotrx)) {
        return "true";
      } else {
        return "";
      }
    } else {
      try {
        result = (String) ParameterUtils.getJSExpressionResult(_requestMap,
            RequestContext.get().getSession(),
            "if (OB.isSalesTransaction() == false) { 'true' } else {''}");
        return result;
      } catch (Exception e) {
        log.error(
            "Error evaluating filter expression: if (OB.isSalesTransaction() == false) { 'true' } else {''}",
            e);
        return "";
      }
    }
  }

}
