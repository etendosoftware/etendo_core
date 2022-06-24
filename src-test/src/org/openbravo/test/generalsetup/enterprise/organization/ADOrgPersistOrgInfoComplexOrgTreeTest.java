package org.openbravo.test.generalsetup.enterprise.organization;

import org.junit.Test;
import org.openbravo.test.base.OBBaseTest;

public class ADOrgPersistOrgInfoComplexOrgTreeTest extends OBBaseTest {

  /**
   * Create a organization tree with different organization types set them ready with cascade No
   * option and validate persist org info for each of them
   */
  @Test
  public void testSetReadyOrganizationWithOutCascade() {
    ADOrgPersistInfoUtility.setTestContextFB();
    // Create a parent organization of type Organization under *
    String org_0 = ADOrgPersistInfoUtility.createOrganization(
        ADOrgPersistInfoConstants.ORGTYPE_ORGANIZATION, ADOrgPersistInfoConstants.ORG_0, true,
        ADOrgPersistInfoConstants.CUR_EURO);
    ADOrgPersistInfoUtility.setAsReady(org_0, "N");
    ADOrgPersistInfoUtility.assertPersistOrgInfo(org_0);

    // Create a parent organization of type Organization under org_0
    String org_01 = ADOrgPersistInfoUtility.createOrganization(
        ADOrgPersistInfoConstants.ORGTYPE_ORGANIZATION, org_0, true,
        ADOrgPersistInfoConstants.CUR_EURO);
    ADOrgPersistInfoUtility.setAsReady(org_01, "N");
    ADOrgPersistInfoUtility.assertPersistOrgInfo(org_01);

    // Create a LE with Acct parent organization of type Organization under org_01
    String org_01_LE1 = ADOrgPersistInfoUtility.createOrganization(
        ADOrgPersistInfoConstants.ORGTYPE_LEGALWITHACCOUNTING, org_01, true,
        ADOrgPersistInfoConstants.CUR_EURO);
    ADOrgPersistInfoUtility.setAsReady(org_01_LE1, "N");
    ADOrgPersistInfoUtility.assertPersistOrgInfo(org_01_LE1);

    // Create a business unit parent organization of type Organization under org_01_LE1
    String org_01_LE1_BU = ADOrgPersistInfoUtility.createOrganization(
        ADOrgPersistInfoUtility.getBusinessUnitOrgType(), org_01_LE1, true,
        ADOrgPersistInfoConstants.CUR_EURO);
    ADOrgPersistInfoUtility.setAsReady(org_01_LE1_BU, "N");
    ADOrgPersistInfoUtility.assertPersistOrgInfo(org_01_LE1_BU);

    // Create a Generic organization of type Organization under org_01_LE1_BU
    String org_01_LE1_BU_GE = ADOrgPersistInfoUtility.createOrganization(
        ADOrgPersistInfoConstants.ORGTYPE_GENERIC, org_01_LE1_BU, false,
        ADOrgPersistInfoConstants.CUR_EURO);
    ADOrgPersistInfoUtility.setAsReady(org_01_LE1_BU_GE, "N");
    ADOrgPersistInfoUtility.assertPersistOrgInfo(org_01_LE1_BU_GE);

    // Create a Generic parent organization of type Organization under org_01_LE1
    String org_01_LE1_GE = ADOrgPersistInfoUtility.createOrganization(
        ADOrgPersistInfoConstants.ORGTYPE_GENERIC, org_01_LE1, true,
        ADOrgPersistInfoConstants.CUR_EURO);
    ADOrgPersistInfoUtility.setAsReady(org_01_LE1_GE, "N");
    ADOrgPersistInfoUtility.assertPersistOrgInfo(org_01_LE1_GE);

    // Create a Generic organization of type Organization under org_01_LE1_GE
    String org_01_LE1_GE_GE = ADOrgPersistInfoUtility.createOrganization(
        ADOrgPersistInfoConstants.ORGTYPE_GENERIC, org_01_LE1_GE, false,
        ADOrgPersistInfoConstants.CUR_EURO);
    ADOrgPersistInfoUtility.setAsReady(org_01_LE1_GE_GE, "N");
    ADOrgPersistInfoUtility.assertPersistOrgInfo(org_01_LE1_GE_GE);

    // Create a LE without Acct parent organization of type Organization under org_01
    String org_01_LE2 = ADOrgPersistInfoUtility.createOrganization(
        ADOrgPersistInfoConstants.ORGTYPE_LEGALWITHOUTACCOUNTING, org_01, true,
        ADOrgPersistInfoConstants.CUR_EURO);
    ADOrgPersistInfoUtility.setAsReady(org_01_LE2, "N");
    ADOrgPersistInfoUtility.assertPersistOrgInfo(org_01_LE2);

    // Create a Generic organization of type Organization under org_01_LE2
    String org_01_LE2_GE = ADOrgPersistInfoUtility.createOrganization(
        ADOrgPersistInfoConstants.ORGTYPE_GENERIC, org_01_LE2, false,
        ADOrgPersistInfoConstants.CUR_EURO);
    ADOrgPersistInfoUtility.setAsReady(org_01_LE2_GE, "N");
    ADOrgPersistInfoUtility.assertPersistOrgInfo(org_01_LE2_GE);
  }

