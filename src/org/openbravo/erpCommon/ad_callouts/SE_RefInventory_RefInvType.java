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
 * All portions are Copyright (C) 2017-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.erpCommon.ad_callouts;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.dal.core.OBContext;
import org.openbravo.materialmgmt.refinventory.ReferencedInventoryUtil;

/**
 * Sets the proposed value from the sequence associated to the referenced inventory type. The
 * sequence is not updated yet.
 *
 */
public class SE_RefInventory_RefInvType extends SimpleCallout {
  private static final Logger log = LogManager.getLogger();

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    try {
      OBContext.setAdminMode(true);
      final String referencedInventoryTypeId = info.getStringParameter("inpmRefinventoryTypeId",
          IsIDFilter.instance);
      final String proposedValue = getNextProposedValueWithoutUpdatingSequence(
          referencedInventoryTypeId);
      info.addResult("inpvalue", proposedValue);
    } catch (Exception logAndIgnore) {
      log.warn("Unexpected error in callout " + this.getClass().getName(), logAndIgnore);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private String getNextProposedValueWithoutUpdatingSequence(
      final String referencedInventoryTypeId) {
    final String nextProposedValue = ReferencedInventoryUtil
        .getProposedValueFromSequenceOrNull(referencedInventoryTypeId, false);
    return StringUtils.isBlank(nextProposedValue) ? "" : "<" + nextProposedValue + ">";
  }
}
