package com.etendoerp.classic.initialorgsetup;

import java.util.Objects;

import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchema;
import org.openbravo.model.financialmgmt.calendar.Calendar;

final class AccountingPackageCandidate {
  private final Organization sourceOrganization;
  private final AcctSchema ledger;
  private final Calendar calendar;

  AccountingPackageCandidate(Organization sourceOrganization, AcctSchema ledger, Calendar calendar) {
    this.sourceOrganization = Objects.requireNonNull(sourceOrganization);
    this.ledger = Objects.requireNonNull(ledger);
    this.calendar = Objects.requireNonNull(calendar);
  }

  Organization getSourceOrganization() {
    return sourceOrganization;
  }

  AcctSchema getLedger() {
    return ledger;
  }

  Calendar getCalendar() {
    return calendar;
  }
}
