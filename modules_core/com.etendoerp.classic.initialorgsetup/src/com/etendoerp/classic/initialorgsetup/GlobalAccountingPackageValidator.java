package com.etendoerp.classic.initialorgsetup;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationAcctSchema;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchemaDefault;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchemaElement;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchemaGL;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchemaTable;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.model.financialmgmt.calendar.PeriodControl;
import org.openbravo.model.financialmgmt.calendar.Year;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.financialmgmt.tax.TaxRateAccounts;
import org.openbravo.model.financialmgmt.tax.TaxZone;

@ApplicationScoped
class GlobalAccountingPackageValidator {
  private static final String ACCOUNT_ELEMENT_TYPE = "AC";

  Optional<String> validate(AccountingPackageCandidate candidate) {
    final List<String> missingParts = new ArrayList<>();
    final Organization sourceOrganization = candidate.getSourceOrganization();
    final String sourceOrgId = sourceOrganization.getId();
    final String ledgerId = candidate.getLedger().getId();
    final String calendarId = candidate.getCalendar().getId();

    requireCount(missingParts, "organization accounting schema",
        count(OrganizationAcctSchema.class,
            "as e where e.organization.id = :orgId and e.accountingSchema.id = :ledgerId",
            "orgId", sourceOrgId, "ledgerId", ledgerId));
    requireCount(missingParts, "accounting schema defaults",
        count(AcctSchemaDefault.class,
            "as e where e.accountingSchema.id = :ledgerId",
            "ledgerId", ledgerId));
    requireCount(missingParts, "accounting schema elements",
        count(AcctSchemaElement.class,
            "as e where e.accountingSchema.id = :ledgerId",
            "ledgerId", ledgerId));
    requireCount(missingParts, "accounting schema GL entries",
        count(AcctSchemaGL.class,
            "as e where e.accountingSchema.id = :ledgerId",
            "ledgerId", ledgerId));
    requireCount(missingParts, "accounting schema tables",
        count(AcctSchemaTable.class,
            "as e where e.accountingSchema.id = :ledgerId",
            "ledgerId", ledgerId));

    final String accountElementId = findAccountElementId(ledgerId);
    if (accountElementId == null) {
      missingParts.add("accounting element");
    } else {
      requireCount(missingParts, "account element values",
          count(ElementValue.class,
              "as e where e.accountingElement.id = :elementId",
              "elementId", accountElementId));
    }

    requireCount(missingParts, "calendar years",
        count(Year.class,
            "as e where e.calendar.id = :calendarId",
            "calendarId", calendarId));
    requireCount(missingParts, "calendar periods",
        count(Period.class,
            "as e where e.year.calendar.id = :calendarId",
            "calendarId", calendarId));
    requireCount(missingParts, "period controls",
        count(PeriodControl.class,
            "as e where e.organization.id = :orgId and e.period.year.calendar.id = :calendarId",
            "orgId", sourceOrgId, "calendarId", calendarId));
    requireCount(missingParts, "tax rates",
        count(TaxRate.class,
            "as e where e.organization.id = :orgId",
            "orgId", sourceOrgId));
    requireZero(missingParts, "tax accounts",
        countTaxRatesMissingAccounts(sourceOrgId, ledgerId));

    final long taxZoneCount = count(TaxZone.class,
        "as e where e.tax.organization.id = :orgId",
        "orgId", sourceOrgId);
    if (taxZoneCount > 0) {
      final long scopedTaxZoneCount = count(TaxZone.class,
          "as e where e.tax.organization.id = :orgId"
              + " and (e.fromCountry is not null or e.fromRegion is not null"
              + " or e.destinationCountry is not null or e.destinationRegion is not null)",
          "orgId", sourceOrgId);
      if (scopedTaxZoneCount != taxZoneCount) {
        missingParts.add("tax zones");
      }
    }

    if (missingParts.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(String.format(
        "Accounting package from organization '%s' (%s) is incomplete: %s.",
        sourceOrganization.getIdentifier(), sourceOrgId, String.join(", ", missingParts)));
  }

  private String findAccountElementId(String ledgerId) {
    final OBQuery<AcctSchemaElement> query = OBDal.getInstance().createQuery(AcctSchemaElement.class,
        "as e where e.accountingSchema.id = :ledgerId"
            + " and e.type = :type"
            + " and e.accountingElement is not null"
            + " order by e.sequenceNumber asc");
    query.setNamedParameter("ledgerId", ledgerId);
    query.setNamedParameter("type", ACCOUNT_ELEMENT_TYPE);
    query.setFilterOnReadableClients(false);
    query.setFilterOnReadableOrganization(false);
    query.setMaxResult(1);

    final AcctSchemaElement accountElement = query.uniqueResult();
    return accountElement == null ? null : accountElement.getAccountingElement().getId();
  }

  private void requireCount(List<String> missingParts, String label, long count) {
    if (count < 1) {
      missingParts.add(label);
    }
  }

  private void requireZero(List<String> missingParts, String label, long count) {
    if (count > 0) {
      missingParts.add(label);
    }
  }

  private long countTaxRatesMissingAccounts(String sourceOrgId, String ledgerId) {
    return count(TaxRate.class,
        "as e where e.organization.id = :orgId"
            + " and e." + TaxRate.PROPERTY_SUMMARYLEVEL + " = false"
            + " and not exists (select 1 from " + TaxRateAccounts.ENTITY_NAME + " as ta"
            + " where ta." + TaxRateAccounts.PROPERTY_TAX + ".id = e.id"
            + " and ta." + TaxRateAccounts.PROPERTY_ACCOUNTINGSCHEMA + ".id = :ledgerId)",
        "orgId", sourceOrgId, "ledgerId", ledgerId);
  }

  private <T extends org.openbravo.base.structure.BaseOBObject> long count(Class<T> entityClass,
      String whereClause, Object... parameters) {
    final OBQuery<T> query = OBDal.getInstance().createQuery(entityClass, whereClause);
    query.setFilterOnReadableClients(false);
    query.setFilterOnReadableOrganization(false);
    for (int i = 0; i < parameters.length; i += 2) {
      query.setNamedParameter((String) parameters[i], parameters[i + 1]);
    }
    return query.count();
  }
}
