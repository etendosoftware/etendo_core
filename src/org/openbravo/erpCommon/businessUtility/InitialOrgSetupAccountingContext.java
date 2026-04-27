/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language governing rights and limitations under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2026 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.businessUtility;

import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationType;

/**
 * Immutable data object passed from Initial Organization Setup to accounting setup handlers.
 * It carries the organization being created and the user-selected accounting options.
 */
public final class InitialOrgSetupAccountingContext {
  private final Client client;
  private final Organization organization;
  private final OrganizationType organizationType;
  private final String currencyId;
  private final String parentOrgId;
  private final String selectedModules;
  private final boolean createAccountingRequested;
  private final boolean hasUploadedCoAFile;

  private InitialOrgSetupAccountingContext(Builder builder) {
    this.client = builder.client;
    this.organization = builder.organization;
    this.organizationType = builder.organizationType;
    this.currencyId = builder.currencyId;
    this.parentOrgId = builder.parentOrgId;
    this.selectedModules = builder.selectedModules;
    this.createAccountingRequested = builder.createAccountingRequested;
    this.hasUploadedCoAFile = builder.hasUploadedCoAFile;
  }

  /**
   * Creates a builder for an accounting setup context.
   *
   * @return a new context builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * @return client that owns the organization being created
   */
  public Client getClient() {
    return client;
  }

  /**
   * @return organization being initialized
   */
  public Organization getOrganization() {
    return organization;
  }

  /**
   * @return organization type selected for the new organization
   */
  public OrganizationType getOrganizationType() {
    return organizationType;
  }

  /**
   * @return accounting currency identifier selected by the user
   */
  public String getCurrencyId() {
    return currencyId;
  }

  /**
   * @return parent organization identifier selected in the setup form
   */
  public String getParentOrgId() {
    return parentOrgId;
  }

  /**
   * @return reference data modules selected in the setup form
   */
  public String getSelectedModules() {
    return selectedModules;
  }

  /**
   * @return whether the user requested accounting setup
   */
  public boolean isCreateAccountingRequested() {
    return createAccountingRequested;
  }

  /**
   * @return whether the user uploaded a chart-of-accounts file
   */
  public boolean hasUploadedCoAFile() {
    return hasUploadedCoAFile;
  }

  /**
   * Builder used to avoid long constructor signatures as the setup context evolves.
   */
  public static final class Builder {
    private Client client;
    private Organization organization;
    private OrganizationType organizationType;
    private String currencyId;
    private String parentOrgId;
    private String selectedModules;
    private boolean createAccountingRequested;
    private boolean hasUploadedCoAFile;

    private Builder() {
    }

    /**
     * Sets the client that owns the organization being initialized.
     *
     * @param client client that owns the organization
     * @return this builder
     */
    public Builder client(Client client) {
      this.client = client;
      return this;
    }

    /**
     * Sets the organization being initialized.
     *
     * @param organization organization being initialized
     * @return this builder
     */
    public Builder organization(Organization organization) {
      this.organization = organization;
      return this;
    }

    /**
     * Sets the organization type selected for the new organization.
     *
     * @param organizationType selected organization type
     * @return this builder
     */
    public Builder organizationType(OrganizationType organizationType) {
      this.organizationType = organizationType;
      return this;
    }

    /**
     * Sets the accounting currency identifier selected by the user.
     *
     * @param currencyId accounting currency identifier
     * @return this builder
     */
    public Builder currencyId(String currencyId) {
      this.currencyId = currencyId;
      return this;
    }

    /**
     * Sets the parent organization identifier selected in the setup form.
     *
     * @param parentOrgId parent organization identifier
     * @return this builder
     */
    public Builder parentOrgId(String parentOrgId) {
      this.parentOrgId = parentOrgId;
      return this;
    }

    /**
     * Sets the reference data modules selected in the setup form.
     *
     * @param selectedModules selected reference data modules
     * @return this builder
     */
    public Builder selectedModules(String selectedModules) {
      this.selectedModules = selectedModules;
      return this;
    }

    /**
     * Sets whether the user requested accounting setup.
     *
     * @param createAccountingRequested true when accounting setup was requested
     * @return this builder
     */
    public Builder createAccountingRequested(boolean createAccountingRequested) {
      this.createAccountingRequested = createAccountingRequested;
      return this;
    }

    /**
     * Sets whether the user uploaded a chart-of-accounts file.
     *
     * @param hasUploadedCoAFile true when a CoA file was uploaded
     * @return this builder
     */
    public Builder hasUploadedCoAFile(boolean hasUploadedCoAFile) {
      this.hasUploadedCoAFile = hasUploadedCoAFile;
      return this;
    }

    /**
     * Builds the immutable accounting setup context.
     *
     * @return accounting setup context
     */
    public InitialOrgSetupAccountingContext build() {
      return new InitialOrgSetupAccountingContext(this);
    }
  }
}
