package com.etendoerp.platform.validation;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import javax.tools.ToolProvider;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.ddlutils.alteration.DataComparator;
import org.apache.ddlutils.alteration.ModelComparator;
import org.apache.ddlutils.io.DataToArraySink;
import org.apache.ddlutils.io.DatabaseDataIO;
import org.apache.ddlutils.io.DatabaseIO;
import org.apache.ddlutils.model.Database;
import org.apache.ddlutils.model.DatabaseData;
import org.apache.ddlutils.platform.ExcludeFilter;
import org.apache.ddlutils.platform.postgresql.PostgreSqlPlatform;
import org.openbravo.base.gen.GenerateEntitiesTask;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.ddlutils.util.OBDataset;
import org.openbravo.ddlutils.util.OBDatasetTable;

/** Runs old and upgraded generated classes in separate JVMs against one disposable database. */
final class UpgradeValidation {
    private UpgradeValidation() {}

    static void verify(PostgreSqlPlatform platform, BasicDataSource source, Database initial,
            Path properties) throws Exception {
        String runtime = System.getProperty("validation.dalClasspath");
        if (runtime == null || runtime.isBlank()) throw new IllegalArgumentException("Missing DAL classpath");
        child(runtime, properties, "v1", "build/platform-v1.log");
        constraints(source);
        String operational = rows(source, "PP_REQUEST", true);
        DatabaseIO xml = new DatabaseIO();
        xml.setValidateXml(false);
        Database target = (Database) initial.clone();
        var description = xml.read(new File("fixtures/v2.xml")).findTable("PP_REQUEST").findColumn("DESCRIPTION");
        target.findTable("PP_REQUEST").addColumn((org.apache.ddlutils.model.Column) description.clone());
        xml.write(target, new File("build/platform-v2-schema.xml"));
        target = xml.read(new File("build/platform-v2-schema.xml"));
        var before = platform.loadModelFromDatabase(new ExcludeFilter());
        if (new ModelComparator(platform.getPlatformInfo(), false).compare(before, target).size() != 1) {
            throw new AssertionError("Expected exactly one additive XML schema change");
        }
        platform.alterTables(before, target, false);
        if (!operational.equals(rows(source, "PP_REQUEST", true))) throw new AssertionError("Schema upgrade changed operational data");

        // Own only the added metadata row; do not reconcile unrelated dictionary or tenant rows.
        Path dictionary = DictionaryFixture.data(target);
        List<String> metadataIds = List.of(DictionaryFixture.columnId("PP_REQUEST", "DESCRIPTION"));
        reconcile(platform, target, dictionary, "AD_COLUMN", "AD_COLUMN_ID", metadataIds, 1);
        StringBuilder managed = new StringBuilder("<?xml version=\"1.0\"?><data>\n");
        for (String id : List.of("GENERAL", "IT")) {
            DictionaryFixture.row(managed, target, "PP_CATEGORY", Map.of("ID", id,
                    "NAME", id.equals("GENERAL") ? "General service requests" : "IT support",
                    "AD_CLIENT_ID", "C1", "AD_ORG_ID", "O1", "ISACTIVE", "Y"));
        }
        managed.append("</data>\n");
        Path managedFile = Path.of("build/platform-v2-data.xml");
        Files.writeString(managedFile, managed);
        String unmanaged = localCategories(source);
        reconcile(platform, target, managedFile, "PP_CATEGORY", "ID", List.of("GENERAL", "IT"), 2);
        if (!unmanaged.equals(localCategories(source)) || !operational.equals(rows(source, "PP_REQUEST", true))) {
            throw new AssertionError("Managed update changed unowned or operational rows");
        }
        System.out.println("PASS: DBSM upgraded XML schema, dictionary and managed data while preserving operational/local rows");

        ModelProvider.refresh();
        Path generated = Path.of("build/upgraded-entities");
        clearGenerated(generated);
        GenerateEntitiesTask generator = new GenerateEntitiesTask();
        generator.setBasePath(new File("build/generator-input").getAbsolutePath());
        generator.setSrcGenPath(generated.toAbsolutePath().toString());
        generator.setPropertiesFile(properties.toAbsolutePath().toString());
        generator.execute();
        Path classes = Path.of("build/upgraded-classes");
        clearGenerated(classes);
        Files.createDirectories(classes);
        List<String> compilerArgs = new ArrayList<>(List.of("-classpath", runtime, "-d", classes.toString(), "--release", "17"));
        try (var files = Files.walk(generated)) {
            files.filter(path -> path.toString().endsWith(".java")).sorted().forEach(path -> compilerArgs.add(path.toString()));
        }
        if (ToolProvider.getSystemJavaCompiler().run(null, null, null, compilerArgs.toArray(String[]::new)) != 0) {
            throw new AssertionError("Actual v2 generated entities did not compile");
        }
        String upgradedRuntime = classes.toAbsolutePath() + File.pathSeparator + runtime;
        child(upgradedRuntime, properties, "v2", "build/platform-v2.log");
        if (!operational.equals(rows(source, "PP_REQUEST", true)) || !unmanaged.equals(localCategories(source))) {
            throw new AssertionError("DAL restart changed original fields or unmanaged categories");
        }
        Map<String, String> snapshot = snapshot(source, target);
        var current = platform.loadModelFromDatabase(new ExcludeFilter());
        if (!new ModelComparator(platform.getPlatformInfo(), false).compare(current, target).isEmpty()) {
            throw new AssertionError("Upgraded database differs from target XML");
        }
        platform.alterTables(current, target, false);
        reconcile(platform, target, dictionary, "AD_COLUMN", "AD_COLUMN_ID", metadataIds, 0);
        reconcile(platform, target, managedFile, "PP_CATEGORY", "ID", List.of("GENERAL", "IT"), 0);
        child(upgradedRuntime, properties, "v2", "build/platform-v2-repeat.log");
        if (!snapshot.equals(snapshot(source, target))) throw new AssertionError("Repeated update changed database contents");
        if (!new ModelComparator(platform.getPlatformInfo(), false)
                .compare(platform.loadModelFromDatabase(new ExcludeFilter()), target).isEmpty()) {
            throw new AssertionError("Repeated update changed the physical schema");
        }
        constraints(source);
        System.out.println("PASS: Generated DAL v2 restarted; repeated schema/data updates produced zero deltas and preserved every table");
    }