  /**
   * Create a organization tree with different organization types set top node organization in the
   * created tree ready with cascade Yes option and validate persist org info for each of them
   */
  @Test
  public void testSetReadyOrganizationWithCascase() {
    ADOrgPersistInfoUtility.setTestContextFB();
    // Create a parent organization of type Organization under *
    String org_0 = ADOrgPersistInfoUtility.createOrganization(
        ADOrgPersistInfoConstants.ORGTYPE_ORGANIZATION, ADOrgPersistInfoConstants.ORG_0, true,
        ADOrgPersistInfoConstants.CUR_USD);

    // Create a parent organization of type Organization under org_0
    String org_01 = ADOrgPersistInfoUtility.createOrganization(
        ADOrgPersistInfoConstants.ORGTYPE_ORGANIZATION, org_0, true,
        ADOrgPersistInfoConstants.CUR_USD);

    // Create a LE with Acct parent organization of type Organization under org_01
    String org_01_LE1 = ADOrgPersistInfoUtility.createOrganization(
        ADOrgPersistInfoConstants.ORGTYPE_LEGALWITHACCOUNTING, org_01, true,
        ADOrgPersistInfoConstants.CUR_USD);

    // Create a business unit parent organization of type Organization under org_01_LE1
    String org_01_LE1_BU = ADOrgPersistInfoUtility.createOrganization(
        ADOrgPersistInfoUtility.getBusinessUnitOrgType(), org_01_LE1, true,
        ADOrgPersistInfoConstants.CUR_USD);

    // Create a Generic organization of type Organization under org_01_LE1_BU
    String org_01_LE1_BU_GE = ADOrgPersistInfoUtility.createOrganization(
        ADOrgPersistInfoConstants.ORGTYPE_GENERIC, org_01_LE1_BU, false,
        ADOrgPersistInfoConstants.CUR_USD);

    // Create a Generic parent organization of type Organization under org_01_LE1
    String org_01_LE1_GE = ADOrgPersistInfoUtility.createOrganization(
        ADOrgPersistInfoConstants.ORGTYPE_GENERIC, org_01_LE1, true,
        ADOrgPersistInfoConstants.CUR_USD);

    // Create a Generic organization of type Organization under org_01_LE1_GE
    String org_01_LE1_GE_GE = ADOrgPersistInfoUtility.createOrganization(
        ADOrgPersistInfoConstants.ORGTYPE_GENERIC, org_01_LE1_GE, false,
        ADOrgPersistInfoConstants.CUR_USD);

    // Create a LE without Acct parent organization of type Organization under org_01
    String org_01_LE2 = ADOrgPersistInfoUtility.createOrganization(
        ADOrgPersistInfoConstants.ORGTYPE_LEGALWITHOUTACCOUNTING, org_01, true,
        ADOrgPersistInfoConstants.CUR_USD);

    // Create a Generic organization of type Organization under org_01_LE2
    String org_01_LE2_GE = ADOrgPersistInfoUtility.createOrganization(
        ADOrgPersistInfoConstants.ORGTYPE_GENERIC, org_01_LE2, false,
        ADOrgPersistInfoConstants.CUR_USD);

    ADOrgPersistInfoUtility.setAsReady(org_0, "Y");
    ADOrgPersistInfoUtility.assertPersistOrgInfo(org_0);
    ADOrgPersistInfoUtility.assertPersistOrgInfo(org_01);
    ADOrgPersistInfoUtility.assertPersistOrgInfo(org_01_LE1);
    ADOrgPersistInfoUtility.assertPersistOrgInfo(org_01_LE1_BU);
    ADOrgPersistInfoUtility.assertPersistOrgInfo(org_01_LE1_BU_GE);
    ADOrgPersistInfoUtility.assertPersistOrgInfo(org_01_LE1_GE);
    ADOrgPersistInfoUtility.assertPersistOrgInfo(org_01_LE1_GE_GE);
    ADOrgPersistInfoUtility.assertPersistOrgInfo(org_01_LE2);
    ADOrgPersistInfoUtility.assertPersistOrgInfo(org_01_LE2_GE);
  }
}
