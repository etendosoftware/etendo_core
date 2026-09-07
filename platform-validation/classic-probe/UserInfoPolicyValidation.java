package com.etendoerp.platform.validation;

import java.util.Properties;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.application.navigationbarcomponents.UserInfoAccessPolicy;

/** Fresh-JVM fail-closed policy composition checks, without database or ERP classes. */
public final class UserInfoPolicyValidation {
  public static void main(String[] args) {
    String mode = args[0];
    String prefix = "org.openbravo.client.application.navigationbarcomponents.";
    var properties = new Properties();
    if (!mode.equals("default") && !mode.equals("erp")) {
      properties.setProperty(UserInfoAccessPolicy.IMPLEMENTATION_PROPERTY, switch (mode) {
        case "platform" -> prefix + "PlatformUserInfoAccessPolicy";
        case "blank" -> "";
        case "invalid" -> "java.lang.String";
        case "missing" -> "missing.Policy";
        default -> throw new IllegalArgumentException(mode);
      });
    }
    OBPropertiesProvider.getInstance().setProperties(properties);
    if (!mode.equals("erp") && UserInfoAccessPolicy.class.getClassLoader().getResource(
        "org/openbravo/client/application/navigationbarcomponents/ErpUserInfoAccessPolicy.class") != null) {
      throw new AssertionError("ERP policy is available in platform composition");
    }
    try {
      var policy = UserInfoAccessPolicy.getInstance();
      if (!mode.equals("platform") && !mode.equals("erp")) {
        throw new AssertionError("Invalid configuration silently selected a policy: " + mode);
      }
      String expected = prefix + (mode.equals("erp") ? "Erp" : "Platform") + "UserInfoAccessPolicy";
      if (!policy.getClass().getName().equals(expected)) throw new AssertionError("Wrong policy");
      if (mode.equals("platform") && policy.isSystemAdministratorOnly(null)) {
        throw new AssertionError("Platform acquired an ERP restriction");
      }
      properties.setProperty(UserInfoAccessPolicy.IMPLEMENTATION_PROPERTY, "missing.Policy");
      if (policy != UserInfoAccessPolicy.getInstance()) throw new AssertionError("Policy changed after startup");
    } catch (ExceptionInInitializerError failure) {
      if (mode.equals("platform") || mode.equals("erp")
          || !(failure.getCause() instanceof org.openbravo.base.exception.OBException)) throw failure;
    }
    System.out.println("PASS: Profile access policy composition " + mode);
  }
}
