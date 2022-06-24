package org.openbravo.erpCommon.calloutsSequence;

import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

import java.util.HashMap;

public interface SL_Order_SequenceActionInterface {

    String get_SL_Order_inpdocumentnoValue(SimpleCallout.CalloutInfo info, HashMap<String, Object> values);

}
