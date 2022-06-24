package com.etendoerp.sequences.parameters;

import com.etendoerp.sequences.transactional.RequiredDimensionException;
import org.openbravo.base.model.domaintype.ForeignKeyDomainType;
import java.util.HashMap;
import java.util.Map;

public class SequenceParameterList {

    Map<String, SequenceParameter> parameterMap;

    public SequenceParameterList() {
        this.parameterMap = new HashMap<>();
    }

    public void setParameter(String sequenceProperty, SequenceParameter sequenceParameter) {
        this.parameterMap.put(sequenceProperty, sequenceParameter);
    }

    public SequenceParameter getParameter(String sequenceProperty) {
        return this.parameterMap.get(sequenceProperty);
    }

    public Map<String, SequenceParameter> getParameterMap() {
        return parameterMap;
    }

    public void putAll(SequenceParameterList sequenceParameterList) {
        parameterMap.putAll(sequenceParameterList.getParameterMap());
    }

    public String generateWhereClause() throws RequiredDimensionException {

        final StringBuilder whereClause = new StringBuilder();
        whereClause.append(" as ent ");
        whereClause.append(" where ");

        StringBuilder conditions = new StringBuilder();
        SequenceParametersUtils.validateRequiredDimensions(this.parameterMap);

        for (Map.Entry<String, SequenceParameter> entry : parameterMap.entrySet()) {
            String key = entry.getKey();
            SequenceParameter sequenceParameter = entry.getValue();
            String id = "";

            if (sequenceParameter.getDomainType() instanceof ForeignKeyDomainType) {
                id = ".id";
            }

            conditions.append(" ent.").append(key).append(id).append("=:").append(key).append(" and ");
        }
        conditions.replace(conditions.lastIndexOf("and"), conditions.toString().length(),"");
        whereClause.append(conditions);
        return whereClause.toString();
    }

    public Map<String, Object> getParameterValues() {
        Map<String, Object> values = new HashMap<>();
        for (Map.Entry<String, SequenceParameter> entry : parameterMap.entrySet()) {
            values.put(entry.getKey(), entry.getValue().getDataValue());
        }
        return values;
    }

}
