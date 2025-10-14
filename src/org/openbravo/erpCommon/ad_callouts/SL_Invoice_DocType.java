/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2001-2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_callouts;

import jakarta.servlet.ServletException;

import com.etendoerp.sequences.NextSequenceValue;
import com.etendoerp.sequences.UINextSequenceValueInterface;
import org.apache.commons.lang3.StringUtils;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.erpCommon.calloutsSequence.CalloutSequence;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.service.db.DalConnectionProvider;

import java.util.HashMap;

public class SL_Invoice_DocType extends SimpleCallout {
    private static final String TRXID = "B82E1C56F57749AD97DD9924624F08D3";
    private static final String INPDOCUMENTNO = "inpdocumentno";

    @Override
    protected void execute(CalloutInfo info) throws ServletException {

        String strChanged = info.getLastFieldChanged();
        if (log4j.isDebugEnabled()) {
            log4j.debug("CHANGED: " + strChanged);
        }

        // Parameters
        String strDocTypeTarget = info.getStringParameter("inpcDoctypetargetId", IsIDFilter.instance);
        String strCInvoiceId = info.getStringParameter("inpcInvoiceId", IsIDFilter.instance);
        String strInvoice = info.getStringParameter("C_Invoice_ID", IsIDFilter.instance);
        Field field = Utilities.getField(info);

        SEInOutDocTypeData[] data = SEInOutDocTypeData.select(this, strDocTypeTarget);
        if (data != null && data.length > 0) {

            HashMap<String, Object> values = new HashMap<>();
            values.put("conProv", this);
            values.put("strDocTypeTarget", strDocTypeTarget);
            values.put("strCInvoiceId", strCInvoiceId);
            values.put("isdocnocontrolled", data[0].isdocnocontrolled);
            values.put("currentnext", data[0].currentnext);

            if (field != null) {
                if (!field.getColumn().getReference().getId().equalsIgnoreCase(TRXID)) {
                    var invoiceSequenceAction = CalloutSequence.getInstance().getSL_Invoice_SequenceAction().get();
                    if (invoiceSequenceAction != null) {
                        invoiceSequenceAction.get_SL_Invoice_inpdocumentnoValue(info, values);
                    }
                } else {
                    if (!StringUtils.isBlank(strInvoice)) {
                        final String strOldDocTypeTarget = SEInOutDocTypeData.selectDoctypetargetinvoice(this, strInvoice);
                        if (!strOldDocTypeTarget.equalsIgnoreCase(strDocTypeTarget)) {
                            String documentNo = Utilities.getDocumentNo(field);
                            if (documentNo != null)
                                info.addResult(INPDOCUMENTNO, documentNo);
                        } else {
                            final String strDocumentNoOld = SEInOutDocTypeData.selectActualinvoicedocumentno(this, strCInvoiceId);
                            info.addResult(INPDOCUMENTNO, strDocumentNoOld);
                        }
                    }
                }
            }
            // Check Document No. again.
            if (field != null) {
                if (StringUtils.isBlank(strInvoice)) {
                    UINextSequenceValueInterface sequenceHandler = null;
                    sequenceHandler = NextSequenceValue.getInstance()
                            .getSequenceHandler(field.getColumn().getReference().getId());
                    if (sequenceHandler != null) {
                        String documentNo = sequenceHandler.generateNextSequenceValue(field,
                                RequestContext.get());
                        info.addResult(INPDOCUMENTNO, "<" + documentNo + ">");
                    }
                } else {
                    if (field.getColumn().getReference().getId().equalsIgnoreCase(TRXID)) {
                        String strOldDocTypeTarget = SEInOutDocTypeData.selectDoctypetargetinvoice(new DalConnectionProvider(), strCInvoiceId);
                        if (!StringUtils.equalsIgnoreCase(strOldDocTypeTarget,
                                strDocTypeTarget)) {
                            info.showWarning(
                                    OBMessageUtils.messageBD(this, "ChangeDocumentType",
                                            info.vars.getLanguage()));
                        }
                    }
                }
            }

            // DocBaseType
            info.addResult("inpdocbasetype", data[0].docbasetype);

            // Payment Rule
            if (StringUtils.endsWith(data[0].docbasetype, "C")) {
                // Payment Rule - Form Of Payment On Credit for Credit Memos Document Type
                info.addResult("inppaymentrule", "P");
            } else {
                // Payment Rule - No Form of Payment for non Credit Memos Document type.
                info.addResult("inppaymentrule", "");
            }
        }
    }

}
