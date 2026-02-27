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
/** Tests for {@link DocDPManagementTemplate}. */
@SuppressWarnings({"java:S120"})

@RunWith(MockitoJUnitRunner.Silent.class)
public class DocDPManagementTemplateTest {

  private DocDPManagementTemplate instance;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    instance = new DocDPManagementTemplate() {
      /**
       * Create fact.
       * @throws ServletException if an error occurs
       */
      @Override
      public Fact createFact(DocDPManagement docDPManagement, AcctSchema as,
          ConnectionProvider conn, Connection con, VariablesSecureApp vars)
          throws ServletException {
        return null;
      }
    };
  }
  /** Get servlet info. */

  @Test
  public void testGetServletInfo() {
    assertEquals("Servlet for the accounting", instance.getServletInfo());
  }
}
