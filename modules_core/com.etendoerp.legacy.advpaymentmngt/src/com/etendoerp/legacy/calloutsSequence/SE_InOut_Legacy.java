package com.etendoerp.legacy.calloutsSequence;

import org.apache.commons.lang3.StringUtils;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.calloutsSequence.SE_InOut_SequenceActionInterface;
import org.openbravo.erpCommon.utility.Utility;

import java.util.HashMap;

public class SE_InOut_Legacy implements SE_InOut_SequenceActionInterface {
    @Override
    public String get_SE_InOut_inpdocumentnoValue(SimpleCallout.CalloutInfo info, HashMap<String, Object> values) {

        ConnectionProvider conProv = (ConnectionProvider) values.get("conProv");
        String strDocType = (String) values.get("strDocType");
        String currentNext = (String) values.get("currentNext");

        String strDocumentNo = null;
        try {
            // Preview delegates on the same path used by persistence, so prefix/suffix match.
            strDocumentNo = Utility.getDocumentNo(conProv, info.vars, info.getWindowId(),
                "M_InOut", strDocType, strDocType, false, false);
        } catch (Exception e) {
            strDocumentNo = null;
        }
        if (StringUtils.isBlank(strDocumentNo)) {
            // Fallback to the previous behavior if the prefixed lookup fails or returns nothing.
            strDocumentNo = currentNext;
        }
        String documentNo = "<" + strDocumentNo + ">";
        info.addResult("inpdocumentno", documentNo);
        return documentNo;
    }
}
