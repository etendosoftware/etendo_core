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
        boolean dal = Boolean.getBoolean("validation.classicDal");
        Path report = Path.of(dal ? "build/classic-dal-result.txt" : "build/classic-model-result.txt");
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
            if (dal) {
                for (var entity : model) {
                    if (entity.isDataSourceBased() || entity.isHQLBased()) continue;
                    Class<?> type = entity.getMappingClass();
                    org.openbravo.base.provider.OBProvider.getInstance().register(type, type, false);
                    org.openbravo.base.provider.OBProvider.getInstance().register(entity.getName(), type, false);
                }
                SessionFactoryController.setRunningInWebContainer(true);
                SessionFactoryController.setInstance(new org.openbravo.dal.core.DalSessionFactoryController());
                var factory = SessionFactoryController.getInstance().getSessionFactory();
                try (var session = factory.openSession()) {
                    var transaction = session.beginTransaction();
                    try {
                        session.doWork(connection -> {
                            try (var statement = connection.createStatement();
                                    var result = statement.executeQuery("show transaction_read_only")) {
                                if (!result.next() || !"on".equals(result.getString(1))) {
                                    throw new IllegalStateException("DAL connection is not read-only");
                                }
                            }
                        });
                        Long products = session.createQuery("select count(p.id) from Product p", Long.class).uniqueResult();
                        if (products == null || products == 0) throw new IllegalStateException("Product HQL returned no records");
                        System.out.println("PASS: Actual Classic DAL Product HQL count: " + products);
                    } finally { transaction.rollback(); }
                }
                String callback = System.getProperty("validation.classicCallback");
                if (callback != null) ((Runnable) Class.forName(callback).getConstructor().newInstance()).run();
            }
            Files.writeString(report, "PASS\nReal Classic dictionary entities: " + model.size()
                    + "\nPersistent generated mappings resolved: " + persistent
                    + "\nProduct properties and generated mapping class resolved\n"
                    + (dal ? "Actual DAL SessionFactory and read-only Product HQL passed\nHTTP and role security remain unverified\n"
                            : "HTTP and DAL startup remain unverified\n"));
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
