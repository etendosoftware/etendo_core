package com.etendoerp.platform.validation;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.ddlutils.alteration.ModelComparator;
import org.apache.ddlutils.io.DatabaseDataIO;
import org.apache.ddlutils.io.DatabaseIO;
import org.apache.ddlutils.model.Database;
import org.apache.ddlutils.platform.ExcludeFilter;
import org.apache.ddlutils.platform.postgresql.PostgreSqlPlatform;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/** Real PostgreSQL component probe; deliberately does not claim OBDal compatibility. */
public final class PersistenceValidation {
    private static final List<String> PASSED = new ArrayList<>();

    private PersistenceValidation() {}

    public static void main(String[] args) throws Exception {
        Path report = Path.of("build", "validation-result.txt");
        Files.createDirectories(report.getParent());
        Files.writeString(report, "RUNNING\n", StandardCharsets.UTF_8);
        String container = "etendo-platform-proof-" + UUID.randomUUID();
        String password = UUID.randomUUID().toString();
        boolean started = false;
        try {
            absent("org.openbravo.dal.service.OBDal");
            absent("org.openbravo.model.common.plm.Product");
            pass("No Etendo runtime or ERP entity classes on the classpath");
            command("docker", "run", "--pull=never", "--rm", "-d", "--name", container,
                    "--label", "etendo.platform.validation=true",
                    "-e", "POSTGRES_PASSWORD=" + password, "-e", "POSTGRES_DB=platform_proof",
                    "-p", "127.0.0.1::5432", "--tmpfs", "/var/lib/postgresql/data", "postgres:16");
            started = true;
            String binding = command("docker", "port", container, "5432/tcp").strip();
            if (!binding.matches("127\\.0\\.0\\.1:[0-9]+")) {
                throw new IllegalStateException("Unexpected database binding");
            }
            String url = "jdbc:postgresql://" + binding + "/platform_proof";
            awaitDatabase(url, password);
            BasicDataSource source = new BasicDataSource();
            try {
                source.setDriverClassName("org.postgresql.Driver");
                source.setUrl(url);
                source.setUsername("postgres");
                source.setPassword(password);
                source.setMaxActive(4);
                verify(source, url, password);
            } finally {
                source.close();
            }
        } catch (Throwable failure) {
            Files.writeString(report, "FAIL\n" + String.join("\n", PASSED) + "\nFailure: "
                    + failure.getClass().getName() + "\n", StandardCharsets.UTF_8);
            throw failure;
        } finally {
            if (started) {
                command("docker", "stop", "--time", "2", container);
            }
        }
        Files.writeString(report, "PASS\n" + String.join("\n", PASSED)
                + "\nNOT TESTED: OBDal, dictionary, generated mappings, full update.database,"
                + " module installation, tenant filters, custom Etendo dialect.\n", StandardCharsets.UTF_8);
        System.out.println("PASS: " + PASSED.size() + " checks; disposable PostgreSQL removed.");
    }

