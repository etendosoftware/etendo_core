package com.etendoerp.sequences.model;

import org.openbravo.base.model.ModelObject;
import org.openbravo.base.model.Reference;

public class SequenceConfiguration extends ModelObject {

    private Reference reference;
    private String generator;
    private String dbSequenceName;
    private String dbSequenceInitial;
    private String dbSequenceIncrement;

    public String getGenerator() {
        return generator;
    }

    public void setGenerator(String generator) {
        this.generator = generator;
    }

    public Reference getReference() {
        return reference;
    }

    public void setReference(Reference reference) {
        this.reference = reference;
    }

    public String getDbSequenceName() {
        return dbSequenceName;
    }

    public void setDbSequenceName(String dbSequenceName) {
        this.dbSequenceName = dbSequenceName;
    }

    public String getDbSequenceInitial() {
        return dbSequenceInitial;
    }

    public void setDbSequenceInitial(String dbSequenceInitial) {
        this.dbSequenceInitial = dbSequenceInitial;
    }

    public String getDbSequenceIncrement() {
        return dbSequenceIncrement;
    }

    public void setDbSequenceIncrement(String dbSequenceIncrement) {
        this.dbSequenceIncrement = dbSequenceIncrement;
    }
}
