package com.smf.mobile.utils.webservices;

/**
 * MIGRATED TO HIBERNATE 6
 * - Replaced org.hibernate.criterion.* with jakarta.persistence.criteria.*
 * - This file was automatically migrated from Criteria API to JPA Criteria API
 * - Review and test thoroughly before committing
 */


import com.smf.mobile.utils.data.MobileIdentifier;
import com.smf.mobile.utils.data.TabConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.client.kernel.reference.UIDefinition;
import org.openbravo.client.kernel.reference.UIDefinitionController;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.Sqlc;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.ui.AuxiliaryInput;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.geography.Country;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.json.JsonUtils;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

public class WindowUtils {
    private static final Logger log = LogManager.getLogger();

    private WindowUtils() {}

    public static JSONArray getParentColumns(Tab tab) {
        JSONArray jsonColumns = new JSONArray();

        tab.getTable()
           .getADColumnList()
           .stream()
           .filter(column -> tab.getTabLevel() > 0 && column.isLinkToParentColumn())
           .forEach(column -> jsonColumns.put(getEntityColumnName(column)));

        return jsonColumns;
    }

    public static String getEntityColumnName(Column column) {
        String name;
        String tableName = column.getTable().getName();
        String columnName = column.getDBColumnName();

        name = ModelProvider.getInstance().getEntity(tableName).getPropertyByColumnName(columnName).getName();

        return name;
    }

    public static JSONArray getTabIdentifiers(Tab tab) throws JSONException {
        JSONArray identifiers = new JSONArray();

        OBCriteria<TabConfiguration> tabConfigurationOBCriteria = OBDal.getInstance().createCriteria(TabConfiguration.class);
        tabConfigurationOBCriteria.addEqual(TabConfiguration.PROPERTY_TAB, tab);
        tabConfigurationOBCriteria.setMaxResults(1);

        TabConfiguration config = (TabConfiguration) tabConfigurationOBCriteria.uniqueResult();
        if (config !=  null) {
            OBCriteria<MobileIdentifier> mobileIdentifierOBCriteria = OBDal.getInstance().createCriteria(MobileIdentifier.class);
            mobileIdentifierOBCriteria.addEqual(MobileIdentifier.PROPERTY_TABCONFIGURATION, config);
            mobileIdentifierOBCriteria.addOrderBy(MobileIdentifier.PROPERTY_SEQUENCENUMBER, true);

            for (MobileIdentifier identifier : mobileIdentifierOBCriteria.list()) {
                JSONObject jsonIdentifier = new JSONObject();
                jsonIdentifier.put("id", identifier.getId());
                jsonIdentifier.put("field", identifier.getField().getId());
                jsonIdentifier.put("sequenceNumber", identifier.getSequenceNumber());
                identifiers.put(jsonIdentifier);
            }
        }

        return identifiers;
    }

