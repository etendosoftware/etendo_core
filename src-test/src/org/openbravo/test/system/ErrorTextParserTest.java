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
 * All portions are Copyright (C) 2010-2018 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.system;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeThat;

import org.junit.Test;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.test.base.OBBaseTest;

/**
 * Test the ErrorTextParser class logic.
 * 
 * @author huehner
 */

public class ErrorTextParserTest extends OBBaseTest {

  @Test
  public void testPGSpanish() throws Exception {
    // only test on pgsql, as specifically testing against es_ES/pgsql error messsage
    assumeThat("Executing only in PostgreSQL", getConnectionProvider().getRDBMS(),
        is(equalTo("POSTGRE")));
    String expectedMessage = "This record cannot be deleted because it is associated with other existing elements. Please see Linked Items";
    String errorMessage = "inserción o actualización en la tabla «c_bpartner» viola la llave foránea «c_bpartner_c_bp_group»";
    doErrorTextParserTestWithoutDB(errorMessage, expectedMessage);
  }

  @Test
  public void testMultipleMessages() throws Exception {
    String messagePrefix;
    if ("POSTGRE".equals(getConnectionProvider().getRDBMS())) {
      messagePrefix = "ERROR: ";
    } else {
      messagePrefix = "ORA-20000: ";
    }
    String expectedMessage = "There is no conversion rate defined from (USD-$) to (AED-د.إ) for date '09-02-2015', Client 'F&B International Group' and Organization 'F&B US East Coast'.";
    String errorMessage = messagePrefix
        + "@NoConversionRate@ (USD-$) @to@ (AED-د.إ) @ForDate@ '09-02-2015', @Client@ 'F&B International Group' @And@ @ACCS_AD_ORG_ID_D@ 'F&B US East Coast'.";
    doErrorTextParserTestWithoutDB(errorMessage, expectedMessage);
  }

  private void doErrorTextParserTestWithoutDB(String errorMessage, String expectedMessage)
      throws Exception {
    ConnectionProvider conn = getConnectionProvider();
    VariablesSecureApp vars = new VariablesSecureApp("", "", "");
    OBError trlError = Utility.translateError(conn, vars, "en_US", errorMessage);
    assertEquals(expectedMessage, trlError.getMessage());
  }

}
