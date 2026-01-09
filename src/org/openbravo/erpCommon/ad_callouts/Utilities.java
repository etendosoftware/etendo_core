package org.openbravo.erpCommon.ad_callouts;

/**
 * MIGRATED TO HIBERNATE 6
 * - Replaced org.hibernate.criterion.* with jakarta.persistence.criteria.*
 * - This file was automatically migrated from Criteria API to JPA Criteria API
 * - Review and test thoroughly before committing
 */


import com.etendoerp.sequences.NextSequenceValue;
import com.etendoerp.sequences.UINextSequenceValueInterface;

import jakarta.persistence.criteria.CriteriaBuilder;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.Restrictions;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;

import java.util.List;

public class Utilities {

        private Utilities(){}

        public static Field getField(SimpleCallout.CalloutInfo info) {
                Field field = null;
                // Get Tab.
                final Tab tab = OBDal.getInstance().get(Tab.class, info.getStringParameter("inpTabId"));
                // Get DocumentNo field that belongs to the tab.
                final OBCriteria<Field> criteria = OBDal.getInstance().createCriteria(Field.class);
                criteria.setFilterOnReadableClients(false);
                criteria.setFilterOnReadableOrganization(false);
                criteria.add(Restrictions.eq(Field.PROPERTY_TAB, tab));
                criteria.add(Restrictions.ilike(Field.PROPERTY_NAME, "%Document No.%"));
                final List<Field> fields = criteria.list();
                if (!fields.isEmpty()) {
                        field = fields.get(0);
                }
                return field;
        }

        public static String getDocumentNo(Field field) {
                UINextSequenceValueInterface sequenceHandler = null;
                sequenceHandler = NextSequenceValue.getInstance()
                    .getSequenceHandler(field.getColumn().getReference().getId());
                if (sequenceHandler != null) {
                        String documentNo = sequenceHandler.generateNextSequenceValue(field,
                            RequestContext.get());
                        return documentNo;
                }
                return null;
        }

}
