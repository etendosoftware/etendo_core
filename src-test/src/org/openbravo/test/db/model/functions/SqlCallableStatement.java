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
 * All portions are Copyright (C) 2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.db.model.functions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.junit.Test;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.DocumentNoData;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.test.base.Issue;
import org.openbravo.test.base.OBBaseTest;

/**
 * Test cases for sqlc callable statements.
 * 
 * @author alostale
 *
 */
public class SqlCallableStatement extends OBBaseTest {

  /** Asserts that sqlc transactional callable statements are closed. */
  @Test
  @Issue("30891")
  public void transactionalCallableStatemetsShouldBeClosed() throws Exception {
    DalConnectionProvider cp = new DalConnectionProvider(false);
    assumeThat("Executing only in Oracle", cp.getRDBMS(), is(equalTo("ORACLE")));

    Connection conn = OBDal.getInstance().getConnection(false);
    for (int i = 0; i < 200; i++) {
      DocumentNoData.nextDocTypeConnection(conn, cp, "466AF4B0136A4A3F9F84129711DA8BD3",
          "23C59575B9CF467C9620760EB255B389", "Y");
    }

    assertOpenCursors();
  }

  /** Asserts that sqlc non transactional callable statements are closed. */
  @Test
  @Issue("30891")
  public void noTransactionalCallableStatemetsShouldBeClosedCallableStatemetsShouldBeClosed()
      throws Exception {
    DalConnectionProvider cp = new DalConnectionProvider(false);
    assumeThat("Executing only in Oracle", cp.getRDBMS(), is(equalTo("ORACLE")));

    for (int i = 0; i < 200; i++) {
      DocumentNoData.nextDocType(cp, "466AF4B0136A4A3F9F84129711DA8BD3",
          "23C59575B9CF467C9620760EB255B389", "Y");
    }

    assertOpenCursors();
  }

  private void assertOpenCursors() throws Exception, SQLException {
    // getting a direct jdbc connection to system DB user because DBA privileges are required to
    // query open_cursor view
    Properties props = OBPropertiesProvider.getInstance().getOpenbravoProperties();
    Connection con = DriverManager.getConnection(props.getProperty("bbdd.url"),
        props.getProperty("bbdd.systemUser"), props.getProperty("bbdd.systemPassword"));

    String query = "select count(*) from v$open_cursor where sql_text like 'CALL AD_Sequence_DocType%' and upper(user_name) = upper(?)";
    PreparedStatement st = con.prepareStatement(query);

    // connecting as DBA, restrict query to only the DB user creating the cursors
    st.setString(1, props.getProperty("bbdd.user"));

    ResultSet rs = st.executeQuery();
    rs.next();
    int openCursors = rs.getInt(1);
    rs.close();
    st.close();
    con.close();

    assertThat("# of open cursors for the statement", openCursors, is(lessThanOrEqualTo(1)));
  }
}
