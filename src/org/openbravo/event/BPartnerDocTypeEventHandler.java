package org.openbravo.event;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.Restrictions;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.BusinessPartnerDocType;
import org.openbravo.model.common.enterprise.Organization;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;

/**
 * Validates uniqueness of Business Partner Document Type per combination of
 * Document Category and Sales Transaction (IsSOTrx).
 */
@Dependent
public class BPartnerDocTypeEventHandler extends EntityPersistenceEventObserver {
  private static final Entity[] entities = { ModelProvider.getInstance().getEntity(BusinessPartnerDocType.ENTITY_NAME)};

  @Override
  protected Entity[] getObservedEntities() { 
    return entities; 
  }

  // Validates on INSERT.
  public void onSave(@Observes EntityNewEvent e) {
    if (!isValidEvent(e)) {
      return;
    }
    final BusinessPartnerDocType bpDocType = (BusinessPartnerDocType) e.getTargetInstance();
    validate(bpDocType);
  }

  // Validates on UPDATE.
  public void onUpdate(@Observes EntityUpdateEvent e) {
    if (!isValidEvent(e)) {
      return;
    }
    final BusinessPartnerDocType bpDocType = (BusinessPartnerDocType) e.getTargetInstance();
    validate(bpDocType);
  }

  /**
   * Ensures there is no other active record for the same Client, Business Partner,
   * Document Category and IsSOTrx.
   * @param bpDocType the candidate row being persisted
   * @throws OBException with key @CBPDT_DUP@ if a duplicate exists
   */
  private void validate(BusinessPartnerDocType bpDocType) {
    if (!bpDocType.isActive()){
      return;
    }
    OBContext.setAdminMode(true);
    try {
      String id = bpDocType.getId();
      Client client = bpDocType.getClient();
      BusinessPartner bp = bpDocType.getBusinessPartner();
      String documentCat = bpDocType.getDocumentcategory();
      Boolean isSotrx = bpDocType.isSotrx();
      Organization org = bpDocType.getOrganization();
  
      OBCriteria<BusinessPartnerDocType> bpDocTypeCriteria = OBDal.getInstance().createCriteria(BusinessPartnerDocType.class);
      bpDocTypeCriteria.add(Restrictions.eq(BusinessPartnerDocType.PROPERTY_CLIENT, client));
      bpDocTypeCriteria.add(Restrictions.eq(BusinessPartnerDocType.PROPERTY_ORGANIZATION, org));
      bpDocTypeCriteria.add(Restrictions.eq(BusinessPartnerDocType.PROPERTY_BUSINESSPARTNER, bp));
      bpDocTypeCriteria.add(Restrictions.eq(BusinessPartnerDocType.PROPERTY_DOCUMENTCATEGORY, documentCat));
      bpDocTypeCriteria.add(Restrictions.eq(BusinessPartnerDocType.PROPERTY_ISSOTRX, isSotrx));
      bpDocTypeCriteria.add(Restrictions.eq(BusinessPartnerDocType.PROPERTY_ACTIVE, true));
      if (id != null) {
        bpDocTypeCriteria.add(Restrictions.ne(BusinessPartnerDocType.PROPERTY_ID, id));
      }
      bpDocTypeCriteria.setMaxResults(1);
      BusinessPartnerDocType bpDocTypeResult = (BusinessPartnerDocType) bpDocTypeCriteria.uniqueResult();
      if (bpDocTypeResult != null) {
        throw new OBException(OBMessageUtils.messageBD("BPDocTypeUnique"));
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
