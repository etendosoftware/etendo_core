package org.openbravo.erpCommon.ad_callouts;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.ServletException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for {@link BusinessPartnerDocTypeValidation}.
 */
@SuppressWarnings({"java:S120"})
@RunWith(MockitoJUnitRunner.class)
public class BusinessPartnerDocTypeValidationTest {

  private static final String INPC_DOCTYPE_ID = "inpcDoctypeId";
  private static final String INPC_DOCTYPE_ID_R = "inpcDoctypeId_R";

  private BusinessPartnerDocTypeValidation callout;

  @Mock
  private SimpleCallout.CalloutInfo info;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    callout = new BusinessPartnerDocTypeValidation();
  }
  /**
   * Execute clears doc type when sales transaction changed.
   * @throws ServletException if an error occurs
   */

  @Test
  public void testExecuteClearsDocTypeWhenSalesTransactionChanged() throws ServletException {
    when(info.getLastFieldChanged()).thenReturn("inpissotrx");

    callout.execute(info);

    verify(info).addResult(INPC_DOCTYPE_ID, "");
    verify(info).addResult(INPC_DOCTYPE_ID_R, "");
  }
  /**
   * Execute clears doc type when document category changed.
   * @throws ServletException if an error occurs
   */

  @Test
  public void testExecuteClearsDocTypeWhenDocumentCategoryChanged() throws ServletException {
    when(info.getLastFieldChanged()).thenReturn("inpdocumentcategory");

    callout.execute(info);

    verify(info).addResult(INPC_DOCTYPE_ID, "");
    verify(info).addResult(INPC_DOCTYPE_ID_R, "");
  }
  /**
   * Execute clears doc type when org changed.
   * @throws ServletException if an error occurs
   */

  @Test
  public void testExecuteClearsDocTypeWhenOrgChanged() throws ServletException {
    when(info.getLastFieldChanged()).thenReturn("inpadOrgId");

    callout.execute(info);

    verify(info).addResult(INPC_DOCTYPE_ID, "");
    verify(info).addResult(INPC_DOCTYPE_ID_R, "");
  }
  /**
   * Execute does not clear doc type for other fields.
   * @throws ServletException if an error occurs
   */

  @Test
  public void testExecuteDoesNotClearDocTypeForOtherFields() throws ServletException {
    when(info.getLastFieldChanged()).thenReturn("inpSomeOtherField");

    callout.execute(info);

    verify(info, never()).addResult(INPC_DOCTYPE_ID, "");
    verify(info, never()).addResult(INPC_DOCTYPE_ID_R, "");
  }
  /**
   * Execute does not clear doc type when field is null.
   * @throws ServletException if an error occurs
   */

  @Test
  public void testExecuteDoesNotClearDocTypeWhenFieldIsNull() throws ServletException {
    when(info.getLastFieldChanged()).thenReturn(null);

    callout.execute(info);

    verify(info, never()).addResult(INPC_DOCTYPE_ID, "");
    verify(info, never()).addResult(INPC_DOCTYPE_ID_R, "");
  }
}
