package com.etendoerp.sequences.annotations;

import com.etendoerp.sequences.DefaultSequenceGenerator;
import org.hibernate.annotations.ValueGenerationType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@ValueGenerationType(generatedBy = DefaultSequenceGenerator.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface Sequence {
    String propertyName();
}
