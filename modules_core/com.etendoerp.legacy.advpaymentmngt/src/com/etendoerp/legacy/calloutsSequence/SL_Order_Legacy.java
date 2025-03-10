package com.etendoerp.legacy.calloutsSequence;

import org.apache.commons.lang3.StringUtils;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.calloutsSequence.SL_Order_SequenceActionInterface;
import java.math.BigDecimal;
import java.util.HashMap;

public class SL_Order_Legacy implements SL_Order_SequenceActionInterface {
    @Override
    public String get_SL_Order_inpdocumentnoValue(SimpleCallout.CalloutInfo info, HashMap<String, Object> values) {

        String currentNextSys = (String) values.get("currentNextSys");
        String currentNext    = (String) values.get("currentNext");

        String documentNo = "";
        if (StringUtils.equalsIgnoreCase(info.vars.getRole(), "System")
                && new BigDecimal(info.vars.getClient())
                .compareTo(new BigDecimal("1000000.0")) < 0) {
            documentNo = "<" + currentNextSys + ">";
        } else {
            documentNo = "<" + currentNext + ">";
        }

        info.addResult("inpdocumentno", documentNo);
        return documentNo;
    }
}
