package org.openbravo.advpaymentmngt.hook;

import org.codehaus.jettison.json.JSONObject;

public interface PaymentProcessHook {

  public JSONObject preProcess(JSONObject params);
  public JSONObject posProcess(JSONObject params);

}
