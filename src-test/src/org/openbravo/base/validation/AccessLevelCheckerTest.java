package org.openbravo.base.validation;

import org.junit.Test;
import org.openbravo.base.exception.OBSecurityException;

/**
 * Tests for {@link AccessLevelChecker}.
 */
public class AccessLevelCheckerTest {

  private static final String ENTITY = "TestEntity";

  // --- ALL checker tests ---

  @Test
  public void testAllAllowsZeroClientAndZeroOrg() {
    AccessLevelChecker.ALL.checkAccessLevel(ENTITY, "0", "0");
  }

  @Test
  public void testAllAllowsNonZeroClientAndNonZeroOrg() {
    AccessLevelChecker.ALL.checkAccessLevel(ENTITY, "100", "200");
  }

  @Test
  public void testAllAllowsNullClientAndNullOrg() {
    AccessLevelChecker.ALL.checkAccessLevel(ENTITY, null, null);
  }

  // --- SYSTEM checker tests ---

  @Test
  public void testSystemAllowsZeroClientAndZeroOrg() {
    AccessLevelChecker.SYSTEM.checkAccessLevel(ENTITY, "0", "0");
  }

  @Test(expected = OBSecurityException.class)
  public void testSystemRejectsNonZeroClient() {
    AccessLevelChecker.SYSTEM.checkAccessLevel(ENTITY, "100", "0");
  }

  @Test(expected = OBSecurityException.class)
  public void testSystemRejectsNonZeroOrg() {
    AccessLevelChecker.SYSTEM.checkAccessLevel(ENTITY, "0", "200");
  }

  @Test(expected = OBSecurityException.class)
  public void testSystemRejectsNullClient() {
    AccessLevelChecker.SYSTEM.checkAccessLevel(ENTITY, null, "0");
  }

  @Test(expected = OBSecurityException.class)
  public void testSystemRejectsNullOrg() {
    AccessLevelChecker.SYSTEM.checkAccessLevel(ENTITY, "0", null);
  }

  // --- SYSTEM_CLIENT checker tests ---

  @Test
  public void testSystemClientAllowsZeroClientAndZeroOrg() {
    AccessLevelChecker.SYSTEM_CLIENT.checkAccessLevel(ENTITY, "0", "0");
  }

  @Test
  public void testSystemClientAllowsNonZeroClientWithZeroOrg() {
    AccessLevelChecker.SYSTEM_CLIENT.checkAccessLevel(ENTITY, "100", "0");
  }

  @Test(expected = OBSecurityException.class)
  public void testSystemClientRejectsNonZeroOrg() {
    AccessLevelChecker.SYSTEM_CLIENT.checkAccessLevel(ENTITY, "0", "200");
  }

  @Test(expected = OBSecurityException.class)
  public void testSystemClientRejectsNullOrg() {
    AccessLevelChecker.SYSTEM_CLIENT.checkAccessLevel(ENTITY, "0", null);
  }

  // --- ORGANIZATION checker tests ---

  @Test
  public void testOrganizationAllowsNonZeroClientAndNonZeroOrg() {
    AccessLevelChecker.ORGANIZATION.checkAccessLevel(ENTITY, "100", "200");
  }

  @Test(expected = OBSecurityException.class)
  public void testOrganizationRejectsZeroClient() {
    AccessLevelChecker.ORGANIZATION.checkAccessLevel(ENTITY, "0", "200");
  }

  @Test(expected = OBSecurityException.class)
  public void testOrganizationRejectsZeroOrg() {
    AccessLevelChecker.ORGANIZATION.checkAccessLevel(ENTITY, "100", "0");
  }

  @Test
  public void testOrganizationAllowsNullClient() {
    // null client is by definition unequal to "0", so it passes
    AccessLevelChecker.ORGANIZATION.checkAccessLevel(ENTITY, null, "200");
  }

  @Test
  public void testOrganizationAllowsNullOrg() {
    // null org is by definition not zero org, so it passes
    AccessLevelChecker.ORGANIZATION.checkAccessLevel(ENTITY, "100", null);
  }

  // --- CLIENT_ORGANIZATION checker tests ---

  @Test
  public void testClientOrganizationAllowsNonZeroClientAnyOrg() {
    AccessLevelChecker.CLIENT_ORGANIZATION.checkAccessLevel(ENTITY, "100", "0");
  }

  @Test
  public void testClientOrganizationAllowsNonZeroClientNonZeroOrg() {
    AccessLevelChecker.CLIENT_ORGANIZATION.checkAccessLevel(ENTITY, "100", "200");
  }

  @Test(expected = OBSecurityException.class)
  public void testClientOrganizationRejectsZeroClient() {
    AccessLevelChecker.CLIENT_ORGANIZATION.checkAccessLevel(ENTITY, "0", "200");
  }

  @Test
  public void testClientOrganizationAllowsNullClient() {
    // null client is by definition unequal to "0", so it passes
    AccessLevelChecker.CLIENT_ORGANIZATION.checkAccessLevel(ENTITY, null, "200");
  }
}
