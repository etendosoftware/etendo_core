package com.etendoerp.platform.validation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.authentication.hashing.PasswordHash;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.session.SessionFactoryController;
import org.openbravo.client.application.CachedPreference;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.service.datasource.DefaultDataSourceService;
import org.openbravo.service.json.DefaultJsonDataService;

/** Exercises actual fetch implementations with an explicitly authenticated test identity. */
public final class ClassicJsonValidation implements Runnable {
    @Override
    public void run() {
        Path report = Path.of("build/classic-json-result.txt");
        try {
            Files.writeString(report, "RUNNING\n");
            String username = System.getenv("PLATFORM_TEST_USERNAME");
            String password = System.getenv("PLATFORM_TEST_PASSWORD");
            if (username == null || password == null) throw new IllegalStateException("Supply test credentials through environment variables");
            String[] identity = new String[4];
            try (var session = SessionFactoryController.getInstance().getSessionFactory().openSession()) {
                session.doWork(connection -> {
                    String sql = "select u.ad_user_id,u.password,r.ad_role_id,r.ad_client_id,ro.ad_org_id "
                            + "from ad_user u join ad_user_roles ur on ur.ad_user_id=u.ad_user_id "
                            + "join ad_role r on r.ad_role_id=ur.ad_role_id "
                            + "join ad_role_orgaccess ro on ro.ad_role_id=r.ad_role_id "
                            + "where u.username=? and u.isactive='Y' and ur.isactive='Y' and r.isactive='Y' and ro.isactive='Y' "
                            + "and exists(select 1 from m_product p where p.ad_client_id=r.ad_client_id and p.ad_org_id=ro.ad_org_id) "
                            + "order by r.ad_role_id,ro.ad_org_id limit 1";
                    try (var statement = connection.prepareStatement(sql)) {
                        statement.setString(1, username);
                        try (var row = statement.executeQuery()) {
                            if (!row.next() || !PasswordHash.matches(password, row.getString(2))) {
                                throw new IllegalStateException("Test authentication or product role resolution failed");
                            }
                            if (PasswordHash.matches(password + "-invalid", row.getString(2))) {
                                throw new IllegalStateException("Invalid password was accepted");
                            }
                            identity[0] = row.getString(1);
                            identity[1] = row.getString(3);
                            identity[2] = row.getString(4);
                            identity[3] = row.getString(5);
                        }
                    }
                });
            }
            OBContext.setOBContext(identity[0], identity[1], identity[2], identity[3], "en_US");
            var preferences = new CachedPreference();
            DefaultJsonDataService.setInstance(new DefaultJsonDataService(preferences, List.of()));
            var structures = new org.openbravo.client.application.window.ApplicationDictionaryCachedStructures();
            structures.init();
            var datasource = new DefaultDataSourceService(preferences, structures);
            datasource.setEntity(ModelProvider.getInstance().getEntity("Product"));
            var parameters = new HashMap<String, String>();
            parameters.put("_startRow", "0");
            parameters.put("_endRow", "10");
            parameters.put("_sortBy", "searchKey");
            parameters.put("windowId", "140");
            parameters.put("tabId", "180");
            parameters.put("moduleId", "0");
            parameters.put("_operationType", "fetch");
            parameters.put("_noCount", "true");
            parameters.put("_noActiveFilter", "true");
            parameters.put("_selectedProperties", "id,name,searchKey,client,organization,uOM,productCategory,taxCategory");
            JSONObject response = new JSONObject(datasource.fetch(parameters)).getJSONObject("response");
            if (response.getInt("status") != 0 || response.getJSONArray("data").length() == 0) {
                throw new IllegalStateException("Original Product fetch failed");
            }
            for (int index = 0; index < response.getJSONArray("data").length(); index++) {
                JSONObject product = response.getJSONArray("data").getJSONObject(index);
                if (!identity[2].equals(product.getString("client"))) throw new IllegalStateException("Foreign client returned");
                product.getString("_identifier");
                product.getString("uOM$_identifier");
            }
            Files.writeString(report, "PASS\nAuthenticated test identity\nOriginal datasource and JSON Product fetch\nReference identifiers and client checks passed\nHTTP and complete security acceptance remain unverified\n");
            System.out.println("PASS: Original Product datasource fetched " + response.getJSONArray("data").length() + " rows");
        } catch (Exception failure) {
            try { Files.writeString(report, "FAIL\n" + failure.getClass().getName() + "\n"); }
            catch (java.io.IOException suppressed) { failure.addSuppressed(suppressed); }
            throw new IllegalStateException("Classic JSON validation failed", failure);
        } finally {
            if (SessionHandler.existsOpenedSessions()) OBDal.getInstance().rollbackAndClose();
            OBContext.setOBContext((OBContext) null);
            SessionHandler.deleteSessionHandler();
            DefaultJsonDataService.setInstance(null);
            org.openbravo.database.SessionInfo.init();
        }
    }
}
