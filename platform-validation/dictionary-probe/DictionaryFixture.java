package com.etendoerp.platform.validation;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.ddlutils.io.DatabaseIO;
import org.apache.ddlutils.model.Database;
import org.apache.ddlutils.model.Table;
import org.w3c.dom.Element;

/** Derives a small physical dictionary from existing core definitions, not replacement mappings. */
final class DictionaryFixture {
    private static final String UPDATED = "2026-09-06 00:00:00";

    private DictionaryFixture() {}

    static Database schema(DatabaseIO xml) throws Exception {
        Database result = new Database();
        result.setName("PLATFORM_DICTIONARY");
        for (String type : new String[] {"Table", "Column", "Reference", "RefTable", "RefSearch",
                "RefList", "Module", "Package", "SequenceConfiguration"}) {
            String relative = type.equals("SequenceConfiguration")
                    ? "com/etendoerp/sequences/model/" : "org/openbravo/base/model/";
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            var document = factory.newDocumentBuilder().parse(new File("../src/" + relative + type + ".hbm.xml"));
            Element mapped = (Element) document.getElementsByTagName("class").item(0);
            String name = mapped.getAttribute("table").toUpperCase(Locale.ROOT);
            Set<String> columns = new LinkedHashSet<>();
            for (var node = mapped.getFirstChild(); node != null; node = node.getNextSibling()) {
                if (!(node instanceof Element element)) continue;
                if (!Set.of("id", "property", "many-to-one").contains(element.getTagName())) continue;
                String column = element.hasAttribute("column") ? element.getAttribute("column") : element.getAttribute("name");
                columns.add(column.toUpperCase(Locale.ROOT));
            }
            if (name.equals("AD_TABLE") || name.equals("AD_COLUMN")) {
                columns.add("HELP");
                columns.add("DEVELOPMENTSTATUS");
            }
            Table original = xml.readplain(new File("../src-db/database/model/tables/" + name + ".xml")).getTable(0);
            Table table = new Table();
            table.setName(name);
            table.setPrimaryKey(original.getPrimaryKey());
            for (String column : columns) {
                var found = original.findColumn(column, false);
                if (found == null) throw new IllegalStateException("Missing physical dictionary column " + name + "." + column);
                table.addColumn((org.apache.ddlutils.model.Column) found.clone());
            }
            result.addTable(table);
        }
        result.mergeWith(xml.read(new File("fixtures/v1.xml")));
        // TableDir resolves the target table from the physical foreign-key column name.
        Table requests = result.findTable("PP_REQUEST");
        requests.findColumn("CATEGORY_ID").setName("PP_CATEGORY_ID");
        requests.getForeignKey(0).getReference(0).setLocalColumnName("PP_CATEGORY_ID");
        if (Boolean.getBoolean("validation.security")) SecurityFixture.addSchema(result, xml);
        result.initialize();
        xml.write(result, new File("build/dictionary-schema.xml"));
        // Resolve foreign-table objects from the final XML, after merging and projecting columns.
        return xml.read(new File("build/dictionary-schema.xml"));
    }

