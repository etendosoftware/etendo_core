package com.smf.securewebservices.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.Restrictions;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.test.base.TestConstants;
import org.openbravo.dal.core.OBContext;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;

import com.auth0.jwt.interfaces.DecodedJWT;

import com.smf.securewebservices.SWSConfig;
import com.smf.securewebservices.data.SMFSWSConfig;

/**
 * Test class for SecureWebServicesUtils.
 */
public class SecureWebServicesUtilsTest extends WeldBaseTest {

  private static final String HS256_PRIVATE_KEY_MOCK = "{\"private-key\":\"-----BEGIN SECRET KEY-----uKOQOkfQPEmFs7CKQhT9UJNQ5DHEZmnBxU/2f5x06YE=-----END SECRET KEY-----\",\"public-key\":\"\"}";
  private static final String ES256_PRIVATE_KEY_MOCK = "{\"private-key\":\"-----BEGIN PRIVATE KEY-----MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgs6Wr9OstUyGI3WIdXUGrx4/DA87e3dst93f7p5NVGSmhRANCAASgaQjofAzCf93v4qs+Z9ou5g74gP/B9Uxn8inJ8/0rShFdV7/60B8EeZxPiiTTe1zvkl9V/5IRkQkXIJrmY4UI-----END PRIVATE KEY-----\",\"public-key\":\"-----BEGIN PUBLIC KEY-----MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEoGkI6HwMwn/d7+KrPmfaLuYO+ID/wfVMZ/IpyfP9K0oRXVe/+tAfBHmcT4ok03tc75JfVf+SEZEJFyCa5mOFCA==-----END PUBLIC KEY-----\"}";
  private static final String HS256_PRIVATE_KEY_MOCK_LEGACY = "*z8iR8Aujg{G-$lPz]+H7U.pv21|P1H=vGRnL[K+7_07@Bq\"~A},AlS^;}60dOq-";

  private static final String ENCRYPTION_ALGORITHM_PREFERENCE = "SMFSWS_EncryptionAlgorithm";
  private static final String ENCRYPTION_ALGORITHM_HS256 = "HS256";
  private static final String ENCRYPTION_ALGORITHM_ES256 = "ES256";
  public static final String ORGANIZATION = "organization";
  public static final String USER = "user";
  public static final String ROLE = "role";