    static public JSONObject computeColumnValues(Tab tab, String parentId, Entity parentEntity, JSONObject context) throws JSONException {
        JSONObject defaultValues = new JSONObject();
        if (tab != null) {
            defaultValues.put("_entityName", ModelProvider.getInstance().getEntityByTableId(tab.getTable().getId()).getName());
            for (Field field : tab.getADFieldList()) {
                if (field.getColumn() == null || (field.getProperty() != null && !field.getProperty().isEmpty())) {
                    continue;
                }
                Property property = ModelProvider.getInstance().getEntityByTableId(tab.getTable().getId()).getPropertyByColumnName(field.getColumn().getDBColumnName());
                if (property.isId()) {
                    continue;
                }
                if (property.isActiveColumn()) {
                    defaultValues.put(property.getName(), true);
                    continue;
                }
                if (field.getColumn().isLinkToParentColumn() && parentId != null) {
                    BaseOBObject parent = OBDal.getInstance().get(property.getTargetEntity().getName(),parentId);
                    if(parent != null){
                        defaultValues.put(property.getName(), parent.getId());
                        defaultValues.put(property.getName() + "$_identifier", parent.getIdentifier());
                    }
                    continue;
                }
                if (property.getName().equalsIgnoreCase("documentno")) {
                    defaultValues.put(property.getName(), "<auto>");
                    continue;
                }

                UIDefinition uiDef = UIDefinitionController.getInstance().getUIDefinition(field.getColumn().getId());
                JSONObject jsonDefaultValue = new JSONObject(uiDef.getFieldProperties(field, false));
                Object defaultValue = jsonDefaultValue.has("value") ? jsonDefaultValue.get("value") : null;

                if (defaultValue == null && field.getColumn().getDefaultValue() != null) {
                    parseDefaultValue(field.getColumn().getDefaultValue(),property.isBoolean(),tab,parentId,parentEntity, context);
                }

                Object value = defaultValue != null ? defaultValue : property.getActualDefaultValue();
                if (value != null) {
                    if (property.isPrimitive() && property.isNumericType()) {
                        defaultValues.put(property.getName(), new BigDecimal(value.toString()));
                    } else {
                        defaultValues.put(property.getName(), value);
                    }

                    if (!property.isPrimitive() && value instanceof String && !((String) value).isEmpty()) {
                        BaseOBObject bob = OBDal.getInstance().get(property.getTargetEntity().getName(), value);
                        if (bob != null) {
                            defaultValues.put(property.getName() + "$_identifier", bob.getIdentifier());
                        }
                    }
                }
            }
        }
        return defaultValues;
    }

    static public Object parseDefaultValue(String _defaultValue, Boolean parseYN, Tab tab, String parentId, Entity parentEntity, JSONObject context) throws JSONException {
        if (_defaultValue == null || _defaultValue.isBlank()) {
            return null;
        }
        String defaultValue = _defaultValue.trim().toUpperCase();
        if (parseYN && defaultValue.equals("Y")) {
            return true;
        } else if (parseYN && defaultValue.equals("N")) {
            return false;
        }
        else {
            User user = OBContext.getOBContext().getUser();
            Organization org = OBContext.getOBContext().getCurrentOrganization();
            String orgId = org.getId();
            String clientId = OBContext.getOBContext().getCurrentClient().getId();
            String separator = defaultValue.startsWith("@SQL=") ? "'" :"";

            //auxInputs
            for (AuxiliaryInput auxInput : tab.getADAuxiliaryInputList()) {
                String value = parseAuxiliaryInput(auxInput, tab.getWindow().getId(), context);
                value = value.equals("Y") ? (separator.isEmpty() ? "true" : "Y") : (value.equals("N") ? (separator.isEmpty() ? "false" : "N") : value);
                defaultValue = defaultValue.replace("@"+auxInput.getName().toUpperCase()+"@", value);
            }
            if(context != null) {
                for (Iterator<?> it = context.keys(); it.hasNext(); ) {
                    String key = (String) it.next();
                    defaultValue = defaultValue.replace(key, separator + context.getString(key) + separator);
                }
            }

            defaultValue = defaultValue.replace("@AD_ORG_ID@", separator+orgId+separator);
            defaultValue = defaultValue.replace("@#AD_ORG_ID@", separator+orgId+separator);
            defaultValue = defaultValue.replace("@AD_CLIENT_ID@", separator+clientId+separator);
            defaultValue = defaultValue.replace("@#AD_CLIENT_ID@", separator+clientId+separator);
            defaultValue = defaultValue.replace("@#DATE@", JsonUtils.createDateTimeFormat().format(new Date()));
            defaultValue = defaultValue.replace("@C_CURRENCY_ID@",org.getCurrency() != null ? org.getCurrency().getId() : "");
            defaultValue = defaultValue.replace("@ISSOTRX@",tab.getWindow().isSalesTransaction() ? (separator.isEmpty() ? "true" : "Y") : (separator.isEmpty() ? "false" : "N"));
            defaultValue = defaultValue.replace("@ACCT_DIMENSION_DISPLAY@","false");
            defaultValue = defaultValue.replace("@#AD_USER_ID@", separator + user.getId() + separator);
            defaultValue = defaultValue.replace("@AD_USER_ID@", separator + user.getId() + separator);
            if (defaultValue.contains("@COUNTRYDEF@")) {
                OBCriteria<Country> countryCriteria = OBDal.getInstance().createCriteria(Country.class);
                countryCriteria.addEqual(Country.PROPERTY_DEFAULT, true);
                countryCriteria.setMaxResults(1);
                Country country = (Country) countryCriteria.uniqueResult();
                String countryId = country != null ? country.getId() : "";
                defaultValue = defaultValue.replace("@COUNTRYDEF@", countryId);
            }



            if (parentId != null) {
                OBCriteria<Column> parentColumnCriteria = OBDal.getInstance().createCriteria(Column.class).addEqual(Column.PROPERTY_TABLE,tab.getTable());
                parentColumnCriteria.addEqual(Column.PROPERTY_LINKTOPARENTCOLUMN,true);
                Column parentColumn = null;
                for (Column column:parentColumnCriteria.list()) {
                    Entity targetEntity = ModelProvider.getInstance().getEntityByTableId(tab.getTable().getId()).getPropertyByColumnName(column.getDBColumnName()).getTargetEntity();
                    if (targetEntity.equals(parentEntity)) {
                        parentColumn = column;
                    }
                }

                if (parentColumn != null) {
                    defaultValue = defaultValue.replace("@"+parentColumn.getDBColumnName().toUpperCase()+"@",separator+parentId+separator);
                }
            }


            if (!separator.isEmpty()) {
                String query = defaultValue.substring(5);
                ConnectionProvider conn = new DalConnectionProvider(false);
                try {
                    PreparedStatement st = conn.getPreparedStatement(query);
                    ResultSet rs = st.executeQuery();
                    rs.next();
                    defaultValue = rs.getString(1);
                    rs.close();
                    st.close();
                } catch (Exception e) {
                    log.error("Cannot execute default value query " + defaultValue, e );
                    defaultValue = null;
                }
                return defaultValue;
            }

            return defaultValue;
        }

    }

