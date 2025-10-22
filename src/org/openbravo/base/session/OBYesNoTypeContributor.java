package org.openbravo.base.session;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.BasicTypeRegistry;

/**
 * Registers OBYesNoType globally in Hibernate.
 * Compatible with Hibernate 6.5+ (preferred over MetadataBuilderContributor).
 */
public class OBYesNoTypeContributor implements TypeContributor {

  @Override
  public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
    BasicTypeRegistry registry =
        typeContributions.getTypeConfiguration().getBasicTypeRegistry();
    registry.register(OBYesNoType.INSTANCE);
  }
}
