package org.openbravo.erpCommon.ad_callouts;

import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

/**
 * Clears the Document Type selector whenever either the "Sales Transaction" flag
 * or the "Document Category" field changes.
 */
public class BusinessPartnerDocTypeValidation extends SimpleCallout {
  // Input name for the Document Type.
  private static final String DOCTYPE = "inpcDoctypeId";
  // Input name for the Document Category.
  private static final String DOCUMENTCATEGORY = "inpdocumentcategory";
  // Input name for the Sales Transaction.
  private static final String ISSOTRX = "inpissotrx";

  /**
   * Handles the on-change event for the bound fields and clears the target selector.
   * @param info the callout context; provides the last changed field and allows returning values
   * @throws ServletException if callout processing fails
   */
  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    String last = info.getLastFieldChanged();
    if (StringUtils.equals(ISSOTRX, last) || StringUtils.equals(DOCUMENTCATEGORY, last)) {
      info.addResult(DOCTYPE, "");
      info.addResult(DOCTYPE + "_R", "");
    }
  }
}
