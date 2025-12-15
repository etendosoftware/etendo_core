/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2010-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package com.etendoerp.legacy.advancedpaymentmngt;

/**
 * MIGRATED TO HIBERNATE 6
 * - Replaced org.hibernate.criterion.* with jakarta.persistence.criteria.*
 * - This file was automatically migrated from Criteria API to JPA Criteria API
 * - Review and test thoroughly before committing
 */


import org.hibernate.LockOptions;
import org.hibernate.Session;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.hibernate.query.Query;
import org.openbravo.advpaymentmngt.utility.FIN_SequenceActionInterface;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.Restrictions;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;

@ApplicationScoped
@Default
public class Fin_UtilityLegacy implements FIN_SequenceActionInterface {

    @Override
    public String getNextDocNumberAndIncrementSeqIfUpdateNext(final boolean updateNext,
                                                                     final Sequence seq) {
        final StringBuilder nextDocNumber = new StringBuilder();
        if (seq.getPrefix() != null) {
            nextDocNumber.append(seq.getPrefix());
        }
        nextDocNumber.append(seq.getNextAssignedNumber().toString());
        if (seq.getSuffix() != null) {
            nextDocNumber.append(seq.getSuffix());
        }

        incrementSeqIfUpdateNext(updateNext, seq);

        return nextDocNumber.toString();
    }

    @Override
    public void incrementSeqIfUpdateNext(final boolean updateNext, final Sequence seq) {
        if (updateNext) {
            seq.setNextAssignedNumber(seq.getNextAssignedNumber() + seq.getIncrementBy());
            OBDal.getInstance().save(seq);
        }
    }
    /**
     * Returns the next sequence number of the Document Type defined for the Organization and document
     * category.
     *
     * @param docType
     *          Document type of the document
     * @param tableName
     *          the name of the table from which the sequence will be taken if the Document Type does
     *          not have any sequence associated.
     * @param updateNext
     *          Flag to update the current number of the sequence
     * @return the next sequence number of the Document Type defined for the Organization and document
     *         category. Null if no sequence is found.
     */
    @Override
    public String getDocumentNo(DocumentType docType, String tableName, boolean updateNext) {
        if (docType != null) {
            Sequence seq = docType.getDocumentSequence();
            if (seq == null && tableName != null) {
                OBCriteria<Sequence> obcSeq = OBDal.getInstance().createCriteria(Sequence.class);
                obcSeq.add(Restrictions.eq(Sequence.PROPERTY_NAME, tableName));
                obcSeq.setMaxResults(1);
                seq = (Sequence) obcSeq.uniqueResult();
            }
            return getDocumentNo(updateNext, seq);
        }
        return null;
    }

    @Override
    public String getDocumentNo(boolean updateNext, Sequence seqParam) {
        if (seqParam == null) {
            return null;
        }
        Sequence seq = getSequenceAndLockIfUpdateNext(updateNext, seqParam);
        return getNextDocNumberAndIncrementSeqIfUpdateNext(updateNext, seq);
    }

    @Override
    public Sequence getSequenceAndLockIfUpdateNext(final boolean updateNext,
                                                          final Sequence seqParam) {
        if (updateNext) {
            // We lock the sequence with a select for update to avoid duplicates
            return lockSequence(seqParam.getId());
        }
        return seqParam;
    }

    /**
     * Returns the next sequence number of the Document Type defined for the Organization and document
     * category. The current number of the sequence is also updated.
     *
     * @param docType
     *          Document type of the document
     * @param tableName
     *          the name of the table from which the sequence will be taken if the Document Type does
     *          not have any sequence associated.
     * @return the next sequence number of the Document Type defined for the Organization and document
     *         category. Null if no sequence is found.
     */
    @Override
    public String getDocumentNo(DocumentType docType, String tableName) {
        return getDocumentNo(docType, tableName, true);
    }

    @Override
    public Sequence lockSequence(String sequenceId) {
        // @formatter:off
        final String where = ""
                + "select s "
                + "from ADSequence s "
                + "where id = :id";
        // @formatter:on
        final Session session = OBDal.getInstance().getSession();
        final Query<Sequence> query = session.createQuery(where, Sequence.class);
        query.setParameter("id", sequenceId);
        query.setMaxResults(1);
        query.setLockOptions(LockOptions.UPGRADE);
        return query.uniqueResult();
    }

    /**
     * Returns the next sequence number of the Document Type defined for the Organization and document
     * category. The current number of the sequence is also updated.
     *
     * @param org
     *          the Organization for which the Document Type is defined. The Document Type can belong
     *          to the parent organization tree of the specified Organization.
     * @param docCategory
     *          the document category of the Document Type.
     * @param tableName
     *          the name of the table from which the sequence will be taken if the Document Type does
     *          not have any sequence associated.
     * @return the next sequence number of the Document Type defined for the Organization and document
     *         category. Null if no sequence is found.
     */
    @Override
    public String getDocumentNo(Organization org, String docCategory, String tableName) {
        DocumentType outDocType = FIN_Utility.getDocumentType(org, docCategory);
        return getDocumentNo(outDocType, tableName, true);
    }

    /**
     * Returns the next sequence number of the Document Type defined for the Organization and document
     * category.
     *
     * @param org
     *          the Organization for which the Document Type is defined. The Document Type can belong
     *          to the parent organization tree of the specified Organization.
     * @param docCategory
     *          the document category of the Document Type.
     * @param tableName
     *          the name of the table from which the sequence will be taken if the Document Type does
     *          not have any sequence associated.
     * @return the next sequence number of the Document Type defined for the Organization and document
     *         category. Null if no sequence is found.
     */
    @Override
    public String getDocumentNo(Organization org, String docCategory, String tableName,
                                boolean updateNext) {
        DocumentType outDocType = FIN_Utility.getDocumentType(org, docCategory);
        return getDocumentNo(outDocType, tableName, updateNext);
    }


}
