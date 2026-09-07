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
        boolean prepareInstance = Boolean.getBoolean("validation.prepareUiInstance");
        if (!prepareInstance) {
            arguments.add("dal-probe/UiDalValidation.java");
            if (Boolean.getBoolean("validation.uiLogin")) arguments.add("dal-probe/UiHttpAuthenticationValidation.java");
        }
        if (javax.tools.ToolProvider.getSystemJavaCompiler().run(null, null, null,
                arguments.toArray(String[]::new)) != 0) throw new AssertionError("Generated UI entities failed compilation");
        if (prepareInstance) {
            var entries = new java.util.ArrayList<String>();
            entries.add(classes.toAbsolutePath().toString());
            for (String entry : runtime.split(java.util.regex.Pattern.quote(java.io.File.pathSeparator))) {
                entries.add(java.nio.file.Path.of(entry).toAbsolutePath().toString());
            }
            java.nio.file.Files.write(classes.resolve("runtime-classpath.txt"), entries);
            System.setProperty("validation.uiInstanceClasses", classes.toAbsolutePath().toString());
            System.out.println("PASS: Retained UI entity compilation completed without test harness classes");
            return;
        }
        String log = Boolean.getBoolean("validation.uiLogin") ? "build/ui-login-runtime.log"
                : Boolean.getBoolean("validation.uiMenu") ? "build/ui-menu-runtime.log"
                : Boolean.getBoolean("validation.uiWindow") ? "build/ui-window-runtime.log"
                : Boolean.getBoolean("validation.uiFieldDefinitions") ? "build/ui-field-definitions-runtime.log"
                : Boolean.getBoolean("validation.uiFields") ? "build/ui-field-runtime.log"
                : Boolean.getBoolean("validation.uiCache") ? "build/ui-cache-runtime.log" : "build/ui-dal-runtime.log";
        Process child = new ProcessBuilder(java.nio.file.Path.of(System.getProperty("java.home"), "bin/java").toString(),
                "-Dvalidation.uiFields=" + Boolean.getBoolean("validation.uiFields"),
                "-Dvalidation.uiFieldDefinitions=" + Boolean.getBoolean("validation.uiFieldDefinitions"),
                "-Dvalidation.uiWindow=" + Boolean.getBoolean("validation.uiWindow"),
                "-Dvalidation.uiMenu=" + Boolean.getBoolean("validation.uiMenu"),
                "-Dvalidation.uiLogin=" + Boolean.getBoolean("validation.uiLogin"),
                "-Dvalidation.uiCache=" + Boolean.getBoolean("validation.uiCache"),
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
        try (var lines = java.nio.file.Files.lines(java.nio.file.Path.of(log))) {
            lines.filter(line -> line.startsWith("PASS:")).forEach(System.out::println);
        }
        System.out.println("PASS: Compiled original UI entities executed through the minimal DAL in an isolated JVM");
        if (Boolean.getBoolean("validation.uiLogin")) {
            Process http = new ProcessBuilder(java.nio.file.Path.of(System.getProperty("java.home"), "bin/java").toString(),
                    "--add-opens=java.base/java.io=ALL-UNNAMED", "--add-opens=java.base/java.lang=ALL-UNNAMED",
                    "--add-opens=java.rmi/sun.rmi.transport=ALL-UNNAMED",
                    "-Dlog4j2.configurationFile=" + new java.io.File("fixtures/log4j2.xml").getAbsolutePath(),
                    "-cp", classes.toAbsolutePath() + java.io.File.pathSeparator + runtime,
                    "com.etendoerp.platform.validation.UiHttpAuthenticationValidation", properties.toAbsolutePath().toString())
                    .redirectErrorStream(true).redirectOutput(new java.io.File("build/ui-http-authentication.log")).start();
            try {
                if (!http.waitFor(50, java.util.concurrent.TimeUnit.SECONDS)) throw new AssertionError("HTTP authentication timed out");
                if (http.exitValue() != 0) throw new AssertionError("HTTP authentication failed: build/ui-http-authentication.log");
            } finally {
                if (http.isAlive()) { http.destroyForcibly(); http.waitFor(5, java.util.concurrent.TimeUnit.SECONDS); }
            }
            try (var lines = java.nio.file.Files.lines(java.nio.file.Path.of("build/ui-http-authentication.log"))) {
                lines.filter(line -> line.startsWith("PASS:")).forEach(System.out::println);
            }
        }
    }

    static void addData(StringBuilder xml, Database schema) {
        try {
            for (var group : SecurityFixture.rows(java.nio.file.Path.of("../src-db/database"), "AD_FIELDGROUP")) {
                if ("1000100001".equals(group.get("AD_FIELDGROUP_ID"))) seed(xml, schema, "AD_FIELDGROUP", group);
            }
            var templates = SecurityFixture.rows(java.nio.file.Path.of(
                    "../modules_core/org.openbravo.client.application/src-db/database"), "OBCLKER_TEMPLATE");
            var form = templates.stream().filter(row -> "C1D176407A354A40815DC46D24D70EB8"
                    .equals(row.get("OBCLKER_TEMPLATE_ID"))).findFirst().orElseThrow();
            form.put("AD_MODULE_ID", "0");
            seed(xml, schema, "OBCLKER_TEMPLATE", form);
            if (Boolean.getBoolean("validation.uiWindow")) {
                var ids = new java.util.HashSet<>(java.util.Set.of("B5124C0A450D4D3A867AEAC7DF64D6F0", "33E04D0799794C6F95F05149D2E04E78",
                        "91DD63545B674BE8801E1FA4F48FF4C6", "2BAD445C2A0343C58E455F9BD379C690",
                        "ADD5EF45333C458098286D0E639B3290"));
                if (Boolean.getBoolean("validation.uiMenu")) ids.add("4C6825EEBF2C440CA6F97C8A042CCB5F");
                if (Boolean.getBoolean("validation.uiLogin")) {
                    ids.addAll(java.util.Set.of("9E97FF309FE44C61A761F50801F79349", "0A9FD7B8976645A380920FF6411AB4A6",
                            "CB89E38CF75545499BF0B91FA6B233E5"));
                    for (var row : SecurityFixture.rows(java.nio.file.Path.of(
                            "../modules_core/org.openbravo.client.application/src-db/database"), "OBUIAPP_NAVBAR_COMPONENT")) {
                        ids.add(row.get("OBCLKER_TEMPLATE_ID"));
                        row.put("AD_MODULE_ID", "0");
                        if ("E404869052A44BC99AE27BDBAE1D6062".equals(row.get("OBUIAPP_NAVBAR_COMPONENT_ID"))) {
                            row.put("ALLROLES", "N");
                            row.put("ISSTATICCOMPONENT", "N");
                        }
                        seed(xml, schema, "OBUIAPP_NAVBAR_COMPONENT", row);
                    }
                    for (String role : List.of("R1", "R_READ")) {
                        seed(xml, schema, "OBUIAPP_NAVBAR_ROLE_ACCESS", Map.of(
                                "OBUIAPP_NAVBAR_ROLE_ACCESS_ID", "PP_NAV_" + role,
                                "OBUIAPP_NAVBAR_COMPONENT_ID", "E404869052A44BC99AE27BDBAE1D6062", "AD_ROLE_ID", role));
                    }
                }
                var sources = java.util.List.of(java.nio.file.Path.of("../modules_core/org.openbravo.client.application/src-db/database"),
                        java.nio.file.Path.of("../modules_core/org.openbravo.service.datasource/src-db/database"));
                var found = new java.util.HashSet<String>();
                for (var source : sources) {
                    for (var row : SecurityFixture.rows(source, "OBCLKER_TEMPLATE")) {
                        if (!ids.contains(row.get("OBCLKER_TEMPLATE_ID"))) continue;
                        found.add(row.get("OBCLKER_TEMPLATE_ID"));
                        row.put("AD_MODULE_ID", "0");
                        seed(xml, schema, "OBCLKER_TEMPLATE", row);
                    }
                }
                if (!found.equals(ids)) throw new AssertionError("Missing original window templates: " + found);
                for (var row : SecurityFixture.rows(sources.get(0), "OBCLKER_TEMPLATE_DEPENDENCY")) {
                    if (ids.contains(row.get("OBCLKER_TEMPLATE_ID")) && ids.contains(row.get("DEPENDSON_TEMPLATE_ID"))) {
                        row.put("AD_MODULE_ID", "0");
                        seed(xml, schema, "OBCLKER_TEMPLATE_DEPENDENCY", row);
                    }
                }
                for (var row : SecurityFixture.rows(sources.get(0), "OBSERDS_DATASOURCE")) {
                    if ("090A37D22E61FE94012E621729090048".equals(row.get("OBSERDS_DATASOURCE_ID"))) {
                        seed(xml, schema, "OBSERDS_DATASOURCE", row);
                    }
                }
            }
        } catch (Exception failure) { throw new IllegalStateException("Original form template metadata unavailable", failure); }
        seed(xml, schema, "AD_FIELDGROUP", Map.of("AD_FIELDGROUP_ID", "PP_REQUEST_DETAILS",
                "NAME", "Request details", "ISCOLLAPSED", "N"));
        if (Boolean.getBoolean("validation.uiMenu")) {
            seed(xml, schema, "AD_TREE", Map.of("AD_TREE_ID", "10", "NAME", "Application menu"));
        }
        for (String table : List.of("PP_CATEGORY", "PP_REQUEST")) {
            String window = DictionaryFixture.columnId(table, "WINDOW");
            String tab = DictionaryFixture.columnId(table, "TAB");
            if (Boolean.getBoolean("validation.uiMenu")) {
                String menu = DictionaryFixture.columnId(table, "MENU");
                seed(xml, schema, "AD_MENU", Map.of("AD_MENU_ID", menu, "NAME", table.equals("PP_REQUEST") ? "Requests" : "Categories",
                        "AD_WINDOW_ID", window, "ACTION", "W", "ISSUMMARY", "N"));
                seed(xml, schema, "AD_TREENODE", Map.of("AD_TREENODE_ID", menu, "AD_TREE_ID", "10", "NODE_ID", menu,
                        "PARENT_ID", "0", "SEQNO", table.equals("PP_REQUEST") ? "20" : "10"));
                seed(xml, schema, "AD_WINDOW_ACCESS", Map.of("AD_WINDOW_ACCESS_ID", window, "AD_WINDOW_ID", window,
                        "AD_ROLE_ID", "R1", "ISREADWRITE", "Y"));
                if (table.equals("PP_REQUEST")) seed(xml, schema, "AD_WINDOW_ACCESS", Map.of(
                        "AD_WINDOW_ACCESS_ID", DictionaryFixture.columnId(window, "READ"), "AD_WINDOW_ID", window,
                        "AD_ROLE_ID", "R_READ", "ISREADWRITE", "N"));
            }
            seed(xml, schema, "AD_WINDOW", Map.of("AD_WINDOW_ID", window,
                    "NAME", table.equals("PP_REQUEST") ? "Requests" : "Categories", "WINDOWTYPE", "M"));
            seed(xml, schema, "AD_TAB", Map.of("AD_TAB_ID", tab, "AD_WINDOW_ID", window,
                    "AD_TABLE_ID", table, "NAME", table.equals("PP_REQUEST") ? "Request" : "Category",
                    "SEQNO", "10", "TABLEVEL", "0", "ISINSERTRECORD", "Y", "ISGRIDVIEWDEFAULT", "Y", "UIPATTERN", "STD"));
            int position = 0;
            for (var column : schema.findTable(table).getColumns()) {
                String id = DictionaryFixture.columnId(table, column.getName());
                boolean editable = !column.isPrimaryKey() && !column.getName().equals("AD_CLIENT_ID")
                        && !column.getName().equals("AD_ORG_ID");
                Map<String, String> field = new LinkedHashMap<>(Map.of("AD_FIELD_ID", DictionaryFixture.columnId(id, "FIELD"),
                        "AD_TAB_ID", tab, "AD_COLUMN_ID", id, "NAME", column.getName(),
                        "SEQNO", Integer.toString(++position * 10), "ISDISPLAYED", editable ? "Y" : "N",
                        "SHOWINRELATION", editable ? "Y" : "N", "ISREADONLY", editable ? "N" : "Y"));
                field.put("DISPLAYLENGTH", column.getSize() == null ? "30"
                        : Integer.toString(Math.max(1, Math.min(60, Integer.parseInt(column.getSize())))));
                if (table.equals("PP_REQUEST") && column.getName().equals("TITLE")) {
                    field.putAll(Map.of("AD_FIELDGROUP_ID", "PP_REQUEST_DETAILS", "STARTNEWLINE", "Y",
                            "STARTINODDCOLUMN", "Y", "DISPLAYLOGIC", "@IsActive@='Y'",
                            "DISPLAYLOGICGRID", "@IsActive@='Y'", "DISPLAYLENGTH", "60"));
                }
                seed(xml, schema, "AD_FIELD", field);
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
