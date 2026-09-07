package com.etendoerp.platform.validation;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.ddlutils.model.Database;

/** Application-owned visual metadata in the original dictionary format. */
final class UiDictionaryFixture {
    private UiDictionaryFixture() {}

    static void verifyRuntime(java.nio.file.Path generated, java.nio.file.Path properties) throws Exception {
        String runtime = System.getProperty("validation.dalClasspath");
        if (runtime == null || runtime.isBlank()) throw new IllegalArgumentException("Missing minimal DAL classpath");
        java.nio.file.Path classes = java.nio.file.Files.createTempDirectory(java.nio.file.Path.of("build"), "ui-dal-classes-");
        java.util.List<String> arguments = new java.util.ArrayList<>(List.of("--release", "17", "-proc:none",
                "-implicit:none", "-classpath", runtime, "-d", classes.toString()));
        try (var paths = java.nio.file.Files.walk(generated)) {
            paths.filter(path -> path.toString().endsWith(".java")).sorted()
                    .forEach(path -> arguments.add(path.toString()));
        }
        arguments.add("dal-probe/UiDalValidation.java");
        if (javax.tools.ToolProvider.getSystemJavaCompiler().run(null, null, null,
                arguments.toArray(String[]::new)) != 0) throw new AssertionError("Generated UI entities failed compilation");
        String log = "build/ui-dal-runtime.log";
        Process child = new ProcessBuilder(java.nio.file.Path.of(System.getProperty("java.home"), "bin/java").toString(),
                "-Dlog4j2.configurationFile=" + new java.io.File("fixtures/log4j2.xml").getAbsolutePath(),
                "-cp", classes.toAbsolutePath() + java.io.File.pathSeparator + runtime,
                "com.etendoerp.platform.validation.UiDalValidation", properties.toAbsolutePath().toString())
                .redirectErrorStream(true).redirectOutput(new java.io.File(log)).start();
        try {
            if (!child.waitFor(50, java.util.concurrent.TimeUnit.SECONDS)) throw new AssertionError("UI DAL timed out: " + log);
            if (child.exitValue() != 0) throw new AssertionError("UI DAL failed: " + log);
        } finally {
            if (child.isAlive()) { child.destroyForcibly(); child.waitFor(5, java.util.concurrent.TimeUnit.SECONDS); }
        }
        System.out.println("PASS: Compiled original UI entities executed through the minimal DAL in an isolated JVM");
    }

    static void addData(StringBuilder xml, Database schema) {
        try {
            var templates = SecurityFixture.rows(java.nio.file.Path.of(
                    "../modules_core/org.openbravo.client.application/src-db/database"), "OBCLKER_TEMPLATE");
            var form = templates.stream().filter(row -> "C1D176407A354A40815DC46D24D70EB8"
                    .equals(row.get("OBCLKER_TEMPLATE_ID"))).findFirst().orElseThrow();
            form.put("AD_MODULE_ID", "0");
            seed(xml, schema, "OBCLKER_TEMPLATE", form);
        } catch (Exception failure) { throw new IllegalStateException("Original form template metadata unavailable", failure); }
        for (String table : List.of("PP_CATEGORY", "PP_REQUEST")) {
            String window = DictionaryFixture.columnId(table, "WINDOW");
            String tab = DictionaryFixture.columnId(table, "TAB");
            seed(xml, schema, "AD_WINDOW", Map.of("AD_WINDOW_ID", window,
                    "NAME", table.equals("PP_REQUEST") ? "Requests" : "Categories", "WINDOWTYPE", "M"));
            seed(xml, schema, "AD_TAB", Map.of("AD_TAB_ID", tab, "AD_WINDOW_ID", window,
                    "AD_TABLE_ID", table, "NAME", table.equals("PP_REQUEST") ? "Request" : "Category",
                    "SEQNO", "10", "TABLEVEL", "0", "ISINSERTRECORD", "Y", "ISGRIDVIEWDEFAULT", "Y"));
            int position = 0;
            for (var column : schema.findTable(table).getColumns()) {
                String id = DictionaryFixture.columnId(table, column.getName());
                boolean editable = !column.isPrimaryKey() && !column.getName().equals("AD_CLIENT_ID")
                        && !column.getName().equals("AD_ORG_ID");
                seed(xml, schema, "AD_FIELD", Map.of("AD_FIELD_ID", DictionaryFixture.columnId(id, "FIELD"),
                        "AD_TAB_ID", tab, "AD_COLUMN_ID", id, "NAME", column.getName(),
                        "SEQNO", Integer.toString(++position * 10), "ISDISPLAYED", editable ? "Y" : "N",
                        "SHOWINRELATION", editable ? "Y" : "N", "ISREADONLY", editable ? "N" : "Y"));
            }
        }
    }

    private static void seed(StringBuilder xml, Database schema, String table, Map<String, String> values) {
        Map<String, String> row = new LinkedHashMap<>(Map.of("AD_CLIENT_ID", "0", "AD_ORG_ID", "0",
                "CREATED", "2026-09-07 00:00:00", "CREATEDBY", "0", "UPDATEDBY", "0", "AD_MODULE_ID", "PLATFORM"));
        row.putAll(values);
        DictionaryFixture.row(xml, schema, table, row);
    }

    static void verify(Connection connection) throws Exception {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(
                "select count(distinct w.ad_window_id), count(distinct t.ad_tab_id), count(f.ad_field_id) "
                + "from ad_window w join ad_tab t on t.ad_window_id=w.ad_window_id "
                + "join ad_field f on f.ad_tab_id=t.ad_tab_id "
                + "join ad_column c on c.ad_column_id=f.ad_column_id and c.ad_table_id=t.ad_table_id "
                + "where t.ad_table_id in ('PP_REQUEST','PP_CATEGORY')")) {
            if (!rows.next() || rows.getInt(1) != 2 || rows.getInt(2) != 2 || rows.getInt(3) != 11) {
                throw new AssertionError("Application window/tab/field dictionary relationships are incomplete");
            }
        }
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(
                "select count(*) from information_schema.tables where lower(table_name) "
                + "in ('m_product','m_warehouse','c_bpartner')")) {
            if (!rows.next() || rows.getInt(1) != 0) throw new AssertionError("ERP tables leaked into UI fixture");
        }
        System.out.println("PASS: Original UI dictionary links two application windows and tabs to fields without ERP tables");
    }
}
