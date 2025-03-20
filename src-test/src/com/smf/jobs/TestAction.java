package com.smf.jobs;

import java.util.HashMap;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.structure.BaseOBObject;

public class TestAction extends Action {
  private HashMap<String, Boolean> metadata = new HashMap<>();

  @Override
  protected ActionResult action(JSONObject parameters, MutableBoolean isStopped) {
    SingletonToTestHooks.getInstance().setMetadata("actionExecuted", true);
    var res = new ActionResult();
    res.setType(Result.Type.SUCCESS);
    res.setMessage("Test action executed");

    return res;
  }


  @Override
  protected Class<?> getInputClass() {
    return BaseOBObject.class;
  }
}
