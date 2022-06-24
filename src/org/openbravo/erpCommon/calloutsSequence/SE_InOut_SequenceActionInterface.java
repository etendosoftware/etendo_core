package org.openbravo.erpCommon.calloutsSequence;

import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

import java.util.HashMap;

public interface SE_InOut_SequenceActionInterface {

    String get_SE_InOut_inpdocumentnoValue(SimpleCallout.CalloutInfo info, HashMap<String, Object> values);

}
