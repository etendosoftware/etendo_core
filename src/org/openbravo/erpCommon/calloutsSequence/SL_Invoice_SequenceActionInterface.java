package org.openbravo.erpCommon.calloutsSequence;

import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

import jakarta.servlet.ServletException;
import java.util.HashMap;

public interface SL_Invoice_SequenceActionInterface {

    String get_SL_Invoice_inpdocumentnoValue(SimpleCallout.CalloutInfo info, HashMap<String, Object> values) throws ServletException;

}
