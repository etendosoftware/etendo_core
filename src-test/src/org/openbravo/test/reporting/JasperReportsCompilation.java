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
 * All portions are Copyright (C) 2016 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.test.reporting;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.openbravo.test.base.Issue;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

/**
 * Test cases covering the jrxml compilation.
 * 
 * @author alostale
 *
 */
public class JasperReportsCompilation {
  /**
   * Compiles a jrxml which includes a String.replace. This case was failing using JDK8 with
   * jdt-compiler-3.1.1.jar. Before fixing the issue, this test case failed with an exception.
   */
  @Test
  @Issue("31709")
  public void jrxmlShouldBeCompiledWithAllSupportedJDKs() throws JRException, IOException {
    String jrxml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
        + "<jasperReport bottomMargin=\"20\" columnWidth=\"535\" leftMargin=\"30\" name=\"ReportTrialBalancePDF\" pageHeight=\"842\" pageWidth=\"595\" rightMargin=\"30\" topMargin=\"20\" uuid=\"94f73212-0a4e-4d01-a77d-7c1011919e14\"\n" //
        + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://jasperreports.sourceforge.net/jasperreports\" xsi:schemaLocation=\"http://jasperreports.sourceforge.net/jasperreports http://jasperreports.sourceforge.net/xsd/jasperreport.xsd\">\n" //
        + "  <import value=\"net.sf.jasperreports.engine.*\"/>\n" //
        + "  <import value=\"java.util.*\"/>\n" //
        + "\n" //
        + "  <pageFooter>\n" //
        + "    <band height=\"40\">\n" //
        + "      <textField>\n" //
        + "        <reportElement height=\"16\" width=\"257\" x=\"245\" y=\"15\"/>\n" //

        // this was the problematic instruction
        + "        <textFieldExpression><![CDATA[\"xx\".replace(\"x\",\"1\")]]></textFieldExpression>\n" //

        + "      </textField>\n" //
        + "    </band>\n" //
        + "  </pageFooter>\n" //
        + "</jasperReport>";
    JasperDesign jasperDesign = JRXmlLoader.load(IOUtils.toInputStream(jrxml, "UTF-8"));
    @SuppressWarnings("unused")
    JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
  }

  /**
   * Compiles a jrxml which includes several JDK8 and JDK11 features.
   */
  @Test
  public void jrxmlShouldBeCompiledWithJDK11OrHigher() throws JRException, IOException {
    String jrxml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
        + "<jasperReport bottomMargin=\"20\" columnWidth=\"535\" leftMargin=\"30\" name=\"ReportTrialBalancePDF\" pageHeight=\"842\" pageWidth=\"595\" rightMargin=\"30\" topMargin=\"20\" uuid=\"94f73212-0a4e-4d01-a77d-7c1011919e14\"\n" //
        + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://jasperreports.sourceforge.net/jasperreports\" xsi:schemaLocation=\"http://jasperreports.sourceforge.net/jasperreports http://jasperreports.sourceforge.net/xsd/jasperreport.xsd\">\n" //
        + "  <import value=\"net.sf.jasperreports.engine.*\"/>\n" //
        + "  <import value=\"java.util.*\"/>\n" //
        + "  <import value=\"java.util.Arrays\"/>\n"
        + "  <import value=\"java.util.stream.Collectors\"/>\n" + "\n" //
        + "  <pageFooter>\n" //
        + "    <band height=\"40\">\n" //
        + "      <textField>\n" //
        + "        <reportElement height=\"16\" width=\"257\" x=\"245\" y=\"15\"/>\n" //

        // this was the problematic instruction
        + "        <textFieldExpression><![CDATA[Arrays.asList(\" \", \" \").stream().filter((var a) -> a.equals(a)).collect(Collectors.joining(\",\"))]]></textFieldExpression>\n" //

        + "      </textField>\n" //
        + "    </band>\n" //
        + "  </pageFooter>\n" //
        + "</jasperReport>";
    JasperDesign jasperDesign = JRXmlLoader.load(IOUtils.toInputStream(jrxml, "UTF-8"));
    @SuppressWarnings("unused")
    JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
  }
}
