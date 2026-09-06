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
        Path report = Path.of(dal ? "build/dal-mapping-result.txt" : security ? "build/security-generation-result.txt" : "build/dictionary-result.txt");
        Path generated = Path.of(security ? "build/security-generated-entities" : "build/generated-entities");
        Files.createDirectories(report.getParent());
        Files.writeString(report, "RUNNING\n");
        Path propertiesFile = null;
        boolean started = false;
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
            properties.setProperty("source.path", new File("build/generator-input").getAbsolutePath());
            OBPropertiesProvider.getInstance().setProperties(properties);
            var model = ModelProvider.getInstance().getModel();
            int expectedEntities = 2 + (Boolean.getBoolean("validation.security") ? SecurityFixture.TABLES.size() : 0);
            if (model.size() != expectedEntities) throw new AssertionError("Unexpected entity count: " + model.size());
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
                for (String type : new String[] {"org.openbravo.model.common.enterprise.Warehouse",
                        "org.openbravo.model.common.businesspartner.BusinessPartner",
                        "org.openbravo.model.ad.access.User"}) {
                    String java = Files.readString(generated.resolve(type.replace('.', '/') + ".java"));
                    if (!java.contains("extends BaseOBObject")) throw new AssertionError("Missing real base object: " + type);
                }
            }
            System.out.println("PASS: Real entity generator produced " + model.size() + " Java entity sources");
            if (dal) {
                Class.forName("com.etendoerp.platform.validation.DalMappingValidation").getMethod("verify").invoke(null);
            }
        } catch (Throwable failure) {
            Files.writeString(report, "FAIL\n" + failure.getClass().getName() + "\n");
            throw failure;
        } finally {
            source.close();
            if (propertiesFile != null) Files.deleteIfExists(propertiesFile);
            if (started) PersistenceValidation.command("docker", "stop", "--time", "2", container);
        }
        Files.writeString(report, "PASS\nReal ModelProvider: two related application entities\nReal Java entity source generation\n"
                + (security ? "Selected security entities, Warehouse and BusinessPartner generated from core metadata\n" : "")
                + (dal ? "Real DAL SessionFactory and generated mappings loaded; HQL entity query passed\n" : "")
                + (dal ? "NOT YET VERIFIED: OBDal persistence, populated security context, security filters, metadata upgrades\n"
                        : "NOT YET VERIFIED BY THIS TASK: generated entity compilation, DAL mappings, OBDal runtime, security filters, metadata upgrades\n"));
    }
}
