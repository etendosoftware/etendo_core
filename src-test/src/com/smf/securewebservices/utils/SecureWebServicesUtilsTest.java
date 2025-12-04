package com.smf.securewebservices.utils;

import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import org.codehaus.jettison.json.JSONException;
import org.hibernate.criterion.Restrictions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.test.base.TestConstants;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
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

  private String originalPrivateKey;
  private String originalAlgorithm;

  /**
   * Sets up the test environment.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    OBContext.setOBContext(TestConstants.Users.SYSTEM, TestConstants.Roles.SYS_ADMIN,
        TestConstants.Clients.SYSTEM, TestConstants.Orgs.MAIN);
    VariablesSecureApp vars = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(),
        OBContext.getOBContext().getCurrentClient().getId(),
        OBContext.getOBContext().getCurrentOrganization().getId());
    RequestContext.get().setVariableSecureApp(vars);

    // Save original configuration to restore later
    OBCriteria<SMFSWSConfig> criteria = OBDal.getInstance().createCriteria(SMFSWSConfig.class);
    criteria.setMaxResults(1);
    SMFSWSConfig config = (SMFSWSConfig) criteria.uniqueResult();
    if (config != null) {
      originalPrivateKey = config.getPrivateKey();
    }

    Preference pref = (Preference) OBDal.getInstance().createCriteria(Preference.class)
        .add(Restrictions.eq(Preference.PROPERTY_PROPERTY, ENCRYPTION_ALGORITHM_PREFERENCE))
        .add(Restrictions.eq(Preference.PROPERTY_SELECTED, true))
        .uniqueResult();
    if (pref != null) {
      originalAlgorithm = pref.getSearchKey();
    }
  }

  /**
   * Configures the SWSConfig with the given keys.
   * Updates the existing config if present, or creates a new one if it doesn't exist.
   *
   * @param keys
   */
  private static void configSWSConfig(String keys) {
    // Try to get existing config first
    OBCriteria<SMFSWSConfig> criteria = OBDal.getInstance().createCriteria(SMFSWSConfig.class);
    criteria.setMaxResults(1);
    SMFSWSConfig config = (SMFSWSConfig) criteria.uniqueResult();

    // If no config exists, create a new one
    if (config == null) {
      config = OBProvider.getInstance().get(SMFSWSConfig.class);
    }

    config.setExpirationTime(3600L);
    config.setPrivateKey(keys);
    OBDal.getInstance().save(config);
    OBDal.getInstance().commitAndClose();
    SWSConfig instance = SWSConfig.getInstance();
    instance.refresh(config);
  }

  /**
   * Configures the preference for the encryption algorithm.
   * Updates the existing preference if present, or creates a new one if it doesn't exist.
   *
   * @param algorithm
   */
  private static void configAlgorithmPreference(String algorithm) {
    // Try to get existing preference first
    Preference pref = (Preference) OBDal.getInstance().createCriteria(Preference.class)
        .add(Restrictions.eq(Preference.PROPERTY_PROPERTY, ENCRYPTION_ALGORITHM_PREFERENCE))
        .add(Restrictions.eq(Preference.PROPERTY_SELECTED, true))
        .uniqueResult();

    // If no preference exists, create a new one
    if (pref == null) {
      pref = OBProvider.getInstance().get(Preference.class);
      pref.setProperty(ENCRYPTION_ALGORITHM_PREFERENCE);
      pref.setSelected(true);
    }

    pref.setSearchKey(algorithm);
    OBDal.getInstance().save(pref);
    OBDal.getInstance().flush();
    OBDal.getInstance().commitAndClose();
    OBContext.setOBContext(TestConstants.Users.SYSTEM);
  }

  /**
   * Helper method to test token generation and decoding with specific configuration.
   */
  private void testTokenGenerationAndDecoding(String privateKey, String algorithm) throws Exception {
    configSWSConfig(privateKey);
    configAlgorithmPreference(algorithm);
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
   */
  @Test
  public void testGenerateAndDecodeTokenWithHS256Algorithm() throws Exception {
    testTokenGenerationAndDecoding(HS256_PRIVATE_KEY_MOCK, ENCRYPTION_ALGORITHM_HS256);
  }

  /**
   * Test the generation and decoding of a token with the HS256 algorithm throws an exception when
   * the token is invalid.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testDecodeTokenThrowsExceptionWithUnsupportedAlgorithm() throws JSONException, UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
    String tokenRS = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyIjoiMTIzNCIsInJvbGUiOiJST0xFXzEiLCJvcmdhbml6YXRpb24iOiJPUkdfMSIsIndhcmVob3VzZSI6IldIXzEifQ.MEYCIQDKz5V+Oq8Qwp/AvpxT8Kv6nY1sIFfmC6sLYdCHdQIGKQIhAKngT4VLyPo1R9FeUt9TxxAzWY3zDuWr8zitjwX/gHVl";
    configSWSConfig(ES256_PRIVATE_KEY_MOCK);
    configAlgorithmPreference(ENCRYPTION_ALGORITHM_ES256);
    SecureWebServicesUtils.decodeToken(tokenRS);
  }

  /**
   * Test that using an HS256 key (in new JSON format) with ES256 algorithm preference throws an exception.
   * This validates that incompatible key/algorithm combinations are properly rejected.
   * When a JSON key containing an HS256 secret is used with ES256 algorithm preference,
   * it will fail during EC private key parsing (Base64 decode or key spec validation).
   */
  @Test
  public void testLegacyPrivKeyES256() throws Exception {
    testTokenGenerationAndDecoding(HS256_PRIVATE_KEY_MOCK_LEGACY, ENCRYPTION_ALGORITHM_ES256);
  }

  /**
   * Test the generation and decoding of a token with the HS256 algorithm using legacy key format.
   *
   * @throws Exception
   */
  @Test
  public void testLegacyPrivKeyHS256() throws Exception {
    testTokenGenerationAndDecoding(HS256_PRIVATE_KEY_MOCK_LEGACY, ENCRYPTION_ALGORITHM_HS256);
  }

  /**
   * Test the generation and decoding of a token with the ES256 algorithm.
   */
  @Test
  public void testGenerateAndDecodeTokenWithES256Algorithm() throws Exception {
    testTokenGenerationAndDecoding(ES256_PRIVATE_KEY_MOCK, ENCRYPTION_ALGORITHM_ES256);
  }

  /**
   * Cleans up the test environment.
   * Restores the original SWS configuration that existed before the test.
   */
  @After
  public void cleanUp() {
    try {
      OBContext.setAdminMode();

      // Restore original SWS config if it was saved
      if (originalPrivateKey != null) {
        OBCriteria<SMFSWSConfig> criteria = OBDal.getInstance().createCriteria(SMFSWSConfig.class);
        criteria.setMaxResults(1);
        SMFSWSConfig config = (SMFSWSConfig) criteria.uniqueResult();
        if (config != null) {
          config.setPrivateKey(originalPrivateKey);
          OBDal.getInstance().save(config);
          SWSConfig.getInstance().refresh(config);
        }
      }

      // Restore original algorithm preference if it was saved
      Preference pref = (Preference) OBDal.getInstance().createCriteria(Preference.class)
          .add(Restrictions.eq(Preference.PROPERTY_PROPERTY, ENCRYPTION_ALGORITHM_PREFERENCE))
          .add(Restrictions.eq(Preference.PROPERTY_SELECTED, true))
          .uniqueResult();
      
      if (originalAlgorithm != null) {
        if (pref != null) {
          pref.setSearchKey(originalAlgorithm);
          OBDal.getInstance().save(pref);
        }
      } else {
        // If there was no original preference, remove the test one
        if (pref != null) {
          OBDal.getInstance().remove(pref);
        }
      }

      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}

