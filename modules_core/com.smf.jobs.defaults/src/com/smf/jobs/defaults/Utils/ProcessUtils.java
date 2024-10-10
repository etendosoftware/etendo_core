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

  /**
   * Generates a summary message based on the number of successful and failed processes.
   * It updates the result type to success, error, or warning, depending on the outcomes.
   *
   * @param <T>
   *     type of the input objects, extending {@link BaseOBObject}.
   * @param result
   *     the {@link ActionResult} to store the result of the process.
   * @param inputs
   *     the list of input objects being processed.
   * @param errors
   *     the number of failed processes.
   * @param success
   *     the number of successful processes.
   * @param originalInput
   *     the original data, which will be set as the output in the result.
   */
  public static <T extends BaseOBObject> void massiveMessageHandler(ActionResult result, List<T> inputs, int errors,
      int success, Data originalInput) {
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
