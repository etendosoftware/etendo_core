package com.etendoerp.sequences.annotations;

import jakarta.enterprise.util.AnnotationLiteral;

public class SequenceQualifier extends AnnotationLiteral<SequenceFilter> implements SequenceFilter {

    String value;

    public SequenceQualifier() {}

    public SequenceQualifier(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return this.value;
    }
}
