package com.etendoerp.platform.validation;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.apache.commons.dbcp.BasicDataSource;

/** Keeps the disposable database alive while the isolated Tomcat process verifies its WAR. */
final class TomcatValidation {
    static void verify(Path properties, BasicDataSource source) throws Exception {
        Process child = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin/java").toString(),
                "-Duser.language=en", "-Duser.country=US",
                "--add-opens=java.base/java.io=ALL-UNNAMED", "--add-opens=java.base/java.lang=ALL-UNNAMED",
                "--add-opens=java.rmi/sun.rmi.transport=ALL-UNNAMED",
                "-cp", System.getProperty("validation.tomcatClasspath"), "com.etendoerp.platform.validation.TomcatProbe",
                new File(Boolean.getBoolean("validation.ui") ? "build/libs/platform-ui.war" : "build/libs/platform-validation.war").getAbsolutePath(),
                properties.toAbsolutePath().toString(), Boolean.toString(Boolean.getBoolean("validation.ui")))
                .redirectErrorStream(true).redirectOutput(new File("build/tomcat-http.log")).start();
        try {
            if (!child.waitFor(55, TimeUnit.SECONDS)) throw new AssertionError("Tomcat verification timed out; inspect build/tomcat-http.log");
            if (child.exitValue() != 0) throw new AssertionError("Tomcat verification failed; inspect build/tomcat-http.log");
        } finally {
            if (child.isAlive()) { child.destroyForcibly(); child.waitFor(5, TimeUnit.SECONDS); }
        }
        String serverLog = java.nio.file.Files.readString(Path.of("build/tomcat-http.log"));
        if (serverLog.contains("SEVERE") || serverLog.contains("forcibly unregistered")
                || serverLog.contains("failed to remove") || serverLog.contains("could not find a logging provider")) {
            throw new AssertionError("Tomcat lifecycle errors or resource leaks; inspect build/tomcat-http.log");
        }
        try (var connection = source.getConnection(); var statement = connection.createStatement();
                var result = statement.executeQuery("select count(*) from pp_request where title='Created through HTTP' and ad_client_id='C1' and ad_org_id='O1'")) {
            if (!result.next() || result.getInt(1) != 1) throw new AssertionError("HTTP did not persist to PostgreSQL");
        }
        System.out.println("PASS: Actual WAR deployed in Tomcat, HTTP checks and redeployment passed; PostgreSQL persistence verified");
    }
}
