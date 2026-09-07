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
            "AD_USER_ROLES", "AD_ROLE_ORGACCESS", "AD_CLIENT", "AD_LANGUAGE", "AD_ORG",
            "AD_TABLE_ACCESS", "AD_TABLE", "AD_CLIENTINFO", "AD_TREE", "AD_TREENODE", "AD_ORGTYPE"));
    private static final Set<String> COLUMNS = Set.of("AD_CLIENT_ID", "AD_ORG_ID", "AD_USER_ID",
            "AD_ROLE_ID", "ISACTIVE", "CREATED", "CREATEDBY", "UPDATED", "UPDATEDBY", "NAME",
            "VALUE", "DESCRIPTION", "USERNAME", "ISPORTAL", "ISWEBSERVICEENABLED", "USERLEVEL",
            "AD_LANGUAGE", "ISRTL", "ISBASELANGUAGE", "ISSYSTEMLANGUAGE", "IS_CLIENT_ADMIN",
            "DEFAULT_AD_CLIENT_ID", "DEFAULT_AD_ORG_ID", "DEFAULT_AD_ROLE_ID", "DEFAULT_AD_LANGUAGE",
            "AD_TABLE_ID", "ISREADONLY", "ISEXCLUDE",
            "AD_TREE_ID", "NODE_ID", "PARENT_ID", "ISREADY", "AD_ORGTYPE_ID", "ISLEGALENTITY",
            "ISBUSINESSUNIT", "ISTRANSACTIONSALLOWED", "ISPERIODCONTROLALLOWED");
    private static final Path CORE = Path.of("../src-db/database");
    private static final List<Path> UI_SOURCES = List.of(
            Path.of("../modules_core/org.openbravo.client.kernel/src-db/database"),
            Path.of("../modules_core/org.openbravo.client.application/src-db/database"),
            Path.of("../modules_core/org.openbravo.userinterface.selector/src-db/database"),
            Path.of("../modules_core/org.openbravo.service.datasource/src-db/database"));

    private SecurityFixture() {}

    // Generic window services retain complete metadata tables, not getter-level projections.
    private static final Set<String> WINDOW_METADATA = Set.of("OBUIAPP_NOTE", "OBSERDS_DATASOURCE_FIELD",
            "OBUIAPP_GC_SYSTEM", "OBUIAPP_GC_TAB", "OBUIAPP_GC_FIELD", "AD_PREFERENCE", "AD_MODEL_OBJECT_MAPPING");

    static Set<String> selectedTables() {
        Set<String> tables = new LinkedHashSet<>(TABLES);
        if (Boolean.getBoolean("validation.originalUi")) {
            tables.addAll(List.of("AD_COLUMN", "AD_WINDOW", "AD_TAB", "AD_TAB_TRL", "AD_FIELD", "AD_WINDOW_ACCESS"));
            tables.addAll(List.of("AD_MODULE", "OBCLKER_TEMPLATE", "OBCLKER_TEMPLATE_DEPENDENCY",
                    "AD_FIELDGROUP", "AD_REFERENCE", "AD_PROCESS", "AD_AUXILIARINPUT",
                    "OBUIAPP_PARAMETER", "AD_MODEL_OBJECT", "AD_VAL_RULE", "AD_CALLOUT",
                    "AD_REF_TABLE", "AD_REF_LIST", "AD_REF_TREE", "AD_REF_TREE_FIELD",
                    "AD_TABLE_TREE", "OBUIAPP_PROCESS", "OBUIAPP_REF_WINDOW",
                    "OBUISEL_SELECTOR", "OBUISEL_SELECTOR_FIELD", "OBCLKER_REF_MASK", "OBSERDS_DATASOURCE",
                    "OBCLKER_UIDEFINITION", "AD_ELEMENT", "AD_ELEMENT_TRL", "AD_FIELD_TRL", "AD_FIELDGROUP_TRL"));
            if (Boolean.getBoolean("validation.uiWindow")) tables.addAll(WINDOW_METADATA.stream().sorted().toList());
        }
        return tables;
    }

    private static boolean selectedColumn(String table, String name) {
        return (Boolean.getBoolean("validation.uiWindow") && WINDOW_METADATA.contains(table))
                || COLUMNS.contains(name) || (Boolean.getBoolean("validation.originalUi")
                && Set.of("AD_COLUMN_ID", "AD_WINDOW_ID", "AD_TAB_ID", "HELP", "SEQNO",
                        "TABLEVEL", "WINDOWTYPE", "ISDISPLAYED", "SHOWINRELATION", "ISUPDATEABLE",
                        "ISINSERTRECORD", "ISGRIDVIEWDEFAULT", "ISSINGLEROW", "GRID_SEQNO",
                        "AD_MODULE_ID", "ISINDEVELOPMENT", "TEMPLATE", "TEMPLATECLASSPATHLOCATION",
                        "TEMPLATE_LANGUAGE", "COMPONENT_TYPE", "OVERRIDES_TEMPLATE_ID",
                        "OBCLKER_TEMPLATE_ID", "DEPENDSON_TEMPLATE_ID", "AD_FIELDGROUP_ID",
                        "ISCOLLAPSED", "PROPERTY", "DISPLAYLOGIC", "DISPLAYLOGICGRID",
                        "STARTNEWLINE", "STARTINODDCOLUMN", "ISSHOWNINSTATUSBAR", "CLIENTCLASS",
                        "DISPLAYLENGTH", "ONCHANGEFUNCTION", "COLUMNNAME", "READONLYLOGIC",
                        "AD_REFERENCE_ID", "AD_REFERENCE_VALUE_ID", "MODEL_IMPL", "UI_IMPL", "ISBASEREFERENCE", "PARENTREFERENCE_ID",
                        "JAVAPACKAGE", "CLASSNAME", "AD_ELEMENT_ID", "AD_FIELD_ID", "ISTRANSLATED",
                        "SQLLOGIC", "ISSESSIONATTR", "FIELDLENGTH", "DEFAULTVALUE", "ISMANDATORY",
                        "ISSECONDARYKEY", "ISPARENT", "ISKEY", "VALIDATEONNEW", "ISAUTOSAVE",
                        "ALLOWSORTING", "ALLOWFILTERING", "IMAGESIZEVALUESACTION", "IMAGEWIDTH", "IMAGEHEIGHT",
                        "ISUSEDSEQUENCE", "ENTITY_ALIAS",
                        "DATAORIGINTYPE", "TABLENAME", "ISFULLYAUDITED", "ISDELETEABLE", "ISVIEW", "HQLQUERY", "IDFKFILTERING",
                        "UIPATTERN", "ISINFOTAB", "ISREADONLYTREE", "ISSHOWTREENODEICONS", "WHERECLAUSE", "ORDERBYCLAUSE",
                        "HQLWHERECLAUSE", "HQLORDERBYCLAUSE", "HQLFILTERCLAUSE", "FILTERCLAUSE", "FILTERNAME",
                        "DISABLE_PARENT_KEY_PROPERTY", "ISTRANSLATIONTAB", "DEFAULTTREEVIEWLOGIC",
                        "SHOWPARENTBUTTONS", "HQLTREEWHERECLAUSE", "EM_OBUIAPP_SELECTION", "EM_OBUIAPP_CAN_ADD",
                        "EM_OBUIAPP_CAN_DELETE", "EM_OBUIAPP_SHOW_SELECT", "EM_OBUIAPP_SELECTION_TYPE",
                        "EM_OBUIAPP_NEWFN", "EM_OBUIAPP_REMOVEFN", "EM_OBUIAPP_SHOW_CLONE_BUTTON", "EM_OBUIAPP_CLONE_CHILDREN",
                        "ISFIRSTFOCUSEDFIELD", "DISPLAYLOGIC_SERVER", "EM_OBUIAPP_COLSPAN", "EM_OBUIAPP_ROWSPAN",
                        "EM_OBUIAPP_VALIDATOR", "EM_OBUIAPP_SUMMARYFN",
                        "SORTNO", "EM_OBUIAPP_DEFAULT_EXPRESSION", "EM_OBUISEL_OUTFIELD_ID", "ISTHREADSAFE",
                        "AD_VAL_RULE_ID", "CODE", "AD_CALLOUT_ID", "AD_PROCESS_ID",
                        "OBUIAPP_PROCESS_ID", "EM_OBUIAPP_PROCESS_ID", "AD_REF_TREE_ID", "AD_TABLE_TREE_ID",
                        "OBUISEL_SELECTOR_ID", "AD_KEY", "AD_DISPLAY", "DISPLAYFIELD_ID", "OBSERDS_DATASOURCE_ID")
                        .contains(name));
    }

    static void addSchema(Database model, DatabaseIO xml) throws Exception {
        for (String name : selectedTables()) {
            Path source = name.startsWith("OBCLKER_")
                    ? Path.of("../modules_core/org.openbravo.client.kernel/src-db/database")
                    : name.startsWith("OBUIAPP_")
                    ? Path.of("../modules_core/org.openbravo.client.application/src-db/database")
                    : name.startsWith("OBUISEL_")
                    ? Path.of("../modules_core/org.openbravo.userinterface.selector/src-db/database")
                    : name.startsWith("OBSERDS_")
                    ? Path.of("../modules_core/org.openbravo.service.datasource/src-db/database") : CORE;
            Table original = xml.readplain(source
                    .resolve("model/tables/" + name + ".xml").toFile()).getTable(0);
            if (Boolean.getBoolean("validation.originalUi")) {
                for (Path uiSource : UI_SOURCES) {
                    Path extension = uiSource.resolve("model/modifiedTables/" + name + ".xml");
                    if (!Files.exists(extension)) continue;
                    for (var column : xml.readplain(extension.toFile()).getTable(0).getColumns()) {
                        if (selectedColumn(name, column.getName())) {
                            if (original.findColumn(column.getName()) != null) {
                                throw new AssertionError("Duplicate UI extension column: " + name + "." + column.getName());
                            }
                            original.addColumn((org.apache.ddlutils.model.Column) column.clone());
                        }
                    }
                }
            }
            if (model.findTable(name) != null) {
                if (Boolean.getBoolean("validation.originalUi")) {
                    for (var column : original.getColumns()) {
                        if (selectedColumn(name, column.getName()) && model.findTable(name).findColumn(column.getName()) == null) {
                            model.findTable(name).addColumn((org.apache.ddlutils.model.Column) column.clone());
                        }
                    }
                }
                continue;
            }
            Table selected = new Table();
            selected.setName(name);
            selected.setPrimaryKey(original.getPrimaryKey());
            for (var column : original.getColumns()) {
                if (column.isPrimaryKey() || selectedColumn(name, column.getName())) {
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
        Set<String> referenceModules = new LinkedHashSet<>();
        for (var row : rows("AD_TABLE")) {
            String name = row.get("TABLENAME").toUpperCase(Locale.ROOT);
            if (!selectedTables().contains(name)) continue;
            tableIds.put(row.get("AD_TABLE_ID"), name);
            packages.add(row.get("AD_PACKAGE_ID"));
            if (Boolean.getBoolean("validation.originalUi")) referenceModules.add(row.get("AD_MODULE_ID"));
            DictionaryFixture.row(data, model, "AD_TABLE", row);
        }
        if (tableIds.size() != selectedTables().size()) throw new AssertionError("Missing source security/UI tables");
        DictionaryFixture.row(data, model, "AD_MODULE", Map.of("AD_MODULE_ID", "0", "NAME", "Core compatibility",
                "JAVAPACKAGE", "org.openbravo", "SEQNO", "0"));
        for (var row : rows("AD_PACKAGE")) {
            if (packages.contains(row.get("AD_PACKAGE_ID"))) {
                if (Boolean.getBoolean("validation.originalUi")) referenceModules.add(row.get("AD_MODULE_ID"));
                else row.put("AD_MODULE_ID", "0");
                DictionaryFixture.row(data, model, "AD_PACKAGE", row);
            }
        }
        Set<String> references = new LinkedHashSet<>();
        Set<String> elements = new LinkedHashSet<>(List.of("245", "246", "607", "608"));
        if (Boolean.getBoolean("validation.originalUi")) references.addAll(List.of("15", "16"));
        for (var row : rows("AD_COLUMN")) {
            String table = tableIds.get(row.get("AD_TABLE_ID"));
            if (table == null || model.findTable(table).findColumn(row.get("COLUMNNAME"), false) == null) continue;
            String column = row.get("COLUMNNAME").toUpperCase(Locale.ROOT);
            if (!selectedColumn(table, column) && !"Y".equals(row.get("ISKEY"))) continue;
            if (Boolean.getBoolean("validation.originalUi")) referenceModules.add(row.get("AD_MODULE_ID"));
            else row.put("AD_MODULE_ID", "0");
            String target = column.equals("CREATEDBY") || column.equals("UPDATEDBY") ? "AD_USER"
                    : column.endsWith("_ID") ? column.substring(0, column.length() - 3) : "";
            if (!"Y".equals(row.get("ISKEY")) && selectedTables().contains(target)) {
                // Project UI selectors to the same physical relationship for this headless profile.
                row.put("AD_REFERENCE_ID", "19");
                row.remove("AD_REFERENCE_VALUE_ID");
            }
            references.add(row.get("AD_REFERENCE_ID"));
            if (row.containsKey("AD_REFERENCE_VALUE_ID")) references.add(row.get("AD_REFERENCE_VALUE_ID"));
            if (row.containsKey("AD_ELEMENT_ID")) elements.add(row.get("AD_ELEMENT_ID"));
            DictionaryFixture.row(data, model, "AD_COLUMN", row);
        }
        if (Boolean.getBoolean("validation.originalUi")) {
            for (var row : rows("AD_ELEMENT")) {
                if (elements.remove(row.get("AD_ELEMENT_ID"))) {
                    DictionaryFixture.row(data, model, "AD_ELEMENT", row);
                    referenceModules.add(row.get("AD_MODULE_ID"));
                }
            }
            if (!elements.isEmpty()) throw new AssertionError("Missing UI element metadata: " + elements);
            for (var row : rows("OBCLKER_UIDEFINITION")) {
                if (references.contains(row.get("AD_REFERENCE_ID"))) {
                    DictionaryFixture.row(data, model, "OBCLKER_UIDEFINITION", row);
                    referenceModules.add(row.get("AD_MODULE_ID"));
                }
            }
        }
        for (var row : rows("AD_REFERENCE")) {
            if (references.contains(row.get("AD_REFERENCE_ID")) && !Set.of("10", "13", "19").contains(row.get("AD_REFERENCE_ID"))) {
                DictionaryFixture.row(data, model, "AD_REFERENCE", row);
                if (Boolean.getBoolean("validation.originalUi")) referenceModules.add(row.get("AD_MODULE_ID"));
            }
        }
        referenceModules.remove("0");
        referenceModules.remove("PLATFORM");
        referenceModules.remove(null);
        if (!referenceModules.isEmpty()) {
            for (var row : rows("AD_MODULE")) {
                if (referenceModules.remove(row.get("AD_MODULE_ID"))) DictionaryFixture.row(data, model, "AD_MODULE", row);
            }
            if (!referenceModules.isEmpty()) throw new AssertionError("Missing reference-owner modules: " + referenceModules);
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
        if (Boolean.getBoolean("validation.originalUi")) {
            for (Path source : UI_SOURCES) result.addAll(rows(source, table));
        }
        return result;
    }

    static List<Map<String, String>> rows(Path source, String table) throws Exception {
        if (!Files.exists(source.resolve("sourcedata/" + table + ".xml"))) return List.of();
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
