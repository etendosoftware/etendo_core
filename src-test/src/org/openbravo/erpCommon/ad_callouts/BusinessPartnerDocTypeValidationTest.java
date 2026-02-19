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
@RunWith(MockitoJUnitRunner.class)
public class BusinessPartnerDocTypeValidationTest {

  private BusinessPartnerDocTypeValidation callout;

  @Mock
  private SimpleCallout.CalloutInfo info;

  @Before
  public void setUp() {
    callout = new BusinessPartnerDocTypeValidation();
  }

  @Test
  public void testExecuteClearsDocTypeWhenSalesTransactionChanged() throws ServletException {
    when(info.getLastFieldChanged()).thenReturn("inpissotrx");

    callout.execute(info);

    verify(info).addResult("inpcDoctypeId", "");
    verify(info).addResult("inpcDoctypeId_R", "");
  }

  @Test
  public void testExecuteClearsDocTypeWhenDocumentCategoryChanged() throws ServletException {
    when(info.getLastFieldChanged()).thenReturn("inpdocumentcategory");

    callout.execute(info);

    verify(info).addResult("inpcDoctypeId", "");
    verify(info).addResult("inpcDoctypeId_R", "");
  }

  @Test
  public void testExecuteClearsDocTypeWhenOrgChanged() throws ServletException {
    when(info.getLastFieldChanged()).thenReturn("inpadOrgId");

    callout.execute(info);

    verify(info).addResult("inpcDoctypeId", "");
    verify(info).addResult("inpcDoctypeId_R", "");
  }

  @Test
  public void testExecuteDoesNotClearDocTypeForOtherFields() throws ServletException {
    when(info.getLastFieldChanged()).thenReturn("inpSomeOtherField");

    callout.execute(info);

    verify(info, never()).addResult("inpcDoctypeId", "");
    verify(info, never()).addResult("inpcDoctypeId_R", "");
  }

  @Test
  public void testExecuteDoesNotClearDocTypeWhenFieldIsNull() throws ServletException {
    when(info.getLastFieldChanged()).thenReturn(null);

    callout.execute(info);

    verify(info, never()).addResult("inpcDoctypeId", "");
    verify(info, never()).addResult("inpcDoctypeId_R", "");
  }
}
