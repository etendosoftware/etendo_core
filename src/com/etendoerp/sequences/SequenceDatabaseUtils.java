package com.etendoerp.sequences;


import com.etendoerp.sequences.parameters.SequenceParameterList;
import com.etendoerp.sequences.transactional.RequiredDimensionException;
import org.apache.commons.lang3.StringUtils;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.dal.service.Restrictions;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.domain.DimensionsList;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.domain.SequenceConfig;
import org.openbravo.model.common.enterprise.DocumentType;

import java.util.*;

public class SequenceDatabaseUtils {

    public static final String PROPERTY_DOCUMENTTYPE        = "documentType";
    public static final String PROPERTY_DOCUMENTTYPE_TARGET = "transactionDocument";
    public static final String PROPERTY_CLIENT              = "client";
    public static final String PROPERTY_ORGANIZATION        = "organization";
    public static final String PROPERTY_COLUMN              = "column";

    public static final String PREFIX = "<";

    public final static String DIMENSION_ERROR_CODE = "SequenceRequiredDimensionError";
    public final static String SEQUENCE_NOT_FOUND   = "SequenceNotFound";

    // The order of the values should not be changed.
    public static final List<String> DEFAULT_DIMENSIONS = Arrays.asList(
            PROPERTY_DOCUMENTTYPE,
            PROPERTY_CLIENT,
            PROPERTY_ORGANIZATION,
            PROPERTY_COLUMN,
            PROPERTY_DOCUMENTTYPE_TARGET
    );

    public static final List<String> REQUIRED_DIMENSIONS = Arrays.asList(
            PROPERTY_CLIENT,
            PROPERTY_ORGANIZATION,
            PROPERTY_COLUMN
    );

    private SequenceDatabaseUtils() {}

    public static List<DimensionsList> getDimensionLists(Reference reference) {
        List<DimensionsList> dimensionsLists = new ArrayList<>();
        SequenceConfig sequenceConfig = getSequenceConfiguration(reference);
        if (sequenceConfig != null) {
            final OBCriteria<DimensionsList> dimensionsListOBCriteria = OBDal.getInstance().createCriteria(DimensionsList.class);
            dimensionsListOBCriteria.add(Restrictions.eq(DimensionsList.PROPERTY_SEQUENCECONFIG, sequenceConfig));
            dimensionsLists = dimensionsListOBCriteria.list();
        }
        return dimensionsLists;
    }

    /**
     * Obtains the SequenceConfiguration of a Reference.
     * The SequenceConfiguration is the Entity which contains the java class name to instantiate.
     * The number of results returned MUST be unique.
     *
     * @param reference Reference use to obtain the SequenceConfiguration
     * @return A SequenceConfiguration
     */
    public static SequenceConfig getSequenceConfiguration(Reference reference) {
        final OBCriteria<SequenceConfig> sequenceConfigOBCriteria = OBDal.getInstance().createCriteria(SequenceConfig.class);
        sequenceConfigOBCriteria.add(Restrictions.eq(SequenceConfig.PROPERTY_REFERENCE, reference));
        return (SequenceConfig) sequenceConfigOBCriteria.uniqueResult();
    }


    public static BaseOBObject getFromClass(Class<? extends BaseOBObject> cls, SequenceParameterList sequenceParameterList) throws RequiredDimensionException {
        Map<String, Object> parameters = sequenceParameterList.getParameterValues();
        OrganizationStructureProvider structureProvider = new OrganizationStructureProvider();
        List<String> orgList = structureProvider.getParentList((String) parameters.get(PROPERTY_ORGANIZATION), true);
        for (String orgId : orgList) {
            parameters.put(PROPERTY_ORGANIZATION, orgId);
            BaseOBObject sequence = search(cls, sequenceParameterList.generateWhereClause(), parameters);
            if (sequence != null) {
                return sequence;
            }
        }
        return null;
    }

    public static BaseOBObject search(Class<? extends BaseOBObject> cls, String whereClause, Map<String, Object> parameters) {
        final OBQuery<BaseOBObject> qry = OBDal.getInstance().createQuery((Class<BaseOBObject>) cls, whereClause);
        qry.setNamedParameters(parameters);
        List<BaseOBObject> listOfSequences = qry.list();
        if (listOfSequences.size() > 1) {
            throw new OBException(getNameOfADSequencesOnConflicts(parameters));
        } else {
            return !listOfSequences.isEmpty() ? listOfSequences.get(0) : null;
        }
    }

    private static String getNameOfADSequencesOnConflicts(Map<String, Object> parameters) {
        String sequenceDocuments = OBMessageUtils.getI18NMessage("OneMoreSequencesWithDocumentType");
        String documentType = (String) parameters.get(PROPERTY_DOCUMENTTYPE);
        if (!StringUtils.isEmpty(documentType)) {
            DocumentType currentDocType = OBDal.getInstance().get(DocumentType.class, documentType);
            sequenceDocuments = String.format(sequenceDocuments, currentDocType.getName());
        }
        return sequenceDocuments;
    }

}
