package com.smf.securewebservices.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.test.base.TestConstants;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import com.smf.securewebservices.SWSConfig;

class SecureWebServicesUtilsTest extends WeldBaseTest {

  SWSConfig configMock = Mockito.mock(SWSConfig.class);

  @Spy
  private ECPublicKey ecPublicKeyMock;

  @Spy
  private ECPrivateKey ecPrivateKeyMock;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    OBContext.setOBContext(TestConstants.Users.ADMIN, TestConstants.Roles.FB_GRP_ADMIN,
        TestConstants.Clients.FB_GRP, TestConstants.Orgs.ESP);
    VariablesSecureApp vars = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(),
        OBContext.getOBContext().getCurrentClient().getId(),
        OBContext.getOBContext().getCurrentOrganization().getId());
    RequestContext.get().setVariableSecureApp(vars);
  }

  @Test
  void testGenerateTokenWithHS256Algorithm() throws Exception {
    User user = new User();
    Role role = new Role();
    Organization org = new Organization();
    Warehouse warehouse = new Warehouse();

    OBContext.setOBContext(TestConstants.Users.ADMIN, TestConstants.Roles.FB_GRP_ADMIN,
        TestConstants.Clients.FB_GRP, TestConstants.Orgs.ESP);
    VariablesSecureApp vars = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(),
        OBContext.getOBContext().getCurrentClient().getId(),
        OBContext.getOBContext().getCurrentOrganization().getId());
    RequestContext.get().setVariableSecureApp(vars);

    try (MockedStatic<SWSConfig> mockedConfig = Mockito.mockStatic(SWSConfig.class)) {
      mockedConfig.when(SWSConfig::getInstance).thenReturn(configMock);
      when(configMock.getPrivateKey()).thenReturn("{\"private-key\":\"HS256_PRIVATE_KEY\",\"public-key\":\"HS256_PUBLIC_KEY\"}");
      when(SecureWebServicesUtils.getPreferenceValue("SMFSWS_EncryptionAlgorithm")).thenReturn("HS256");

      String token = SecureWebServicesUtils.generateToken(user, role, org, warehouse);

      DecodedJWT decodedToken = JWT.require(Algorithm.HMAC256("HS256_PRIVATE_KEY"))
          .withIssuer("sws")
          .build()
          .verify(token);

      assertEquals(user.getId(), decodedToken.getClaim("user").asString());
      assertEquals(role.getId(), decodedToken.getClaim("role").asString());
      assertEquals(org.getId(), decodedToken.getClaim("organization").asString());
      assertEquals(warehouse.getId(), decodedToken.getClaim("warehouse").asString());
    }
  }

  @Test
  void testGenerateTokenWithES256Algorithm() throws Exception {
    User user = new User();
    Role role = new Role();
    Organization org = new Organization();
    Warehouse warehouse = new Warehouse();

    try (MockedStatic<SWSConfig> mockedConfig = Mockito.mockStatic(SWSConfig.class)) {
      mockedConfig.when(SWSConfig::getInstance).thenReturn(configMock);
      when(configMock.getPrivateKey()).thenReturn("{\"private-key\":\"ES256_PRIVATE_KEY\",\"public-key\":\"ES256_PUBLIC_KEY\"}");
      when(SecureWebServicesUtils.getPreferenceValue("SMFSWS_EncryptionAlgorithm")).thenReturn("ES256");
      when(SecureWebServicesUtils.getECPrivateKey("ES256_PRIVATE_KEY")).thenReturn(ecPrivateKeyMock);
      when(SecureWebServicesUtils.getECPublicKey(configMock)).thenReturn(ecPublicKeyMock);

      String token = SecureWebServicesUtils.generateToken(user, role, org, warehouse);

      DecodedJWT decodedToken = JWT.require(Algorithm.ECDSA256((ECPublicKey) ecPublicKeyMock))
          .withIssuer("sws")
          .build()
          .verify(token);

      assertEquals(user.getId(), decodedToken.getClaim("user").asString());
      assertEquals(role.getId(), decodedToken.getClaim("role").asString());
      assertEquals(org.getId(), decodedToken.getClaim("organization").asString());
      assertEquals(warehouse.getId(), decodedToken.getClaim("warehouse").asString());
    }
  }

  @Test
  void testDecodeTokenWithES256Algorithm() throws Exception {
    try (MockedStatic<SWSConfig> mockedConfig = Mockito.mockStatic(SWSConfig.class)) {
      mockedConfig.when(SWSConfig::getInstance).thenReturn(configMock);
      String token = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyIjoiMTIzNCIsInJvbGUiOiJST0xFXzEiLCJvcmdhbml6YXRpb24iOiJPUkdfMSIsIndhcmVob3VzZSI6IldIXzEifQ.MEYCIQDKz5V+Oq8Qwp/AvpxT8Kv6nY1sIFfmC6sLYdCHdQIGKQIhAKngT4VLyPo1R9FeUt9TxxAzWY3zDuWr8zitjwX/gHVl";

      when(configMock.getPrivateKey()).thenReturn(
          "{\"private-key\":\"ES256_PRIVATE_KEY\",\"public-key\":\"ES256_PUBLIC_KEY\"}");
      when(SecureWebServicesUtils.getECPublicKey(configMock)).thenReturn(ecPublicKeyMock);

      DecodedJWT decodedToken = SecureWebServicesUtils.decodeToken(token);

      assertEquals("1234", decodedToken.getClaim("user").asString());
      assertEquals("ROLE_1", decodedToken.getClaim("role").asString());
      assertEquals("ORG_1", decodedToken.getClaim("organization").asString());
      assertEquals("WH_1", decodedToken.getClaim("warehouse").asString());
    }
  }

  @Test
  void testDecodeTokenWithHS256Algorithm() throws Exception {
    try (MockedStatic<SWSConfig> mockedConfig = Mockito.mockStatic(SWSConfig.class)) {
      mockedConfig.when(SWSConfig::getInstance).thenReturn(configMock);
      String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyIjoiMTIzNCIsInJvbGUiOiJST0xFXzEiLCJvcmdhbml6YXRpb24iOiJPUkdfMSIsIndhcmVob3VzZSI6IldIXzEifQ.nVfIiXhjhMAbHMqSGe6xALyB9gF-zNvuBhXKTa4pXiU";

      when(configMock.getPrivateKey()).thenReturn(
          "{\"private-key\":\"HS256_PRIVATE_KEY\",\"public-key\":\"HS256_PUBLIC_KEY\"}");
      when(SecureWebServicesUtils.getPreferenceValue("SMFSWS_EncryptionAlgorithm")).thenReturn("HS256");

      DecodedJWT decodedToken = SecureWebServicesUtils.decodeToken(token);

      assertEquals("1234", decodedToken.getClaim("user").asString());
      assertEquals("ROLE_1", decodedToken.getClaim("role").asString());
      assertEquals("ORG_1", decodedToken.getClaim("organization").asString());
      assertEquals("WH_1", decodedToken.getClaim("warehouse").asString());
    }
  }

  @Test
  void testDecodeTokenThrowsExceptionWithInvalidToken() {
    String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyIjoiMTIzNCIsInJvbGUiOiJST0xFXzEiLCJvcmdhbml6YXRpb24iOiJPUkdfMSIsIndhcmVob3VzZSI6IldIXzEifQ";

    assertThrows(IllegalArgumentException.class, () -> {
      SecureWebServicesUtils.decodeToken(invalidToken);
    });
  }

  @Test
  void testDecodeTokenThrowsExceptionWithUnsupportedAlgorithm() {
    String token = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyIjoiMTIzNCIsInJvbGUiOiJST0xFXzEiLCJvcmdhbml6YXRpb24iOiJPUkdfMSIsIndhcmVob3VzZSI6IldIXzEifQ.MEYCIQDKz5V+Oq8Qwp/AvpxT8Kv6nY1sIFfmC6sLYdCHdQIGKQIhAKngT4VLyPo1R9FeUt9TxxAzWY3zDuWr8zitjwX/gHVl";

    assertThrows(IllegalArgumentException.class, () -> {
      SecureWebServicesUtils.decodeToken(token);
    });
  }
}