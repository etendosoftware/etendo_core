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
                connection.rollback();
                Files.writeString(report, "PASS\nHTTP and independent JDBC agree on all " + expected.size()
                        + " test-client Product IDs, organizations, names and search keys\nForeign-client control products excluded: "
                        + foreign + "\nRestricted-organization and Classic lifecycle comparison remain unverified\n");
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
