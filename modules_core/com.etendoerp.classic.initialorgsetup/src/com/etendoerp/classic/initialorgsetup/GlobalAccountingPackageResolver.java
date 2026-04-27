package com.etendoerp.classic.initialorgsetup;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchema;
import org.openbravo.model.financialmgmt.calendar.Calendar;

@ApplicationScoped
class GlobalAccountingPackageResolver {

  List<AccountingPackageCandidate> resolve(String clientId, String currencyId) {
    final String whereClause = "as org where org.client.id = :clientId"
        + " and org.organizationType.legalEntityWithAccounting = true"
        + " and org.ready = true"
        + " and org.allowPeriodControl = true"
        + " and org.generalLedger is not null"
        + " and org.calendar is not null"
        + " and org.generalLedger.currency.id = :currencyId"
        + " order by org.name asc";

    final OBQuery<Organization> query = OBDal.getInstance().createQuery(Organization.class, whereClause);
    query.setNamedParameter("clientId", clientId);
    query.setNamedParameter("currencyId", currencyId);
    query.setFilterOnReadableClients(false);
    query.setFilterOnReadableOrganization(false);

    return query.list().stream()
        .map(this::toCandidate)
        .collect(Collectors.toList());
  }

  private AccountingPackageCandidate toCandidate(Organization sourceOrganization) {
    final AcctSchema ledger = sourceOrganization.getGeneralLedger();
    final Calendar calendar = sourceOrganization.getCalendar();
    return new AccountingPackageCandidate(sourceOrganization, ledger, calendar);
  }
}
