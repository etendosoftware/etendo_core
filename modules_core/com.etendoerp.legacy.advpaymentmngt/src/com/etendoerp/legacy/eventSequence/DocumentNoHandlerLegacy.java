package com.etendoerp.legacy.eventSequence;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.application.eventSequence.DocumentNoHandlerSequenceActionInterface;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.model.common.order.Order;

import java.util.ArrayList;
import java.util.List;

import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.service.db.DalConnectionProvider;

public class DocumentNoHandlerLegacy implements DocumentNoHandlerSequenceActionInterface {
    private static Entity[] entities = null;
    private static Property[] documentNoProperties = null;
    private static Property[] documentTypeProperties = null;
    private static Property[] documentTypeTargetProperties = null;
    private static Property[] processedProperties = null;

    @Override
    public void handleEvent(EntityPersistenceEvent event) {
        int index = 0;
        for (int i = 0; i < entities.length; i++) {
          if (entities[i] == event.getTargetInstance().getEntity()) {
            index = i;
            break;
          }
        }
        Entity entity = entities[index];
        Property documentNoProperty = documentNoProperties[index];
        Property documentTypeProperty = documentTypeProperties[index];
        Property docTypeTargetProperty = documentTypeTargetProperties[index];
        Property processedProperty = processedProperties[index];

        String documentNo = (String) event.getCurrentState(documentNoProperty);
        boolean processed = false;
        Object oProcessed = (processedProperty == null ? false
            : event.getCurrentState(processedProperty));
        if (oProcessed instanceof String) {
          processed = "Y".equals(oProcessed.toString());
        } else if (oProcessed instanceof Boolean) {
          processed = (Boolean) oProcessed;
        }
        if (documentNo == null || documentNo.startsWith("<") && !processed) {
          final DocumentType docTypeTarget = (docTypeTargetProperty == null ? null
              : (DocumentType) event.getCurrentState(docTypeTargetProperty));
          final DocumentType docType = (documentTypeProperty == null ? null
              : (DocumentType) event.getCurrentState(documentTypeProperty));
          // use empty strings instead of null
          final String docTypeTargetId = docTypeTarget != null ? docTypeTarget.getId() : "";
          final String docTypeId = docType != null ? docType.getId() : "";
          String windowId = RequestContext.get().getRequestParameter("windowId");
          if (windowId == null) {
            windowId = "";
          }

          // recompute it
          documentNo = Utility.getDocumentNo(OBDal.getInstance().getConnection(false),
              new DalConnectionProvider(false), RequestContext.get().getVariablesSecureApp(), windowId,
              entity.getTableName(), docTypeTargetId, docTypeId, false, true);
          event.setCurrentState(documentNoProperty, documentNo);
        }
    }

    @Override
    public Entity[] getObservedEntities() {
        if (entities == null) {
            List<Entity> entityList = new ArrayList<>();
            List<Property> documentNoPropertyList = new ArrayList<>();
            List<Property> documentTypePropertyList = new ArrayList<>();
            List<Property> documentTypeTargetPropertyList = new ArrayList<>();
            List<Property> processedPropertyList = new ArrayList<>();
            for (Entity entity : ModelProvider.getInstance().getModel()) {
                for (Property prop : entity.getProperties()) {
                    if (prop.isUsedSequence() && !prop.isSequence()) {
                        entityList.add(entity);
                        documentNoPropertyList.add(prop);
                        if (entity.hasProperty(Order.PROPERTY_DOCUMENTTYPE)) {
                            documentTypePropertyList.add(entity.getProperty(Order.PROPERTY_DOCUMENTTYPE));
                        } else {
                            documentTypePropertyList.add(null);
                        }
                        if (entity.hasProperty(Order.PROPERTY_TRANSACTIONDOCUMENT)) {
                            documentTypeTargetPropertyList
                                    .add(entity.getProperty(Order.PROPERTY_TRANSACTIONDOCUMENT));
                        } else {
                            documentTypeTargetPropertyList.add(null);
                        }
                        if (entity.hasProperty(Order.PROPERTY_PROCESSED)) {
                            processedPropertyList.add(entity.getProperty(Order.PROPERTY_PROCESSED));
                        } else {
                            processedPropertyList.add(null);
                        }
                        break;
                    }
                }
            }
            entities = entityList.toArray(new Entity[entityList.size()]);
            documentNoProperties = documentNoPropertyList
                    .toArray(new Property[documentNoPropertyList.size()]);
            documentTypeProperties = documentTypePropertyList
                    .toArray(new Property[documentTypePropertyList.size()]);
            documentTypeTargetProperties = documentTypeTargetPropertyList
                    .toArray(new Property[documentTypeTargetPropertyList.size()]);
            processedProperties = processedPropertyList
                    .toArray(new Property[processedPropertyList.size()]);
        }
        return entities;
    }
}
