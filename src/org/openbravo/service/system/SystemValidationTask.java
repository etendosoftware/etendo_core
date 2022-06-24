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
 * All portions are Copyright (C) 2009-2021 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.service.system;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.DalInitializingTask;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.module.Module;

/**
 * Performs different types of validations on the basis of the type parameter.
 * 
 * @author mtaal
 */
public class SystemValidationTask extends DalInitializingTask {
  private static final Logger log = LogManager.getLogger("SystemValidation");

  private String type;
  private boolean failOnError = false;
  private String moduleJavaPackage;

  @Override
  protected void doExecute() {
    final Module module = getModule();

    // does both module and database
    if (getType().contains("module")) {
      log.info("Validating Modules");

      // Validate module
      final SystemValidationResult result = SystemService.getInstance()
          .validateModule(module, null);

      if (result.getErrors().isEmpty() && result.getWarnings().isEmpty()) {
        log.warn("Validation successfull no warnings or errors");
      } else {
        final String errors = SystemService.getInstance().logValidationResult(log, result);
        if (!result.getErrors().isEmpty() && failOnError) {
          throw new OBException(errors);
        }
      }
    }
  }

  private Module getModule() {
    if (getModuleJavaPackage() == null) {
      return null;
    }
    final OBCriteria<Module> modules = OBDal.getInstance().createCriteria(Module.class);
    modules.add(Restrictions.eq(Module.PROPERTY_JAVAPACKAGE, moduleJavaPackage));

    if (modules.list().size() == 0) {
      throw new OBException("Module with javapackage " + moduleJavaPackage + " does not exist");
    }
    return modules.list().get(0);
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public boolean isFailOnError() {
    return failOnError;
  }

  public void setFailOnError(boolean failOnError) {
    this.failOnError = failOnError;
  }

  public String getModuleJavaPackage() {
    return moduleJavaPackage;
  }

  public void setModuleJavaPackage(String moduleJavaPackage) {
    this.moduleJavaPackage = moduleJavaPackage;
  }
}
