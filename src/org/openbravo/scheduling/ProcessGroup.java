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
 * All portions are Copyright (C) 2014 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.scheduling;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.model.ad.ui.ProcessGroupList;
import org.openbravo.model.ad.ui.ProcessRequest;
import org.openbravo.model.ad.ui.ProcessRun;
import org.openbravo.service.db.DalBaseProcess;

/**
 * Process used to execute process groups
 */
public class ProcessGroup extends DalBaseProcess {

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {

    ConnectionProvider conn = bundle.getConnection();
    final VariablesSecureApp vars = bundle.getContext().toVars();
    final ProcessRequest processRequest = OBDal.getInstance()
        .get(ProcessRequest.class, bundle.getProcessRequestId());
    final ProcessRun processRun = OBDal.getInstance()
        .get(ProcessRun.class, bundle.getProcessRunId());
    final org.openbravo.model.ad.ui.ProcessGroup group = processRequest.getProcessGroup();

    OBCriteria<ProcessGroupList> processListcri = OBDal.getInstance()
        .createCriteria(ProcessGroupList.class);
    processListcri.add(Restrictions.eq(ProcessGroupList.PROPERTY_PROCESSGROUP, group));
    processListcri.addOrderBy(ProcessGroupList.PROPERTY_SEQUENCENUMBER, true);
    List<ProcessGroupList> processList = processListcri.list();

    // Since Hibernate lazyloads objects and the subprocesses may access these processes as well,
    // the following is necessary to pre-load the objects and "solidify" them
    for (ProcessGroupList prolist : processList) {
      Hibernate.initialize(prolist);
      Hibernate.initialize(prolist.getProcess());
    }

    GroupInfo groupInfo = new GroupInfo(group, processRequest, processRun, processList,
        group.isStopTheGroupExecutionWhenAProcessFails(), vars, conn);
    // Launch the Process Group execution
    groupInfo.executeNextProcess();

  }

}
