package com.etendoerp.platform.compat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.openbravo.authentication.hashing.PasswordHash;
import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.session.SessionFactoryController;
import org.openbravo.client.application.CachedPreference;
import org.openbravo.client.application.window.ApplicationDictionaryCachedStructures;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.service.datasource.DefaultDataSourceService;
import org.openbravo.service.json.DefaultJsonDataService;

/** Loopback validation adapter for the original Product datasource fetch contract. */
public final class ProductServlet extends HttpServlet {
    private static final Set<String> PARAMETERS = Set.of("_startRow", "_endRow", "_sortBy", "_selectedProperties",
            "windowId", "tabId", "moduleId", "_operationType", "_noCount", "_noActiveFilter", "_textMatchStyle",
            "isImplicitFilterApplied", "sendOriginalIDBack", "_extraProperties", "Constants_FIELDSEPARATOR",
            "_className", "Constants_IDENTIFIER", "_componentId", "_dataSource", "isc_metaDataPrefix", "isc_dataFormat");

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Platform-Runtime", "platform-core");
        try {
            if (!Set.of("127.0.0.1", "0:0:0:0:0:0:0:1", "::1").contains(request.getRemoteAddr())) {
                response.sendError(403); return;
            }
            String[] identity = authenticate(request.getHeader("Authorization"));
            if (identity == null) {
                response.setHeader("WWW-Authenticate", "Basic realm=\"platform-validation\", charset=\"UTF-8\"");
                response.sendError(401); return;
            }
            if (!Set.of("GET", "POST").contains(request.getMethod())) { response.sendError(405); return; }
            var parameters = new HashMap<String, String>();
            for (var entry : request.getParameterMap().entrySet()) {
                if (entry.getValue().length != 1) { response.sendError(400, "Duplicate parameter"); return; }
                String name = entry.getKey(), value = entry.getValue()[0];
                if (name.startsWith("@Product.") && name.endsWith("@") && "null".equals(value)) continue;
                if (!PARAMETERS.contains(name)) { response.sendError(400, "Unsupported parameter: " + name); return; }
                parameters.put(name, value);
            }
            if (!"fetch".equals(parameters.getOrDefault("_operationType", "fetch"))) { response.sendError(405); return; }
            int start = Integer.parseInt(parameters.getOrDefault("_startRow", "0"));
            int end = Integer.parseInt(parameters.getOrDefault("_endRow", "100"));
            if (start < 0 || end < start || end - start > 100 || end > 100000) { response.sendError(400); return; }
            parameters.put("_startRow", Integer.toString(start));
            parameters.put("_endRow", Integer.toString(end));
            if (!"140".equals(parameters.getOrDefault("windowId", "140"))
                    || !"180".equals(parameters.getOrDefault("tabId", "180"))) { response.sendError(400); return; }
            OBContext.setOBContext(identity[0], identity[1], identity[2], identity[3], "en_US");
            var entity = ModelProvider.getInstance().getEntity("Product");
            OBContext.getOBContext().getEntityAccessChecker().checkReadable(entity);
            var preferences = new CachedPreference();
            var structures = new ApplicationDictionaryCachedStructures();
            structures.init();
            var datasource = new DefaultDataSourceService(preferences, structures,
                    new DefaultJsonDataService(preferences, List.of()));
            datasource.setEntity(entity);
            String result = datasource.fetch(parameters);
            OBDal.getInstance().rollbackAndClose();
            response.getWriter().write(result);
        } catch (OBSecurityException failure) {
            response.sendError(403);
        } catch (IllegalArgumentException failure) {
            response.sendError(400);
        } catch (Exception failure) {
            getServletContext().log("Product fetch failed", failure);
            response.sendError(500);
        } finally {
            try { if (SessionHandler.existsOpenedSessions()) OBDal.getInstance().rollbackAndClose(); }
            finally {
                OBContext.setOBContext((OBContext) null);
                SessionHandler.deleteSessionHandler();
                org.openbravo.database.SessionInfo.init();
            }
        }
    }

    private String[] authenticate(String authorization) {
        if (authorization == null || !authorization.startsWith("Basic ") || authorization.length() > 2048) return null;
        String credentials;
        try { credentials = new String(Base64.getDecoder().decode(authorization.substring(6)), StandardCharsets.UTF_8); }
        catch (IllegalArgumentException invalid) { return null; }
        int separator = credentials.indexOf(':');
        if (separator <= 0) return null;
        String[] identity = new String[4];
        try (var session = SessionFactoryController.getInstance().getSessionFactory().openSession()) {
            session.doWork(connection -> {
                String sql = "select u.ad_user_id,u.password,r.ad_role_id,r.ad_client_id,ro.ad_org_id "
                        + "from ad_user u join ad_user_roles ur on ur.ad_user_id=u.ad_user_id "
                        + "join ad_role r on r.ad_role_id=ur.ad_role_id join ad_role_orgaccess ro on ro.ad_role_id=r.ad_role_id "
                        + "where u.username=? and u.isactive='Y' and u.islocked='N' and ur.isactive='Y' "
                        + "and r.isactive='Y' and ro.isactive='Y' "
                        + "and exists(select 1 from m_product p where p.ad_client_id=r.ad_client_id and p.ad_org_id=ro.ad_org_id) "
                        + "order by r.ad_role_id,ro.ad_org_id limit 1";
                try (var statement = connection.prepareStatement(sql)) {
                    statement.setString(1, credentials.substring(0, separator));
                    try (var row = statement.executeQuery()) {
                        if (!row.next() || row.getString(2) == null
                                || !PasswordHash.matches(credentials.substring(separator + 1), row.getString(2))) return;
                        identity[0] = row.getString(1); identity[1] = row.getString(3);
                        identity[2] = row.getString(4); identity[3] = row.getString(5);
                    }
                }
            });
        }
        return identity[0] == null ? null : identity;
    }
}
