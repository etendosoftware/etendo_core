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
 * All portions are Copyright (C) 2016-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application.attachment;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.application.Parameter;
import org.openbravo.client.application.window.ApplicationDictionaryCachedStructures;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.utility.AttachmentMethod;

/**
 * Callout executed only on Metadata Tab of "Windows, Tabs and Fields" window. It calculates the
 * Sequence Number to set on the new metadata when the Attachment Method is selected.
 */
public class MetadataOnTab extends SimpleCallout {
  private static final String WINDOWTABSFIELDS_WINDOW_ID = "102";

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    final String windowId = info.getWindowId();
    if (!WINDOWTABSFIELDS_WINDOW_ID.equals(windowId)) {
      return;
    }
    final String methodId = info.getStringParameter("inpcAttachmentMethodId", IsIDFilter.instance);
    final String tabId = info.getStringParameter("inpadTabId", IsIDFilter.instance);
    if (StringUtils.isEmpty(methodId)) {
      info.addResult("inpseqno", "");
    } else {
      int seqNo = getNextSeqNo(methodId, tabId);
      info.addResult("inpseqno", seqNo);
    }
  }

  private int getNextSeqNo(String methodId, String tabId) {
    ApplicationDictionaryCachedStructures adcs = WeldUtils
        .getInstanceFromStaticBeanManager(ApplicationDictionaryCachedStructures.class);

    AttachmentMethod attMethod = OBDal.getInstance().get(AttachmentMethod.class, methodId);
    Tab tab = adcs.getTab(tabId);
    OBCriteria<Parameter> critParam = OBDal.getInstance().createCriteria(Parameter.class);
    critParam.add(Restrictions.eq(Parameter.PROPERTY_ATTACHMENTMETHOD, attMethod));
    critParam.add(Restrictions.eq(Parameter.PROPERTY_TAB, tab));
    critParam.setProjection(Projections.max(Parameter.PROPERTY_SEQUENCENUMBER));

    if (critParam.uniqueResult() == null) {
      return 10;
    }
    long maxSeqNo = (Long) critParam.uniqueResult();

    return (int) (maxSeqNo + 10);
  }
}
