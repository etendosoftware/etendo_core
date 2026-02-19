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
/** Tests for {@link DocFINFinAccTransactionTemplate}. */
@SuppressWarnings({"java:S120"})

@RunWith(MockitoJUnitRunner.Silent.class)
public class DocFINFinAccTransactionTemplateTest {

  private DocFINFinAccTransactionTemplate instance;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    instance = new DocFINFinAccTransactionTemplate() {
      /**
       * Create fact.
       * @throws ServletException if an error occurs
       */
      @Override
      public Fact createFact(DocFINFinAccTransaction docFAT, AcctSchema as,
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
