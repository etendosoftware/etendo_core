package com.etendoerp.classic.initialorgsetup;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.openbravo.erpCommon.businessUtility.InitialOrgSetupAccountingContext;
import org.openbravo.erpCommon.businessUtility.InitialOrgSetupAccountingHandler;
import org.openbravo.erpCommon.businessUtility.InitialOrgSetupAccountingResult;

@ApplicationScoped
public class GlobalAccountingInitialOrgSetupHandler implements InitialOrgSetupAccountingHandler {

  @Inject
  private GlobalAccountingPackageResolver packageResolver;

  @Inject
  private GlobalAccountingPackageValidator packageValidator;

  @Inject
  private GlobalAccountingPackageCloner packageCloner;

  @Override
  public boolean applies(InitialOrgSetupAccountingContext context) {
    return context.getOrganizationType() != null
        && context.getOrganizationType().isLegalEntityWithAccounting();
  }

  @Override
  public InitialOrgSetupAccountingResult wire(InitialOrgSetupAccountingContext context) {
    final List<AccountingPackageCandidate> candidates = packageResolver.resolve(
        context.getClient().getId(), context.getCurrencyId());
    if (candidates.isEmpty()) {
      return InitialOrgSetupAccountingResult.error(String.format(
          "No ready legal-with-accounting organization package found for ledger currency '%s'.",
          context.getCurrencyId()));
    }

    final List<String> validationErrors = new ArrayList<>();
    for (AccountingPackageCandidate candidate : candidates) {
      final Optional<String> validationError = packageValidator.validate(candidate);
      if (validationError.isPresent()) {
        validationErrors.add(validationError.get());
        continue;
      }

      try {
        packageCloner.cloneInto(context, candidate);
      } catch (RuntimeException exception) {
        return InitialOrgSetupAccountingResult.error(String.format(
            "Classic accounting package cloning failed for source organization '%s' (%s): %s",
            candidate.getSourceOrganization().getIdentifier(),
            candidate.getSourceOrganization().getId(), exception.getMessage()));
      }
      return InitialOrgSetupAccountingResult.success();
    }

    return InitialOrgSetupAccountingResult.error(String.join(" ", validationErrors));
  }
}
