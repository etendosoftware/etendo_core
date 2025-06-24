package org.openbravo.test.invoice;

import java.util.List;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.model.common.invoice.Invoice;

import com.smf.jobs.ActionResult;
import com.smf.jobs.Data;
import com.smf.jobs.Result;
import com.smf.jobs.defaults.CloneRecords;

/**
 * This class extends the CloneRecords class and is used to clone invoices in the system.
 * It overrides the run method to set the parameters for the clone records process before running it.
 * It also provides a static method to clone a list of BaseOBObject.
 *
 * The run method takes a Data object, a JSONObject, and a MutableBoolean object as parameters.
 * The Data object contains the records to be cloned.
 * The JSONObject contains the parameters for the clone records process.
 * The MutableBoolean object indicates whether the process has been stopped.
 *
 * The static callCloneRecordsJob method takes a list of BaseOBObject and a Class object as parameters.
 * The list of BaseOBObject contains the records to be cloned.
 * The Class object represents the type of the BaseOBObject.
 * The method returns the ID of the first cloned record.
 *
 * If any exception occurs during the process, it is caught and rethrown as an OBException.
 */
public class CallCloneInvoice extends CloneRecords {

  /**
   * This method is used to run the clone records process.
   * It takes a Data object, a JSONObject, and a MutableBoolean object as parameters.
   *
   * It first calls the setParameters method with the JSONObject as the parameter.
   * This method is used to set the parameters for the clone records process.
   *
   * Then it calls the run method of the superclass (CloneRecords) with the Data object and the MutableBoolean object as parameters.
   * The run method of the superclass is responsible for running the clone records process.
   * The result of the run method is returned.
   *
   * @param input The Data object containing the records to be cloned.
   * @param param The JSONObject containing the parameters for the clone records process.
   * @param stopped The MutableBoolean object indicating whether the process has been stopped.
   * @return ActionResult The result of the clone records process.
   */
  public ActionResult run(Data input, JSONObject param, MutableBoolean stopped) {
    setParameters(param);
    return super.run(input, stopped);
  }

  /**
   * This static method is used to clone records in the system.
   * It takes a list of BaseOBObject and a Class object as parameters.
   *
   * It first creates an instance of the CallCloneInvoice class using the WeldUtils class.
   * Then it creates a JSONObject and sets the "copyChildren" property to true.
   * A Data object is created with the JSONObject and the Class object as parameters, and the list of BaseOBObject is set as its contents.
   * The CallCloneInvoice's run method is then called with the Data object, the JSONObject, and a new MutableBoolean object as parameters.
   * The result of the run method is stored in a Result object.
   *
   * If the type of the result is ERROR, an OBException is thrown with the message of the result.
   * If the result has output, the ID of the first cloned record is retrieved and stored in the clonedRecordId variable.
   *
   * If any exception occurs during the process, it is caught and rethrown as an OBException with the message of the original exception.
   *
   * @param bobList The list of BaseOBObject to be cloned.
   * @param entity The Class object representing the type of the BaseOBObject.
   * @return String The ID of the first cloned record.
   * @throws OBException If there is an error during the cloning of the records.
   */
  public static String callCloneRecordsJob(List<BaseOBObject> bobList, Class<? extends BaseOBObject> entity) {
    String clonedRecordId = "";
    try {
      CallCloneInvoice cloneRecordsProcess = WeldUtils.getInstanceFromStaticBeanManager(CallCloneInvoice.class);
      JSONObject jsonData = new JSONObject();
      jsonData.put("copyChildren", true);
      Data data = new Data(jsonData, entity);
      data.setContents(bobList);
      final MutableBoolean mutableBoolean = new MutableBoolean(false);
      var result = cloneRecordsProcess.run(data, jsonData, mutableBoolean);

      if (result.getType().equals(Result.Type.ERROR)) {
        throw new OBException(result.getMessage());
      }
      if (result.getOutput().isPresent() && !result.getOutput().get().getContents().isEmpty()) {
        Invoice clonedInvoice = (Invoice) result.getOutput().get().getContents().get(0);
        if (clonedInvoice != null) {
          clonedRecordId = clonedInvoice.getId();
        }
      }
    } catch (JSONException | OBException e) {
      throw new OBException(e.getMessage());
    }
    return clonedRecordId;
  }
}
