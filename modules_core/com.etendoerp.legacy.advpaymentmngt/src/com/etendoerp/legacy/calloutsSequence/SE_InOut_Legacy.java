package com.etendoerp.legacy.calloutsSequence;

import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.calloutsSequence.SE_InOut_SequenceActionInterface;

import java.util.HashMap;

public class SE_InOut_Legacy implements SE_InOut_SequenceActionInterface {
    @Override
    public String get_SE_InOut_inpdocumentnoValue(SimpleCallout.CalloutInfo info, HashMap<String, Object> values) {

        String currentNext = (String) values.get("currentNext");

        String documentNo = "<" + currentNext + ">";
        info.addResult("inpdocumentno", documentNo);
        return documentNo;
    }
}
