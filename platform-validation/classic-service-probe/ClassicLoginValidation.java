package com.etendoerp.platform.validation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeMap;
import org.openbravo.base.secureApp.LoginUtils;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.service.db.DalConnectionProvider;

/** Exercises original login defaults and session initialization on the owned Classic copy. */
public final class ClassicLoginValidation implements Runnable {
    @Override
    public void run() {
        try {
            String expectedLocation = System.getProperty("validation.loginLocation", "/classes/java/classicJson/");
            if (!LoginUtils.class.getProtectionDomain().getCodeSource().getLocation().toString().contains(expectedLocation)) {
                throw new AssertionError("Login implementation did not load from the requested comparison output");
            }
            String username = System.getenv("PLATFORM_TEST_USERNAME");
            String password = System.getenv("PLATFORM_TEST_PASSWORD");
            if (username == null || password == null) throw new IllegalStateException("Supply test credentials through environment variables");
            var connection = new DalConnectionProvider(false);
            String user = LoginUtils.checkUserPassword(connection, username, password);
            if (user == null || LoginUtils.checkUserPassword(connection, username, password + "-invalid") != null) {
                throw new AssertionError("Original password verification failed");
            }
            var defaults = LoginUtils.getLoginDefaults(user, "", connection);
            String language = LoginUtils.getDefaultLanguage(connection, user);
            String rtl = LoginUtils.isDefaultRtl(connection, user);
            if (language == null || language.isBlank()) language = "en_US";
            var vars = new CapturingVariables(user, defaults.client, defaults.org, defaults.role, language);
            if (!LoginUtils.fillSessionArguments(connection, vars, user, language, rtl,
                    defaults.role, defaults.client, defaults.org, defaults.warehouse)) {
                throw new AssertionError("Original login session initialization failed");
            }
            var snapshot = new TreeMap<String, String>();
            for (String key : vars.keys) {
                if (key.equals("#CSRF_Token")) continue;
                snapshot.put(key, vars.getSessionValue(key));
            }
            if (vars.getSessionValue("#CSRF_Token").isBlank() || !"N".equals(vars.getSessionValue("#loggingIn"))) {
                throw new AssertionError("Session CSRF token or completion flag missing");
            }
            for (String[] denied : new String[][] {
                    {"ET27_NONEXISTENT_ROLE", defaults.client, defaults.org},
                    {defaults.role, "ET27_NONEXISTENT_CLIENT", defaults.org},
                    {defaults.role, defaults.client, "ET27_NONEXISTENT_ORG"}}) {
                String csrf = vars.getSessionValue("#CSRF_Token");
                var context = OBContext.getOBContext();
                if (LoginUtils.fillSessionArguments(connection, vars, user, language, rtl,
                        denied[0], denied[1], denied[2], defaults.warehouse)
                        || OBContext.getOBContext() != context || !csrf.equals(vars.getSessionValue("#CSRF_Token"))) {
                    throw new AssertionError("Invalid scope was accepted or mutated the established session");
                }
            }
            vars.setSessionValue("#Light_Login", "Y");
            var context = OBContext.getOBContext();
            String csrf = vars.getSessionValue("#CSRF_Token");
            if (!LoginUtils.fillSessionArguments(connection, vars, user, language, rtl,
                    defaults.role, defaults.client, defaults.org, defaults.warehouse)
                    || OBContext.getOBContext() != context || csrf.equals(vars.getSessionValue("#CSRF_Token"))) {
                throw new AssertionError("Same-scope light login did not preserve context and rotate CSRF");
            }
            StringBuilder report = new StringBuilder();
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            snapshot.forEach((key, value) -> report.append(key).append('=').append(
                    java.util.HexFormat.of().formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))))
                    .append('\n'));
            Path reportPath = Path.of(System.getProperty("validation.loginReport", "build/classic-login-session.txt"));
            Files.writeString(reportPath, report);
            Files.setPosixFilePermissions(reportPath, java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
            String baseline = System.getProperty("validation.loginBaseline");
            if (baseline != null && !Files.readString(Path.of(baseline)).equals(report.toString())) {
                throw new AssertionError("ERP session values differ from the original LoginUtils baseline");
            }
            if (Boolean.getBoolean("validation.profileContribution")) {
                var profile = Class.forName("org.openbravo.client.application.navigationbarcomponents.UserInfoComponent")
                        .getDeclaredConstructor().newInstance();
                if (!profile.getClass().getProtectionDomain().getCodeSource().getLocation().toString()
                        .endsWith("platform-ui-components.jar")
                        || !defaults.warehouse.equals(profile.getClass().getMethod("getContextWarehouseId").invoke(profile))) {
                    throw new AssertionError("Shared ERP profile changed warehouse context");
                }
                var support = org.openbravo.base.secureApp.LoginSessionSupport.getInstance();
                OBContext.setAdminMode();
                try {
                    var account = OBDal.getInstance().get(org.openbravo.model.ad.access.User.class, user);
                    var previous = account.getDefaultWarehouse();
                    try {
                        support.setUserDefaultWarehouse(account, null);
                        if (account.getDefaultWarehouse() != previous) throw new AssertionError("Null default changed user");
                        if (defaults.warehouse == null || defaults.warehouse.isEmpty()) throw new AssertionError("ERP fixture needs warehouse");
                        support.setUserDefaultWarehouse(account, defaults.warehouse);
                        var warehouse = account.getDefaultWarehouse();
                        if (warehouse == null || !defaults.warehouse.equals(warehouse.getId())) throw new AssertionError("ERP default assignment failed");
                        var options = support.getRoleWarehouseOptions(java.util.Set.of(warehouse.getOrganization().getId()), defaults.client);
                        if (options.stream().noneMatch(row -> defaults.warehouse.equals(row[0]))) throw new AssertionError("ERP warehouse missing from options");
                    } finally { account.setDefaultWarehouse(previous); }
                } finally { OBContext.restorePreviousMode(); }
                System.out.println("PASS: Shared ERP profile preserves warehouse context, default assignment and scoped options");
            }
            System.out.println("PASS: Original password, defaults, full/light session and invalid role/client/organization denial");
            if (baseline != null) System.out.println("PASS: All " + snapshot.size() + " deterministic login session values match original LoginUtils");
        } catch (Exception failure) {
            throw new IllegalStateException("Classic login validation failed", failure);
        } finally {
            if (SessionHandler.existsOpenedSessions()) OBDal.getInstance().rollbackAndClose();
            OBContext.setOBContext((OBContext) null);
            SessionHandler.deleteSessionHandler();
            org.openbravo.database.SessionInfo.init();
        }
    }

    private static final class CapturingVariables extends VariablesSecureApp {
        private final java.util.Set<String> keys = new java.util.TreeSet<>();

        CapturingVariables(String user, String client, String org, String role, String language) {
            super(user, client, org, role, language);
        }

        @Override
        public void setSessionValue(String key, String value) {
            super.setSessionValue(key, value);
            keys.add(key);
        }
    }
}
