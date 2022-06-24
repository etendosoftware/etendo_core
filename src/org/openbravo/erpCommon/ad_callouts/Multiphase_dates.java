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
 * All portions are Copyright (C) 2012-2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.erpCommon.ad_callouts;

import java.text.ParseException;
import java.util.Date;

import javax.servlet.ServletException;

import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.project.Project;
import org.openbravo.model.project.ProjectPhase;

public class Multiphase_dates extends SimpleCallout {

  private final String PROJECTTASK_TAB = "490";
  private final String PROJECTPHASE_TAB = "478";

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    try {
      String tab = info.getStringParameter("inpTabId", null);
      Date dateStart = OBDateUtils.getDate(info.getStringParameter("inpstartdate", null));
      Date datePlanned = OBDateUtils.getDate(info.getStringParameter("inpdatecontract", null));
      Date dateEnd = OBDateUtils.getDate(info.getStringParameter("inpenddate", null));

      Project pr = OBDal.getInstance()
          .get(Project.class, info.getStringParameter("inpcProjectId", null));
      Date dateStartProject = pr.getStartingDate();
      Date dateEndProject = pr.getEndingDate();
      Date datePlannedProject = pr.getContractDate();

      String warningMessage = OBMessageUtils.messageBD("MultiphaseDates");

      if (tab.equals(PROJECTPHASE_TAB)) {
        if (dateStart != null && dateStartProject != null
            && dateStart.compareTo(dateStartProject) < 0) {
          info.addResult("WARNING", warningMessage);
        } else if (dateStart != null && dateEndProject != null
            && dateEndProject.compareTo(dateStart) < 0) {
          info.addResult("WARNING", warningMessage);
        } else if (dateStart != null && datePlannedProject != null
            && datePlannedProject.compareTo(dateStart) < 0) {
          info.addResult("WARNING", warningMessage);
        }
      }

      if (tab.equals(PROJECTTASK_TAB)) {
        ProjectPhase prph = OBDal.getInstance()
            .get(ProjectPhase.class, info.getStringParameter("inpcProjectphaseId", null));
        Date dateStartPhase = prph.getStartingDate();
        Date dateEndPhase = prph.getEndingDate();
        Date datePlannedPhase = prph.getContractDate();

        if (dateStart != null && dateStartPhase != null
            && dateStart.compareTo(dateStartPhase) < 0) {
          info.addResult("WARNING", warningMessage);
        } else if (dateStart != null && dateEndPhase != null
            && dateEndPhase.compareTo(dateStart) < 0) {
          info.addResult("WARNING", warningMessage);
        } else if (dateStart != null && datePlannedPhase != null
            && datePlannedPhase.compareTo(dateStart) < 0) {
          info.addResult("WARNING", warningMessage);
        }
      }

      if (dateStart != null && datePlanned != null && datePlanned.compareTo(dateStart) < 0) {
        info.addResult("WARNING", warningMessage);
      } else if (dateStart != null && dateEnd != null && dateEnd.compareTo(dateStart) < 0) {
        info.addResult("WARNING", warningMessage);
      }

    } catch (ParseException e) {
      log4j.error("Process failed checking dates in Multiphase Project", e);
    }

  }
}
