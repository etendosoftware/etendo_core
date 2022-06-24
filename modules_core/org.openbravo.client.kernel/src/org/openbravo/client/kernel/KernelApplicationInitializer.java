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
 * All portions are Copyright (C) 2011-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.kernel;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.client.application.report.JmxReportCache;
import org.openbravo.client.application.window.ApplicationDictionaryCachedStructures;
import org.openbravo.client.application.window.JmxApplicationDictionaryCachedStructures;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.jmx.MBeanRegistry;
import org.openbravo.model.ad.system.SystemInformation;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * An {@link ApplicationInitializer} in charge of doing some initialization tasks like checking if
 * both Tomcat and DB are configured to use the same time and registering some standard jmx beans.
 * 
 * @author mtaal
 */
@ApplicationScoped
public class KernelApplicationInitializer implements ApplicationInitializer {
  private static final Logger log4j = LogManager.getLogger();
  private static final String sqlDateTimeFormat = "DD-MM-YYYY HH24:MI:SS";
  private static final String javaDateTimeFormat = "dd-MM-yyyy HH:mm:ss";
  private static final long THRESHOLD = 5000; // 5 seconds
  private static final String PRODUCTION_INSTANCE = "P";

  @Inject
  private StaticResourceProvider resourceProvider;

  @Inject
  private JmxReportCache reportCache;

  @Inject
  private ApplicationDictionaryCachedStructures adCachedStructures;

  @Inject
  private JmxApplicationDictionaryCachedStructures adcsJmx;

  @Override
  public void initialize() {
    checkDatabaseAndTomcatDateTime();
    registerMBeans();
    setModulesAsNotInDevelopment();
  }

  private void checkDatabaseAndTomcatDateTime() {
    // This method checks if both Tomcat and DB are configured to use the same time. If there
    // is a difference bigger than a few seconds, it logs a warning.
    try {
      Date tomcatDate = new Date(); // Tomcat time
      Date dbDate = getDatabaseDateTime(); // Database time
      log4j.debug("Tomcat Time: " + tomcatDate + ", Database Time: " + dbDate);
      if (dbDate != null) {
        long difference = Math.abs(tomcatDate.getTime() - dbDate.getTime());
        if (difference > THRESHOLD) {
          log4j.warn("Tomcat and Database do not have the same time. Tomcat Time: " + tomcatDate
              + ", Database Time: " + dbDate);
        }
      } else {
        log4j.error(
            "Received null as Database time. Not possible to check time differences with Tomcat.");
      }
    } catch (Exception ex) {
      log4j.error("Could not check if Tomcat and Database have the same time.", ex);
    }
  }

  private Date getDatabaseDateTime() {
    Date date = null;
    try {
      // We retrieve the time from the database, using the predefined sql date-time format
      String now = DateTimeData.now(new DalConnectionProvider(false), sqlDateTimeFormat);
      SimpleDateFormat formatter = new SimpleDateFormat(javaDateTimeFormat);
      date = formatter.parse(now);
    } catch (Exception ex) {
      log4j.error("Could not get the Database time.", ex);
    }
    return date;
  }

  private void registerMBeans() {
    MBeanRegistry.registerMBean(KernelConstants.RESOURCE_COMPONENT_ID, resourceProvider);
    MBeanRegistry.registerMBean(JmxReportCache.MBEAN_NAME, reportCache);
    MBeanRegistry.registerMBean(JmxApplicationDictionaryCachedStructures.MBEAN_NAME, adcsJmx);
  }

  private void setModulesAsNotInDevelopment() {
    log4j.debug("Checking instance purpose and In Development modules");
    if (PRODUCTION_INSTANCE.equals(getInstancePurpose()) && adCachedStructures.isInDevelopment()) {
      adCachedStructures.setNotInDevelopment();
    }
  }

  private String getInstancePurpose() {
    return (String) OBDal.getInstance()
        .getSession()
        .createQuery("select " + SystemInformation.PROPERTY_INSTANCEPURPOSE + " from "
            + SystemInformation.ENTITY_NAME)
        .uniqueResult();
  }
}
