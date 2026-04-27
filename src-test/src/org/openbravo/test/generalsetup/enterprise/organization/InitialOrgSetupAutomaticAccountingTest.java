/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2026 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.test.generalsetup.enterprise.organization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.fileupload.FileItem;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.businessUtility.InitialOrgSetup;
import org.openbravo.erpCommon.businessUtility.InitialOrgSetupAccountingContext;
import org.openbravo.erpCommon.businessUtility.InitialOrgSetupAccountingHandler;
import org.openbravo.erpCommon.businessUtility.InitialOrgSetupAccountingResult;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.system.Language;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationType;

public class InitialOrgSetupAutomaticAccountingTest {

  @Test
  public void legalWithAccountingOrganizationRequiresAccountingHook() throws Exception {
    OrganizationType legalWithAccounting = mock(OrganizationType.class);
    when(legalWithAccounting.isLegalEntityWithAccounting()).thenReturn(true);
    OrganizationType regularOrganization = mock(OrganizationType.class);
    when(regularOrganization.isLegalEntityWithAccounting()).thenReturn(false);

    assertTrue(requiresAccountingHook(legalWithAccounting));
    assertFalse(requiresAccountingHook(regularOrganization));
    assertFalse(requiresAccountingHook(null));
  }

  @Test
  public void successfulAccountingHookReceivesOrganizationContextAndReturnsSuccess() throws Exception {
    InitialOrgSetup initialOrgSetup = newInitialOrgSetup();
    Client client = mock(Client.class);
    Organization organization = mock(Organization.class);
    OrganizationType organizationType = mock(OrganizationType.class);
    FileItem coaFile = mock(FileItem.class);
    when(coaFile.getSize()).thenReturn(0L);
    setField(initialOrgSetup, "client", client);
    setField(initialOrgSetup, "org", organization);

    AtomicReference<InitialOrgSetupAccountingContext> capturedContext = new AtomicReference<>();
    InitialOrgSetupAccountingHandler handler = new InitialOrgSetupAccountingHandler() {
      @Override
      public boolean applies(InitialOrgSetupAccountingContext context) {
        return true;
      }

      @Override
      public InitialOrgSetupAccountingResult wire(InitialOrgSetupAccountingContext context) {
        capturedContext.set(context);
        return InitialOrgSetupAccountingResult.success();
      }
    };
    setField(initialOrgSetup, "accountingHandlers", List.of(handler));

    OBError result = runAccountingHook(initialOrgSetup, organizationType, "parent-org", "modules",
        true, coaFile, "102");

    assertEquals("Success", result.getType());
    assertSame(client, capturedContext.get().getClient());
    assertSame(organization, capturedContext.get().getOrganization());
    assertSame(organizationType, capturedContext.get().getOrganizationType());
    assertEquals("102", capturedContext.get().getCurrencyId());
    assertEquals("parent-org", capturedContext.get().getParentOrgId());
    assertEquals("modules", capturedContext.get().getSelectedModules());
    assertTrue(capturedContext.get().isCreateAccountingRequested());
    assertFalse(capturedContext.get().hasUploadedCoAFile());
  }

  private boolean requiresAccountingHook(OrganizationType organizationType) throws Exception {
    Method method = InitialOrgSetup.class.getDeclaredMethod("requiresAccountingHook",
        OrganizationType.class);
    method.setAccessible(true);
    return (boolean) method.invoke(newInitialOrgSetup(), organizationType);
  }

  private OBError runAccountingHook(InitialOrgSetup initialOrgSetup, OrganizationType organizationType,
      String parentOrgId, String selectedModules, boolean createAccounting, FileItem coaFile,
      String currencyId) throws Exception {
    Method method = InitialOrgSetup.class.getDeclaredMethod("runAccountingHook", OrganizationType.class,
        String.class, String.class, boolean.class, FileItem.class, String.class);
    method.setAccessible(true);
    return (OBError) method.invoke(initialOrgSetup, organizationType, parentOrgId, selectedModules,
        createAccounting, coaFile, currencyId);
  }

  private InitialOrgSetup newInitialOrgSetup() {
    OBContext obContext = mock(OBContext.class);
    when(obContext.getLanguage()).thenReturn(mock(Language.class));
    try (MockedStatic<OBContext> context = mockStatic(OBContext.class)) {
      context.when(OBContext::getOBContext).thenReturn(obContext);
      return new InitialOrgSetup(mock(Client.class));
    }
  }

  private void setField(Object target, String fieldName, Object value) throws Exception {
    Field field = InitialOrgSetup.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
}
