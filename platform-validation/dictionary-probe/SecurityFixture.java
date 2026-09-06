package com.etendoerp.platform.validation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import org.apache.ddlutils.io.DatabaseIO;
import org.apache.ddlutils.model.Database;
import org.apache.ddlutils.model.Table;

/** Selects original dictionary rows for the first security/compatibility generation slice. */
final class SecurityFixture {
    static final Set<String> TABLES = new LinkedHashSet<>(List.of("AD_USER", "AD_ROLE",
            "AD_USER_ROLES", "AD_ROLE_ORGACCESS", "AD_CLIENT", "AD_LANGUAGE", "AD_ORG", "M_WAREHOUSE", "C_BPARTNER",
            "AD_TABLE_ACCESS", "AD_TABLE", "AD_CLIENTINFO", "OBUIAPP_PROCESS"));
    private static final Set<String> COLUMNS = Set.of("AD_CLIENT_ID", "AD_ORG_ID", "AD_USER_ID",
            "AD_ROLE_ID", "ISACTIVE", "CREATED", "CREATEDBY", "UPDATED", "UPDATEDBY", "NAME",
            "VALUE", "DESCRIPTION", "USERNAME", "ISPORTAL", "ISWEBSERVICEENABLED", "USERLEVEL",
            "AD_LANGUAGE", "ISRTL", "ISBASELANGUAGE", "ISSYSTEMLANGUAGE", "IS_CLIENT_ADMIN",
            "DEFAULT_AD_CLIENT_ID", "DEFAULT_AD_ORG_ID", "DEFAULT_AD_ROLE_ID", "DEFAULT_AD_LANGUAGE",
            "DEFAULT_M_WAREHOUSE_ID", "C_BPARTNER_ID", "AD_TABLE_ID", "ISREADONLY", "ISEXCLUDE");
    private static final Path CORE = Path.of("../src-db/database");
    private static final Path UI = Path.of("../modules_core/org.openbravo.client.application/src-db/database");

    private SecurityFixture() {}

    static void addSchema(Database model, DatabaseIO xml) throws Exception {
        for (String name : TABLES) {
            if (model.findTable(name) != null) continue;
            Table original = xml.readplain((name.equals("OBUIAPP_PROCESS") ? UI : CORE)
                    .resolve("model/tables/" + name + ".xml").toFile()).getTable(0);
            Table selected = new Table();
            selected.setName(name);
            selected.setPrimaryKey(original.getPrimaryKey());
            for (var column : original.getColumns()) {
                if (column.isPrimaryKey() || COLUMNS.contains(column.getName())) {
                    selected.addColumn((org.apache.ddlutils.model.Column) column.clone());
                }
            }
            model.addTable(selected);
        }
    }

    static void addData(StringBuilder data, Database model) throws Exception {
        Map<String, String> tableIds = new LinkedHashMap<>();
        Set<String> packages = new LinkedHashSet<>();
        for (var row : rows("AD_TABLE")) {
            String name = row.get("TABLENAME").toUpperCase(Locale.ROOT);
            if (!TABLES.contains(name)) continue;
            tableIds.put(row.get("AD_TABLE_ID"), name);
            packages.add(row.get("AD_PACKAGE_ID"));
            DictionaryFixture.row(data, model, "AD_TABLE", row);
        }
        if (tableIds.size() != TABLES.size()) throw new AssertionError("Missing source security tables");
        DictionaryFixture.row(data, model, "AD_MODULE", Map.of("AD_MODULE_ID", "0", "NAME", "Core compatibility",
                "JAVAPACKAGE", "org.openbravo", "SEQNO", "0"));
        for (var row : rows("AD_PACKAGE")) {
            if (packages.contains(row.get("AD_PACKAGE_ID"))) {
                row.put("AD_MODULE_ID", "0");
                DictionaryFixture.row(data, model, "AD_PACKAGE", row);
            }
        }
        Set<String> references = new LinkedHashSet<>();
        for (var row : rows("AD_COLUMN")) {
            String table = tableIds.get(row.get("AD_TABLE_ID"));
            if (table == null || model.findTable(table).findColumn(row.get("COLUMNNAME"), false) == null) continue;
            String column = row.get("COLUMNNAME").toUpperCase(Locale.ROOT);
            if (!COLUMNS.contains(column) && !"Y".equals(row.get("ISKEY"))) continue;
            row.put("AD_MODULE_ID", "0");
            String target = column.equals("CREATEDBY") || column.equals("UPDATEDBY") ? "AD_USER"
                    : column.endsWith("_ID") ? column.substring(0, column.length() - 3) : "";
            if (!"Y".equals(row.get("ISKEY")) && TABLES.contains(target)) {
                // Project UI selectors to the same physical relationship for this headless profile.
                row.put("AD_REFERENCE_ID", "19");
                row.remove("AD_REFERENCE_VALUE_ID");
            }
            references.add(row.get("AD_REFERENCE_ID"));
            if (row.containsKey("AD_REFERENCE_VALUE_ID")) references.add(row.get("AD_REFERENCE_VALUE_ID"));
            DictionaryFixture.row(data, model, "AD_COLUMN", row);
        }
        for (var row : rows("AD_REFERENCE")) {
            if (references.contains(row.get("AD_REFERENCE_ID")) && !Set.of("10", "13", "19").contains(row.get("AD_REFERENCE_ID"))) {
                DictionaryFixture.row(data, model, "AD_REFERENCE", row);
            }
        }
        for (var row : rows("AD_REF_LIST")) {
            if (references.contains(row.get("AD_REFERENCE_ID"))) DictionaryFixture.row(data, model, "AD_REF_LIST", row);
        }
        for (var row : rows("AD_REF_TABLE")) {
            if (references.contains(row.get("AD_REFERENCE_ID"))) DictionaryFixture.row(data, model, "AD_REF_TABLE", row);
        }
    }

    /** Streams the existing source-data format without loading the entire XML DOM. */
    private static List<Map<String, String>> rows(String table) throws Exception {
        List<Map<String, String>> result = new ArrayList<>(rows(CORE, table));
        if (Set.of("AD_TABLE", "AD_COLUMN", "AD_PACKAGE").contains(table)) result.addAll(rows(UI, table));
        return result;
    }

    private static List<Map<String, String>> rows(Path source, String table) throws Exception {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
        List<Map<String, String>> result = new ArrayList<>();
        try (var input = Files.newInputStream(source.resolve("sourcedata/" + table + ".xml"))) {
            var reader = factory.createXMLStreamReader(input);
            Map<String, String> row = null;
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String name = reader.getLocalName();
                    if (name.equals(table)) row = new LinkedHashMap<>();
                    else if (row != null) row.put(name, reader.getElementText());
                } else if (event == XMLStreamConstants.END_ELEMENT && reader.getLocalName().equals(table)) {
                    result.add(row);
                    row = null;
                }
            }
            reader.close();
        }
        return result;
    }
}