    static Path data(Database schema) throws Exception {
        StringBuilder data = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<data>\n");
        row(data, schema, "AD_MODULE", Map.of("AD_MODULE_ID", "PLATFORM", "NAME", "Platform fixture",
                "JAVAPACKAGE", "com.etendoerp.platform.fixture", "SEQNO", "0"));
        row(data, schema, "AD_PACKAGE", Map.of("AD_PACKAGE_ID", "FIXTURE", "NAME", "Fixture entities",
                "JAVAPACKAGE", "com.etendoerp.platform.fixture", "AD_MODULE_ID", "PLATFORM"));
        for (String id : new String[] {"10", "13", "19"}) {
            row(data, schema, "AD_REFERENCE", Map.of("AD_REFERENCE_ID", id, "NAME", "Reference " + id,
                    "ISBASEREFERENCE", "Y", "MODEL_IMPL", "org.openbravo.base.model.domaintype."
                            + (id.equals("19") ? "TableDirDomainType" : "StringDomainType")));
        }
        for (String name : new String[] {"PP_CATEGORY", "PP_REQUEST"}) {
            boolean category = name.equals("PP_CATEGORY");
            row(data, schema, "AD_TABLE", Map.of("AD_TABLE_ID", name, "NAME", category ? "ProofCategory" : "ProofRequest",
                    "CLASSNAME", category ? "Category" : "Request", "TABLENAME", name,
                    "DATAORIGINTYPE", "Table", "ACCESSLEVEL", "7", "AD_PACKAGE_ID", "FIXTURE", "ISDELETEABLE", "Y"));
            Table application = schema.findTable(name);
            int position = 0;
            for (var column : application.getColumns()) {
                Map<String, String> values = new LinkedHashMap<>();
                values.put("AD_COLUMN_ID", columnId(name, column.getName()));
                values.put("AD_TABLE_ID", name);
                values.put("AD_MODULE_ID", "PLATFORM");
                values.put("COLUMNNAME", column.getName());
                values.put("NAME", column.getName().equals("AD_CLIENT_ID") ? "Client"
                        : column.getName().equals("AD_ORG_ID") ? "Organization"
                        : column.getName().equals("ISACTIVE") ? "Active"
                        : column.getName().equals("PP_CATEGORY_ID") ? "Category"
                        : column.getName().substring(0, 1) + column.getName().substring(1).toLowerCase(Locale.ROOT));
                values.put("AD_REFERENCE_ID", column.isPrimaryKey() ? "13" : column.getName().equals("ISACTIVE") ? "20"
                        : column.getName().endsWith("_ID") ? "19" : "10");
                values.put("FIELDLENGTH", column.getSize());
                values.put("POSITION", Integer.toString(++position));
                values.put("SEQNO", Integer.toString(position));
                values.put("ISKEY", column.isPrimaryKey() ? "Y" : "N");
                values.put("ISMANDATORY", column.isRequired() ? "Y" : "N");
                values.put("ISUPDATEABLE", "Y");
                values.put("ISIDENTIFIER", column.getName().equals("NAME") || column.getName().equals("TITLE") ? "Y" : "N");
                if (Boolean.getBoolean("validation.originalUi") && name.equals("PP_REQUEST")
                        && column.getName().equals("TITLE")) values.put("READONLYLOGIC", "@IsActive@='N'");
                row(data, schema, "AD_COLUMN", values);
            }
        }
        if (Boolean.getBoolean("validation.security")) SecurityFixture.addData(data, schema);
        if (Boolean.getBoolean("validation.originalUi")) UiDictionaryFixture.addData(data, schema);
        data.append("</data>\n");
        Path path = Path.of("build", "dictionary-data.xml");
        Files.writeString(path, data);
        return path;
    }

    static String columnId(String table, String column) {
        // DBSM compares metadata primary keys numerically in radix 32.
        return java.util.UUID.nameUUIDFromBytes((table + ":" + column)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString().replace("-", "").toUpperCase(Locale.ROOT);
    }

    static void row(StringBuilder xml, Database schema, String tableName, Map<String, String> supplied) {
        Map<String, String> values = new LinkedHashMap<>();
        if (Boolean.getBoolean("validation.originalUi")) {
            values.putAll(Map.of("AD_CLIENT_ID", "0", "AD_ORG_ID", "0", "CREATED", UPDATED,
                    "CREATEDBY", "0", "UPDATEDBY", "0"));
            if (tableName.equals("AD_MODULE")) values.put("DESCRIPTION", "Platform validation module metadata");
        }
        values.put("UPDATED", UPDATED);
        for (var column : schema.findTable(tableName).getColumns()) {
            if (column.getType().equals("CHAR") && "1".equals(column.getSize())) {
                values.put(column.getName(), column.getName().equals("ISACTIVE") ? "Y" : "N");
            }
        }
        values.putAll(supplied);
        xml.append("  <").append(tableName).append(">\n");
        for (var value : values.entrySet()) {
            if (value.getValue() == null) continue;
            if (schema.findTable(tableName).findColumn(value.getKey(), false) == null) continue;
            xml.append("    <").append(value.getKey()).append("><![CDATA[")
                    .append(value.getValue()).append("]]></").append(value.getKey()).append(">\n");
        }
        xml.append("  </").append(tableName).append(">\n");
    }
}
