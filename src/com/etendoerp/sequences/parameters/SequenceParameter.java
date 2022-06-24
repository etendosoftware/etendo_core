package com.etendoerp.sequences.parameters;

import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.domaintype.DomainType;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.domain.Reference;

public class SequenceParameter {

    private final Object dataValue;
    private Column column;
    private DomainType domainType;

    public SequenceParameter(Object value, Column column) {
        this.dataValue = value;
        this.column = column;
        loadDomainType();
    }

    public SequenceParameter(Object value, DomainType domainType) {
        this.dataValue = value;
        this.domainType = domainType;
    }

    public Object getDataValue() {
        return dataValue;
    }

    public DomainType getDomainType() {
        return domainType;
    }

    /**
     * Sets the DomainType
     */
    public void loadDomainType() {
        if (column != null) {
            Reference reference = column.getReferenceSearchKey();
            if (reference == null) {
                reference = column.getReference();
            }
            this.domainType = ModelProvider.getInstance().getReference(reference.getId()).getDomainType();
        }
    }

}
