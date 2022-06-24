package com.etendoerp.sequences.dimensions;

import com.etendoerp.sequences.SequenceUtils;
import org.openbravo.data.Sqlc;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.domain.DimensionsList;

public class DimensionListRequestParameterFormat extends ASequenceDimensionList<String> {

    public DimensionListRequestParameterFormat(Column column) {
        super(column);
    }

    /**
     * Saves a SequenceDimension Object transforming the key.
     * The column key will be transformed to a RequestContext parameter format without the external module and db prefix.
     * Ex: The column key 'Column_Name_Id' will be transformed to 'inpcolumnNameId'.
     * Ex: The column key 'Em_Dbprefix_Column_Name_Id' will be transformed to 'inpcolumnNameId'.
     * @param key The name of the column.
     * @param dimension The dimension to save.
     */
    @Override
    public void setDimensionTransformingKey(String key, SequenceDimension dimension) {
        this.dimensionMap.put(transformKey(key), dimension);
    }

    /**
     * Gets the SequenceDimension Object mapped with the truncated value of a Key.
     * The expected key must have the format of a RequestContext parameter 'inpcolumnNameId'.
     * The key will be truncated taking off the external module and db prefix.
     * Ex: The key 'inpemDbprefixColumnNameId' will be truncated to 'inpcolumnNameId'.
     * @param key The value used to search a dimension
     * @return A SequenceDimension Object
     */
    @Override
    public SequenceDimension getDimensionTransformingKey(String key) {
        return this.dimensionMap.get(SequenceUtils.truncateParameter(key));
    }

    @Override
    public void handleDimensionListEntity(DimensionsList dimensionsList) {
        SequenceDimension sequenceDimension = new SequenceDimension(dimensionsList);
        this.setDimensionTransformingKey(dimensionsList.getColumn().getDBColumnName(), sequenceDimension);
    }

    /**
     * Transform a column with the format 'Column_Name_Id' to a RequestContext format 'inpcolumnNameId'.
     * The external module and database prefix from the column will be removed.
     * @param key The column to Transform.
     * @return The transformed column.
     */
    public String transformKey(String key) {
        String transformedColumn = SequenceUtils.INP + Sqlc.TransformaNombreColumna(key);
        return SequenceUtils.truncateParameter(transformedColumn);
    }

    /**
     * Gets the SequenceDimension Object mapped with a column in a RequestContext format.
     * The expected key must have the original format of a column 'Column_Name_Id'.
     * The column key will be transformed to a RequestContext format before performing the search.
     * @param key The column name.
     * @return SequenceDimension object.
     */
    SequenceDimension getDimensionTransformingOriginalColumn(String key) {
        return this.dimensionMap.get(transformKey(key));
    }

}
