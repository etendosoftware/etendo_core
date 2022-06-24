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
 * All portions are Copyright (C) 2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.test.generalsetup.enterprise.organization;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.InitialOrgSetup;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationAcctSchema;
import org.openbravo.model.common.enterprise.OrganizationType;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchema;
import org.openbravo.model.financialmgmt.calendar.Calendar;
import org.openbravo.service.db.CallProcess;
import org.openbravo.service.db.CallStoredProcedure;

class ADOrgPersistInfoUtility {

  static void setTestContextFB() {
    OBContext.setOBContext(ADOrgPersistInfoConstants.OPENBRAVO_USER_ID,
        ADOrgPersistInfoConstants.FB_GROUP_ADMIN_ROLE_ID, ADOrgPersistInfoConstants.CLIENT_FB,
        ADOrgPersistInfoConstants.ORG_FB_FBGROUP);
  }

  static void setTestContextQA() {
    OBContext.setOBContext(ADOrgPersistInfoConstants.OPENBRAVO_USER_ID,
        ADOrgPersistInfoConstants.QA_TESTING_ADMIN_ROLE_ID, ADOrgPersistInfoConstants.CLIENT_QA,
        ADOrgPersistInfoConstants.ORG_QA_SPAIN);
  }

  /**
   * Create organization with type orgType under strParentOrg
   */
  static String createOrganization(String newOrgType, String strParentOrg, boolean summary,
      String currencyId) {
    long number = System.currentTimeMillis();
    InitialOrgSetup initialOrg = new InitialOrgSetup(OBContext.getOBContext().getCurrentClient());
    initialOrg.createOrganization("Test_" + number, "", newOrgType, strParentOrg, "", "", "", false,
        null, "", false, false, false, false, false);
    Organization org = OBDal.getInstance().get(Organization.class, initialOrg.getOrgId());
    org.setSummaryLevel(summary);
    if (StringUtils.equals(newOrgType, ADOrgPersistInfoConstants.ORGTYPE_LEGALWITHACCOUNTING)) {
      org.setCurrency(OBDal.getInstance().get(Currency.class, currencyId));
      org.setAllowPeriodControl(true);
      org.setCalendar(ADOrgPersistInfoUtility.createCalendar(org));
      org.setGeneralLedger(ADOrgPersistInfoUtility.createAcctSchema(org, currencyId));
      OBDal.getInstance().commitAndClose();
      ADOrgPersistInfoUtility.setAcctSchema(org);
    }
    return org.getId();
  }

  private static Calendar createCalendar(final Organization org) {
    Calendar calendar = OBProvider.getInstance().get(Calendar.class);
    calendar.setName(org.getName() + " Calendar");
    calendar.setOrganization(org);
    calendar.setClient(org.getClient());
    OBDal.getInstance().save(calendar);
    OBDal.getInstance().flush();
    return calendar;
  }

  private static AcctSchema createAcctSchema(final Organization org, final String currencyId) {
    AcctSchema acctSchema = OBProvider.getInstance().get(AcctSchema.class);
    acctSchema.setOrganization(org);
    acctSchema.setName(org.getName() + " GL");
    acctSchema.setClient(org.getClient());
    acctSchema.setCurrency(OBDal.getInstance().get(Currency.class, currencyId));
    OBDal.getInstance().save(acctSchema);
    OBDal.getInstance().flush();
    return acctSchema;
  }

  private static void setAcctSchema(final Organization org) {
    OrganizationAcctSchema orgAcctSchema = OBProvider.getInstance()
        .get(OrganizationAcctSchema.class);
    orgAcctSchema.setOrganization(org);
    orgAcctSchema.setClient(org.getClient());
    orgAcctSchema.setAccountingSchema(org.getGeneralLedger());
    OBDal.getInstance().save(orgAcctSchema);
    OBDal.getInstance().flush();
  }

  static void setAsReady(final String orgId, final String isCascade) {
    final Map<String, String> parameters = new HashMap<String, String>(1);
    parameters.put("Cascade", isCascade);
    final ProcessInstance pinstance = CallProcess.getInstance()
        .call("AD_Org_Ready", orgId, parameters);
    if (pinstance.getResult() == 0L) {
      throw new RuntimeException(pinstance.getErrorMsg());
    }
    OBDal.getInstance().commitAndClose();
  }

