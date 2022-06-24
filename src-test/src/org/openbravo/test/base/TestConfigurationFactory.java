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
 * All portions are Copyright (C) 2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.test.base;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Order;
import org.apache.logging.log4j.core.config.plugins.Plugin;

/**
 * Configuration Factory used in testing environment. This let us override programatically the
 * configuration defined in log4j2-test.xml using the {@link TestXmlLogConfiguration } class
 */
@Plugin(name = "TestConfigurationFactory", category = ConfigurationFactory.CATEGORY)
@Order(10)
public class TestConfigurationFactory extends ConfigurationFactory {

  public static final String[] SUFFIXES = new String[] { ".xml", "*" };

  @Override
  protected String[] getSupportedTypes() {
    return SUFFIXES;
  }

  @Override
  public Configuration getConfiguration(final LoggerContext loggerContext,
      ConfigurationSource source) {
    return new TestXmlLogConfiguration(loggerContext, source);
  }
}
