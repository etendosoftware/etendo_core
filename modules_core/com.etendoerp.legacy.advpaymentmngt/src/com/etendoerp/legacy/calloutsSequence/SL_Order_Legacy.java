package com.etendoerp.legacy.calloutsSequence;

import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.calloutsSequence.SL_Order_SequenceActionInterface;
import org.openbravo.erpCommon.utility.Utility;
import java.util.HashMap;

public class SL_Order_Legacy implements SL_Order_SequenceActionInterface {
    @Override
    public String get_SL_Order_inpdocumentnoValue(SimpleCallout.CalloutInfo info, HashMap<String, Object> values) {

        ConnectionProvider conProv = (ConnectionProvider) values.get("conProv");
        String strDocTypeTarget = (String) values.get("strDocTypeTarget");

        // Preview delegates on the same path used by persistence, so prefix/suffix match.
        String strDocumentNo = Utility.getDocumentNo(conProv, info.vars, info.getWindowId(),
            "C_Order", strDocTypeTarget, strDocTypeTarget, false, false);
        String documentNo = "<" + strDocumentNo + ">";
        info.addResult("inpdocumentno", documentNo);
        return documentNo;
    }
}
