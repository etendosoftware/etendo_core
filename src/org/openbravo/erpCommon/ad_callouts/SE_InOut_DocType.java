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

import javax.servlet.ServletException;

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

public class SE_InOut_DocType extends SimpleCallout {
  private static final String TRXID = "B82E1C56F57749AD97DD9924624F08D3";
  private static final String INPMOVEMENTTYPE = "inpmovementtype";
  private static final String INPDOCUMENTNO = "inpdocumentno";

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameter
    String strDocType = info.getStringParameter("inpcDoctypeId", IsIDFilter.instance);
    String strInOut = info.getStringParameter("M_InOut_ID", IsIDFilter.instance);
    Field field = Utilities.getField(info);

    // Movement Type and Document No.
    SEInOutDocTypeData[] data = SEInOutDocTypeData.select(this, strDocType);
    if (data != null && data.length > 0) {
      if (StringUtils.equals(data[0].docbasetype, "MMS")) {
        info.addResult(INPMOVEMENTTYPE, "C-");
      } else if (StringUtils.equals(data[0].docbasetype, "MMR")) {
        info.addResult(INPMOVEMENTTYPE, "V+");
      } else {
        info.addResult(INPMOVEMENTTYPE, null);
      }
      if (StringUtils.equals(data[0].isdocnocontrolled, "Y")) {
        HashMap<String, Object> values = new HashMap<>();
        values.put("currentNext", data[0].currentnext);
        if (field != null) {
          if (!field.getColumn().getReference().getId().equalsIgnoreCase(TRXID)) {
            var inOutSequenceAction = CalloutSequence.getInstance().getSE_InOut_SequenceAction().get();
            if (inOutSequenceAction != null) {
              inOutSequenceAction.get_SE_InOut_inpdocumentnoValue(info, values);
            }
          } else {
            if (!StringUtils.isBlank(strInOut)) {
              final String strOldDocTypeTarget = SEInOutDocTypeData.selectDoctypetargetinout(this, strInOut);
              if (!strOldDocTypeTarget.equalsIgnoreCase(strDocType)) {
                String documentNo = Utilities.getDocumentNo(field);
                if (documentNo != null)
                  info.addResult(INPDOCUMENTNO, documentNo);
              } else {
                final String strDocumentNoOld = SEInOutDocTypeData.selectDocumentnoInOut(this, strInOut);
                info.addResult(INPDOCUMENTNO, strDocumentNoOld);
              }
            }
          }
        }
      }

      // Check Document No. again.
      if (StringUtils.isBlank(strInOut)) {
        if (field != null) {
          UINextSequenceValueInterface sequenceHandler = null;
          sequenceHandler = NextSequenceValue.getInstance()
              .getSequenceHandler(field.getColumn().getReference().getId());
          if (sequenceHandler != null) {
            String documentNo = sequenceHandler.generateNextSequenceValue(field,
                RequestContext.get());
            info.addResult(INPDOCUMENTNO, "<" + documentNo + ">");
          }
        }
      } else {
        if (field!= null && field.getColumn() != null && field.getColumn().getReference().getId().equalsIgnoreCase(TRXID)) {
          final String oldDocumentType = SEInOutDocTypeData.selectDoctypetargetinout(new DalConnectionProvider(), strInOut);
          if (!StringUtils.equalsIgnoreCase(oldDocumentType,
              strDocType)) {
            info.showWarning(
                OBMessageUtils.messageBD(this, "ChangeDocumentType",
                    info.vars.getLanguage()));
          }
        }
      }
    }
  }
}
