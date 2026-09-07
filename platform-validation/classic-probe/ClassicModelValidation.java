package com.etendoerp.platform.validation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.session.SessionFactoryController;
import org.openbravo.database.SessionInfo;

/** Resolves the installed dictionary against actual Classic-generated entity classes. */
public final class ClassicModelValidation {
    private ClassicModelValidation() {}

    public static void main(String[] args) throws Exception {
        Path report = Path.of("build/classic-model-result.txt");
        Files.createDirectories(report.getParent());
        Files.writeString(report, "RUNNING\n");
        Properties external = new Properties();
        try (var input = Files.newInputStream(Path.of(args[0]))) { external.load(input); }
        Properties safe = new Properties();
        for (String key : new String[] {"bbdd.rdbms", "bbdd.driver", "bbdd.url", "bbdd.sid", "bbdd.user", "bbdd.password"}) {
            safe.setProperty(key, external.getProperty(key));
        }
        if (!safe.getProperty("bbdd.sid").matches("[A-Za-z0-9_\\-]+")) {
            throw new IllegalArgumentException("Expected a plain database name");
        }
        safe.setProperty("bbdd.sid", safe.getProperty("bbdd.sid")
                + "?options=-c%20default_transaction_read_only%3Don&connectTimeout=5&socketTimeout=30");
        safe.setProperty("bbdd.sessionConfig", "select 1");
        safe.setProperty("hibernate.hbm2ddl.auto", "none");
        safe.setProperty("hibernate.connection.pool_size", "2");
        try {
            OBPropertiesProvider.getInstance().setProperties(safe);
            var model = ModelProvider.getInstance().getModel();
            if (Boolean.getBoolean("validation.generateClassic")) {
                Path privateProperties = Files.createTempFile(Path.of("build"), "classic-model-", ".properties",
                        java.nio.file.attribute.PosixFilePermissions.asFileAttribute(
                                java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")));
                try {
                    try (var writer = Files.newBufferedWriter(privateProperties)) { safe.store(writer, "Read-only generation"); }
                    var generator = new org.openbravo.base.gen.GenerateEntitiesTask();
                    generator.setBasePath(Path.of("build/generator-input").toAbsolutePath().toString());
                    generator.setSrcGenPath(Path.of("build/classic-generated-entities").toAbsolutePath().toString());
                    generator.setPropertiesFile(privateProperties.toAbsolutePath().toString());
                    generator.execute();
                    System.out.println("PASS: Generated actual Classic dictionary entities: " + model.size());
                } finally { Files.deleteIfExists(privateProperties); }
                return;
            }
            for (var entity : model) {
                // The real generator intentionally excludes virtual datasource/HQL entities.
                if (entity.isDataSourceBased() || entity.isHQLBased()) continue;
                if (!Files.exists(Path.of("build/classic-generated-entities",
                        entity.getClassName().replace('.', '/') + ".java"))) {
                    throw new IllegalStateException("Missing fresh generated source: " + entity.getClassName());
                }
                if (entity.getMappingClass() == null) {
                    throw new IllegalStateException("Missing generated class: " + entity.getClassName());
                }
            }
            var product = ModelProvider.getInstance().getEntity("Product");
            for (String property : new String[] {"id", "searchKey", "name", "client", "organization", "uOM", "productCategory", "taxCategory"}) {
                product.getProperty(property);
            }
            long persistent = model.stream().filter(entity -> !entity.isDataSourceBased() && !entity.isHQLBased()).count();
            Files.writeString(report, "PASS\nReal Classic dictionary entities: " + model.size()
                    + "\nPersistent generated mappings resolved: " + persistent
                    + "\nProduct properties and generated mapping class resolved\nHTTP and DAL startup remain unverified\n");
            System.out.println("PASS: Existing Classic dictionary and generated mapping classes: " + model.size());
        } catch (Throwable failure) {
            Files.writeString(report, "FAIL\n" + failure.getClass().getName() + "\n");
            throw failure;
        } finally {
            SessionFactoryController.setInstance(null);
            SessionInfo.init();
        }
    }
}
