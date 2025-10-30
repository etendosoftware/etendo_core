package com.etendoerp.legacy.calloutsSequence;

import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_callouts.SEInOutDocTypeData;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.calloutsSequence.SL_Invoice_SequenceActionInterface;
import org.openbravo.erpCommon.utility.Utility;

import jakarta.enterprise.context.Dependent;
import jakarta.servlet.ServletException;

@Dependent
public class SL_Invoice_Legacy implements SL_Invoice_SequenceActionInterface {
    @Override
    public String get_SL_Invoice_inpdocumentnoValue(SimpleCallout.CalloutInfo info, HashMap<String, Object> values) throws ServletException {

        ConnectionProvider conProv     = (ConnectionProvider) values.get("conProv");
        String strDocTypeTarget        = (String) values.get("strDocTypeTarget");
        String strCInvoiceId           = (String) values.get("strCInvoiceId");
        String isdocnocontrolled       = (String) values.get("isdocnocontrolled");
        String currentnext             = (String) values.get("currentnext");

        String strDoctypetargetinvoice = SEInOutDocTypeData.selectDoctypetargetinvoice(conProv, strCInvoiceId);
        String documentNo = "";

        // Documentno
        // check if doc type target is different, in this case assign new
        // documentno otherwise maintain the previous one
        if (StringUtils.isEmpty(strDoctypetargetinvoice) || !StringUtils.equals(strDoctypetargetinvoice, strDocTypeTarget)) {
            String strDocumentNo = StringUtils.equals(isdocnocontrolled, "Y")
                                ? currentnext
                                : Utility.getDocumentNo(conProv, info.vars.getClient(), "C_Invoice", false);
            documentNo = "<" + strDocumentNo + ">";
        } else if (StringUtils.isNotEmpty(strDoctypetargetinvoice)
            && StringUtils.equals(strDoctypetargetinvoice, strDocTypeTarget)) {
            documentNo = SEInOutDocTypeData.selectActualinvoicedocumentno(conProv, strCInvoiceId);
        }

        info.addResult("inpdocumentno", documentNo);
        return documentNo;
    }
}
