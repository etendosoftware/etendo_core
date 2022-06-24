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
 * All portions are Copyright (C) 2013-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.erpCommon;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.CustomerAccounts;
import org.openbravo.model.common.businesspartner.VendorAccounts;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.accounting.AccountingFact;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchema;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;

/**
 * This class provides methods to retrieve the Initial Balance of a customer for a given date and
 * Accounting Schema
 */
public class ReportsUtility {
  private ReportsUtility() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Return the Initial Balance of a Customer for the given date, Organization and Accounting Schema
   * 
   * @param orgId
   *          Id of the Organization for which the Initial Balance would be retrieved
   * @param acctSchemaId
   *          Id of an Accounting Schema for which the Initial Balance would be retrieved
   * @param bpartnerId
   *          If of the Business Parter for which the Initial Balance would be retrieved
   * @param dateFrom
   *          Starting Date from which the Initial Balance should be calculated
   * @return A BigDecimal that represents the Initial balance at that date, for the Business Partner
   */
  public static BigDecimal getBeginningBalance(final String orgId, final String acctSchemaId,
      final String bpartnerId, final String dateFrom) {
    return getBeginningBalance(orgId, acctSchemaId, bpartnerId, dateFrom, true, null);
  }

  /**
   * Return the Initial Balance of a Customer or Vendor for the given date, Organization and
   * Accounting Schema
   * 
   * @param orgId
   *          Id of the Organization for which the Initial Balance would be retrieved
   * @param acctSchemaId
   *          Id of an Accounting Schema for which the Initial Balance would be retrieved
   * @param bpartnerId
   *          If of the Business Parter for which the Initial Balance would be retrieved
   * @param dateFrom
   *          Starting Date from which the Initial Balance should be calculated
   * @param isCustomer
   *          If true, the Business Partner is considered a Customer, if not, it is considered a
   *          Vendor
   * @return A BigDecimal that represents the Initial balance at that date, for the Business Partner
   */
  public static BigDecimal getBeginningBalance(final String orgId, final String acctSchemaId,
      final String bpartnerId, String dateFrom, boolean isCustomer) {
    return getBeginningBalance(orgId, acctSchemaId, bpartnerId, dateFrom, isCustomer, null);
  }

  /**
   * Return the Initial Balance of a Customer or Vendor for the given date, Organization, Accounting
   * Schema and Currency
   * 
   * @param orgId
   *          Id of the Organization for which the Initial Balance would be retrieved
   * @param acctSchemaId
   *          Id of an Accounting Schema for which the Initial Balance would be retrieved
   * @param bpartnerId
   *          If of the Business Parter for which the Initial Balance would be retrieved
   * @param dateFrom
   *          Starting Date from which the Initial Balance should be calculated
   * @param isCustomer
   *          If true, the Business Partner is considered a Customer, if not, it is considered a
   *          Vendor
   * @param currency
   *          The Currency for which the Initial Balance will be calculated
   * @return A BigDecimal that represents the Initial balance at that date, for the Business Partner
   */
  public static BigDecimal getBeginningBalance(final String orgId, final String acctSchemaId,
      final String bpartnerId, final String dateFrom, final boolean isCustomer,
      final String currency) {
    if (dateFrom == null || "".equals(dateFrom)) {
      return BigDecimal.ZERO;
    }

    final OBCriteria<AccountingFact> initialBalanceQuery = OBDal.getInstance()
        .createCriteria(AccountingFact.class);
    initialBalanceQuery.add(Restrictions.eq(AccountingFact.PROPERTY_ACCOUNTINGSCHEMA,
        OBDal.getInstance().get(AcctSchema.class, acctSchemaId)));
    initialBalanceQuery.add(Restrictions.eq(AccountingFact.PROPERTY_BUSINESSPARTNER,
        OBDal.getInstance().get(BusinessPartner.class, bpartnerId)));
    initialBalanceQuery
        .add(Restrictions.in(AccountingFact.PROPERTY_ORGANIZATION, getOrgList(orgId)));
    try {
      initialBalanceQuery.add(
          Restrictions.lt(AccountingFact.PROPERTY_ACCOUNTINGDATE, OBDateUtils.getDate(dateFrom)));
    } catch (ParseException pe) {
      // do nothing
    }
    if (currency != null) {
      initialBalanceQuery.add(Restrictions.eq(AccountingFact.PROPERTY_CURRENCY,
          OBDal.getInstance().getProxy(Currency.class, currency)));
    }

    final List<ElementValue> validAccountsList = getValidAccounts(acctSchemaId, bpartnerId,
        isCustomer);
    if (!validAccountsList.isEmpty()) {
      initialBalanceQuery.add(Restrictions.in(AccountingFact.PROPERTY_ACCOUNT, validAccountsList));
    }

    initialBalanceQuery.setFilterOnReadableOrganization(false);

    final ProjectionList projections = Projections.projectionList();
    projections.add(Projections.sum(currency == null ? AccountingFact.PROPERTY_DEBIT
        : AccountingFact.PROPERTY_FOREIGNCURRENCYDEBIT));
    projections.add(Projections.sum(currency == null ? AccountingFact.PROPERTY_CREDIT
        : AccountingFact.PROPERTY_FOREIGNCURRENCYCREDIT));
    initialBalanceQuery.setProjection(projections);

    return getBalanceFromQuery(initialBalanceQuery);

  }

