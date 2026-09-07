package com.etendoerp.platform.validation;

import org.openbravo.authentication.hashing.PasswordHash;
import org.openbravo.base.secureApp.LoginSessionSupport;
import org.openbravo.base.secureApp.LoginUtils;
import org.openbravo.base.secureApp.PlatformLoginSessionSupport;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.openbravo.service.db.DalConnectionProvider;

/** Original login service integration on a disposable ERP-free dictionary, not a browser login. */
public final class FullLoginValidation implements Runnable {
    @Override
    public void run() {
        var original = OBContext.getOBContext();
        try {
            OBPropertiesProvider.getInstance().getOpenbravoProperties().setProperty(
                    LoginSessionSupport.IMPLEMENTATION_PROPERTY, PlatformLoginSessionSupport.class.getName());
            OBPropertiesProvider.getInstance().getOpenbravoProperties().setProperty("login.trial.user.lock", "1");
            OBPropertiesProvider.getInstance().getOpenbravoProperties().setProperty("ui.userInfo.accessPolicy.class",
                    "org.openbravo.client.application.navigationbarcomponents.PlatformUserInfoAccessPolicy");
            var dal = OBDal.getInstance();
            var user = dal.get(User.class, "U1");
            String password = java.util.UUID.randomUUID().toString();
            user.setUsername("platform-login-validation");
            user.setPassword(PasswordHash.generateHash(password));
            user.setDefaultRole(original.getRole());
            user.setDefaultClient(original.getCurrentClient());
            user.setDefaultOrganization(original.getCurrentOrganization());
            user.setDefaultLanguage(original.getLanguage());
            dal.flush();
            var connection = new DalConnectionProvider(false);
            if (!"U1".equals(LoginUtils.getValidUserId(connection, user.getUsername(), password))
                    || LoginUtils.getValidUserId(connection, user.getUsername(), password + "-invalid") != null) {
                throw new AssertionError("Original platform credential validation failed");
            }
            if (!user.isLocked() || LoginUtils.getValidUserId(connection, user.getUsername(), password) != null) {
                throw new AssertionError("Original user-lock policy was not enforced");
            }
            user.setLocked(false);
            dal.flush();
            var defaults = LoginUtils.getLoginDefaults("U1", "", connection);
            if (!"R1".equals(defaults.role) || !"C1".equals(defaults.client) || !"O1".equals(defaults.org)
                    || !"".equals(defaults.warehouse)) throw new AssertionError("Platform login defaults failed");
            var vars = new VariablesSecureApp("U1", "C1", "O1", "R1", "en_US");
            if (!LoginUtils.fillSessionArguments(connection, vars, "U1", "en_US", "N", defaults.role,
                    defaults.client, defaults.org, defaults.warehouse)) throw new AssertionError("Full platform session failed");
            if (!"N".equals(vars.getSessionValue("#loggingIn")) || vars.getSessionValue("#CSRF_Token").isBlank()) {
                throw new AssertionError("Platform session is not initialized");
            }
            for (var expected : java.util.Map.of("#AD_User_ID", "U1", "#AD_Role_ID", "R1",
                    "#AD_Client_ID", "C1", "#AD_Org_ID", "O1", "#AD_Language", "en_US").entrySet()) {
                if (!expected.getValue().equals(vars.getSessionValue(expected.getKey()))) {
                    throw new AssertionError("Incorrect original session scope: " + expected.getKey());
                }
            }
            if (!vars.getSessionValue("#M_Warehouse_ID").isEmpty()) {
                throw new AssertionError("Platform session acquired an ERP warehouse");
            }
            var authorizedContext = OBContext.getOBContext();
            var profile = Class.forName("org.openbravo.client.application.navigationbarcomponents.UserInfoComponent")
                    .getDeclaredConstructor().newInstance();
            if (!profile.getClass().getProtectionDomain().getCodeSource().getLocation().toString()
                    .endsWith("platform-ui-components.jar")
                    || !"".equals(profile.getClass().getMethod("getContextWarehouseId").invoke(profile))
                    || !"R1".equals(profile.getClass().getMethod("getContextRoleId").invoke(profile))
                    || !"O1".equals(profile.getClass().getMethod("getContextOrganizationId").invoke(profile))) {
                throw new AssertionError("Shared original profile did not resolve the platform session");
            }
            String csrf = vars.getSessionValue("#CSRF_Token");
            var profileRoles = (java.util.List<?>) profile.getClass().getMethod("getUserRolesInfo").invoke(profile);
            var profileRoleIds = new java.util.HashSet<String>();
            for (Object entry : profileRoles) {
                profileRoleIds.add((String) entry.getClass().getMethod("getRoleId").invoke(entry));
            }
            if (!profileRoleIds.equals(java.util.Set.of("R1", "R_READ", "R_EXCLUDE", "R_INACTIVE"))) {
                throw new AssertionError("Original profile role membership changed: " + profileRoleIds);
            }
            var restrictedRole = dal.get(org.openbravo.model.ad.access.Role.class, "R_INACTIVE");
            var assignment = dal.get(org.openbravo.model.ad.access.UserRoles.class, "R_INACTIVE");
            try {
                restrictedRole.setRestrictbackend(true);
                dal.flush();
                assertRoleHidden(profile.getClass(), "R_INACTIVE");
                restrictedRole.setRestrictbackend(false);
                restrictedRole.setActive(false);
                dal.flush();
                assertRoleHidden(profile.getClass(), "R_INACTIVE");
                restrictedRole.setActive(true);
                assignment.setActive(false);
                dal.flush();
                assertRoleHidden(profile.getClass(), "R_INACTIVE");
            } finally {
                restrictedRole.setRestrictbackend(false);
                restrictedRole.setActive(true);
                assignment.setActive(true);
                dal.flush();
            }
            System.out.println("PASS: Original profile excludes backend-restricted/inactive roles and inactive assignments without ERP policy");
            var roleInfo = Class.forName("org.openbravo.client.application.navigationbarcomponents.RoleInfo")
                    .getConstructor(Object[].class).newInstance((Object) new Object[] {"R1", "Role", "C1", "Client"});
            var organizations = (java.util.Map<?, ?>) roleInfo.getClass().getMethod("getOrganizations").invoke(roleInfo);
            var warehouses = (java.util.Map<?, ?>) roleInfo.getClass().getMethod("getOrganizationWarehouses").invoke(roleInfo);
            if (organizations.isEmpty() || !organizations.keySet().equals(warehouses.keySet())
                    || warehouses.values().stream().anyMatch(value -> !((java.util.List<?>) value).isEmpty())) {
                throw new AssertionError("Original platform profile did not retain organizations without warehouses");
            }
            for (String[] scope : new String[][] {{"missing-role", "C1", "O1"},
                    {"R1", "C2", "O1"}, {"R1", "C1", "missing-org"}}) {
                if (LoginUtils.fillSessionArguments(connection, vars, "U1", "en_US", "N",
                        scope[0], scope[1], scope[2], "")
                        || OBContext.getOBContext() != authorizedContext
                        || !csrf.equals(vars.getSessionValue("#CSRF_Token"))
                        || !"R1".equals(vars.getSessionValue("#AD_Role_ID"))
                        || !"C1".equals(vars.getSessionValue("#AD_Client_ID"))
                        || !"O1".equals(vars.getSessionValue("#AD_Org_ID"))) {
                    throw new AssertionError("Denied login scope mutated the authorized session");
                }
            }
            System.out.println("PASS: Original platform login rejects invalid role/client/organization without session mutation");
            System.out.println("PASS: Original password, defaults and full login session execute on the ERP-free dictionary");
        } catch (Exception failure) {
            throw new IllegalStateException("Original platform login service failed", failure);
        } finally {
            OBContext.setOBContext(original);
        }
    }

    private static void assertRoleHidden(Class<?> profileType, String roleId) throws Exception {
        Object fresh = profileType.getDeclaredConstructor().newInstance();
        var entries = (java.util.List<?>) profileType.getMethod("getUserRolesInfo").invoke(fresh);
        for (Object entry : entries) {
            if (roleId.equals(entry.getClass().getMethod("getRoleId").invoke(entry))) {
                throw new AssertionError("Profile exposed a restricted role: " + roleId);
            }
        }
        if (entries.size() != 3) throw new AssertionError("Profile lost unrelated authorized roles");
    }
}