  static String getBusinessUnitOrgType() {
    String businessUnitOrgType = null;
    try {
      OBContext.setAdminMode(false);
      Client client = OBDal.getInstance().get(Client.class, ADOrgPersistInfoConstants.CLIENT_0);
      Organization org0 = OBDal.getInstance()
          .get(Organization.class, ADOrgPersistInfoConstants.ORG_0);
      final OBCriteria<OrganizationType> criteria = OBDal.getInstance()
          .createCriteria(OrganizationType.class);
      criteria.add(Restrictions.eq(OrganizationType.PROPERTY_BUSINESSUNIT, true));
      criteria.add(Restrictions.eq(OrganizationType.PROPERTY_ACTIVE, true));
      criteria.add(Restrictions.eq(OrganizationType.PROPERTY_CLIENT, client));
      criteria.add(Restrictions.eq(OrganizationType.PROPERTY_ORGANIZATION, org0));
      criteria.setMaxResults(1);
      if (criteria.uniqueResult() != null) {
        businessUnitOrgType = ((OrganizationType) criteria.uniqueResult()).getId();
      } else {
        OrganizationType orgType = OBProvider.getInstance().get(OrganizationType.class);
        orgType.setName("Business Unit");
        orgType.setClient(client);
        orgType.setOrganization(org0);
        orgType.setActive(true);
        orgType.setBusinessUnit(true);
        OBDal.getInstance().save(orgType);
        OBDal.getInstance().flush();
        OBDal.getInstance().commitAndClose();
        businessUnitOrgType = orgType.getId();
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return businessUnitOrgType;
  }

  static String runFunction(final String functionName, final String orgId) {
    final ArrayList<Object> parameters = new ArrayList<Object>();
    parameters.add(orgId);
    return (String) CallStoredProcedure.getInstance().call(functionName, parameters, null);
  }

  /**
   * Validates persist organization information set by AD_Org_Ready DB procedure against the
   * information provided by OrganizationStructureProvider
   */
  static void assertPersistOrgInfo(String orgId) {
    if (StringUtils.isNotEmpty(orgId)) {
      Organization org = OBDal.getInstance().get(Organization.class, orgId);
      OrganizationStructureProvider osp = OBContext.getOBContext()
          .getOrganizationStructureProvider();
      osp.reInitialize();

      /* Assert Legal Entity */
      assertEquals("Match Legal Entity of Organization through OrganizationStructureProvider",
          osp.getLegalEntity(org) == null ? null : osp.getLegalEntity(org).getId(),
          org.getLegalEntityOrganization() == null ? null
              : org.getLegalEntityOrganization().getId());
      assertEquals("Match Legal Entity of Organization through AD_GET_ORG_LE_BU",
          getLegalEntityOrBusinessUnitOrg(ADOrgPersistInfoConstants.FUNCTION_AD_ORG_LE_BU, orgId,
              "LE"),
          org.getLegalEntityOrganization() == null ? null
              : org.getLegalEntityOrganization().getId());
      assertEquals("Match Legal Entity of Organization through AD_GET_ORG_LE_BU_TREENODE",
          getLegalEntityOrBusinessUnitOrg(ADOrgPersistInfoConstants.FUNCTION_AD_ORG_LE_BU_TN, orgId,
              "LE"),
          org.getLegalEntityOrganization() == null ? null
              : org.getLegalEntityOrganization().getId());

      /* Assert Business Unit */
      assertEquals(
          "Match Business Unit Organization of Organization through OrganizationStructureProvider",
          ADOrgPersistInfoUtility.getBusinessUnitOrganization(orgId) == null ? null
              : ADOrgPersistInfoUtility.getBusinessUnitOrganization(orgId).getId(),
          org.getBusinessUnitOrganization() == null ? null
              : org.getBusinessUnitOrganization().getId());
      assertEquals("Match Business Unit Organization of Organization through AD_GET_ORG_LE_BU",
          getLegalEntityOrBusinessUnitOrg(ADOrgPersistInfoConstants.FUNCTION_AD_ORG_LE_BU, orgId,
              "BU"),
          org.getBusinessUnitOrganization() == null ? null
              : org.getBusinessUnitOrganization().getId());
      assertEquals(
          "Match Business Unit Organization of Organization through AD_GET_ORG_LE_BU_TREENODE",
          getLegalEntityOrBusinessUnitOrg(ADOrgPersistInfoConstants.FUNCTION_AD_ORG_LE_BU_TN, orgId,
              "BU"),
          org.getBusinessUnitOrganization() == null ? null
              : org.getBusinessUnitOrganization().getId());

      /* Assert Period Control Allowed */
      assertEquals(
          "Match Period Control Allowed Organization through OrganizationStructureProvider",
          osp.getPeriodControlAllowedOrganization(org) == null ? null
              : osp.getPeriodControlAllowedOrganization(org).getId(),
          org.getPeriodControlAllowedOrganization() == null ? null
              : org.getPeriodControlAllowedOrganization().getId());
      assertEquals("Match Period Control Allowed Organization through ad_org_getperiodcontrolallow",
          runFunction(ADOrgPersistInfoConstants.FUNCTION_AD_ORG_GETPERIODCONTROLALLOW, orgId),
          org.getPeriodControlAllowedOrganization() == null ? null
              : org.getPeriodControlAllowedOrganization().getId());
      assertEquals(
          "Match Period Control Allowed Organization through ad_org_getperiodcontrolallowtn",
          runFunction(ADOrgPersistInfoConstants.FUNCTION_AD_ORG_GETPERIODCONTROLALLOWTN, orgId),
          org.getPeriodControlAllowedOrganization() == null ? null
              : org.getPeriodControlAllowedOrganization().getId());

      /* Assert Calendar Owner */
      final Organization calOrg = ADOrgPersistInfoUtility.getCalendarOrganization(orgId);
      assertEquals("Match Calendar Owner Organization through OrganizationStructureProvider",
          calOrg == null ? null : calOrg.getId(), org.getCalendarOwnerOrganization() == null ? null
              : org.getCalendarOwnerOrganization().getId());
      assertEquals("Match Calendar Owner Organization through ad_org_getcalendarowner",
          runFunction(ADOrgPersistInfoConstants.FUNCTION_AD_ORG_GETCALENDAROWNER, orgId),
          org.getCalendarOwnerOrganization() == null ? null
              : org.getCalendarOwnerOrganization().getId());
      assertEquals("Match Calendar Owner Organization through ad_org_getcalendarownertn",
          runFunction(ADOrgPersistInfoConstants.FUNCTION_AD_ORG_GETCALENDAROWNERTN, orgId),
          org.getCalendarOwnerOrganization() == null ? null
              : org.getCalendarOwnerOrganization().getId());

      /* Assert Inherited Calendar */
      if (calOrg != null) {
        assertEquals("Match Inherited Calendar", calOrg.getCalendar().getId(),
            org.getInheritedCalendar().getId());
      }
    }
  }

  private static String getLegalEntityOrBusinessUnitOrg(final String functionName,
      final String orgId, final String ptype) {
    String returnValue = "";
    try {
      final ArrayList<Object> parameters = new ArrayList<Object>();
      parameters.add(orgId);
      parameters.add(ptype);
      returnValue = (String) CallStoredProcedure.getInstance().call(functionName, parameters, null);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    return returnValue;
  }

  private static Organization getCalendarOrganization(String orgId) {
    Organization calOrg = null;
    OrganizationStructureProvider osp = OBContext.getOBContext().getOrganizationStructureProvider();
    for (String org : osp.getParentList(orgId, true)) {
      calOrg = OBDal.getInstance().get(Organization.class, org);
      if (calOrg.getCalendar() != null) {
        break;
      } else {
        calOrg = null;
      }
    }
    return calOrg;
  }

  private static Organization getBusinessUnitOrganization(String orgId) {
    Organization businessUnitOrg = null;
    OrganizationStructureProvider osp = OBContext.getOBContext().getOrganizationStructureProvider();
    for (String org : osp.getParentList(orgId, true)) {
      businessUnitOrg = OBDal.getInstance().get(Organization.class, org);
      if (businessUnitOrg.getOrganizationType().isBusinessUnit()) {
        break;
      } else {
        businessUnitOrg = null;
      }
    }
    return businessUnitOrg;
  }
}
