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

@RunWith(MockitoJUnitRunner.class)
public class DocGLJournalTemplateTest {

  @Test
  public void testGetServletInfo() {
    DocGLJournalTemplate instance = new DocGLJournalTemplate() {
      @Override
      public Fact createFact(DocGLJournal docGLJournal, AcctSchema as, ConnectionProvider conn,
          Connection con, VariablesSecureApp vars) throws ServletException {
        return null;
      }
    };
    assertEquals("Servlet for the accounting", instance.getServletInfo());
  }

  @Test
  public void testConstructorDoesNotThrow() {
    DocGLJournalTemplate instance = new DocGLJournalTemplate() {
      @Override
      public Fact createFact(DocGLJournal docGLJournal, AcctSchema as, ConnectionProvider conn,
          Connection con, VariablesSecureApp vars) throws ServletException {
        return null;
      }
    };
    assertNotNull(instance);
  }
}
