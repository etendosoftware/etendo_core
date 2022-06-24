package com.etendoerp.sequences.dimensions;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.model.ad.domain.DimensionsList;

/**
 * This class contains a DimensionList entity, set by a user in the Sequence Configuration,
 * mapped with the corresponding Sequence.class entity Property, used to perform the search of a Sequence.
 */
public class SequenceDimension {

    public final static String AD_SEQUENCE_ENTITY = "ADSequence";

    //Entity class for entity DimensionsList (stored in table ad_dimensions_list).
    private final DimensionsList originalDimensionList;

    //Sequence.class property
    private Property property;

    public SequenceDimension(DimensionsList dimensionsList) {
        this.originalDimensionList = dimensionsList;
        initializeProperty();
    }

    public void initializeProperty() {
        if (this.originalDimensionList != null) {
            Entity sequenceEntity = ModelProvider.getInstance().getEntity(AD_SEQUENCE_ENTITY);
            String columnName = this.originalDimensionList.getColumn().getDBColumnName();
            this.property = sequenceEntity.getPropertyByColumnName(columnName, false);
        }
    }

    public DimensionsList getOriginalDimensionList() {
        return originalDimensionList;
    }

    public Property getProperty() {
        return this.property;
    }

    public String getPropertyName() {
        return this.property.getName();
    }

}
