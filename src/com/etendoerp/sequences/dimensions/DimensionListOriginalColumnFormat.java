package com.etendoerp.sequences.dimensions;

import com.etendoerp.sequences.SequenceUtils;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.domain.DimensionsList;

public class DimensionListOriginalColumnFormat extends ASequenceDimensionList<String>{

    public DimensionListOriginalColumnFormat(Column column) {
        super(column);
    }

    /**
     * Saves a SequenceDimension Object using the truncated value of a key.
     * The column key will be truncated and converted to lower case.
     * Ex: The column parameter 'Em_Dbpref_Column_Name_Id' will be truncated to 'column_name_id'.
     * @param key The column name to be truncated and used has Key.
     * @param dimension The dimension to save.
     */
    @Override
    public void setDimensionTransformingKey(String key, SequenceDimension dimension) {
        this.dimensionMap.put(transformKey(key), dimension);
    }

    /**
     * Gets the SequenceDimension Object mapped with the column key parameter.
     * The column key will be truncated and converted to lower case before used to obtain the SequenceDimension.
     * Ex: The column parameter 'Em_Dbpref_Column_Name_Id' will be truncated to 'column_name_id'.
     * @param key The column name to be truncated and used has Key.
     * @return A SequenceDimension Object
     */
    @Override
    public SequenceDimension getDimensionTransformingKey(String key) {
        return this.dimensionMap.get(transformKey(key));
    }

    @Override
    public void handleDimensionListEntity(DimensionsList dimensionsList) {
        SequenceDimension sequenceDimension = new SequenceDimension(dimensionsList);
        this.setDimensionTransformingKey(dimensionsList.getColumn().getDBColumnName(), sequenceDimension);
    }

    /**
     * Converts a column key with the format 'Column_Name_Id' taking off the the external module
     * and database prefix. The resulting value is returned in lowercase.
     * @param key The column name to transform
     * @return The transformed column in lower case.
     */
    public String transformKey(String key) {
        String truncatedColumn = SequenceUtils.truncateColumn(key);
        return truncatedColumn.toLowerCase();
    }

}
