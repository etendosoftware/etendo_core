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
            "AD_TABLE_ACCESS", "AD_TABLE", "AD_CLIENTINFO", "OBUIAPP_PROCESS", "AD_TREE", "AD_TREENODE", "AD_ORGTYPE"));
    private static final Set<String> COLUMNS = Set.of("AD_CLIENT_ID", "AD_ORG_ID", "AD_USER_ID",
            "AD_ROLE_ID", "ISACTIVE", "CREATED", "CREATEDBY", "UPDATED", "UPDATEDBY", "NAME",
            "VALUE", "DESCRIPTION", "USERNAME", "ISPORTAL", "ISWEBSERVICEENABLED", "USERLEVEL",
            "AD_LANGUAGE", "ISRTL", "ISBASELANGUAGE", "ISSYSTEMLANGUAGE", "IS_CLIENT_ADMIN",
            "DEFAULT_AD_CLIENT_ID", "DEFAULT_AD_ORG_ID", "DEFAULT_AD_ROLE_ID", "DEFAULT_AD_LANGUAGE",
            "DEFAULT_M_WAREHOUSE_ID", "C_BPARTNER_ID", "AD_TABLE_ID", "ISREADONLY", "ISEXCLUDE",
            "AD_TREE_ID", "NODE_ID", "PARENT_ID", "ISREADY", "AD_ORGTYPE_ID", "ISLEGALENTITY",
            "ISBUSINESSUNIT", "ISTRANSACTIONSALLOWED", "ISPERIODCONTROLALLOWED");
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
        for (String name : List.of("PP_CATEGORY", "PP_REQUEST")) {
            Table application = model.findTable(name);
            for (String column : List.of("AD_CLIENT_ID", "AD_ORG_ID", "ISACTIVE")) {
                application.addColumn((org.apache.ddlutils.model.Column) model.findTable("AD_USER").findColumn(column).clone());
            }
            for (String target : List.of("AD_CLIENT", "AD_ORG")) {
                var key = new org.apache.ddlutils.model.ForeignKey();
                key.setName(name + "_" + target + "_FK");
                key.setForeignTableName(target);
                var reference = new org.apache.ddlutils.model.Reference();
                reference.setLocalColumnName(target + "_ID");
                reference.setForeignColumnName(target + "_ID");
                key.addReference(reference);
                application.addForeignKey(key);
            }
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

    /** Creates only fixture-owned security rows; no credentials or existing database are read. */
    static Path securityData(Database model) throws Exception {
        StringBuilder data = new StringBuilder("<?xml version=\"1.0\"?><data>\n");
        seed(data, model, "AD_LANGUAGE", Map.of("AD_LANGUAGE_ID", "LANG", "AD_LANGUAGE", "en_US", "NAME", "English", "ISBASELANGUAGE", "Y"));
        for (String client : List.of("0", "C1", "C2")) {
            seed(data, model, "AD_CLIENT", Map.of("AD_CLIENT_ID", client, "NAME", "Client " + client, "VALUE", client, "AD_LANGUAGE", "en_US"));
        }
        seed(data, model, "AD_ORGTYPE", Map.of("AD_ORGTYPE_ID", "TYPE", "NAME", "Organization type"));
        for (String org : List.of("0", "O1", "O2", "O3")) {
            String client = org.equals("0") ? "0" : org.equals("O3") ? "C2" : "C1";
            seed(data, model, "AD_ORG", Map.of("AD_ORG_ID", org, "AD_CLIENT_ID", client, "NAME", org, "VALUE", org, "AD_ORGTYPE_ID", "TYPE", "ISREADY", "Y"));
        }
        for (String client : List.of("C1", "C2")) {
            seed(data, model, "AD_TREE", Map.of("AD_TREE_ID", client, "AD_CLIENT_ID", client, "NAME", "Organization tree", "AD_TABLE_ID", "155"));
        }
        for (String org : List.of("O1", "O2", "O3")) {
            String client = org.equals("O3") ? "C2" : "C1";
            seed(data, model, "AD_TREENODE", Map.of("AD_TREENODE_ID", org, "AD_TREE_ID", client, "AD_CLIENT_ID", client, "NODE_ID", org, "PARENT_ID", "0"));
        }
        seed(data, model, "AD_USER", Map.of("AD_USER_ID", "0", "NAME", "System"));
        seed(data, model, "AD_USER", Map.of("AD_USER_ID", "U1", "NAME", "Platform user", "AD_CLIENT_ID", "C1", "AD_ORG_ID", "O1"));
        seed(data, model, "AD_ROLE", Map.of("AD_ROLE_ID", "R1", "NAME", "Platform role", "AD_CLIENT_ID", "C1", "AD_ORG_ID", "O1", "USERLEVEL", "O"));
        seed(data, model, "AD_USER_ROLES", Map.of("AD_USER_ROLES_ID", "UR1", "AD_USER_ID", "U1", "AD_ROLE_ID", "R1", "AD_CLIENT_ID", "C1", "AD_ORG_ID", "O1"));
        seed(data, model, "AD_ROLE_ORGACCESS", Map.of("AD_ROLE_ORGACCESS_ID", "RO1", "AD_ROLE_ID", "R1", "AD_CLIENT_ID", "C1", "AD_ORG_ID", "O1"));
        for (String table : List.of("PP_CATEGORY", "PP_REQUEST")) {
            seed(data, model, "AD_TABLE_ACCESS", Map.of("AD_TABLE_ACCESS_ID", table, "AD_TABLE_ID", table,
                    "AD_ROLE_ID", "R1", "AD_CLIENT_ID", "C1", "AD_ORG_ID", "O1"));
        }
        for (String role : List.of("R_READ", "R_EXCLUDE", "R_INACTIVE")) {
            seed(data, model, "AD_ROLE", Map.of("AD_ROLE_ID", role, "NAME", role, "AD_CLIENT_ID", "C1", "AD_ORG_ID", "O1", "USERLEVEL", "O"));
            seed(data, model, "AD_USER_ROLES", Map.of("AD_USER_ROLES_ID", role, "AD_USER_ID", "U1", "AD_ROLE_ID", role, "AD_CLIENT_ID", "C1", "AD_ORG_ID", "O1"));
            seed(data, model, "AD_ROLE_ORGACCESS", Map.of("AD_ROLE_ORGACCESS_ID", role, "AD_ROLE_ID", role, "AD_CLIENT_ID", "C1", "AD_ORG_ID", "O1"));
            seed(data, model, "AD_TABLE_ACCESS", Map.of("AD_TABLE_ACCESS_ID", role, "AD_TABLE_ID", "PP_REQUEST",
                    "AD_ROLE_ID", role, "AD_CLIENT_ID", "C1", "AD_ORG_ID", "O1", "ISREADONLY", role.equals("R_READ") ? "Y" : "N",
                    "ISEXCLUDE", role.equals("R_EXCLUDE") ? "Y" : "N", "ISACTIVE", role.equals("R_INACTIVE") ? "N" : "Y"));
        }
        for (String suffix : List.of("O2", "C2", "INACTIVE")) {
            String client = suffix.equals("C2") ? "C2" : "C1";
            String org = suffix.equals("C2") ? "O3" : suffix.equals("O2") ? "O2" : "O1";
            seed(data, model, "PP_CATEGORY", Map.of("ID", suffix, "NAME", "Category " + suffix,
                    "AD_CLIENT_ID", client, "AD_ORG_ID", org));
            seed(data, model, "PP_REQUEST", Map.of("ID", suffix, "TITLE", "Request " + suffix,
                    "PP_CATEGORY_ID", suffix, "AD_CLIENT_ID", client, "AD_ORG_ID", org,
                    "ISACTIVE", suffix.equals("INACTIVE") ? "N" : "Y"));
        }
        seed(data, model, "PP_CATEGORY", Map.of("ID", "GENERAL", "NAME", "General requests",
                "AD_CLIENT_ID", "C1", "AD_ORG_ID", "O1"));
        data.append("</data>\n");
        Path output = Path.of("build/security-data.xml");
        Files.writeString(output, data);
        return output;
    }

    private static void seed(StringBuilder xml, Database model, String table, Map<String, String> supplied) {
        Map<String, String> values = new LinkedHashMap<>(Map.of("AD_CLIENT_ID", "0", "AD_ORG_ID", "0",
                "CREATED", "2026-09-06 00:00:00", "UPDATED", "2026-09-06 00:00:00", "CREATEDBY", "0", "UPDATEDBY", "0"));
        values.putAll(supplied);
        DictionaryFixture.row(xml, model, table, values);
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
