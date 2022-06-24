package com.etendoerp.sequences.dimensions;

import com.etendoerp.sequences.SequenceDatabaseUtils;
import com.etendoerp.sequences.parameters.SequenceParameter;
import com.etendoerp.sequences.parameters.SequenceParameterList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.domain.DimensionsList;
import org.openbravo.model.ad.domain.Reference;

import java.util.HashMap;
import java.util.Map;

public abstract class ASequenceDimensionList<T> {

    protected static final Logger log = LogManager.getLogger();

    Map<T, SequenceDimension> dimensionMap;
    Reference reference;
    Column column;

    public ASequenceDimensionList(Column column) {
        this.dimensionMap = new HashMap<>();
        this.column = column;
        loadReference();
        loadDimensionsMap();
    }

    public void setDimension(T key, SequenceDimension dimension) {
        dimensionMap.put(key, dimension);
    }

    public SequenceDimension getDimension(T key) {
        return dimensionMap.get(key);
    }

    public Boolean isEmpty () {
        return dimensionMap.isEmpty();
    }

    /**
     * Verifies that the dimensions set by a user are in the parameter list.
     *
     * @param sequenceParameterList The parameters used to search a sequence.
     */
    public void validateParametersList(SequenceParameterList sequenceParameterList) {
        var parameterMap = sequenceParameterList.getParameterMap();
        for (Map.Entry<T, SequenceDimension> entry : this.dimensionMap.entrySet()) {
            String sequencePropertyName = entry.getValue().getPropertyName();
            SequenceParameter parameter = parameterMap.get(sequencePropertyName);
            if (parameter == null || (parameter.getDataValue() instanceof String && ((String) parameter.getDataValue()).isBlank())) {
                T sequenceColumn = entry.getKey();
                String sequenceColumnName = sequenceColumn.toString();
                String errMessage = "The Column "+ sequenceColumnName +" MUST be included in the request and have a value. " +
                        "The Column has been set in the Dimension List and will be used to filter the Sequence.";
                log.error(errMessage);
                String[] params = {sequenceColumnName};
                String message = OBMessageUtils.getI18NMessage("SequenceDimensionListError", params);
                throw new OBException(message);
            }
        }
    }

    public abstract void setDimensionTransformingKey(T key, SequenceDimension dimension);

    public abstract SequenceDimension getDimensionTransformingKey(T key);

    public abstract void handleDimensionListEntity(DimensionsList dimensionsList);

    public void loadReference() {
        if (this.column != null) {
            Reference auxRef = this.column.getReferenceSearchKey();
            if (auxRef == null) {
                auxRef = this.column.getReference();
            }
            this.reference = auxRef;
        }
    }

    public void loadDimensionsMap() {
        if (reference != null) {
            var list = SequenceDatabaseUtils.getDimensionLists(this.reference);
            for (var dimension : list) {
                handleDimensionListEntity(dimension);
            }
        }
    }

}
