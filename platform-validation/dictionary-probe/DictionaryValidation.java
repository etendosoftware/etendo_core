package com.etendoerp.platform.validation;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Properties;
import java.util.UUID;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.ddlutils.io.DatabaseDataIO;
import org.apache.ddlutils.io.DatabaseIO;
import org.apache.ddlutils.alteration.ModelComparator;
import org.apache.ddlutils.platform.ExcludeFilter;
import org.apache.ddlutils.platform.postgresql.PostgreSqlPlatform;
import org.openbravo.base.gen.GenerateEntitiesTask;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.session.OBPropertiesProvider;

/** Exercises the real dictionary model and generators with no ERP business metadata. */
public final class DictionaryValidation {
    private DictionaryValidation() {}

    public static void main(String[] args) throws Exception {
        String container = "etendo-platform-dictionary-" + UUID.randomUUID();
        String password = UUID.randomUUID().toString();
        boolean security = Boolean.getBoolean("validation.security");
        boolean dal = Boolean.getBoolean("validation.dal");
        boolean obdal = Boolean.getBoolean("validation.obdal");
        boolean upgrade = Boolean.getBoolean("validation.upgrade");
        boolean tomcat = Boolean.getBoolean("validation.tomcat");
        boolean originalUi = Boolean.getBoolean("validation.originalUi");
        Path report = Path.of(tomcat ? "build/tomcat-result.txt" : upgrade ? "build/platform-result.txt" : obdal ? "build/obdal-result.txt" : dal ? "build/dal-mapping-result.txt" : security ? "build/security-generation-result.txt" : "build/dictionary-result.txt");
        if (originalUi) report = Path.of("build/ui-dictionary-result.txt");
        Path generated = Path.of(originalUi ? "build/ui-generated-entities"
                : security ? "build/security-generated-entities" : "build/generated-entities");
        Files.createDirectories(report.getParent());
        Files.writeString(report, "RUNNING\n");
        Path propertiesFile = null;
        boolean started = false;
        boolean retain = false;
        BasicDataSource source = new BasicDataSource();
        try {
            PersistenceValidation.command("docker", "run", "--pull=never", "--rm", "-d", "--name", container,
                    "--label", "etendo.platform.validation=true", "-e", "POSTGRES_PASSWORD=" + password,
                    "-e", "POSTGRES_DB=platform_proof", "-p", "127.0.0.1::5432",
                    "--tmpfs", "/var/lib/postgresql/data", "postgres:16");
            started = true;
            String binding = PersistenceValidation.command("docker", "port", container, "5432/tcp").strip();
            if (!binding.matches("127\\.0\\.0\\.1:[0-9]+")) throw new IllegalStateException("Unexpected binding");
            String baseUrl = "jdbc:postgresql://" + binding;
            PersistenceValidation.awaitDatabase(baseUrl + "/platform_proof", password);
            source.setDriverClassName("org.postgresql.Driver");
            source.setUrl(baseUrl + "/platform_proof");
            source.setUsername("postgres");
            source.setPassword(password);
            source.setMaxActive(4);
            PostgreSqlPlatform platform = new PostgreSqlPlatform();
            platform.setDataSource(source);
            platform.setMaxThreads(1);
            DatabaseIO xml = new DatabaseIO();
            xml.setValidateXml(false);
            var schema = DictionaryFixture.schema(xml);
            if (!platform.createTables(schema, false, false)) throw new AssertionError("Dictionary schema failed");
            platform.enableNOTNULLColumns(schema);
            if (!platform.createAllFKs(schema, false)) throw new AssertionError("Foreign keys failed");
            var actualSchema = platform.loadModelFromDatabase(new ExcludeFilter());
            var differences = new ModelComparator(platform.getPlatformInfo(), false).compare(actualSchema, schema);
            if (!differences.isEmpty()) throw new AssertionError("Dictionary schema differs from XML: " + differences);
            var data = new DatabaseDataIO();
            data.setFailOnError(true);
            data.writeDataToDatabase(platform, schema, new String[] {DictionaryFixture.data(schema).toUri().toString()});
            if (originalUi) {
                try (var connection = source.getConnection()) { UiDictionaryFixture.verify(connection); }
            }

            Properties properties = new Properties();
            properties.setProperty("bbdd.rdbms", "POSTGRE");
            properties.setProperty("bbdd.driver", "org.postgresql.Driver");
            properties.setProperty("bbdd.url", baseUrl);
            properties.setProperty("bbdd.sid", "platform_proof");
            properties.setProperty("bbdd.user", "postgres");
            properties.setProperty("bbdd.password", password);
            properties.setProperty("bbdd.sessionConfig", "select 1");
            // Existing dictionary mappings read NUMERIC metadata into Integer properties.
            // DBSM validates physical types above; Hibernate must never change this schema.
            properties.setProperty("hibernate.hbm2ddl.auto", "none");
            if (tomcat) {
                properties.setProperty("platform.validation.token", UUID.randomUUID().toString());
                properties.setProperty("platform.validation.readOnlyToken", UUID.randomUUID().toString());
            }
            if (Boolean.getBoolean("validation.obdal")) {
                properties.setProperty("dal.security.tableAccessOnly", "true");
                data.writeDataToDatabase(platform, schema, new String[] {SecurityFixture.securityData(schema).toUri().toString()});
            }
            properties.setProperty("source.path", new File("build/generator-input").getAbsolutePath());
            OBPropertiesProvider.getInstance().setProperties(properties);
            var model = ModelProvider.getInstance().getModel();
            if (originalUi) {
                for (String[] link : new String[][] {{"ADField", "tab", "ADTab"},
                        {"ADField", "column", "ADColumn"}, {"ADTab", "window", "ADWindow"},
                        {"ADTab", "table", "ADTable"}}) {
                    var target = ModelProvider.getInstance().getEntity(link[0]).getProperty(link[1]).getTargetEntity();
                    if (target == null || !target.getName().equals(link[2])) {
                        throw new AssertionError("Original UI model relationship failed: " + java.util.Arrays.toString(link));
                    }
                }
                if (model.stream().anyMatch(entity -> java.util.Set.of("Product", "Warehouse", "BusinessPartner")
                        .contains(entity.getName()))) throw new AssertionError("ERP entity leaked into original UI model");
                System.out.println("PASS: Original UI model resolves Field/Column/Tab/Window relationships without ERP entities");
            }
            int expectedEntities = 2 + (security ? SecurityFixture.selectedTables().size() : 0);
            if (model.size() != expectedEntities) throw new AssertionError("Unexpected entity count: " + model.size());
            if (security && model.stream().anyMatch(entity -> "OBUIAPP_Process".equals(entity.getName()))) {
                throw new AssertionError("Minimal security must not require the UI Process entity");
            }
            if (security && (model.stream().anyMatch(entity -> "BusinessPartner".equals(entity.getName()))
                    || ModelProvider.getInstance().getEntity("ADUser").hasProperty("businessPartner"))) {
                throw new AssertionError("Minimal security must not require the ERP business-partner association");
            }
            var request = ModelProvider.getInstance().getEntity("ProofRequest");
            if (!request.getProperty("title").getColumnName().equals("TITLE")) {
                throw new AssertionError("Dictionary property naming failed");
            }
            if (!request.getProperty("category").getTargetEntity().getName().equals("ProofCategory")) {
                throw new AssertionError("Dictionary relationship was not resolved");
            }
            System.out.println("PASS: Real ModelProvider resolved two related entities from PostgreSQL metadata");

            propertiesFile = Files.createTempFile(Path.of("build"), "dictionary-", ".properties",
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            try (var writer = Files.newBufferedWriter(propertiesFile)) {
                properties.store(writer, "Disposable database configuration");
            }
            GenerateEntitiesTask generate = new GenerateEntitiesTask();
            generate.setBasePath(new File("build/generator-input").getAbsolutePath());
            generate.setSrcGenPath(generated.toAbsolutePath().toString());
            generate.setPropertiesFile(propertiesFile.toAbsolutePath().toString());
            // This directory is exclusively an ignored output of this probe. Fresh generation
            // avoids the full-ERP incremental timestamp query against absent metadata kinds.
            if (Files.exists(generated)) {
                try (var paths = Files.walk(generated)) {
                    for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
                }
            }
            generate.execute();
            for (var entity : model) {
                Path java = generated.resolve(entity.getClassName().replace('.', '/') + ".java");
                if (!Files.exists(java)) {
                    throw new AssertionError("Entity generator did not produce " + entity.getClassName());
                }
            }
            if (security) {
                for (String type : new String[] {"org.openbravo.model.ad.access.User"}) {
                    String java = Files.readString(generated.resolve(type.replace('.', '/') + ".java"));
                    if (!java.contains("extends BaseOBObject")) throw new AssertionError("Missing real base object: " + type);
                }
            }
            System.out.println("PASS: Real entity generator produced " + model.size() + " Java entity sources");
            if (dal) {
                Class.forName("com.etendoerp.platform.validation.DalMappingValidation").getMethod("verify").invoke(null);
            }
            if (upgrade) UpgradeValidation.verify(platform, source, schema, propertiesFile);
            if (tomcat) TomcatValidation.verify(propertiesFile, source);
            if (tomcat && Boolean.getBoolean("validation.keepDatabase")) {
                Files.writeString(Path.of("build/platform-instance-location.txt"),
                        propertiesFile.toAbsolutePath() + "\n" + container + "\n");
                System.out.println("Retained private configuration: " + propertiesFile.toAbsolutePath());
                System.out.println("Retained owned database container: " + container);
                retain = true;
            }
        } catch (Throwable failure) {
            Files.writeString(report, "FAIL\n" + failure.getClass().getName() + "\n");
            throw failure;
        } finally {
            try {
                source.close();
            } finally {
                try {
                    if (propertiesFile != null && !retain) Files.deleteIfExists(propertiesFile);
                } finally {
                    if (started && !retain) PersistenceValidation.command("docker", "stop", "--time", "2", container);
                }
            }
        }
        Files.writeString(report, "PASS\nReal ModelProvider: two related application entities\nReal Java entity source generation\n"
                + (originalUi ? "Original window/tab/field metadata and model relationships verified without ERP entities\n"
                        + "NOT YET VERIFIED: original UI startup, generated UI entity compilation or UI-driven CRUD\n" : "")
                + (security ? "Selected security entities generated without Warehouse, BusinessPartner or UI Process\n" : "")
                + (dal ? "Real DAL SessionFactory and generated mappings loaded; HQL entity query passed\n" : "")
                + (tomcat ? "Actual WAR deployment in isolated Tomcat: HTTP persistence, HQL, security and redeployment passed\n"
                        : upgrade ? "Complete generated DAL v1/v2 lifecycle: persistence, security, constraints, XML schema and managed data, preservation and idempotence\n"
                        : obdal ? "Real non-admin context; OBDal persistence, filters, rollback and restricted table grants passed\n"
                        + "NOT YET VERIFIED: runtime metadata upgrades, full update.database and module installation\n"
                        : dal ? "NOT YET VERIFIED: OBDal persistence, populated security context, security filters, metadata upgrades\n"
                        : "NOT YET VERIFIED BY THIS TASK: generated entity compilation, DAL mappings, OBDal runtime, security filters, metadata upgrades\n"));
    }
}
