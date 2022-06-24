package org.openbravo.advpaymentmngt.utility;

import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;

public interface FIN_SequenceActionInterface {
    Sequence getSequenceAndLockIfUpdateNext(boolean updateNext,
                                                   Sequence seqParam);

    String getNextDocNumberAndIncrementSeqIfUpdateNext(boolean updateNext,
                                                       Sequence seq);

    void incrementSeqIfUpdateNext(boolean updateNext, Sequence seq);

    String getDocumentNo(DocumentType docType, String tableName, boolean updateNext);

    String getDocumentNo(boolean updateNext, Sequence seqParam);

    String getDocumentNo(DocumentType docType, String tableName);

    Sequence lockSequence(String sequenceId);

    String getDocumentNo(Organization org, String docCategory, String tableName);

    String getDocumentNo(Organization org, String docCategory, String tableName,
                         boolean updateNext);
}
