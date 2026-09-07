package com.etendoerp.platform.validation;

import java.lang.reflect.Proxy;
import java.util.Properties;
import org.openbravo.base.secureApp.LoginSessionSupport;
import org.openbravo.base.secureApp.LoginUtils;
import org.openbravo.base.secureApp.PlatformLoginSessionSupport;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.database.ConnectionProvider;

/** Proves explicit platform session composition without an ERP classpath or database access. */
public final class PlatformLoginSessionValidation {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected composition test mode");
        for (String type : new String[] {"org.openbravo.model.common.plm.Product",
                "org.openbravo.model.common.enterprise.Warehouse",
                "org.openbravo.model.common.businesspartner.BusinessPartner",
                "org.openbravo.base.secureApp.ErpLoginSessionSupport"}) {
            try {
                Class.forName(type, false, PlatformLoginSessionValidation.class.getClassLoader());
                throw new AssertionError("ERP class present in platform session composition: " + type);
            } catch (ClassNotFoundException expected) { }
        }
        Properties properties = new Properties();
        String implementation = switch (args[0]) {
            case "platform" -> PlatformLoginSessionSupport.class.getName();
            case "blank" -> "";
            case "invalid" -> "java.lang.String";
            case "missing" -> "org.openbravo.base.secureApp.MissingSessionSupport";
            case "default" -> null;
            default -> throw new IllegalArgumentException("Unknown test mode");
        };
        if (implementation != null) properties.setProperty(LoginSessionSupport.IMPLEMENTATION_PROPERTY, implementation);
        OBPropertiesProvider.getInstance().setProperties(properties);
        if (!args[0].equals("platform")) {
            try {
                LoginSessionSupport.getInstance();
                throw new AssertionError("Missing or invalid composition silently accepted");
            } catch (ExceptionInInitializerError expected) {
                if (!(expected.getCause() instanceof org.openbravo.base.exception.OBException)) throw expected;
            }
            System.out.println("PASS: Server session composition fails closed for " + args[0]);
            return;
        }
        var support = LoginSessionSupport.getInstance();
        if (support.getClass() != PlatformLoginSessionSupport.class) throw new AssertionError("Wrong contribution");
        var noDatabase = (ConnectionProvider) Proxy.newProxyInstance(PlatformLoginSessionValidation.class.getClassLoader(),
                new Class<?>[] {ConnectionProvider.class}, (proxy, method, values) -> {
                    throw new AssertionError("Platform business contribution accessed the database: " + method.getName());
                });
        if (!"".equals(LoginUtils.getDefaultWarehouse(noDatabase, "C1", "O1", "R1"))
                || !"".equals(support.getUserDefaultWarehouse(noDatabase, "U1", "C1", "O1", "R1"))
                || support.getContextWarehouseId() != null
                || support.isAccountingDimensionConfigCentrally(null)) {
            throw new AssertionError("Platform warehouse/accounting semantics changed");
        }
        var vars = new VariablesSecureApp("U1", "C1", "O1");
        vars.setSessionValue("#AD_User_ID", "U1");
        vars.setSessionValue("#AD_Role_ID", "R1");
        support.initializeApproval(noDatabase, vars, "R1", "U1");
        support.initializeAccounting(noDatabase, vars, null, false, "O1", "C1");
        if (!"U1".equals(vars.getSessionValue("#AD_User_ID")) || !"R1".equals(vars.getSessionValue("#AD_Role_ID"))
                || !vars.getSessionValue("$C_AcctSchema_ID").isEmpty()
                || !vars.getSessionValue("#Approval_Amt").isEmpty()
                || !vars.getSessionValue("#Approval_C_Currency_ID").isEmpty()) {
            throw new AssertionError("Platform contribution changed security state or added accounting state");
        }
        properties.setProperty(LoginSessionSupport.IMPLEMENTATION_PROPERTY, "java.lang.String");
        if (LoginSessionSupport.getInstance() != support) throw new AssertionError("Composition changed after initialization");
        System.out.println("PASS: Original warehouse facade delegates to explicit platform support without ERP classes or SQL");
        System.out.println("PASS: Session contribution is stable per application class loader and does not authorize requests");
    }
}