  /**
   * Configures the SWSConfig with the given keys.
   *
   * @param keys
   */
  private void configSWSConfig(String keys) {
    try {
      OBContext.setAdminMode();
      // Set SYSTEM context (client 0) for system-level entities
      OBContext.setOBContext(TestConstants.Users.SYSTEM, TestConstants.Roles.SYS_ADMIN,
          TestConstants.Clients.SYSTEM, TestConstants.Orgs.MAIN);
      
      // Try to find existing config first
      OBCriteria<SMFSWSConfig> criteria = OBDal.getInstance().createCriteria(SMFSWSConfig.class);
      criteria.setMaxResults(1);
      SMFSWSConfig config = (SMFSWSConfig) criteria.uniqueResult();
      
      // If doesn't exist, create new one
      if (config == null) {
        config = OBProvider.getInstance().get(SMFSWSConfig.class);
        config.setNewOBObject(true);
      }
      
      config.setExpirationTime(0L);
      config.setPrivateKey(keys);
      OBDal.getInstance().save(config);
      OBDal.getInstance().flush();
      SWSConfig instance = SWSConfig.getInstance();
      instance.refresh(config);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Configures the preference for the encryption algorithm.
   *
   * @param algorithm
   */
  private void configAlgorithmPreference(String algorithm) {
    try {
      OBContext.setAdminMode();
      // Set SYSTEM context for preference creation
      OBContext.setOBContext(TestConstants.Users.SYSTEM, TestConstants.Roles.SYS_ADMIN,
          TestConstants.Clients.SYSTEM, TestConstants.Orgs.MAIN);
      
      // Try to find existing preference first
      Preference pref = (Preference) OBDal.getInstance().createCriteria(Preference.class)
          .add(Restrictions.eq(Preference.PROPERTY_PROPERTY, ENCRYPTION_ALGORITHM_PREFERENCE))
          .add(Restrictions.eq(Preference.PROPERTY_SELECTED, true))
          .uniqueResult();
      
      // If doesn't exist, create new one
      if (pref == null) {
        pref = OBProvider.getInstance().get(Preference.class);
        pref.setNewOBObject(true);
        pref.setProperty(ENCRYPTION_ALGORITHM_PREFERENCE);
        pref.setSelected(true);
      }
      
      pref.setSearchKey(algorithm);
      OBDal.getInstance().save(pref);
      OBDal.getInstance().flush();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Test the generation and decoding of a token with the HS256 algorithm.
   */
  @Test
  void testGenerateAndDecodeTokenWithHS256Algorithm() throws Exception {
    configSWSConfig(HS256_PRIVATE_KEY_MOCK);
    configAlgorithmPreference(ENCRYPTION_ALGORITHM_HS256);
    User user = OBContext.getOBContext().getUser();
    Role role = OBContext.getOBContext().getRole();
    Organization org = OBContext.getOBContext().getCurrentOrganization();
    Warehouse warehouse = user.getDefaultWarehouse();

    String token = SecureWebServicesUtils.generateToken(user, role, org, warehouse);
    DecodedJWT decodedToken = SecureWebServicesUtils.decodeToken(token);

    assertEquals(user.getId(), decodedToken.getClaim(USER).asString());
    assertEquals(role.getId(), decodedToken.getClaim(ROLE).asString());
    assertEquals(org.getId(), decodedToken.getClaim(ORGANIZATION).asString());
  }

  /**
   * Test the generation and decoding of a token with the ES256 algorithm.
   */
  @Test
  void testGenerateAndDecodeTokenWithES256Algorithm() throws Exception {
    configSWSConfig(ES256_PRIVATE_KEY_MOCK);
    configAlgorithmPreference(ENCRYPTION_ALGORITHM_ES256);
    User user = OBContext.getOBContext().getUser();
    Role role = OBContext.getOBContext().getRole();
    Organization org = OBContext.getOBContext().getCurrentOrganization();
    Warehouse warehouse = user.getDefaultWarehouse();

    String token = SecureWebServicesUtils.generateToken(user, role, org, warehouse);
    DecodedJWT decodedToken = SecureWebServicesUtils.decodeToken(token);

    assertEquals(user.getId(), decodedToken.getClaim(USER).asString());
    assertEquals(role.getId(), decodedToken.getClaim(ROLE).asString());
    assertEquals(org.getId(), decodedToken.getClaim(ORGANIZATION).asString());
  }

  /**
   * Test the generation and decoding of a token with the HS256 algorithm throws an exception when
   * the token is invalid.
   */
  @Test
  void testDecodeTokenThrowsExceptionWithUnsupportedAlgorithm() {
    String tokenRS = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyIjoiMTIzNCIsInJvbGUiOiJST0xFXzEiLCJvcmdhbml6YXRpb24iOiJPUkdfMSIsIndhcmVob3VzZSI6IldIXzEifQ.MEYCIQDKz5V+Oq8Qwp/AvpxT8Kv6nY1sIFfmC6sLYdCHdQIGKQIhAKngT4VLyPo1R9FeUt9TxxAzWY3zDuWr8zitjwX/gHVl";
    configSWSConfig(ES256_PRIVATE_KEY_MOCK);
    configAlgorithmPreference(ENCRYPTION_ALGORITHM_ES256);
    assertThrows(IllegalArgumentException.class, () -> SecureWebServicesUtils.decodeToken(tokenRS));
  }

  /**
   * Test the generation and decoding of a token with the ES256 algorithm throws an exception when
   * the token is invalid.
   *
   * @throws Exception
   */
  @Test
  void testLegacyPrivKeyES256() throws Exception {
    configSWSConfig(HS256_PRIVATE_KEY_MOCK_LEGACY);
    configAlgorithmPreference(ENCRYPTION_ALGORITHM_ES256);
    User user = OBContext.getOBContext().getUser();
    Role role = OBContext.getOBContext().getRole();
    Organization org = OBContext.getOBContext().getCurrentOrganization();
    Warehouse warehouse = user.getDefaultWarehouse();

    String token = SecureWebServicesUtils.generateToken(user, role, org, warehouse);
    DecodedJWT decodedToken = SecureWebServicesUtils.decodeToken(token);

    assertEquals(user.getId(), decodedToken.getClaim(USER).asString());
    assertEquals(role.getId(), decodedToken.getClaim(ROLE).asString());
    assertEquals(org.getId(), decodedToken.getClaim(ORGANIZATION).asString());
  }

  /**
   * Test the generation and decoding of a token with the HS256 algorithm.
   *
   * @throws Exception
   */
  @Test
  void testLegacyPrivKeyHS256() throws Exception {
    configSWSConfig(HS256_PRIVATE_KEY_MOCK_LEGACY);
    configAlgorithmPreference(ENCRYPTION_ALGORITHM_HS256);
    User user = OBContext.getOBContext().getUser();
    Role role = OBContext.getOBContext().getRole();
    Organization org = OBContext.getOBContext().getCurrentOrganization();
    Warehouse warehouse = user.getDefaultWarehouse();

    String token = SecureWebServicesUtils.generateToken(user, role, org, warehouse);
    DecodedJWT decodedToken = SecureWebServicesUtils.decodeToken(token);

    assertEquals(user.getId(), decodedToken.getClaim(USER).asString());
    assertEquals(role.getId(), decodedToken.getClaim(ROLE).asString());
    assertEquals(org.getId(), decodedToken.getClaim(ORGANIZATION).asString());
  }

  /**
   * Cleans up the test environment.
   */
  @AfterEach
  public void cleanUp() {
    try {
      OBContext.setAdminMode();
      OBCriteria<SMFSWSConfig> criteria = OBDal.getInstance().createCriteria(SMFSWSConfig.class);
      criteria.setMaxResults(1);
      SMFSWSConfig config = (SMFSWSConfig) criteria.uniqueResult();
      OBDal.getInstance().remove(config);

      Preference pref = (Preference) OBDal.getInstance().createCriteria(Preference.class)
          .add(Restrictions.eq(Preference.PROPERTY_PROPERTY, ENCRYPTION_ALGORITHM_PREFERENCE))
          .add(Restrictions.eq(Preference.PROPERTY_SELECTED, true))
          .uniqueResult();
      if(pref != null) {
        OBDal.getInstance().remove(pref);
      }

      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
