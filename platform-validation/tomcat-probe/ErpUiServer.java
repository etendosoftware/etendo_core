package com.etendoerp.platform.validation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipFile;

/** Deploys ERP UI only against an explicitly owned disposable Classic copy. */
public final class ErpUiServer {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("Expected ERP WAR, copy properties and HTTP port");
        Properties configuration = new Properties();
        try (var input = Files.newInputStream(Path.of(args[1]))) { configuration.load(input); }
        String container = configuration.getProperty("platform.validation.copyContainer", "");
        if (!container.matches("etendo-platform-classic-copy-[a-f0-9-]{36}")) {
            throw new IllegalArgumentException("An owned Classic copy is required");
        }
        if (!"true true".equals(command("docker", "inspect", "--format",
                "{{.State.Running}} {{index .Config.Labels \"etendo.platform.classic-copy\"}}", container))) {
            throw new IllegalArgumentException("Classic copy is not running or lacks its ownership label");
        }
        String binding = command("docker", "port", container, "5432/tcp");
        if (!binding.matches("127\\.0\\.0\\.1:[0-9]+")
                || !configuration.getProperty("bbdd.url", "").equals("jdbc:postgresql://" + binding)
                || !configuration.getProperty("bbdd.sid", "").equals("platform_switch")) {
            throw new IllegalArgumentException("Configuration does not target the owned copy");
        }
        Path deployment = Files.createTempDirectory(Path.of("build"), "erp-ui-deployment-",
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"))).toAbsolutePath();
        try (var zip = new ZipFile(args[0])) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                Path target = deployment.resolve(entry.getName()).normalize();
                if (!target.startsWith(deployment)) throw new IllegalArgumentException("Unsafe WAR path");
                if (entry.isDirectory()) Files.createDirectories(target);
                else {
                    Files.createDirectories(target.getParent());
                    try (var input = zip.getInputStream(entry)) { Files.copy(input, target); }
                }
            }
        }
        configuration.setProperty("background.policy", "no-execute");
        configuration.setProperty("import.disable.process", "true");
        configuration.setProperty("cluster", "false");
        configuration.setProperty("redis.host", "");
        configuration.setProperty("redis.yaml", "");
        configuration.setProperty("hibernate.hbm2ddl.auto", "none");
        configuration.setProperty("attach.path", Files.createDirectories(deployment.resolve("attachments")).toString());
        Path privateProperties = deployment.resolve("WEB-INF/Openbravo.properties");
        Files.createFile(privateProperties, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        try (var output = Files.newOutputStream(privateProperties)) { configuration.store(output, "Owned ERP UI validation copy"); }
        Path descriptor = deployment.resolve("WEB-INF/web.xml");
        Files.writeString(descriptor, Files.readString(descriptor).replaceAll(
                "(<param-name>start-scheduler-on-load</param-name>\\s*<param-value>)true", "$1false"));
        System.out.println("ERP UI deployment prepared against verified owned Classic copy");
        // Use the validation console configuration, not a build-time rebuild appender.
        System.setProperty("log4j2.configurationFile", Path.of("fixtures/log4j2.xml").toAbsolutePath().toString());
        CompatibilityServer.main(new String[] {deployment.toString(), privateProperties.toString(), args[2], "/etendo"});
    }

    private static String command(String... arguments) throws Exception {
        Process process = new ProcessBuilder(arguments).redirectErrorStream(true).start();
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("Copy ownership verification timed out");
        }
        if (process.exitValue() != 0) throw new IllegalStateException("Copy ownership verification failed");
        return new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();
    }
}
