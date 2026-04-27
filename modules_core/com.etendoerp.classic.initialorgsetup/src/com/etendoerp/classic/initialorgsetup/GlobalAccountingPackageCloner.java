package com.etendoerp.classic.initialorgsetup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.businessUtility.InitialOrgSetupAccountingContext;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationAcctSchema;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchema;
import org.openbravo.model.financialmgmt.tax.TaxCategory;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.financialmgmt.tax.TaxRateAccounts;
import org.openbravo.model.financialmgmt.tax.TaxZone;

@ApplicationScoped
class GlobalAccountingPackageCloner {
  private static final String ZERO_ORG_ID = "0";

  void cloneInto(InitialOrgSetupAccountingContext context, AccountingPackageCandidate candidate) {
    final Organization targetOrganization = context.getOrganization();
    final AcctSchema targetLedger = candidate.getLedger();

    wireOrganization(targetOrganization, targetLedger, candidate);
    ensureOrganizationAcctSchema(targetOrganization, targetLedger);

    final Map<String, TaxCategory> taxCategoriesBySourceId = cloneTaxCategories(candidate,
        targetOrganization);
    final Map<String, org.openbravo.model.common.businesspartner.TaxCategory> bpTaxCategoriesBySourceId =
        cloneBusinessPartnerTaxCategories(candidate, targetOrganization);
    final Map<String, TaxRate> taxesBySourceId = cloneTaxes(candidate, targetOrganization,
        taxCategoriesBySourceId, bpTaxCategoriesBySourceId);

    restoreTaxRelationships(candidate, taxesBySourceId);
    cloneTaxZones(candidate, targetOrganization, taxesBySourceId);
    cloneTaxAccounts(candidate, targetOrganization, targetLedger, taxesBySourceId);

    OBDal.getInstance().flush();
  }

  private void wireOrganization(Organization targetOrganization, AcctSchema targetLedger,
      AccountingPackageCandidate candidate) {
    targetOrganization.setCurrency(targetLedger.getCurrency());
    targetOrganization.setGeneralLedger(targetLedger);
    targetOrganization.setCalendar(candidate.getCalendar());
    targetOrganization.setAllowPeriodControl(true);
    OBDal.getInstance().save(targetOrganization);
  }

  private void ensureOrganizationAcctSchema(Organization targetOrganization, AcctSchema targetLedger) {
    final OrganizationAcctSchema existing = uniqueResult(OrganizationAcctSchema.class,
        "as e where e.organization.id = :orgId and e.accountingSchema.id = :ledgerId",
        "orgId", targetOrganization.getId(), "ledgerId", targetLedger.getId());
    if (existing != null) {
      return;
    }

    final OrganizationAcctSchema organizationAcctSchema = OBProvider.getInstance()
        .get(OrganizationAcctSchema.class);
    organizationAcctSchema.setClient(targetOrganization.getClient());
    organizationAcctSchema.setOrganization(targetOrganization);
    organizationAcctSchema.setAccountingSchema(targetLedger);
    OBDal.getInstance().save(organizationAcctSchema);
  }

  private Map<String, TaxCategory> cloneTaxCategories(AccountingPackageCandidate candidate,
      Organization targetOrganization) {
    final Map<String, TaxCategory> clonesBySourceId = new HashMap<>();
    final List<TaxCategory> sourceTaxCategories = list(TaxCategory.class,
        "as e where e.organization.id = :orgId order by e.name asc, e.id asc",
        "orgId", candidate.getSourceOrganization().getId());

    for (TaxCategory sourceTaxCategory : sourceTaxCategories) {
      final TaxCategory clonedTaxCategory = (TaxCategory) DalUtil.copy(sourceTaxCategory, false);
      clonedTaxCategory.setClient(targetOrganization.getClient());
      clonedTaxCategory.setOrganization(targetOrganization);
      OBDal.getInstance().save(clonedTaxCategory);
      clonesBySourceId.put(sourceTaxCategory.getId(), clonedTaxCategory);
    }

    return clonesBySourceId;
  }

