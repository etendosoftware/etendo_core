package com.etendoerp.sequences;

import com.etendoerp.sequences.annotations.Sequence;
import org.hibernate.Session;
import org.hibernate.tuple.AnnotationValueGeneration;
import org.hibernate.tuple.GenerationTiming;
import org.hibernate.tuple.ValueGenerator;

public abstract class DefaultSequenceGenerator implements AnnotationValueGeneration<Sequence>  {
    private static final long serialVersionUID = 7231888894694188218L;
    protected String propertyValue;

    protected DefaultSequenceGenerator(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    public String getPropertyValue() {
        return propertyValue;
    }

    @Override
    public void initialize(Sequence sequence, Class<?> aClass) {
        this.propertyValue = sequence.propertyName();
    }

    /**
     * This method generates the sequence value to be inserted into the database.
     * The strategy will depend on the class that implements this method.
     * @param session The Session from which the request originates.
     * @param owner The instance of the object owning the attribute for which we are generating a value.
     * @return The generated value
     */
    public abstract String generateValue(Session session, Object owner);

    /**
     * Returns the in-memory generated value
     * @return {@code true}
     */
    @Override
    public ValueGenerator<String> getValueGenerator() {
        return this::generateValue;
    }

    /**
     * Defines when this class will generate a value. By default the value will be generated only on inserts.
     * @return The {@link GenerationTiming#INSERT} is used by default.
     */
    @Override
    public GenerationTiming getGenerationTiming() {
        return GenerationTiming.INSERT;
    }

    /**
     * Returns false because the value is generated in-memory.
     * @return false
     */
    @Override
    public boolean referenceColumnInSql() {
        return false;
    }

    /**
     * Returns null because the value is generated in-memory.
     * @return null
     */
    @Override
    public String getDatabaseGeneratedReferencedColumnValue() {
        return null;
    }
}