    private static void reconcile(PostgreSqlPlatform platform, Database model, Path file, String table,
            String key, List<String> ids, int expectedChanges) throws Exception {
        DatabaseDataIO io = new DatabaseDataIO();
        var reader = io.getConfiguredCompareDataReader(model);
        DataToArraySink sink = (DataToArraySink) reader.getSink();
        sink.start();
        io.writeDataToDatabase(reader, file.toFile());
        sink.end();
        var selected = new Vector<org.apache.commons.beanutils.DynaBean>();
        for (var bean : sink.getVector()) {
            if (model.getDynaClassFor(bean).getTable().getName().equals(table) && ids.contains(bean.get(key))) selected.add(bean);
        }
        if (selected.size() != ids.size()) throw new AssertionError("Missing target XML rows: " + table);
        DatabaseData desired = new DatabaseData(model);
        desired.insertDynaBeansFromVector(table, selected);
        OBDataset dataset = new OBDataset(desired);
        OBDatasetTable selection = new OBDatasetTable();
        selection.setName(table);
        selection.setSecondarywhereclause(key + " IN ('" + String.join("','", ids) + "')");
        for (var column : model.findTable(table).getColumns()) selection.getIncludedColumns().add(column.getName());
        dataset.getTableList().add(selection);
        DataComparator comparator = new DataComparator(platform.getPlatformInfo(), false);
        comparator.compareToUpdate(model, platform, desired, dataset, null);
        if (comparator.getChanges().size() != expectedChanges) {
            throw new AssertionError("Unexpected " + table + " delta: " + comparator.getChanges());
        }
        try (var connection = platform.borrowConnection()) {
            platform.alterData(connection, model, comparator.getChanges());
        }
    }

    private static void child(String runtime, Path properties, String phase, String log) throws Exception {
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin/java").toString(),
                "-Dvalidation.obdal=true", "-Dvalidation.phase=" + phase,
                "-Dlog4j2.configurationFile=" + new File("fixtures/log4j2.xml").getAbsolutePath(), "-cp", runtime,
                "com.etendoerp.platform.validation.DalMappingValidation", properties.toAbsolutePath().toString())
                .redirectErrorStream(true).redirectOutput(new File(log)).start();
        if (!process.waitFor(50, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
            throw new AssertionError("DAL phase timed out; inspect " + log);
        }
        if (process.exitValue() != 0) throw new AssertionError("DAL phase failed; inspect " + log);
        System.out.println("PASS: Separate generated DAL " + phase + " process completed (" + log + ")");
    }

    private static void constraints(BasicDataSource source) throws Exception {
        for (String state : List.of("23503", "23502")) {
            String title = state.equals("23502") ? "null" : "'Invalid'";
            String category = state.equals("23503") ? "'MISSING'" : "'GENERAL'";
            try (var connection = source.getConnection(); var statement = connection.createStatement()) {
                try {
                    statement.executeUpdate("insert into pp_request(id,title,pp_category_id,ad_client_id,ad_org_id,isactive) values ('INVALID',"
                            + title + "," + category + ",'C1','O1','Y')");
                    throw new AssertionError("Constraint was not enforced: " + state);
                } catch (SQLException expected) {
                    if (!state.equals(expected.getSQLState())) throw expected;
                }
            }
        }
        System.out.println("PASS: Actual DAL schema enforces PostgreSQL FK and NOT NULL constraints");
    }

    private static Map<String, String> snapshot(BasicDataSource source, Database model) throws Exception {
        Map<String, String> result = new LinkedHashMap<>();
        for (var table : model.getTables()) result.put(table.getName(), rows(source, table.getName(), false));
        return result;
    }

    private static String rows(BasicDataSource source, String table, boolean omitDescription) throws Exception {
        return query(source, "select coalesce(string_agg(j, E'\\n' order by j),'') from (select (to_jsonb(t)"
                + (omitDescription ? " - 'description'" : "") + ")::text j from " + table + " t) rows");
    }

    private static String localCategories(BasicDataSource source) throws Exception {
        return query(source, "select string_agg(to_jsonb(t)::text,E'\\n' order by id) from pp_category t where id not in ('GENERAL','IT')");
    }

    private static String query(BasicDataSource source, String sql) throws Exception {
        try (var connection = source.getConnection(); var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            if (!result.next()) throw new AssertionError("Missing snapshot result");
            return result.getString(1);
        }
    }

    private static void clearGenerated(Path directory) throws Exception {
        if (Files.exists(directory)) {
            try (var paths = Files.walk(directory)) {
                for (var path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }
}