    private static void verify(BasicDataSource source, String url, String password) throws Exception {
        PostgreSqlPlatform platform = new PostgreSqlPlatform();
        platform.setDataSource(source);
        platform.setMaxThreads(1);
        DatabaseIO xml = new DatabaseIO();
        xml.setValidateXml(false);
        Database v1 = xml.read(new File("fixtures/v1.xml"));
        Database v2 = xml.read(new File("fixtures/v2.xml"));
        check(platform.createTables(v1, false, false), "DBSM schema creation failed");
        platform.enableNOTNULLColumns(v1);
        check(platform.createAllFKs(v1, false), "DBSM foreign-key activation failed");
        check(scalar(source, "select count(*) from information_schema.tables where table_schema='public'") == 2,
                "Expected exactly two non-ERP tables");
        pass("DBSM created two related tables from XML in an empty PostgreSQL database");

        DatabaseDataIO data = new DatabaseDataIO();
        data.setFailOnError(true);
        data.writeDataToDatabase(platform, v1,
                new String[] {new File("fixtures/categories.xml").toURI().toString()});
        check(scalar(source, "select count(*) from pp_category where id='GENERAL' and name='General requests'") == 1,
                "Managed XML data was not imported");
        pass("DBSM imported managed reference data from XML");

        try (SessionFactory factory = sessions(url, password, false)) {
            try (var session = factory.openSession()) {
                var tx = session.beginTransaction();
                Category category = session.find(Category.class, "GENERAL");
                session.persist(new Request("REQUEST_1", "Library access", category));
                tx.commit();
            }
            try (var session = factory.openSession()) {
                List<Request> found = session.createQuery(
                        "select r from ProofRequest r join fetch r.category c where c.name = :name order by r.id",
                        Request.class).setParameter("name", "General requests").setMaxResults(10).getResultList();
                check(found.size() == 1 && "Library access".equals(found.get(0).title), "HQL relationship query failed");
            }
            pass("Hibernate 6 / Jakarta persisted and queried a relationship using parameterized HQL");
            try (var session = factory.openSession()) {
                var tx = session.beginTransaction();
                session.persist(new Request("ROLLED_BACK", "Not committed", session.find(Category.class, "GENERAL")));
                session.flush();
                tx.rollback();
            }
            check(scalar(source, "select count(*) from pp_request where id='ROLLED_BACK'") == 0,
                    "Rollback did not preserve the database");
            pass("A flushed Hibernate insert was rolled back");
        }

        try (Connection connection = source.getConnection(); var statement = connection.createStatement()) {
            try {
                statement.executeUpdate("insert into pp_request(id,title,category_id) values ('INVALID','Invalid','MISSING')");
                throw new AssertionError("Foreign key was not enforced");
            } catch (SQLException expected) {
                check("23503".equals(expected.getSQLState()), "Unexpected constraint failure");
            }
        }
        pass("PostgreSQL enforced the XML-defined foreign key");

        Database actual = platform.loadModelFromDatabase(new ExcludeFilter());
        platform.alterTables(actual, v2, false);
        check(scalar(source, "select count(*) from information_schema.columns where table_name='pp_request' and column_name='description'") == 1,
                "DBSM did not apply the new XML column");
        check(scalar(source, "select count(*) from pp_request where id='REQUEST_1' and title='Library access'") == 1,
                "Operational data was lost during upgrade");
        pass("DBSM applied XML v2 and preserved existing operational data");

        try (SessionFactory factory = sessions(url, password, true); var session = factory.openSession()) {
            var tx = session.beginTransaction();
            RequestV2 request = session.find(RequestV2.class, "REQUEST_1");
            request.description = "Available after schema upgrade";
            tx.commit();
        }
        check(scalar(source, "select count(*) from pp_request where description='Available after schema upgrade'") == 1,
                "Hibernate could not use the upgraded column");
        pass("Hibernate validated and used the upgraded schema without automatic DDL");

        Database afterUpgrade = platform.loadModelFromDatabase(new ExcludeFilter());
        var changes = new ModelComparator(platform.getPlatformInfo(), false).compare(afterUpgrade, v2);
        check(changes.isEmpty(), "Schema still differs from XML: " + changes);
        platform.alterTables(afterUpgrade, v2, false);
        check(scalar(source, "select count(*) from pp_request") == 1, "Repeated schema update changed operational rows");
        pass("DBSM detected no further schema changes and a repeated schema update preserved data");
    }

    private static SessionFactory sessions(String url, String password, boolean upgraded) {
        Configuration config = new Configuration();
        config.setProperty("hibernate.connection.url", url);
        config.setProperty("hibernate.connection.username", "postgres");
        config.setProperty("hibernate.connection.password", password);
        config.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver");
        config.setProperty("hibernate.hbm2ddl.auto", "validate");
        config.setProperty("hibernate.show_sql", "false");
        config.addAnnotatedClass(Category.class);
        config.addAnnotatedClass(upgraded ? RequestV2.class : Request.class);
        return config.buildSessionFactory();
    }

    private static long scalar(BasicDataSource source, String sql) throws SQLException {
        try (var connection = source.getConnection(); var statement = connection.createStatement();
                var result = statement.executeQuery(sql)) {
            check(result.next(), "Query returned no row");
            return result.getLong(1);
        }
    }

    private static void awaitDatabase(String url, String password) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        while (System.nanoTime() < deadline) {
            try (var connection = DriverManager.getConnection(url, "postgres", password)) {
                return;
            } catch (SQLException starting) {
                Thread.sleep(250);
            }
        }
        throw new IllegalStateException("Disposable PostgreSQL did not start within 30 seconds");
    }

    private static String command(String... args) throws Exception {
        Process process = new ProcessBuilder(args).redirectErrorStream(true).start();
        if (!process.waitFor(45, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("Container operation timed out");
        }
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (process.exitValue() != 0) {
            throw new IllegalStateException("Container operation failed: " + output);
        }
        return output;
    }

    private static void absent(String name) throws Exception {
        try {
            Class.forName(name);
            throw new AssertionError("Unexpected ERP runtime class: " + name);
        } catch (ClassNotFoundException expected) {
            // Absence is part of this component boundary check.
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void pass(String message) {
        PASSED.add(message);
        System.out.println("PASS: " + message);
    }

    @Entity(name = "ProofCategory") @Table(name = "pp_category")
    public static class Category {
        @Id @Column(length = 32) public String id;
        @Column(nullable = false, length = 100) public String name;
        public Category() {}
    }

    @Entity(name = "ProofRequest") @Table(name = "pp_request")
    public static class Request {
        @Id @Column(length = 32) public String id;
        @Column(nullable = false, length = 100) public String title;
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "category_id", nullable = false) public Category category;
        public Request() {}
        public Request(String id, String title, Category category) {
            this.id = id;
            this.title = title;
            this.category = category;
        }
    }

    @Entity(name = "ProofRequestV2") @Table(name = "pp_request")
    public static class RequestV2 {
        @Id @Column(length = 32) public String id;
        @Column(nullable = false, length = 100) public String title;
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "category_id", nullable = false) public Category category;
        @Column(length = 255) public String description;
        public RequestV2() {}
    }
}
