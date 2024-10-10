package com.smf.jobs.defaults.Utils;

import java.util.List;

import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import com.smf.jobs.ActionResult;
import com.smf.jobs.Data;
import com.smf.jobs.Result;

public class ProcessUtils {

  private ProcessUtils() {
    throw new UnsupportedOperationException("This class should not be instantiated");
  }

  public static <T extends BaseOBObject> void massiveMessageHandler(ActionResult result, List<T> inputs, int errors, int success, Data originalInput) {
    if (inputs.size() > 1) {
      if (success == inputs.size()) {
        result.setType(Result.Type.SUCCESS);
      } else if (errors == inputs.size()) {
        result.setType(Result.Type.ERROR);
      } else {
        result.setType(Result.Type.WARNING);
      }
      result.setMessage(String.format(OBMessageUtils.messageBD("DJOBS_PostUnpostMessage"), success, errors));
      result.setOutput(originalInput);
    }
  }

}
