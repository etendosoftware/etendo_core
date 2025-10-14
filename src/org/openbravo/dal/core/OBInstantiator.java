package org.openbravo.dal.core;

import java.io.Serializable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.spi.EntityInstantiator;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.DynamicOBObject;
import org.openbravo.base.structure.Identifiable;
import org.openbravo.base.util.Check;

/**
 * Hibernate 6.0 entity instantiator for Etendo DAL entities.
 * Implements the EntityInstantiator SPI (Hibernate 6.0.x).
 */
public class OBInstantiator implements EntityInstantiator {

  private static final long serialVersionUID = 1L;
  private static final Logger log = LogManager.getLogger();

  private final String entityName;
  private final Class<?> mappedClass;

  public OBInstantiator(PersistentClass mappingInfo) {
    this.entityName = mappingInfo.getEntityName();
    this.mappedClass = mappingInfo.getMappedClass();
    log.debug("Creating OBInstantiator for {}", entityName);
  }

  /** Instantiate a new instance of the entity. */
  @Override
  public Object instantiate(SessionFactoryImplementor sessionFactory) {
    if (mappedClass != null) {
      final Identifiable obObject = (Identifiable) OBProvider.getInstance().get(mappedClass);
      Check.isTrue(
          obObject.getEntityName().equals(entityName),
          "Entity name mismatch. Expected " + entityName + " but got " + obObject.getEntityName()
      );
      return obObject;
    } else {
      final DynamicOBObject dob = new DynamicOBObject();
      dob.setEntityName(entityName);
      return dob;
    }
  }

  /**
   * Instantiate an instance and set its id using the parameter. Used by Hibernate when loading
   * existing instances from the database.
   *
   * @param id
   *          the id to set in the instance
   */
  public Object instantiate(Object id, SessionFactoryImplementor sessionFactory) {
    if (mappedClass != null) {
      final Identifiable obObject = (Identifiable) OBProvider.getInstance().get(mappedClass);
      if (id != null) {
        obObject.setId(id);
      }
      Check.isTrue(
          obObject.getEntityName().equals(entityName),
          "Entity name mismatch. Expected " + entityName + " but got " + obObject.getEntityName()
      );
      return obObject;
    } else {
      final DynamicOBObject dob = new DynamicOBObject();
      dob.setEntityName(entityName);
      if (id instanceof String) {
        dob.setId((String) id);
      }
      return dob;
    }
  }

  /**
   * Returns true if the object is an instance of the Entity handled by the OBInstantiator.
   *
   * @param object
   *          the object to compare with the Entity managed here
   * @return true if the object is an Entity managed by this class
   */
  @Override
  public boolean isInstance(Object object, SessionFactoryImplementor sessionFactory) {
    if (object instanceof Identifiable) {
      if (mappedClass != null) {
        return mappedClass.isInstance(object);
      }
      return entityName.equals(((Identifiable) object).getEntityName());
    }
    return false;
  }

  /**
   * Indica si el objeto es exactamente de la misma clase "mappedClass".
   * (Requisito del SPI de Hibernate 6.0: org.hibernate.tuple.Instantiator)
   */
  @Override
  public boolean isSameClass(Object object, SessionFactoryImplementor sessionFactory) {
    if (object == null) {
      return false;
    }
    if (mappedClass != null) {
      return object.getClass() == mappedClass;
    }
    // Para el caso dinámico, como no hay mappedClass fija, devolvemos false.
    // (Hibernate usa esto para optimizaciones de identidad)
    return false;
  }
}
