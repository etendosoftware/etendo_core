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

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.calloutsSequence.CalloutSequence;
import org.openbravo.erpCommon.reference.ListData;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.utils.FormatUtilities;

import java.util.HashMap;

public class SL_Order_DocType extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameters
    String strOrder = info.getStringParameter("inpcOrderId", IsIDFilter.instance);
    String strBPartner = info.getStringParameter("inpcBpartnerId", IsIDFilter.instance);
    String strDocTypeTarget = info.getStringParameter("inpcDoctypetargetId", IsIDFilter.instance);
    String strDocType = info.getStringParameter("inpcDoctypeId", IsIDFilter.instance);
    String docNo = info.getStringParameter("inpdocumentno");
    String strDescription = info.getStringParameter("inpdescription");

    if (StringUtils.isNotEmpty(strDocTypeTarget)) {
      String paymentRule = "P";
      String invoiceRule = "D";
      String deliveryRule = "A";

      boolean newDocNo = StringUtils.isEmpty(docNo);
      if (!newDocNo && docNo.startsWith("<") && docNo.endsWith(">")) {
        newDocNo = true;
      }

      // Document Sequence
      String ad_sequence_id = "0";
      if (!newDocNo && !StringUtils.equals(strDocType, "0")) {
        SLOrderDocTypeData[] data = SLOrderDocTypeData.select(this, strDocType);
        if (data != null && data.length > 0) {
          ad_sequence_id = data[0].adSequenceId;
        }
      }

      // Document No
      SLOrderDocTypeData[] dataNew = SLOrderDocTypeData.select(this, strDocTypeTarget);

      String docSubTypeSO = "";
      boolean isSOTrx = true;
      if (dataNew != null && dataNew.length > 0) {

        // DocSubTypeSO
        docSubTypeSO = dataNew[0].docsubtypeso;
        if (docSubTypeSO == null) {
          docSubTypeSO = "--";
        }
        info.addResult("inpordertype", docSubTypeSO);

        // Description for Quotation
        String strOldDocSubTypeSO = SLOrderDocTypeData.selectOldDocSubType(this, strOrder);
        if (!StringUtils.equals(docSubTypeSO, "OB")
            && StringUtils.equals(strOldDocSubTypeSO, "OB")) {
          String strOldDocNo = SLOrderDocTypeData.selectOldDocNo(this, strOrder);
          info.addResult("inpdescription",
              FormatUtilities
                  .replaceJS(Utility.messageBD(this, "Quotation", info.vars.getLanguage()) + " "
                      + strOldDocNo + ". " + strDescription));
        }

        if (StringUtils.equals(dataNew[0].isdocnocontrolled, "Y")) {
          String strOldDocTypeTarget = SLOrderDocTypeData.selectOldDocTypeTargetId(this, strOrder);
          if (!newDocNo && !StringUtils.equals(ad_sequence_id, dataNew[0].adSequenceId)
              && !StringUtils.equalsIgnoreCase(strOldDocTypeTarget, strDocTypeTarget)) {
            newDocNo = true;
          }
          if (newDocNo) {
            HashMap<String, Object> values = new HashMap<>();
            values.put("currentNextSys", dataNew[0].currentnextsys);
            values.put("currentNext", dataNew[0].currentnext);

            var orderSequenceAction = CalloutSequence.getInstance().getSL_Order_SequenceAction().get();
            if (orderSequenceAction != null) {
              orderSequenceAction.get_SL_Order_inpdocumentnoValue(info, values);
            }
          }
        }

        // Payment Rule, Invoice Rule, Delivery Rule
        paymentRule = "P";
        invoiceRule = StringUtils.equals(docSubTypeSO, "PR")
            || StringUtils.equals(docSubTypeSO, "WI") ? "I" : "D";
        deliveryRule = "A";

        if (StringUtils.equals(dataNew[0].issotrx, "N")) {
          isSOTrx = false;
        }
      }

      if (!StringUtils.equalsIgnoreCase(docSubTypeSO, "WR")) {

        // Get Business Partner Data
        SLOrderDocTypeData[] dataBP = SLOrderDocTypeData.BPartner(this, strBPartner);

        // Get Payment Rule from business partner
        if (dataBP != null && dataBP.length > 0) {
          String bpPaymentRule = isSOTrx ? dataBP[0].paymentrule : dataBP[0].paymentrulepo;
          if (StringUtils.isNotEmpty(bpPaymentRule)) {
            if (StringUtils.equals(bpPaymentRule, "B")
                || (isSOTrx && (StringUtils.equals(bpPaymentRule, "S")
                    || StringUtils.equals(bpPaymentRule, "U")))) {
              bpPaymentRule = "P";
            }
            paymentRule = bpPaymentRule;
          }

          // Get Invoice Rule from business partner for other than Credit and Prepay Order
          invoiceRule = StringUtils.equals(docSubTypeSO, "PR")
              || StringUtils.equals(docSubTypeSO, "WI") ? "I" : dataBP[0].invoicerule;

          // Get Delivery Rule from business partner
          deliveryRule = dataBP[0].deliveryrule;

          // Get Delivery Via Rule from business partner
          if (StringUtils.isNotEmpty(dataBP[0].deliveryviarule)) {
            info.addResult("inpdeliveryviarule", dataBP[0].deliveryviarule);
          }
        }

        // Added by gorkaion remove when feature request 4350 is done
        FieldProvider[] l = null;
        try {
          ComboTableData comboTableData = new ComboTableData(info.vars, this, "LIST", "",
              "C_Order InvoiceRule", "",
              Utility.getContext(this, info.vars, "#AccessibleOrgTree", "SLOrderDocType"),
              Utility.getContext(this, info.vars, "#User_Client", "SLOrderDocType"), 0);
          Utility.fillSQLParameters(this, info.vars, null, comboTableData, "SLOrderDocType", "");
          l = comboTableData.select(false);
          comboTableData = null;
        } catch (Exception ex) {
          throw new ServletException(ex);
        }
        // Load All Invoice Rules for Non POS Order Document Type
        if (l != null && l.length > 0) {
          info.addSelect("inpinvoicerule");
          for (int i = 0; i < l.length; i++) {
            info.addSelectResult(l[i].getField("id"),
                FormatUtilities.replaceJS(l[i].getField("name")),
                StringUtils.equalsIgnoreCase(l[i].getField("id"), invoiceRule));
          }
          info.endSelect();
        } else {
          info.addResult("inpinvoicerule", "");
        }

      } else {
        // Load only selected Invoice Rules for POS Order Document Type
        info.addSelect("inpinvoicerule");
        info.addSelectResult("D", FormatUtilities.replaceJS(ListData.selectName(this, "150", "D")),
            StringUtils.equals(invoiceRule, "D"));
        info.addSelectResult("I", FormatUtilities.replaceJS(ListData.selectName(this, "150", "I")),
            StringUtils.equals(invoiceRule, "I"));
        info.addSelectResult("O", FormatUtilities.replaceJS(ListData.selectName(this, "150", "O")),
            StringUtils.equals(invoiceRule, "O"));
        info.endSelect();
      }

      // Set Payment Rule
      if (StringUtils.isNotEmpty(paymentRule)) {
        info.addResult("inppaymentrule", paymentRule);
      }

      // Set Delivery Rule
      if (StringUtils.isNotEmpty(deliveryRule)) {
        info.addResult("inpdeliveryrule", deliveryRule);
      }
    }

  }
}