  private Map<String, org.openbravo.model.common.businesspartner.TaxCategory> cloneBusinessPartnerTaxCategories(
      AccountingPackageCandidate candidate, Organization targetOrganization) {
    final Map<String, org.openbravo.model.common.businesspartner.TaxCategory> clonesBySourceId =
        new HashMap<>();
    final List<org.openbravo.model.common.businesspartner.TaxCategory> sourceTaxCategories = list(
        org.openbravo.model.common.businesspartner.TaxCategory.class,
        "as e where e.organization.id = :orgId order by e.name asc, e.id asc",
        "orgId", candidate.getSourceOrganization().getId());

    for (org.openbravo.model.common.businesspartner.TaxCategory sourceTaxCategory : sourceTaxCategories) {
      final org.openbravo.model.common.businesspartner.TaxCategory clonedTaxCategory =
          (org.openbravo.model.common.businesspartner.TaxCategory) DalUtil.copy(sourceTaxCategory,
              false);
      clonedTaxCategory.setClient(targetOrganization.getClient());
      clonedTaxCategory.setOrganization(targetOrganization);
      OBDal.getInstance().save(clonedTaxCategory);
      clonesBySourceId.put(sourceTaxCategory.getId(), clonedTaxCategory);
    }

    return clonesBySourceId;
  }

  private Map<String, TaxRate> cloneTaxes(AccountingPackageCandidate candidate,
      Organization targetOrganization, Map<String, TaxCategory> taxCategoriesBySourceId,
      Map<String, org.openbravo.model.common.businesspartner.TaxCategory> bpTaxCategoriesBySourceId) {
    final Map<String, TaxRate> clonesBySourceId = new HashMap<>();
    final List<TaxRate> sourceTaxes = list(TaxRate.class,
        "as e where e.organization.id = :orgId order by e.lineNo asc, e.name asc, e.id asc",
        "orgId", candidate.getSourceOrganization().getId());

    for (TaxRate sourceTax : sourceTaxes) {
      final TaxRate clonedTax = (TaxRate) DalUtil.copy(sourceTax, false);
      clonedTax.setClient(targetOrganization.getClient());
      clonedTax.setOrganization(targetOrganization);
      clonedTax.setTaxCategory(taxCategoriesBySourceId.get(sourceTax.getTaxCategory().getId()));
      clonedTax.setBusinessPartnerTaxCategory(sourceTax.getBusinessPartnerTaxCategory() == null ? null
          : bpTaxCategoriesBySourceId.get(sourceTax.getBusinessPartnerTaxCategory().getId()));
      clonedTax.setParentTaxRate(null);
      clonedTax.setTaxBase(null);
      OBDal.getInstance().save(clonedTax);
      clonesBySourceId.put(sourceTax.getId(), clonedTax);
    }

    return clonesBySourceId;
  }

  private void restoreTaxRelationships(AccountingPackageCandidate candidate,
      Map<String, TaxRate> taxesBySourceId) {
    final List<TaxRate> sourceTaxes = list(TaxRate.class,
        "as e where e.organization.id = :orgId order by e.id asc",
        "orgId", candidate.getSourceOrganization().getId());

    for (TaxRate sourceTax : sourceTaxes) {
      final TaxRate clonedTax = taxesBySourceId.get(sourceTax.getId());
      if (clonedTax == null) {
        continue;
      }
      if (sourceTax.getParentTaxRate() != null) {
        clonedTax.setParentTaxRate(taxesBySourceId.get(sourceTax.getParentTaxRate().getId()));
      }
      if (sourceTax.getTaxBase() != null) {
        clonedTax.setTaxBase(taxesBySourceId.get(sourceTax.getTaxBase().getId()));
      }
      OBDal.getInstance().save(clonedTax);
    }
  }

