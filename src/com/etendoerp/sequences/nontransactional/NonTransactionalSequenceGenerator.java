package com.etendoerp.sequences.nontransactional;

import com.etendoerp.sequences.DefaultSequenceGenerator;
import com.etendoerp.sequences.SequenceDatabaseUtils;
import com.etendoerp.sequences.services.NonTransactionalSequenceServiceImpl;
import org.hibernate.Session;
import org.openbravo.base.model.Property;
import org.openbravo.base.structure.BaseOBObject;

/**
 * Returns the next value from a database sequence associated to a certain property of an entity.
 * @see NonTransactionalSequenceServiceImpl
 */
public class NonTransactionalSequenceGenerator extends DefaultSequenceGenerator {
    private static final long serialVersionUID = -6352429640146444769L;

    public NonTransactionalSequenceGenerator(String propertyValue) {
        super(propertyValue);
    }

    @Override
    public String generateValue(Session session, Object owner) {
        String value;
        BaseOBObject current = (BaseOBObject) owner;
        Property sequenceProperty = current.getEntity().getProperty(propertyValue);

        value = (String) current.get(propertyValue);

        if (value != null && !value.isBlank() && !value.startsWith(SequenceDatabaseUtils.PREFIX)) {
            // The value was changed by the user, keep it.
            return value;
        }

        value = NonTransactionalSequenceServiceImpl.INSTANCE.nextValue(sequenceProperty.getDBSequenceName());
        return value;
    }
}
