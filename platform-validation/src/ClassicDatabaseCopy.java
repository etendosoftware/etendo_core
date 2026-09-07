package com.etendoerp.platform.validation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.DriverManager;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Creates an owned disposable Classic copy without writing to the source database. */
public final class ClassicDatabaseCopy {
    public static void main(String[] args) throws Exception {
        Properties source = new Properties();
        try (var input = Files.newInputStream(Path.of(args[0]))) { source.load(input); }
        String base = source.getProperty("bbdd.url"), database = source.getProperty("bbdd.sid");
        if (base == null || !base.matches("jdbc:postgresql://[A-Za-z0-9.\\-]+:[0-9]+")
                || database == null || !database.matches("[A-Za-z0-9_\\-]+")) throw new IllegalArgumentException("Expected PostgreSQL host:port and database name");
        var address = java.net.URI.create(base.substring(5));
        Path directory = Files.createTempDirectory(Path.of("build"), "classic-copy-",
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"))).toAbsolutePath();
        Path dump = directory.resolve("database.dump");
        String container = "etendo-platform-classic-copy-" + UUID.randomUUID();
        String password = UUID.randomUUID().toString();
        boolean started = false, success = false;
        try {
            command(directory, Map.of("PGPASSWORD", source.getProperty("bbdd.password"),
                            "PGOPTIONS", "-c default_transaction_read_only=on"),
                    "docker", "run", "--pull=never", "--rm", "-e", "PGPASSWORD", "-e", "PGOPTIONS",
                    "-v", directory + ":/backup", "postgres:16", "pg_dump", "--host",
                    LOOPBACK_HOSTS.contains(address.getHost()) ? "host.docker.internal" : address.getHost(),
                    "--port", Integer.toString(address.getPort()),
                    "--username", source.getProperty("bbdd.user"), "--dbname", database,
                    "--format=custom", "--no-owner", "--no-privileges", "--file", "/backup/database.dump");
            command(directory, Map.of("POSTGRES_PASSWORD", password), "docker", "run", "--pull=never", "--rm", "-d",
                    "--name", container, "--label", "etendo.platform.validation=true", "--label", "etendo.platform.classic-copy=true",
                    "-e", "POSTGRES_PASSWORD", "-e", "POSTGRES_DB=platform_switch", "-p", "127.0.0.1::5432", "postgres:16");
            started = true;
            String binding = command(directory, Map.of(), "docker", "port", container, "5432/tcp").strip();
            if (!binding.matches("127\\.0\\.0\\.1:[0-9]+")) throw new IllegalStateException("Unexpected copy binding");
            String port = binding.substring(binding.lastIndexOf(':') + 1);
            boolean ready = false;
            for (int attempt = 0; attempt < 30; attempt++) {
                try (var connection = DriverManager.getConnection("jdbc:postgresql://" + binding + "/platform_switch", "postgres", password)) {
                    ready = true; break;
                } catch (java.sql.SQLException pending) { Thread.sleep(500); }
            }
            if (!ready) throw new IllegalStateException("Copy database did not start");
            command(directory, Map.of("PGPASSWORD", password), "docker", "run", "--pull=never", "--rm", "-e", "PGPASSWORD",
                    "-v", directory + ":/backup:ro", "postgres:16", "pg_restore", "--host", "host.docker.internal", "--port", port,
                    "--username", "postgres", "--dbname", "platform_switch", "--no-owner", "--no-privileges", "--exit-on-error", "/backup/database.dump");
            Properties copy = new Properties();
            copy.putAll(source);
            copy.setProperty("bbdd.url", "jdbc:postgresql://" + binding);
            copy.setProperty("bbdd.sid", "platform_switch");
            copy.setProperty("bbdd.user", "postgres");
            copy.setProperty("bbdd.password", password);
            copy.setProperty("bbdd.systemUser", "postgres");
            copy.setProperty("bbdd.systemPassword", password);
            copy.setProperty("background.policy", "no-execute");
            copy.setProperty("hibernate.hbm2ddl.auto", "none");
            copy.setProperty("platform.validation.copyContainer", container);
            copy.setProperty("attach.path", Files.createDirectories(directory.resolve("attachments")).toString());
            Path config = directory.resolve("Openbravo.properties");
            Files.createFile(config, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            try (var writer = Files.newBufferedWriter(config)) { copy.store(writer, "Owned Classic lifecycle validation copy"); }
            try (var connection = DriverManager.getConnection("jdbc:postgresql://" + binding + "/platform_switch", "postgres", password);
                    var statement = connection.createStatement(); var rows = statement.executeQuery("select count(*) from m_product")) {
                rows.next();
                if (rows.getLong(1) == 0) throw new IllegalStateException("Restored copy contains no products");
                System.out.println("PASS: Classic copy restored; products: " + rows.getLong(1));
            }
            Files.writeString(Path.of("build/classic-copy-location.txt"), config + "\n" + container + "\n");
            System.out.println("Copy configuration: " + config);
            System.out.println("Owned container: " + container);
            success = true;
        } finally {
            Files.deleteIfExists(dump);
            if (started && !success) command(directory, Map.of(), "docker", "stop", "--time", "2", container);
        }
    }

    private static String command(Path directory, Map<String, String> environment, String... arguments) throws Exception {
        Path output = Files.createTempFile(directory, "command-", ".log");
        var builder = new ProcessBuilder(arguments).redirectErrorStream(true).redirectOutput(output.toFile());
        builder.environment().putAll(environment);
        Process process = builder.start();
        try {
            if (!process.waitFor(180, TimeUnit.SECONDS)) throw new IllegalStateException(arguments[0] + " timed out");
            if (process.exitValue() != 0) throw new IllegalStateException(arguments[0] + " failed; inspect " + output);
            return Files.readString(output);
        } finally { if (process.isAlive()) { process.destroyForcibly(); process.waitFor(5, TimeUnit.SECONDS); } }
    }

    private static final java.util.Set<String> LOOPBACK_HOSTS = java.util.Set.of("127.0.0.1", "localhost");
}
