package com.etendoerp.platform.validation;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.ddlutils.model.Database;

/** Application-owned visual metadata in the original dictionary format. */
final class UiDictionaryFixture {
    private UiDictionaryFixture() {}

    static void addData(StringBuilder xml, Database schema) {
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
                "CREATED", "2026-09-07 00:00:00", "CREATEDBY", "0", "UPDATEDBY", "0"));
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
