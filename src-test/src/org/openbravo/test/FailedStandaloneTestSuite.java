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

package org.openbravo.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.openbravo.client.application.test.ApplicationTest;
import org.openbravo.client.application.test.GenerateTypesJSTest;
import org.openbravo.client.application.test.MenuTemplateTest;
import org.openbravo.client.kernel.freemarker.test.FreemarkerTemplateProcessorTest;
import org.openbravo.client.kernel.freemarker.test.GenerateComponentTest;
import org.openbravo.client.kernel.freemarker.test.LabelTest;
import org.openbravo.client.kernel.test.CompressionTest;
import org.openbravo.test.model.IndexesTest;
import org.openbravo.test.reporting.JasperReportsCompilation;
import org.openbravo.test.views.ViewGeneration;
import org.openbravo.test.xml.ClientExportImportTest;

/**
 * This test class contains test cases that are executable, valid and do not require
 * Tomcat to be running.
 * These tests all fail on 10/02/2022.
 * <p>
 * Test cases requiring Tomcat (ie. testing web service requests) should be inclued in
 * {@link WebserviceTestSuite}.
 *
 * @author mtaal
 * @see WebserviceTestSuite
 */

@RunWith(Suite.class)
@Suite.SuiteClasses({

        // model
        IndexesTest.class, //

        // xml
        ClientExportImportTest.class, //

        // client application
        ApplicationTest.class, //
        GenerateTypesJSTest.class, //
        MenuTemplateTest.class, //

        // client kernel
        FreemarkerTemplateProcessorTest.class, //
        GenerateComponentTest.class, //
        LabelTest.class, //
        CompressionTest.class, //

        // jasper
        JasperReportsCompilation.class, //

        // others
        ViewGeneration.class,


})
public class FailedStandaloneTestSuite {
}
