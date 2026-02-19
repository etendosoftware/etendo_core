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
/** Tests for {@link DocInternalConsumptionTemplate}. */
@SuppressWarnings({"java:S120"})

@RunWith(MockitoJUnitRunner.class)
public class DocInternalConsumptionTemplateTest {
  /** Get servlet info. */

  @Test
  public void testGetServletInfo() {
    DocInternalConsumptionTemplate instance = new DocInternalConsumptionTemplate() {
      /** Create fact. */
      @Override
      public Fact createFact(DocInternalConsumption docInternalConsumption, AcctSchema as,
          ConnectionProvider conn, Connection con, VariablesSecureApp vars)
          throws ServletException {
        return null;
      }
    };
    assertEquals("Servlet for the accounting", instance.getServletInfo());
  }
  /** Constructor does not throw. */

  @Test
  public void testConstructorDoesNotThrow() {
    DocInternalConsumptionTemplate instance = new DocInternalConsumptionTemplate() {
      /** Create fact. */
      @Override
      public Fact createFact(DocInternalConsumption docInternalConsumption, AcctSchema as,
          ConnectionProvider conn, Connection con, VariablesSecureApp vars)
          throws ServletException {
        return null;
      }
    };
    assertNotNull(instance);
  }
  /** Get serial version uid. */

  @Test
  public void testGetSerialVersionUID() {
    assertEquals(1L, DocInternalConsumptionTemplate.getSerialVersionUID());
  }
}