    public static String parseAuxiliaryInput(AuxiliaryInput input, String windowId, JSONObject context) {
        try {
            String code = input.getValidationCode();
            log.debug("Auxiliary Input: " + input.getName() + " Code:" + code);
            Object fvalue = null;
            if (code.startsWith("@SQL=")) {
                ArrayList<String> params = new ArrayList<>();
                String sql = UIDefinition.parseSQL(code, params);
                log.debug("Transformed SQL code: " + sql);
                int indP = 1;
                try (PreparedStatement ps = OBDal.getInstance().getConnection(false).prepareStatement(sql)) {
                    for (String parameter : params) {
                        String value;
                        if (parameter.charAt(0) == '#') {
                            value = Utility.getContext(new DalConnectionProvider(false),
                                    RequestContext.get().getVariablesSecureApp(), parameter, windowId);
                            if (value == null) {
                                value = context.optString(parameter);
                            }
                        } else {
                            String fieldId = "inp" + Sqlc.TransformaNombreColumna(parameter);
                            value = context.optString(fieldId);
                        }
                        log.debug("Parameter: " + parameter + ": Value " + value);
                        ps.setObject(indP++, value);
                    }
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        fvalue = rs.getObject(1);
                    }
                }
            } else if (code.startsWith("@")) {
                String codeWithoutAt = code.substring(1, code.length() - 1);
                fvalue = Utility.getContext(new DalConnectionProvider(false),
                        RequestContext.get().getVariablesSecureApp(), codeWithoutAt, windowId);
            } else {
                fvalue = code;
            }
            return fvalue != null ? fvalue.toString() : "";
        } catch (Exception e) {
            log.error(
                    "Error while computing auxiliary input parameter: " + input.getName() + " from tab: "
                            + input.getTab().getName() + " of window: " + input.getTab().getWindow().getName(),
                    e);
        }
        return "";
    }
}
