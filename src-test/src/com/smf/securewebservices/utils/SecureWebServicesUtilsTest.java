package com.smf.securewebservices.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.hibernate.criterion.Restrictions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.test.base.TestConstants;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
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

  private static final String HS256_PRIVATE_KEY_MOCK = "{\"private-key\":\"-----BEGIN SECRET KEY-----\\nKpTlGYVO1aUq62eoJQp+FnoLbLG0NyqkinA9TrbxJGQ=\\n-----END SECRET KEY-----\",\"public-key\":\"\"}";
  private static final String ES256_PRIVATE_KEY_MOCK = "{\"private-key\":\"-----BEGIN PRIVATE KEY-----\\nMIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgGCwdf4q+/1xT94jl\\nCyC67oHYThVwLfgSKUG9DUK/4lChRANCAATZxYkUTuZoi6OX8LPB1bV19V7E/q2k\\nwb6+aJijXx/1Udqa72rN8CCP7Qo7cRtpUFTVul4e0ovQelcsjQX6GX9Y\\n-----END PRIVATE KEY-----\",\"public-key\":\"-----BEGIN PUBLIC KEY-----\\nMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE2cWJFE7maIujl/CzwdW1dfVexP6t\\npMG+vmiYo18f9VHamu9qzfAgj+0KO3EbaVBU1bpeHtKL0HpXLI0F+hl/WA==\\n-----END PUBLIC KEY-----\"}";

  private static final String ENCRYPTION_ALGORITHM_PREFERENCE = "SMFSWS_EncryptionAlgorithm";
  private static final String ENCRYPTION_ALGORITHM_HS256 = "HS256";
  private static final String ENCRYPTION_ALGORITHM_ES256 = "ES256";

  /**
   * Sets up the test environment.
   *
   * @throws Exception if an error occurs during setup
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
  }

  /**
   * Configures the SWSConfig with the given keys.
   *
   * @param keys
   */
  private static void configSWSConfig(String keys) {
    SMFSWSConfig config = OBProvider.getInstance().get(SMFSWSConfig.class);
    config.setExpirationTime(0L);
    config.setPrivateKey(keys);
    OBDal.getInstance().save(config);
    OBDal.getInstance().commitAndClose();
    SWSConfig instance = SWSConfig.getInstance();
    instance.refresh(config);
  }

  /**
   * Configures the preference for the encryption algorithm.
   *
   * @param algorithm
   */
  private static void configAlgorithmPreference(String algorithm) {
    Preference pref = OBProvider.getInstance().get(Preference.class);
    pref.setProperty(ENCRYPTION_ALGORITHM_PREFERENCE);
    pref.setSearchKey(algorithm);
    pref.setSelected(true);
    OBDal.getInstance().save(pref);
    OBDal.getInstance().flush();
    pref.setSearchKey(algorithm);
    pref.setSelected(true);
    OBDal.getInstance().save(pref);
    OBDal.getInstance().flush();
    OBDal.getInstance().commitAndClose();
    OBContext.setOBContext(TestConstants.Users.SYSTEM);
  }

  /**
   * Test the generation and decoding of a token with the HS256 algorithm.
   */
  @Test
  public void testGenerateAndDecodeTokenWithHS256Algorithm() throws Exception {
    configSWSConfig(HS256_PRIVATE_KEY_MOCK);
    configAlgorithmPreference(ENCRYPTION_ALGORITHM_HS256);
    User user = OBContext.getOBContext().getUser();
    Role role = OBContext.getOBContext().getRole();
    Organization org = OBContext.getOBContext().getCurrentOrganization();
    Warehouse warehouse = user.getDefaultWarehouse();

    String token = SecureWebServicesUtils.generateToken(user, role, org, warehouse);
    DecodedJWT decodedToken = SecureWebServicesUtils.decodeToken(token);

    assertEquals(user.getId(), decodedToken.getClaim("user").asString());
    assertEquals(role.getId(), decodedToken.getClaim("role").asString());
    assertEquals(org.getId(), decodedToken.getClaim("organization").asString());
  }

  /**
   * Test the generation and decoding of a token with the ES256 algorithm.
   */
  @Test
  public void testGenerateAndDecodeTokenWithES256Algorithm() throws Exception {
    configSWSConfig(ES256_PRIVATE_KEY_MOCK);
    configAlgorithmPreference(ENCRYPTION_ALGORITHM_ES256);
    User user = OBContext.getOBContext().getUser();
    Role role = OBContext.getOBContext().getRole();
    Organization org = OBContext.getOBContext().getCurrentOrganization();
    Warehouse warehouse = user.getDefaultWarehouse();

    String token = SecureWebServicesUtils.generateToken(user, role, org, warehouse);
    DecodedJWT decodedToken = SecureWebServicesUtils.decodeToken(token);

    assertEquals(user.getId(), decodedToken.getClaim("user").asString());
    assertEquals(role.getId(), decodedToken.getClaim("role").asString());
    assertEquals(org.getId(), decodedToken.getClaim("organization").asString());
  }

  /**
   * Test the generation and decoding of a token with the HS256 algorithm throws an exception when
   * the token is invalid.
   */
  @Test
  public void testDecodeTokenThrowsExceptionWithUnsupportedAlgorithm() {
    String tokenRS = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyIjoiMTIzNCIsInJvbGUiOiJST0xFXzEiLCJvcmdhbml6YXRpb24iOiJPUkdfMSIsIndhcmVob3VzZSI6IldIXzEifQ.MEYCIQDKz5V+Oq8Qwp/AvpxT8Kv6nY1sIFfmC6sLYdCHdQIGKQIhAKngT4VLyPo1R9FeUt9TxxAzWY3zDuWr8zitjwX/gHVl";
    configSWSConfig(ES256_PRIVATE_KEY_MOCK);
    configAlgorithmPreference(ENCRYPTION_ALGORITHM_ES256);
    assertThrows(IllegalArgumentException.class, () -> SecureWebServicesUtils.decodeToken(tokenRS));
  }

  /**
   * Cleans up the test environment.
   */
  @After
  public void cleanUp() {
    OBContext.setAdminMode();
    try {
      OBCriteria<SMFSWSConfig> criteria = OBDal.getInstance().createCriteria(SMFSWSConfig.class);
      criteria.setMaxResults(1);
      SMFSWSConfig config = (SMFSWSConfig) criteria.uniqueResult();
      OBDal.getInstance().remove(config);

      Preference pref = (Preference) OBDal.getInstance().createCriteria(Preference.class)
          .add(Restrictions.eq(Preference.PROPERTY_PROPERTY, ENCRYPTION_ALGORITHM_PREFERENCE)).add(Restrictions.eq(Preference.PROPERTY_SELECTED, true))
          .uniqueResult();
      OBDal.getInstance().remove(pref);

      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}

