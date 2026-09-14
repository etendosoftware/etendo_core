package com.etendoerp.legacy.calloutsSequence;

import org.apache.commons.lang3.StringUtils;
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
        String currentNext = (String) values.get("currentNext");

        String strDocumentNo = null;
        try {
            // Preview delegates on the same path used by persistence, so prefix/suffix match.
            strDocumentNo = Utility.getDocumentNo(conProv, info.vars, info.getWindowId(),
                "C_Order", strDocTypeTarget, strDocTypeTarget, false, false);
        } catch (Exception e) {
            strDocumentNo = null;
        }
        if (StringUtils.isBlank(strDocumentNo)) {
            // Fallback to the previous behavior if the prefixed lookup fails or returns nothing.
            // The former "System" branch (currentNextSys) is intentionally not restored here:
            // it compared vars.getRole() (an AD_ROLE_ID) against the literal "System", which
            // can never match, so this was the only branch ever reached in practice.
            strDocumentNo = currentNext;
        }
        String documentNo = "<" + strDocumentNo + ">";
        info.addResult("inpdocumentno", documentNo);
        return documentNo;
    }
}
