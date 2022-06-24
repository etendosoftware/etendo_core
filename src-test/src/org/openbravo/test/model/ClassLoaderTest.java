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
 * All portions are Copyright (C) 2010-2021 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.test.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.Servlet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.junit.Test;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.domain.ModelImplementation;
import org.openbravo.test.base.OBBaseTest;

/**
 * Tests registered classes in Application Dictionary
 * 
 * @author iperdomo
 */
public class ClassLoaderTest extends OBBaseTest {

  private static final Logger log = LogManager.getLogger();
  private static List<String> notFoundClasses = new ArrayList<String>();
  private static List<String> notServletClasses = new ArrayList<String>();
  private static boolean initialized = false;

  /**
   * Test if all registered classes in Application Dictionary can be loaded. Consistency test to
   * have a clean web.xml
   */
  @Test
  public void modelClassesShouldBeImplemented() {
    loadModel();

    logErrors(notFoundClasses, "Missing classes");
    assertThat("Missing classes defined in AD_Model_Object", notFoundClasses, is(empty()));
  }

  /**
   * Test if all registered classes in Application Dictionary implement Servlet when needed.
   * Consistency test to have a clean web.xml
   */
  @Test
  public void modelClassesShouldImplementServlet() {
    loadModel();

    logErrors(notServletClasses, "Classes not implementing Servlet");
    assertThat("Classes not implement Servlet defined in AD_Model_Object", notServletClasses,
        is(empty()));
  }

  private void loadModel() {
    if (initialized) {
      return;
    }

    initialized = true;
    notFoundClasses = new ArrayList<String>();
    notServletClasses = new ArrayList<String>();

    setSystemAdministratorContext();

    // "S" - "Servlet"
    // "C" - "ContextParam"
    // "L" - "Listener"
    // "ST" - "Session timeout"
    // "F" - "Filter"
    // "R" - "Resource"

    final Object[] in = { "L", "F" };

    // Checking listener and filters classes
    OBCriteria<ModelImplementation> obc = OBDal.getInstance()
        .createCriteria(ModelImplementation.class);
    obc.add(Restrictions.in(ModelImplementation.PROPERTY_OBJECTTYPE, in));

    // these don't need to implement Servlet
    checkClasses("Listener/Filter", obc.list(), notFoundClasses, new ArrayList<String>());

    // Checking manual servlets
    obc = OBDal.getInstance().createCriteria(ModelImplementation.class);
    obc.add(Restrictions.eq(ModelImplementation.PROPERTY_OBJECTTYPE, "S"));
    obc.add(Restrictions.isNull(ModelImplementation.PROPERTY_SPECIALFORM));
    obc.add(Restrictions.isNull(ModelImplementation.PROPERTY_PROCESS));
    obc.add(Restrictions.isNull(ModelImplementation.PROPERTY_CALLOUT));

    checkClasses("Manual Servlet", obc.list(), notFoundClasses, notServletClasses);

    // Checking servlets associated to forms
    OBQuery<ModelImplementation> obq = OBDal.getInstance()
        .createQuery(ModelImplementation.class,
            "objectType = 'S' and specialForm is not null and specialForm.active = true");

    checkClasses("Form", obq.list(), notFoundClasses, notServletClasses);

    // Check servlets associated to processes/reports
    obq = OBDal.getInstance()
        .createQuery(ModelImplementation.class,
            "objectType = 'S' and process is not null and process.active = true and process.uIPattern = 'M' and process.report = false");

    checkClasses("Process", obq.list(), notFoundClasses, notServletClasses);

    // Checking servlets associated to tabs
    obq = OBDal.getInstance()
        .createQuery(ModelImplementation.class,
            "objectType = 'S' and tab is not null and tab.active = true and tab.window.active = true");

    checkClasses("Tab", obq.list(), notFoundClasses, notServletClasses);
  }

  private void checkClasses(String type, List<ModelImplementation> models, List<String> notFound,
      List<String> notServlet) {
    for (ModelImplementation mi : models) {
      String className = mi.getJavaClassName();
      try {
        Class<?> clz = Class.forName(className);
        if (!Servlet.class.isAssignableFrom(clz)) {
          notServlet.add(type + " - " + mi.getId() + ": " + className);
        }

      } catch (ClassNotFoundException e) {
        if ("Listener/Filter".equals(type)
            && mi.getJavaClassName().startsWith("org.apache.catalina.filters")) {
          log.info("Not checking filter {}, which might be implemented by Tomcat", className);
        } else {
          notFound.add(type + " - " + mi.getId() + " : " + className);
        }
      }
    }
  }

  private void logErrors(final List<String> classes, String msg) {
    if (classes.isEmpty()) {
      return;
    }
    log.error("== " + msg + " ==");
    for (String nf : classes) {
      log.error("  " + nf);
    }
    log.error("Total: " + classes.size());
  }
}
