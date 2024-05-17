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
 * All portions are Copyright (C) 2022 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.report.jasper;

import java.util.concurrent.Executor;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.dal.core.DalThreadCleaner;
import org.openbravo.dal.core.OBContext;

import net.sf.jasperreports.engine.fill.JRBaseFiller;
import net.sf.jasperreports.engine.fill.JRFillSubreport;
import net.sf.jasperreports.engine.fill.ThreadExecutorSubreportRunner;

/**
 * An extension of the standard ThreadExecutorSubreportRunner to be able to close the DAL connection
 * when the filling of the subreport is completed. This way we avoid having database connection
 * leaks in case subreports make use of DAL.
 */
class SubreportRunner extends ThreadExecutorSubreportRunner {
  private static final Logger log = LogManager.getLogger();

  private HttpServletRequest httpRequest;

  SubreportRunner(JRFillSubreport fillSubreport, JRBaseFiller subreportFiller,
      Executor threadExecutor, HttpServletRequest httpRequest) {
    super(fillSubreport, subreportFiller, threadExecutor);
    this.httpRequest = httpRequest;
  }

  @Override
  public void run() {
    try {
      setOBContext();
      super.run();
    } finally {
      cleanUp();
    }
  }

  private void setOBContext() {
    if (httpRequest == null) {
      log.warn("Could not set OBContext for the subreport filling");
      return;
    }
    try {
      OBContext.setOBContext(httpRequest);
    } catch (IllegalStateException e) {
      // If session has already been invalidated, IllegalStateException is thrown. Just log it
      // but don't fail.
      log.error("Could not set OBContext in request for URI {} - ", httpRequest.getRequestURI(), e);
    }
  }

  /**
   * Closes the default DAL session (which ideally should not be used by the subreport, but just in
   * case) and the rest of the sessions, i.e., the read-only DAL session. Cleanups the OBContext.
   * The default DAL session is rolled back because we do not want to persist any change, reports
   * are read-only by definition.
   */
  private void cleanUp() {
    DalThreadCleaner.getInstance().cleanWithRollback();
  }
}
