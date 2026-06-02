package com.etendoerp.sequences;

import java.lang.reflect.Member;
import java.util.EnumSet;

import com.etendoerp.sequences.annotations.Sequence;
import org.hibernate.Session;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.AnnotationBasedGenerator;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.generator.GeneratorCreationContext;

/**
 * Base class for Hibernate sequence generators that produce values before statement execution.
 * Subclasses define the actual generation strategy by implementing {@link #generateValue(Session, Object)}.
 */
public abstract class DefaultSequenceGenerator implements AnnotationBasedGenerator<Sequence>, BeforeExecutionGenerator {
    private static final long serialVersionUID = 7231888894694188218L;
    protected String propertyValue;

    protected DefaultSequenceGenerator(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    public String getPropertyValue() {
        return propertyValue;
    }

    @Override
    public void initialize(Sequence sequence, Member member, GeneratorCreationContext context) {
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

    @Override
    public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue,
        EventType eventType) {
        return generateValue((Session) session, owner);
    }

    /**
     * Defines when this class will generate a value. By default the value will be generated only on inserts.
     * @return The insert-only event set is used by default.
     */
    @Override
    public EnumSet<EventType> getEventTypes() {
        return EventTypeSets.INSERT_ONLY;
    }
}