  private static List<ElementValue> getValidAccounts(final String acctSchemaId,
      final String bpartnerId, final boolean isCustomer) {
    if (isCustomer) {
      return getValidAccountsListCustomer(acctSchemaId, bpartnerId);
    } else {
      return getValidAccountsListVendor(acctSchemaId, bpartnerId);
    }
  }

  private static List<ElementValue> getValidAccountsListCustomer(final String acctSchemaId,
      final String bpartnerId) {
    final List<ElementValue> result = new ArrayList<ElementValue>();
    final OBCriteria<CustomerAccounts> obc = OBDal.getInstance()
        .createCriteria(CustomerAccounts.class);
    obc.add(Restrictions.eq(CustomerAccounts.PROPERTY_BUSINESSPARTNER,
        OBDal.getInstance().get(BusinessPartner.class, bpartnerId)));
    obc.add(Restrictions.eq(AccountingFact.PROPERTY_ACCOUNTINGSCHEMA,
        OBDal.getInstance().get(AcctSchema.class, acctSchemaId)));
    obc.setFilterOnReadableOrganization(false);
    obc.setFilterOnActive(false);
    for (final CustomerAccounts ca : obc.list()) {
      if (ca.getCustomerReceivablesNo() != null) {
        result.add(ca.getCustomerReceivablesNo().getAccount());
      }
      if (ca.getCustomerPrepayment() != null) {
        result.add(ca.getCustomerPrepayment().getAccount());
      }
    }
    return result;
  }

  private static List<ElementValue> getValidAccountsListVendor(final String acctSchemaId,
      final String bpartnerId) {
    final List<ElementValue> result = new ArrayList<ElementValue>();
    final OBCriteria<VendorAccounts> obc = OBDal.getInstance().createCriteria(VendorAccounts.class);
    obc.add(Restrictions.eq(VendorAccounts.PROPERTY_BUSINESSPARTNER,
        OBDal.getInstance().get(BusinessPartner.class, bpartnerId)));
    obc.add(Restrictions.eq(VendorAccounts.PROPERTY_ACCOUNTINGSCHEMA,
        OBDal.getInstance().get(AcctSchema.class, acctSchemaId)));
    obc.setFilterOnReadableOrganization(false);
    obc.setFilterOnActive(false);
    for (final VendorAccounts va : obc.list()) {
      if (va.getVendorLiability() != null) {
        result.add(va.getVendorLiability().getAccount());
      }
      if (va.getVendorPrepayment() != null) {
        result.add(va.getVendorPrepayment().getAccount());
      }
    }
    return result;
  }

  private static BigDecimal getBalanceFromQuery(
      final OBCriteria<AccountingFact> initialBalanceQuery) {
    @SuppressWarnings("rawtypes")
    final List resultList = initialBalanceQuery.list();
    if (resultList != null && !resultList.isEmpty()) {
      final Object[] resultSet = (Object[]) resultList.get(0);
      final BigDecimal debit = (resultSet[0] != null) ? (BigDecimal) resultSet[0] : BigDecimal.ZERO;
      final BigDecimal credit = (resultSet[1] != null) ? (BigDecimal) resultSet[1]
          : BigDecimal.ZERO;
      return debit.subtract(credit);
    }
    return BigDecimal.ZERO;
  }

  private static List<Organization> getOrgList(final String orgId) {
    final List<Organization> orgList = new ArrayList<Organization>();
    for (final String org : new OrganizationStructureProvider().getChildTree(orgId, true)) {
      orgList.add(OBDal.getInstance().get(Organization.class, org));
    }
    return orgList;
  }

}
