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
 * All portions are Copyright (C) 2009-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.system;

import static org.hamcrest.Matchers.not;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.junit.Test;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.module.Module;
import org.openbravo.service.system.ModuleValidator;
import org.openbravo.service.system.SystemValidationResult;
import org.openbravo.service.system.SystemValidationResult.SystemValidationType;
import org.openbravo.test.base.OBBaseTest;

/**
 * Tests System Validation.
 * 
 * @see ModuleValidator
 * 
 * @author mtaal
 */

public class SystemValidatorTest extends OBBaseTest {

  private static final Logger log = LogManager.getLogger();

  /**
   * Performs module validation using the {@link ModuleValidator}.
   */
  @Test
  public void testModulesValidation() {
    String postPublication = System.getProperty("post.publication");
    assumeThat("Ignoring test case during post publication cycle", postPublication, not("true"));

    setSystemAdministratorContext();
    List<String> updatedModules = null;
    try {
      updatedModules = setModulesInDev();
      final ModuleValidator moduleValidator = new ModuleValidator();
      final SystemValidationResult result = moduleValidator.validate();
      printResult(result, true);
    } finally {
      resetModules(updatedModules);
    }
  }

  private List<String> setModulesInDev() {
    List<String> updatedModules = new ArrayList<String>();
    OBCriteria<Module> qModules = OBDal.getInstance().createCriteria(Module.class);
    qModules.add(Restrictions.eq(Module.PROPERTY_INDEVELOPMENT, false));
    for (Module mod : qModules.list()) {
      mod.setInDevelopment(true);
      updatedModules.add(mod.getId());
    }
    OBDal.getInstance().flush();
    return updatedModules;
  }

  private void printResult(SystemValidationResult result, boolean allowFail) {
    for (SystemValidationType validationType : result.getWarnings().keySet()) {
      log.warn("\n+++++++++++++++++++++++++++++++++++++++++++++++++++");
      log.warn("Warnings for Validation type: " + validationType);
      log.warn("\n+++++++++++++++++++++++++++++++++++++++++++++++++++");
      final List<String> warnings = result.getWarnings().get(validationType);
      for (String warning : warnings) {
        log.warn(warning);
      }
    }

    final StringBuilder sb = new StringBuilder();
    for (SystemValidationType validationType : result.getErrors().keySet()) {
      sb.append("\n+++++++++++++++++++++++++++++++++++++++++++++++++++");
      sb.append("Errors for Validation type: " + validationType);
      sb.append("\n+++++++++++++++++++++++++++++++++++++++++++++++++++");
      final List<String> errors = result.getErrors().get(validationType);
      for (String err : errors) {
        sb.append(err);
        if (sb.length() > 0) {
          sb.append("\n");
        }
      }
    }
    log.error(sb.toString());
    if (allowFail && sb.length() > 0) {
      fail(sb.toString());
    }
  }

  private void resetModules(List<String> updatedModules) {
    if (updatedModules == null || updatedModules.isEmpty()) {
      return;
    }

    @SuppressWarnings("rawtypes")
    Query upd = OBDal.getInstance()
        .getSession()
        .createQuery("update ADModule set inDevelopment = false where id in (:mods)");
    upd.setParameterList("mods", updatedModules);
    upd.executeUpdate();

    OBDal.getInstance().flush();
  }
}
