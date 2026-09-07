package com.etendoerp.platform.validation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Read-only readiness check for an externally installed Classic database. */
public final class ClassicDatabaseValidation {
    private ClassicDatabaseValidation() {}

    public static void main(String[] args) throws Exception {
        Path report = Path.of("build/classic-database-result.txt");
        Files.createDirectories(report.getParent());
        Files.writeString(report, "RUNNING\n");
        try {
            if (args.length != 1) throw new IllegalArgumentException("Supply classicProperties");
            Properties source = new Properties();
            try (var input = Files.newInputStream(Path.of(args[0]))) { source.load(input); }
            if (!"POSTGRE".equals(required(source, "bbdd.rdbms"))) {
                throw new IllegalArgumentException("Only PostgreSQL is supported");
            }
            String base = required(source, "bbdd.url");
            String database = required(source, "bbdd.sid");
            if (!base.matches("jdbc:postgresql://[A-Za-z0-9.\\-]+:[0-9]+")
                    || !database.matches("[A-Za-z0-9_\\-]+")) {
                throw new IllegalArgumentException("Expected PostgreSQL host:port and plain database name");
            }
            Properties connectionProperties = new Properties();
            connectionProperties.setProperty("user", required(source, "bbdd.user"));
            connectionProperties.setProperty("password", required(source, "bbdd.password"));
            connectionProperties.setProperty("connectTimeout", "5");
            connectionProperties.setProperty("socketTimeout", "15");
            connectionProperties.setProperty("options", "-c default_transaction_read_only=on -c statement_timeout=10000");
            try (var connection = DriverManager.getConnection(base + "/" + database, connectionProperties)) {
                connection.setReadOnly(true);
                connection.setAutoCommit(false);
                try (var statement = connection.createStatement()) {
                    try (var result = statement.executeQuery("show transaction_read_only")) {
                        if (!result.next() || !"on".equals(result.getString(1))) {
                            throw new IllegalStateException("Read-only transaction was not enabled");
                        }
                    }
                    try (var result = statement.executeQuery("select count(*) from ad_table where name='Product' and lower(tablename)='m_product' and isactive='Y'")) {
                        if (!result.next() || result.getInt(1) != 1) throw new IllegalStateException("Product dictionary entry is missing");
                    }
                    try (var result = statement.executeQuery("select count(distinct javapackage) from ad_module where javapackage in ('org.openbravo.service.json','org.openbravo.service.datasource') and isactive='Y'")) {
                        if (!result.next() || result.getInt(1) != 2) throw new IllegalStateException("JSON/datasource module registrations are missing");
                    }
                    long count;
                    try (var result = statement.executeQuery("select count(*) from m_product")) {
                        result.next();
                        count = result.getLong(1);
                    }
                    if (count == 0) throw new IllegalStateException("Product acceptance requires persisted products");
                    connection.rollback();
                    Files.writeString(report, "PASS\nRead-only Classic PostgreSQL connection\nProduct dictionary and JSON/datasource registrations present\nPersisted product count: " + count + "\nHTTP/module startup remains unverified\n");
                    System.out.println("PASS: Read-only Classic database preflight; persisted products: " + count);
                }
            }
        } catch (SQLException failure) {
            // Driver messages can contain connection details; expose only the SQLSTATE.
            Files.writeString(report, "FAIL\nDatabase preflight SQLSTATE: " + failure.getSQLState() + "\n");
            throw new IllegalStateException("Database preflight failed; SQLSTATE " + failure.getSQLState());
        } catch (Exception failure) {
            Files.writeString(report, "FAIL\n" + failure.getClass().getSimpleName() + "\n");
            throw failure;
        }
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank() || value.contains("${")) {
            throw new IllegalArgumentException("Missing or unresolved property: " + key);
        }
        return value;
    }
}
