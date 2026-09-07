package com.etendoerp.platform.validation;

import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.catalina.startup.Tomcat;

/** Runs the compatibility WAR independently of any existing Tomcat deployment. */
public final class CompatibilityServer {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("Expected WAR, external properties, and loopback port");
        System.setProperty("platform.validation.properties", Path.of(args[1]).toAbsolutePath().toString());
        Tomcat tomcat = new Tomcat();
        Path base = Files.createTempDirectory(Path.of("build"), "compat-tomcat-").toAbsolutePath();
        tomcat.setBaseDir(base.toString());
        tomcat.getHost().setAppBase(Files.createDirectories(base.resolve("webapps")).toString());
        tomcat.setPort(Integer.parseInt(args[2]));
        tomcat.getConnector().setProperty("address", "127.0.0.1");
        tomcat.getConnector().setProperty("maxThreads", "4");
        tomcat.getConnector().setProperty("minSpareThreads", "1");
        var context = tomcat.addWebapp("/etendo", Path.of(args[0]).toAbsolutePath().toString());
        context.setParentClassLoader(CompatibilityServer.class.getClassLoader());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { tomcat.stop(); tomcat.destroy(); }
            catch (Exception failure) { System.err.println("Tomcat shutdown failed: " + failure.getClass().getSimpleName()); }
        }));
        tomcat.start();
        if (!context.getState().isAvailable()) {
            tomcat.stop(); tomcat.destroy();
            throw new IllegalStateException("Compatibility WAR failed to start");
        }
        System.out.println("READY: http://127.0.0.1:" + tomcat.getConnector().getLocalPort()
                + "/etendo/org.openbravo.service.datasource/Product");
        tomcat.getServer().await();
    }
}
