package com.etendoerp.platform.validation;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.Base64;
import java.util.HashSet;
import java.util.Properties;
import org.codehaus.jettison.json.JSONObject;

/** Compares the full test-client HTTP Product page with independent read-only JDBC rows. */
public final class ProductDatabaseValidation {
    public static void main(String[] args) throws Exception {
        Path report = Path.of("build/product-database-result.txt");
        Files.createDirectories(report.getParent());
        Files.writeString(report, "RUNNING\n");
        try {
            URI endpoint = URI.create(args[1]);
            if (!"127.0.0.1".equals(endpoint.getHost()) || !"http".equals(endpoint.getScheme())) {
                throw new IllegalArgumentException("Loopback HTTP required");
            }
            String username = System.getenv("PLATFORM_TEST_USERNAME"), password = System.getenv("PLATFORM_TEST_PASSWORD");
            if (username == null || password == null) throw new IllegalArgumentException("Supply test credentials in environment variables");
            String auth = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
            var request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Basic " + auth)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("_operationType=fetch&windowId=140&tabId=180&_startRow=0&_endRow=100"
                            + "&_sortBy=searchKey&_noActiveFilter=true&_selectedProperties=id,client,organization,name,searchKey")).build();
            var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new AssertionError("Product HTTP request failed");
            String client = response.headers().firstValue("X-Platform-Client").orElseThrow();
            var json = new JSONObject(response.body()).getJSONObject("response");
            if (json.getInt("status") != 0) throw new AssertionError("Datasource returned an error");
            var data = json.getJSONArray("data");
            Properties external = new Properties();
            try (var input = Files.newInputStream(Path.of(args[0]))) { external.load(input); }
            String base = external.getProperty("bbdd.url"), database = external.getProperty("bbdd.sid");
            if (base == null || !base.matches("jdbc:postgresql://[A-Za-z0-9.\\-]+:[0-9]+")
                    || database == null || !database.matches("[A-Za-z0-9_\\-]+")) throw new IllegalArgumentException("Invalid database address");
            Properties connectionProperties = new Properties();
            connectionProperties.setProperty("user", external.getProperty("bbdd.user"));
            connectionProperties.setProperty("password", external.getProperty("bbdd.password"));
            connectionProperties.setProperty("options", "-c default_transaction_read_only=on -c statement_timeout=10000");
            connectionProperties.setProperty("connectTimeout", "5");
            try (var connection = DriverManager.getConnection(base + "/" + database, connectionProperties)) {
                connection.setReadOnly(true);
                connection.setAutoCommit(false);
                var expected = new java.util.HashMap<String, String[]>();
                try (var statement = connection.prepareStatement("select m_product_id,ad_org_id,name,value from m_product where ad_client_id=?")) {
                    statement.setString(1, client);
                    try (var rows = statement.executeQuery()) {
                        while (rows.next()) expected.put(rows.getString(1), new String[] {rows.getString(2), rows.getString(3), rows.getString(4)});
                    }
                }
                if (expected.isEmpty() || expected.size() > 101) throw new AssertionError("This gate requires a test client with 1..101 products");
                var actualIds = new HashSet<String>();
                for (int i = 0; i < data.length(); i++) {
                    var row = data.getJSONObject(i);
                    String id = row.getString("id");
                    String[] sql = expected.get(id);
                    if (!actualIds.add(id) || sql == null || !client.equals(row.getString("client"))
                            || !sql[0].equals(row.getString("organization")) || !sql[1].equals(row.getString("name"))
                            || !sql[2].equals(row.getString("searchKey"))) throw new AssertionError("HTTP and JDBC Product values differ");
                }
                if (!actualIds.equals(expected.keySet())) throw new AssertionError("HTTP ID set differs from the complete test-client Product set");
                int foreign;
                try (var statement = connection.prepareStatement("select count(*) from m_product where ad_client_id<>?")) {
                    statement.setString(1, client);
                    try (var rows = statement.executeQuery()) { rows.next(); foreign = rows.getInt(1); }
                }
                if (foreign == 0) throw new AssertionError("Missing foreign-client control products");
                int restrictedChecks = 0;
                String rolesSql = "select distinct r.ad_role_id,ro.ad_org_id from ad_user u "
                        + "join ad_user_roles ur on ur.ad_user_id=u.ad_user_id join ad_role r on r.ad_role_id=ur.ad_role_id "
                        + "join ad_role_orgaccess ro on ro.ad_role_id=r.ad_role_id join ad_org o on o.ad_org_id=ro.ad_org_id "
                        + "where u.username=? and ur.isactive='Y' and r.isactive='Y' and ro.isactive='Y' "
                        + "and o.isactive='Y' and r.ad_client_id=? order by r.ad_role_id,ro.ad_org_id";
                var checkedRoles = new HashSet<String>();
                try (var roles = connection.prepareStatement(rolesSql)) {
                    roles.setString(1, username); roles.setString(2, client);
                    try (var contexts = roles.executeQuery()) {
                        while (contexts.next()) {
                            String role = contexts.getString(1), org = contexts.getString(2);
                            if (!checkedRoles.add(role)) continue;
                            var expectedRoleIds = new HashSet<String>();
                            String expectedSql = "with recursive grants(id) as (select ro.ad_org_id from ad_role_orgaccess ro "
                                    + "join ad_org o on o.ad_org_id=ro.ad_org_id where ro.ad_role_id=? and ro.isactive='Y' and o.isactive='Y'), "
                                    + "tree as (select n.node_id,n.parent_id from ad_treenode n join ad_clientinfo c on c.ad_tree_org_id=n.ad_tree_id where c.ad_client_id=?), "
                                    + "downward(id) as (select id from grants union select t.node_id from tree t join downward d on t.parent_id=d.id), "
                                    + "upward(id) as (select id from grants union select t.parent_id from tree t join upward u on t.node_id=u.id) "
                                    + "select p.m_product_id from m_product p where p.ad_client_id=? and "
                                    + "(exists(select 1 from grants where id='0') or p.ad_org_id='0' or p.ad_org_id in (select id from downward union select id from upward))";
                            try (var statement = connection.prepareStatement(expectedSql)) {
                                statement.setString(1, role); statement.setString(2, client); statement.setString(3, client);
                                try (var rows = statement.executeQuery()) { while (rows.next()) expectedRoleIds.add(rows.getString(1)); }
                            }
                            if (expectedRoleIds.isEmpty() || expectedRoleIds.size() >= expected.size()) continue;
                            var restrictedRequest = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(30))
                                    .header("Authorization", "Basic " + auth).header("X-Platform-Role", role)
                                    .header("X-Platform-Organization", org).header("Content-Type", "application/x-www-form-urlencoded")
                                    .POST(HttpRequest.BodyPublishers.ofString("_operationType=fetch&windowId=140&tabId=180&_startRow=0&_endRow=100"
                                            + "&_sortBy=searchKey&_noActiveFilter=true&_selectedProperties=id,client,organization")).build();
                            var restrictedResponse = HttpClient.newHttpClient().send(restrictedRequest, HttpResponse.BodyHandlers.ofString());
                            if (restrictedResponse.statusCode() == 403) continue; // Organization grants do not imply table access.
                            if (restrictedResponse.statusCode() != 200) throw new AssertionError("Restricted context request failed");
                            var restrictedJson = new JSONObject(restrictedResponse.body()).getJSONObject("response");
                            if (restrictedJson.getInt("status") != 0) throw new AssertionError("Restricted datasource returned an error");
                            var actualRoleIds = new HashSet<String>();
                            var restrictedData = restrictedJson.getJSONArray("data");
                            for (int i = 0; i < restrictedData.length(); i++) actualRoleIds.add(restrictedData.getJSONObject(i).getString("id"));
                            if (!actualRoleIds.equals(expectedRoleIds)) throw new AssertionError("Restricted organization HTTP IDs differ from independent grants/tree query");
                            restrictedChecks++;
                            System.out.println("PASS: Restricted organization context; visible products: " + actualRoleIds.size()
                                    + "; excluded same-client products: " + (expected.size() - actualRoleIds.size()));
                        }
                    }
                }
                if (restrictedChecks == 0) throw new AssertionError("No existing restricted role was verified; an authorized test context is required");
                var restored = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
                if (restored.statusCode() != 200) throw new AssertionError("Administrator context was not restored");
                var restoredIds = new HashSet<String>();
                var restoredData = new JSONObject(restored.body()).getJSONObject("response").getJSONArray("data");
                for (int i = 0; i < restoredData.length(); i++) restoredIds.add(restoredData.getJSONObject(i).getString("id"));
                if (!restoredIds.equals(expected.keySet())) throw new AssertionError("Restricted requests leaked into administrator context");
                connection.rollback();
                Files.writeString(report, "PASS\nHTTP and independent JDBC agree on all " + expected.size()
                        + " test-client Product IDs, organizations, names and search keys\nForeign-client control products excluded: "
                        + foreign + "\nRestricted-organization contexts verified: " + restrictedChecks
                        + "\nAdministrator context restored after restricted requests\nClassic lifecycle comparison remains unverified\n");
                System.out.println("PASS: HTTP/JDBC Product equality; client rows: " + expected.size() + "; excluded foreign rows: " + foreign);
            }
        } catch (java.sql.SQLException failure) {
            Files.writeString(report, "FAIL\nSQLSTATE " + failure.getSQLState() + "\n");
            throw new IllegalStateException("Database comparison failed; SQLSTATE " + failure.getSQLState());
        } catch (Exception | AssertionError failure) {
            Files.writeString(report, "FAIL\n" + failure.getClass().getName() + "\n");
            throw failure;
        }
    }
}
