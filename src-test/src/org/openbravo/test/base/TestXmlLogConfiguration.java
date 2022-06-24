package org.openbravo.test.base;/*
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
                                * All portions are Copyright (C) 2018 Openbravo SLU
                                * All Rights Reserved.
                                * Contributor(s):  ______________________________________.
                                ************************************************************************
                                */

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;

/**
 * This log4j configuration overrides the XmlConfiguration adding programatically the
 * TestLogAppender, which is used by some tests to make assertions on log entries.
 */
public class TestXmlLogConfiguration extends XmlConfiguration {

  public TestXmlLogConfiguration(LoggerContext loggerContext,
      final ConfigurationSource configSource) {
    super(loggerContext, configSource);
  }

  @Override
  protected void doConfigure() {
    super.doConfigure();

    final Appender appender = new TestLogAppender("TestLogAppender", getRootLogger().getFilter());
    addAppender(appender);
    getRootLogger().addAppender(appender, Level.OFF, getRootLogger().getFilter());
  }
}
