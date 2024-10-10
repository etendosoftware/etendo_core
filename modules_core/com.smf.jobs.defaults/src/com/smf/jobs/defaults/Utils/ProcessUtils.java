package com.smf.jobs.defaults.Utils;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableInt;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.erpCommon.utility.OBError;
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
   * @param result
   *     the {@link ActionResult} to update with the result type and message
   * @param inputs
   *     a list of inputs of type {@link T} extending {@link BaseOBObject}
   * @param errors
   *     a {@link MutableInt} representing the count of errors
   * @param success
   *     a {@link MutableInt} representing the count of successes
   * @param originalInput
   *     the original {@link Data} input to include in the result output
   * @param <T>
   *     the type of input objects
   */
  public static <T extends BaseOBObject> void massiveMessageHandler(ActionResult result, List<T> inputs,
      MutableInt errors, MutableInt success, Data originalInput) {
    if (inputs.size() > 1) {
      if (success.intValue() == inputs.size()) {
        result.setType(Result.Type.SUCCESS);
      } else if (errors.intValue() == inputs.size()) {
        result.setType(Result.Type.ERROR);
      } else {
        result.setType(Result.Type.WARNING);
      }
      result.setMessage(String.format(OBMessageUtils.messageBD("DJOBS_PostUnpostMessage"), success, errors));
      result.setOutput(originalInput);
    }
  }

  /**
   * Updates the result based on the message type and increments the error or success counters.
   *
   * @param result
   *     the {@link ActionResult} object to be updated with the message
   * @param message
   *     the {@link OBError} object containing the message type and content
   * @param errors
   *     a {@link MutableInt} representing the count of errors to be incremented if the message type is "error"
   * @param success
   *     a {@link MutableInt} representing the count of successes to be incremented if the message type is "success"
   */
  public static void updateResult(ActionResult result, OBError message, MutableInt errors, MutableInt success) {
    if (StringUtils.equalsIgnoreCase("error", message.getType())) {
      errors.increment();
    }
    if (StringUtils.equalsIgnoreCase("success", message.getType())) {
      success.increment();
    }
    result.setMessage(message.getTitle().isEmpty() ? message.getMessage() : message.getTitle().concat(": ").concat(
        message.getMessage()));
  }

}
