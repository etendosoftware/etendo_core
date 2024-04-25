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

import java.util.concurrent.ExecutorService;

import org.openbravo.client.kernel.RequestContext;

import net.sf.jasperreports.engine.fill.JRBaseFiller;
import net.sf.jasperreports.engine.fill.JRFillContext;
import net.sf.jasperreports.engine.fill.JRFillSubreport;
import net.sf.jasperreports.engine.fill.JRSubreportRunner;
import net.sf.jasperreports.engine.fill.ThreadPoolSubreportRunnerFactory;

/**
 * This class extends the jasper reports standard ThreadPoolSubreportRunnerFactory to make use of
 * {@link SubreportRunner} instances to fill the subreports.
 */
public class SubreportRunnerFactory extends ThreadPoolSubreportRunnerFactory {

  private static final String THREAD_POOL_KEY = "org.openbravo.report.jasper.JRThreadSubreportRunner.ThreadPool";

  @Override
  public JRSubreportRunner createSubreportRunner(JRFillSubreport fillSubreport,
      JRBaseFiller subreportFiller) {
    JRFillContext fillContext = subreportFiller.getFillContext();
    ExecutorServiceDisposable executor = (ExecutorServiceDisposable) fillContext
        .getFillCache(THREAD_POOL_KEY);
    if (executor == null) {
      ExecutorService threadExecutor = createThreadExecutor(fillContext);
      executor = new ExecutorServiceDisposable(threadExecutor);
      fillContext.setFillCache(THREAD_POOL_KEY, executor);
    }

    return new SubreportRunner(fillSubreport, subreportFiller, executor.getExecutorService(),
        RequestContext.get().getRequest());
  }
}