  private void cloneTaxZones(AccountingPackageCandidate candidate, Organization targetOrganization,
      Map<String, TaxRate> taxesBySourceId) {
    final List<TaxZone> sourceTaxZones = list(TaxZone.class,
        "as e where e.tax.organization.id = :orgId order by e.id asc",
        "orgId", candidate.getSourceOrganization().getId());

    for (TaxZone sourceTaxZone : sourceTaxZones) {
      final TaxRate clonedTax = taxesBySourceId.get(sourceTaxZone.getTax().getId());
      if (clonedTax == null) {
        continue;
      }

      final TaxZone clonedTaxZone = (TaxZone) DalUtil.copy(sourceTaxZone, false);
      clonedTaxZone.setClient(targetOrganization.getClient());
      clonedTaxZone.setOrganization(targetOrganization);
      clonedTaxZone.setTax(clonedTax);
      OBDal.getInstance().save(clonedTaxZone);
    }
  }

  private void cloneTaxAccounts(AccountingPackageCandidate candidate, Organization targetOrganization,
      AcctSchema targetLedger, Map<String, TaxRate> taxesBySourceId) {
    final Map<String, AccountingCombination> combinationsBySourceId = new HashMap<>();
    final List<TaxRateAccounts> sourceTaxAccounts = list(TaxRateAccounts.class,
        "as e where e.tax.organization.id = :orgId and e.accountingSchema.id = :ledgerId"
            + " order by e.tax.id asc, e.id asc",
        "orgId", candidate.getSourceOrganization().getId(), "ledgerId", candidate.getLedger().getId());

    for (TaxRateAccounts sourceTaxAccount : sourceTaxAccounts) {
      final TaxRate clonedTax = taxesBySourceId.get(sourceTaxAccount.getTax().getId());
      if (clonedTax == null) {
        continue;
      }

      final TaxRateAccounts clonedTaxAccount = (TaxRateAccounts) DalUtil.copy(sourceTaxAccount, false);
      clonedTaxAccount.setClient(targetOrganization.getClient());
      clonedTaxAccount.setOrganization(targetOrganization);
      clonedTaxAccount.setTax(clonedTax);
      clonedTaxAccount.setAccountingSchema(targetLedger);
      clonedTaxAccount.setTaxDue(cloneDerivedCombination(sourceTaxAccount.getTaxDue(),
          targetOrganization, targetLedger, combinationsBySourceId));
      clonedTaxAccount.setTaxLiability(cloneDerivedCombination(sourceTaxAccount.getTaxLiability(),
          targetOrganization, targetLedger, combinationsBySourceId));
      clonedTaxAccount.setTaxCredit(cloneDerivedCombination(sourceTaxAccount.getTaxCredit(),
          targetOrganization, targetLedger, combinationsBySourceId));
      clonedTaxAccount.setTaxReceivables(cloneDerivedCombination(
          sourceTaxAccount.getTaxReceivables(), targetOrganization, targetLedger,
          combinationsBySourceId));
      clonedTaxAccount.setTaxExpense(cloneDerivedCombination(sourceTaxAccount.getTaxExpense(),
          targetOrganization, targetLedger, combinationsBySourceId));
      clonedTaxAccount.setTaxDueTransitory(cloneDerivedCombination(
          sourceTaxAccount.getTaxDueTransitory(), targetOrganization, targetLedger,
          combinationsBySourceId));
      clonedTaxAccount.setTaxCreditTransitory(cloneDerivedCombination(
          sourceTaxAccount.getTaxCreditTransitory(), targetOrganization, targetLedger,
          combinationsBySourceId));
      OBDal.getInstance().save(clonedTaxAccount);
    }
  }

