package com.etendoerp.legacy.calloutsSequence;

import org.apache.commons.lang3.StringUtils;
import org.openbravo.erpCommon.ad_callouts.SEInOutDocTypeData;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.calloutsSequence.SL_Invoice_SequenceActionInterface;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;

import javax.servlet.ServletException;
import java.util.HashMap;

public class SL_Invoice_Legacy implements SL_Invoice_SequenceActionInterface {
    @Override
    public String get_SL_Invoice_inpdocumentnoValue(SimpleCallout.CalloutInfo info, HashMap<String, Object> values) throws ServletException {

        ConnectionProvider conProv     = (ConnectionProvider) values.get("conProv");
        String strDocTypeTarget        = (String) values.get("strDocTypeTarget");
        String strCInvoiceId           = (String) values.get("strCInvoiceId");

        String strDoctypetargetinvoice = SEInOutDocTypeData.selectDoctypetargetinvoice(conProv, strCInvoiceId);
        String documentNo = "";

        // Documentno
        // check if doc type target is different, in this case assign new
        // documentno otherwise maintain the previous one
        if (StringUtils.isEmpty(strDoctypetargetinvoice) || !StringUtils.equals(strDoctypetargetinvoice, strDocTypeTarget)) {
            // Preview delegates on the same path used by persistence, so prefix/suffix match.
            String strDocumentNo = Utility.getDocumentNo(conProv, info.vars, info.getWindowId(),
                "C_Invoice", strDocTypeTarget, strDocTypeTarget, false, false);
            documentNo = "<" + strDocumentNo + ">";
        } else if (StringUtils.isNotEmpty(strDoctypetargetinvoice)
            && StringUtils.equals(strDoctypetargetinvoice, strDocTypeTarget)) {
            documentNo = SEInOutDocTypeData.selectActualinvoicedocumentno(conProv, strCInvoiceId);
        }

        info.addResult("inpdocumentno", documentNo);
        return documentNo;
    }
}
