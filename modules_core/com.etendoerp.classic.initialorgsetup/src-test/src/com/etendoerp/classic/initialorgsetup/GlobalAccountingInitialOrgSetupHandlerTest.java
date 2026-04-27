package com.etendoerp.classic.initialorgsetup;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.openbravo.erpCommon.businessUtility.InitialOrgSetupAccountingContext;
import org.openbravo.erpCommon.businessUtility.InitialOrgSetupAccountingResult;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationType;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchema;
import org.openbravo.model.financialmgmt.calendar.Calendar;

public class GlobalAccountingInitialOrgSetupHandlerTest {
  private GlobalAccountingInitialOrgSetupHandler handler;
  private GlobalAccountingPackageResolver packageResolver;
  private GlobalAccountingPackageValidator packageValidator;
  private GlobalAccountingPackageCloner packageCloner;
  private InitialOrgSetupAccountingContext context;
  private Client client;

  @Before
  public void setUp() throws Exception {
    handler = new GlobalAccountingInitialOrgSetupHandler();
    packageResolver = mock(GlobalAccountingPackageResolver.class);
    packageValidator = mock(GlobalAccountingPackageValidator.class);
    packageCloner = mock(GlobalAccountingPackageCloner.class);
    inject("packageResolver", packageResolver);
    inject("packageValidator", packageValidator);
    inject("packageCloner", packageCloner);

    client = mock(Client.class);
    when(client.getId()).thenReturn("client-id");
    context = new InitialOrgSetupAccountingContext(client, mock(Organization.class),
        mock(OrganizationType.class), "102", "parent-org", "", true, false);
  }

  @Test
  public void appliesOnlyToLegalWithAccountingOrganizations() {
    OrganizationType legalWithAccounting = mock(OrganizationType.class);
    when(legalWithAccounting.isLegalEntityWithAccounting()).thenReturn(true);
    OrganizationType regularOrganization = mock(OrganizationType.class);
    when(regularOrganization.isLegalEntityWithAccounting()).thenReturn(false);

    assertTrue(handler.applies(new InitialOrgSetupAccountingContext(client, mock(Organization.class),
        legalWithAccounting, "102", "parent-org", "", true, false)));
    assertFalse(handler.applies(new InitialOrgSetupAccountingContext(client, mock(Organization.class),
        regularOrganization, "102", "parent-org", "", true, false)));
  }

  @Test
  public void returnsErrorWhenNoReadyAccountingPackageExists() {
    when(packageResolver.resolve("client-id", "102")).thenReturn(List.of());

    InitialOrgSetupAccountingResult result = handler.wire(context);

    assertTrue(result.isHandled());
    assertFalse(result.isSuccess());
    assertTrue(result.getMessage().contains("No ready legal-with-accounting organization package"));
    verifyNoInteractions(packageValidator, packageCloner);
  }

  @Test
  public void clonesFirstValidAccountingPackage() {
    AccountingPackageCandidate invalid = candidate("invalid-org");
    AccountingPackageCandidate valid = candidate("valid-org");
    when(packageResolver.resolve("client-id", "102")).thenReturn(List.of(invalid, valid));
    when(packageValidator.validate(invalid)).thenReturn(Optional.of("invalid package"));
    when(packageValidator.validate(valid)).thenReturn(Optional.empty());

    InitialOrgSetupAccountingResult result = handler.wire(context);

    assertTrue(result.isHandled());
    assertTrue(result.isSuccess());
    verify(packageCloner).cloneInto(context, valid);
  }

  @Test
  public void surfacesCloneFailuresAsHandledErrors() {
    AccountingPackageCandidate candidate = candidate("source-org");
    when(packageResolver.resolve("client-id", "102")).thenReturn(List.of(candidate));
    when(packageValidator.validate(candidate)).thenReturn(Optional.empty());
    doThrow(new RuntimeException("copy failed")).when(packageCloner).cloneInto(context, candidate);

    InitialOrgSetupAccountingResult result = handler.wire(context);

    assertTrue(result.isHandled());
    assertFalse(result.isSuccess());
    assertTrue(result.getMessage().contains("copy failed"));
    assertTrue(result.getMessage().contains("source-org"));
  }

  private AccountingPackageCandidate candidate(String sourceOrgId) {
    Organization sourceOrganization = mock(Organization.class);
    when(sourceOrganization.getId()).thenReturn(sourceOrgId);
    when(sourceOrganization.getIdentifier()).thenReturn(sourceOrgId);
    return new AccountingPackageCandidate(sourceOrganization, mock(AcctSchema.class),
        mock(Calendar.class));
  }

  private void inject(String fieldName, Object value) throws Exception {
    Field field = GlobalAccountingInitialOrgSetupHandler.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(handler, value);
  }
}