  private AccountingCombination cloneDerivedCombination(AccountingCombination sourceCombination,
      Organization targetOrganization, AcctSchema targetLedger,
      Map<String, AccountingCombination> combinationsBySourceId) {
    if (sourceCombination == null) {
      return null;
    }

    AccountingCombination clonedCombination = combinationsBySourceId.get(sourceCombination.getId());
    if (clonedCombination != null) {
      return clonedCombination;
    }

    clonedCombination = (AccountingCombination) DalUtil.copy(sourceCombination, false);
    clonedCombination.setClient(targetOrganization.getClient());
    clonedCombination.setOrganization(targetOrganization);
    clonedCombination.setAccountingSchema(targetLedger);

    final boolean strippedDimensions = sanitizeCombinationDimensions(clonedCombination);
    if (strippedDimensions) {
      // The textual representation becomes stale once org-scoped dimensions are stripped.
      clonedCombination.setAlias(null);
      clonedCombination.setCombination(null);
      clonedCombination.setDescription(null);
      clonedCombination.setFullyQualified(false);
    }

    OBDal.getInstance().save(clonedCombination);
    combinationsBySourceId.put(sourceCombination.getId(), clonedCombination);
    return clonedCombination;
  }

  private boolean sanitizeCombinationDimensions(AccountingCombination combination) {
    boolean stripped = false;

    if (combination.getTrxOrganization() != null) {
      combination.setTrxOrganization(null);
      stripped = true;
    }
    if (combination.getLocationFromAddress() != null) {
      combination.setLocationFromAddress(null);
      stripped = true;
    }
    if (combination.getLocationToAddress() != null) {
      combination.setLocationToAddress(null);
      stripped = true;
    }
    if (isUnsafeOrgScopedReference(combination.getProduct())) {
      combination.setProduct(null);
      stripped = true;
    }
    if (isUnsafeOrgScopedReference(combination.getBusinessPartner())) {
      combination.setBusinessPartner(null);
      stripped = true;
    }
    if (isUnsafeOrgScopedReference(combination.getSalesRegion())) {
      combination.setSalesRegion(null);
      stripped = true;
    }
    if (isUnsafeOrgScopedReference(combination.getProject())) {
      combination.setProject(null);
      stripped = true;
    }
    if (isUnsafeOrgScopedReference(combination.getSalesCampaign())) {
      combination.setSalesCampaign(null);
      stripped = true;
    }
    if (isUnsafeOrgScopedReference(combination.getActivity())) {
      combination.setActivity(null);
      stripped = true;
    }
    if (isUnsafeOrgScopedReference(combination.getStDimension())) {
      combination.setStDimension(null);
      stripped = true;
    }
    if (isUnsafeOrgScopedReference(combination.getNdDimension())) {
      combination.setNdDimension(null);
      stripped = true;
    }

    return stripped;
  }

  private boolean isUnsafeOrgScopedReference(BaseOBObject reference) {
    if (reference == null || !reference.getEntity().hasProperty("organization")) {
      return false;
    }

    final Object organizationValue = reference.get("organization");
    if (!(organizationValue instanceof Organization)) {
      return false;
    }

    final Organization referencedOrganization = (Organization) organizationValue;
    return referencedOrganization != null && !ZERO_ORG_ID.equals(referencedOrganization.getId());
  }

  private <T extends BaseOBObject> List<T> list(Class<T> entityClass, String whereClause,
      Object... parameters) {
    final OBQuery<T> query = query(entityClass, whereClause, parameters);
    return query.list();
  }

  private <T extends BaseOBObject> T uniqueResult(Class<T> entityClass, String whereClause,
      Object... parameters) {
    final OBQuery<T> query = query(entityClass, whereClause, parameters);
    query.setMaxResult(1);
    return query.uniqueResult();
  }

  private <T extends BaseOBObject> OBQuery<T> query(Class<T> entityClass, String whereClause,
      Object... parameters) {
    final OBQuery<T> query = OBDal.getInstance().createQuery(entityClass, whereClause);
    query.setFilterOnReadableClients(false);
    query.setFilterOnReadableOrganization(false);
    for (int i = 0; i < parameters.length; i += 2) {
      query.setNamedParameter((String) parameters[i], parameters[i + 1]);
    }
    return query;
  }
}
