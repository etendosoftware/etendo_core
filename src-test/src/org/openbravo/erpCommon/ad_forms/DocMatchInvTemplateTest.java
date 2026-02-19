package org.openbravo.erpCommon.ad_forms;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;

import javax.servlet.ServletException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.database.ConnectionProvider;

@RunWith(MockitoJUnitRunner.Silent.class)
public class DocMatchInvTemplateTest {

  private DocMatchInvTemplate instance;

  @Before
  public void setUp() {
    instance = new DocMatchInvTemplate() {
      @Override
      public Fact createFact(DocMatchInv docMatchInv, AcctSchema as,
          ConnectionProvider conn, Connection con, VariablesSecureApp vars)
          throws ServletException {
        return null;
      }
    };
  }

  @Test
  public void testGetServletInfo() {
    assertEquals("Servlet for the accounting", instance.getServletInfo());
  }
}
