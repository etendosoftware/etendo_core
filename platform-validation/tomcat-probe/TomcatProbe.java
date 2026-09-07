package com.etendoerp.platform.validation;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;
import java.util.zip.ZipFile;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.util.ServerInfo;

/** Deploys the actual WAR to an isolated Tomcat instance and exercises it through TCP HTTP. */
public final class TomcatProbe {
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
            .version(HttpClient.Version.HTTP_1_1).build();

    public static void main(String[] args) throws Exception {
        Path war = Path.of(args[0]).toAbsolutePath();
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(Path.of(args[1]))) { properties.load(reader); }
        System.setProperty("platform.validation.properties", Path.of(args[1]).toAbsolutePath().toString());
        try (ZipFile zip = new ZipFile(war.toFile())) {
            if (zip.getEntry("WEB-INF/classes/com/etendoerp/platform/web/RequestServlet.class") == null
                    || zip.getEntry("WEB-INF/classes/com/etendoerp/platform/validation/DalMappingValidation.class") != null
                    || zip.stream().anyMatch(entry -> entry.getName().contains("jakarta.servlet-api"))) {
                throw new AssertionError("WAR packaging boundary failed");
            }
        }
        String token = properties.getProperty("platform.validation.token");
        String readOnly = properties.getProperty("platform.validation.readOnlyToken");
        String expected = null;
        for (int iteration = 0; iteration < 2; iteration++) {
            Tomcat tomcat = new Tomcat();
            Path base = Files.createTempDirectory(Path.of("build"), "tomcat-").toAbsolutePath();
            tomcat.setBaseDir(base.toString());
            tomcat.getHost().setAppBase(Files.createDirectories(base.resolve("webapps")).toString());
            tomcat.setPort(0);
            tomcat.getConnector().setProperty("address", "127.0.0.1");
            tomcat.getConnector().setProperty("maxThreads", "1");
            tomcat.getConnector().setProperty("minSpareThreads", "1");
            var context = tomcat.addWebapp("/platform", war.toString());
            context.setParentClassLoader(TomcatProbe.class.getClassLoader());
            try {
                tomcat.start();
                if (!context.getState().isAvailable()) throw new AssertionError("WAR failed deployment");
                String url = "http://127.0.0.1:" + tomcat.getConnector().getLocalPort() + "/platform/requests";
                expect(send(url, "GET", null), 401);
                expect(send(url, "GET", "invalid"), 401);
                if (iteration == 0) {
                    var created = send(url + "?title=Created%20through%20HTTP", "POST", token);
                    expect(created, 201);
                    expected = created.body();
                    if (!expected.matches("[0-9a-f]{32}\\tCreated through HTTP\\n")) throw new AssertionError("Unexpected created response");
                }
                var listed = send(url + "?title=Created%20through%20HTTP", "GET", token);
                expect(listed, 200);
                if (!expected.equals(listed.body())) throw new AssertionError("HTTP query lost persisted record");
                var all = send(url, "GET", token);
                expect(all, 200);
                if (!expected.equals(all.body())) throw new AssertionError("HTTP tenant/organization filters leaked control rows");
                expect(send(url + "?title=Forbidden", "POST", readOnly), 403);
                var readable = send(url, "GET", readOnly);
                expect(readable, 200);
                if (!expected.equals(readable.body())) throw new AssertionError("Read-only response differs");
                expect(send(url + "?title=Invalid%0AInput", "POST", token), 400);
                expect(send(url, "GET", null), 401);
                var afterFailure = send(url, "GET", token);
                expect(afterFailure, 200);
                if (!expected.equals(afterFailure.body())) throw new AssertionError("Denied request leaked a transaction or context");
                System.out.println("PASS: WAR HTTP persistence, HQL, isolation and authorization on " + ServerInfo.getServerInfo()
                        + (iteration == 0 ? "" : " after redeployment"));
            } finally {
                try { tomcat.stop(); } finally { tomcat.destroy(); }
            }
        }
    }

    private static HttpResponse<String> send(String url, String method, String token) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20));
        if (token != null) request.header("Authorization", "Bearer " + token);
        return CLIENT.send(request.method(method, HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
    }

    private static void expect(HttpResponse<String> response, int status) {
        if (response.statusCode() != status) throw new AssertionError("Expected HTTP " + status + ", got " + response.statusCode() + ": " + response.body());
    }
}
