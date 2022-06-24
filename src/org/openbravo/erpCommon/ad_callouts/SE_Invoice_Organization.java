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
 * All portions are Copyright (C) 2013-2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.erpCommon.ad_callouts;

import javax.servlet.ServletException;

import com.etendoerp.sequences.NextSequenceValue;
import com.etendoerp.sequences.UINextSequenceValueInterface;
import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.filter.RequestFilter;
import org.openbravo.base.filter.ValueListFilter;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.CashVATUtil;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;

import java.util.List;

public class SE_Invoice_Organization extends SimpleCallout {
    private static final RequestFilter filterYesNo = new ValueListFilter("Y", "N");

    @Override
    protected void execute(CalloutInfo info) throws ServletException {
        final String strinpissotrx = info.getStringParameter("inpissotrx", filterYesNo);
        final String strOrgId = info.getStringParameter("inpadOrgId", IsIDFilter.instance);
        final String strBPartnerId = info.getStringParameter("inpcBpartnerId", IsIDFilter.instance);
        final String strBPartnerLocationId = info.getStringParameter("inpcBpartnerLocationId",
                IsIDFilter.instance);
        final String strInvoice = info.getStringParameter("C_Invoice_ID", IsIDFilter.instance);

        info.addResult("inpiscashvat",
                CashVATUtil.isCashVAT(strinpissotrx, strOrgId, strBPartnerId, strBPartnerLocationId));

        /* Check Document No. */
        if (StringUtils.isBlank(strInvoice)) {
            Field field = Utilities.getField(info);
            if (field != null) {
                UINextSequenceValueInterface sequenceHandler = null;
                sequenceHandler = NextSequenceValue.getInstance()
                        .getSequenceHandler(field.getColumn().getReference().getId());
                if (sequenceHandler != null) {
                    String documentNo = sequenceHandler.generateNextSequenceValue(field,
                            RequestContext.get());
                    info.addResult("inpdocumentno", "<" + documentNo + ">");
                }
            }
        }
    }
}
