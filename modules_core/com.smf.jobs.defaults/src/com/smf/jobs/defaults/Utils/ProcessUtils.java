package com.smf.jobs.defaults.Utils;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;
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
   * Updates the result type and message based on the success and error counts.
   * For multiple inputs, sets the result type to success, error, or warning.
   * For a single input, uses the message type to determine the result type.
   *
   * @param result
   *     the {@link ActionResult} to update with the result type and message
   * @param message
   *     the {@link OBError} containing the result type for a single input
   * @param inputs
   *     the list of input items of type {@link T} extending {@link BaseOBObject}
   * @param errors
   *     the error count as a {@link MutableInt}
   * @param success
   *     the success count as a {@link MutableInt}
   * @param originalInput
   *     the original {@link Data} input to include in the result
   * @param <T>
   *     the type of input objects
   */
  public static <T extends BaseOBObject> void massiveMessageHandler(ActionResult result, OBError message, List<T> inputs,
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

    if (inputs.size() == 1){
      if (StringUtils.equalsIgnoreCase("error", message.getType())){
        result.setType(Result.Type.ERROR);
      } else if (StringUtils.equalsIgnoreCase("success", message.getType())) {
        result.setType(Result.Type.SUCCESS);
      } else {
        result.setType(Result.Type.WARNING);
      }
      result.setMessage(message.getMessage());
      result.setOutput(originalInput);
    }
  }

  /**
   * Increments the error or success counter based on the message type.
   *
   * @param message
   *     the {@link OBError} object containing the message type and content
   * @param errors
   *     a {@link MutableInt} representing the count of errors to be incremented if the message type is "error"
   * @param success
   *     a {@link MutableInt} representing the count of successes to be incremented if the message type is "success"
   */
  public static void updateResult(OBError message, MutableInt errors, MutableInt success) {
    if (StringUtils.equalsIgnoreCase("error", message.getType())) {
      errors.increment();
    }
    if (StringUtils.equalsIgnoreCase("success", message.getType())) {
      success.increment();
    }
  }

}
