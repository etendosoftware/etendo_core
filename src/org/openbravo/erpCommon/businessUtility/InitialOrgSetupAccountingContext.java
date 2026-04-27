package org.openbravo.erpCommon.businessUtility;

import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationType;

public final class InitialOrgSetupAccountingContext {
  private final Client client;
  private final Organization organization;
  private final OrganizationType organizationType;
  private final String currencyId;
  private final String parentOrgId;
  private final String selectedModules;
  private final boolean createAccountingRequested;
  private final boolean hasUploadedCoAFile;

  public InitialOrgSetupAccountingContext(Client client, Organization organization,
      OrganizationType organizationType, String currencyId, String parentOrgId,
      String selectedModules, boolean createAccountingRequested, boolean hasUploadedCoAFile) {
    this.client = client;
    this.organization = organization;
    this.organizationType = organizationType;
    this.currencyId = currencyId;
    this.parentOrgId = parentOrgId;
    this.selectedModules = selectedModules;
    this.createAccountingRequested = createAccountingRequested;
    this.hasUploadedCoAFile = hasUploadedCoAFile;
  }

  public Client getClient() {
    return client;
  }

  public Organization getOrganization() {
    return organization;
  }

  public OrganizationType getOrganizationType() {
    return organizationType;
  }

  public String getCurrencyId() {
    return currencyId;
  }

  public String getParentOrgId() {
    return parentOrgId;
  }

  public String getSelectedModules() {
    return selectedModules;
  }

  public boolean isCreateAccountingRequested() {
    return createAccountingRequested;
  }

  public boolean hasUploadedCoAFile() {
    return hasUploadedCoAFile;
  }
}
