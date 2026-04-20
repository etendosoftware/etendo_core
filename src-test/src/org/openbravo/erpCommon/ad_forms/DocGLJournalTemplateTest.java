package org.openbravo.erpCommon.ad_forms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.Connection;

import javax.servlet.ServletException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.database.ConnectionProvider;
/** Tests for {@link DocGLJournalTemplate}. */
@SuppressWarnings({"java:S120"})

@RunWith(MockitoJUnitRunner.class)
public class DocGLJournalTemplateTest {
  /** Get servlet info. */

  @Test
  public void testGetServletInfo() {
    DocGLJournalTemplate instance = new DocGLJournalTemplate() {
      /**
       * Create fact.
       * @throws ServletException if an error occurs
       */
      @Override
      public Fact createFact(DocGLJournal docGLJournal, AcctSchema as, ConnectionProvider conn,
          Connection con, VariablesSecureApp vars) throws ServletException {
        return null;
      }
    };
    assertEquals("Servlet for the accounting", instance.getServletInfo());
  }
  /** Constructor does not throw. */

  @Test
  public void testConstructorDoesNotThrow() {
    DocGLJournalTemplate instance = new DocGLJournalTemplate() {
      /**
       * Create fact.
       * @throws ServletException if an error occurs
       */
      @Override
      public Fact createFact(DocGLJournal docGLJournal, AcctSchema as, ConnectionProvider conn,
          Connection con, VariablesSecureApp vars) throws ServletException {
        return null;
      }
    };
    assertNotNull(instance);
  }
}
