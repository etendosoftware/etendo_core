package com.etendoerp.platform.validation;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.HashSet;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

/** Black-box checks against the separately running Product compatibility WAR. */
public final class ProductHttpValidation {
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final String PARAMETERS = "_operationType=fetch&windowId=140&tabId=180&moduleId=0"
            + "&_sortBy=searchKey&_noCount=true&_noActiveFilter=true"
            + "&_selectedProperties=id,name,searchKey,client,organization,uOM,productCategory,taxCategory";

    public static void main(String[] args) throws Exception {
        Path report = Path.of("build/product-http-result.txt");
        Files.createDirectories(report.getParent());
        Files.writeString(report, "RUNNING\n");
        String user = System.getenv("PLATFORM_TEST_USERNAME"), password = System.getenv("PLATFORM_TEST_PASSWORD");
        if (user == null || password == null) throw new IllegalArgumentException("Supply test credentials in environment variables");
        URI uri = URI.create(args[0]);
        if (!"127.0.0.1".equals(uri.getHost()) || !"http".equals(uri.getScheme())
                || !"/etendo/org.openbravo.service.datasource/Product".equals(uri.getPath())) {
            throw new IllegalArgumentException("Expected exact Product path on loopback HTTP");
        }
        String auth = "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
        try {
            expect(send(uri, null, PARAMETERS), 401);
            expect(send(uri, "Basic invalid", PARAMETERS), 401);
            String invalid = "Basic " + Base64.getEncoder().encodeToString((user + ":" + password + "-invalid").getBytes(StandardCharsets.UTF_8));
            expect(send(uri, invalid, PARAMETERS), 401);
            var full = send(uri, auth, PARAMETERS + "&_startRow=0&_endRow=100");
            expect(full, 200);
            if (!"platform-core".equals(full.headers().firstValue("X-Platform-Runtime").orElse(""))) {
                throw new AssertionError("Response did not come from platform runtime");
            }
            JSONArray all = data(full);
            String role = full.headers().firstValue("X-Platform-Role").orElseThrow();
            String organization = full.headers().firstValue("X-Platform-Organization").orElseThrow();
            var explicit = send(uri, auth, PARAMETERS + "&_startRow=0&_endRow=100", role, organization);
            if (data(explicit).length() != all.length()) throw new AssertionError("Explicit context differs from selected context");
            expect(send(uri, auth, PARAMETERS, "UNAUTHORIZEDROLE", organization), 403);
            expect(send(uri, auth, PARAMETERS, role, "UNAUTHORIZEDORG"), 403);
            if (all.length() == 0 || all.length() > 101) throw new AssertionError("Invalid result page size");
            var ids = new HashSet<String>();
            String client = all.getJSONObject(0).getString("client");
            String previous = "";
            for (int i = 0; i < all.length(); i++) {
                JSONObject row = all.getJSONObject(i);
                if (!ids.add(row.getString("id"))) throw new AssertionError("Duplicate Product ID");
                if (!client.equals(row.getString("client"))) throw new AssertionError("Multiple clients returned");
                String key = row.getString("searchKey");
                if (previous.compareTo(key) > 0) throw new AssertionError("Unexpected searchKey order for test data");
                previous = key;
                for (String reference : new String[] {"_identifier", "uOM$_identifier", "productCategory$_identifier", "taxCategory$_identifier"}) {
                    if (row.getString(reference).isBlank()) throw new AssertionError("Missing reference identifier");
                }
            }
            JSONArray page = data(send(uri, auth, PARAMETERS + "&_startRow=0&_endRow=4"));
            JSONArray next = data(send(uri, auth, PARAMETERS + "&_startRow=5&_endRow=9"));
            if (page.length() != 5 || next.length() != 5 || all.length() < 10) throw new AssertionError("Pagination fixture requires ten products");
            for (int i = 0; i < 5; i++) {
                if (!page.getJSONObject(i).getString("id").equals(all.getJSONObject(i).getString("id"))
                        || !next.getJSONObject(i).getString("id").equals(all.getJSONObject(i + 5).getString("id"))) {
                    throw new AssertionError("Page results differ from full fetch");
                }
            }
            expect(send(uri, auth, "_operationType=remove"), 405);
            expect(send(uri, auth, "_where=1%3D1"), 400);
            expect(send(uri, auth, "windowId=999&tabId=180"), 400);
            expect(send(uri, auth, "_startRow=-1"), 400);
            expect(send(uri, null, PARAMETERS), 401);
            JSONArray after = data(send(uri, auth, PARAMETERS + "&_startRow=0&_endRow=100"));
            if (after.length() != all.length()) throw new AssertionError("Rejected requests changed results");
            Files.writeString(report, "PASS\nExact Product URL on actual Tomcat\nHTTP authentication and invalid password rejection\n"
                    + "Original JSON fetch, selected fields, reference identifiers, ordering and two pages\n"
                    + "Explicit context retained; unassigned role and organization rejected\n"
                    + "Unsupported parameters and mutations rejected\nReturned products: " + all.length()
                    + "\nCross-role/organization security and Classic mode comparison remain unverified\n");
            System.out.println("PASS: Product HTTP contract checks; returned products: " + all.length());
        } catch (Exception | AssertionError failure) {
            Files.writeString(report, "FAIL\n" + failure.getClass().getName() + "\n");
            throw failure;
        }
    }

    private static JSONArray data(HttpResponse<String> result) throws Exception {
        expect(result, 200);
        JSONObject response = new JSONObject(result.body()).getJSONObject("response");
        if (response.getInt("status") != 0) throw new AssertionError("Datasource returned an error envelope");
        return response.getJSONArray("data");
    }

    private static HttpResponse<String> send(URI uri, String auth, String body) throws Exception {
        return send(uri, auth, body, null, null);
    }

    private static HttpResponse<String> send(URI uri, String auth, String body, String role, String organization) throws Exception {
        var request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        if (auth != null) request.header("Authorization", auth);
        if (role != null) request.header("X-Platform-Role", role);
        if (organization != null) request.header("X-Platform-Organization", organization);
        return CLIENT.send(request.POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private static void expect(HttpResponse<String> response, int status) {
        if (response.statusCode() != status) throw new AssertionError("Expected HTTP " + status + ", received " + response.statusCode());
    }
}
