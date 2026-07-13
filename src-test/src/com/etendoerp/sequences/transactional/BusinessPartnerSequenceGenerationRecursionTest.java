package com.etendoerp.sequences.transactional;

import static org.junit.Assert.assertNotNull;

import java.util.UUID;

import org.junit.Test;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.domain.DimensionsList;
import org.openbravo.model.ad.domain.SequenceConfig;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Category;
import org.openbravo.test.base.OBBaseTest;

/**
 * Reproduces a {@link StackOverflowError} caused by calling {@code session.flush()} inside
 * {@link TransactionalSequenceUtils#lockSequence(String)}.
 *
 * <p>{@code DefaultTransactionalSequence.generateValue()} is invoked BY Hibernate itself, from
 * inside {@code AbstractEntityPersister.insert()}, as part of an ALREADY IN-PROGRESS flush
 * ({@code preInsertInMemoryValueGeneration}, called while {@code ActionQueue.executeActions()}
 * is iterating). Calling {@code session.flush()} again from {@code lockSequence()} re-enters
 * that same action-queue processing, which re-triggers {@code generateValue()} for the same
 * pending insert, recursing until the stack overflows.</p>
 *
 * <p>Neither {@link TransactionalSequenceUtilsTest} nor
 * {@link DefaultTransactionalSequencePreloadRaceTest} catch this: both call
 * {@code getNextValueFromSequence()} directly, outside of any Hibernate-driven insert flush.
 * Saving a real {@link BusinessPartner} (whose {@code EM_Etgo_Identifier} column is wired to
 * this exact generator — see ETP-4469) is the only way to exercise the real call path.</p>
 */
public class BusinessPartnerSequenceGenerationRecursionTest extends OBBaseTest {

  @Test
  public void savingABusinessPartnerMustNotStackOverflowOnSequenceGeneration() {
    setTestUserContext();
    addReadWriteAccess(BusinessPartner.class);
    addReadWriteAccess(Category.class);
    addReadWriteAccess(Sequence.class);
    addReadWriteAccess(SequenceConfig.class);
    addReadWriteAccess(DimensionsList.class);
    addReadWriteAccess(Column.class);

    BusinessPartner bp = OBProvider.getInstance().get(BusinessPartner.class);
    String uniqueSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    bp.setName("SeqRecursionTest-" + uniqueSuffix);
    bp.setSearchKey("SEQREC-" + uniqueSuffix);
    bp.setBusinessPartnerCategory(OBDal.getInstance().get(Category.class, TEST_BP_CATEGORY_ID));

    // Deliberately do NOT set emEtgoIdentifier: DefaultTransactionalSequence.generateValue()
    // only takes the real, locked generation path when the property is blank.
    OBDal.getInstance().save(bp);
    OBDal.getInstance().flush();

    assertNotNull("BusinessPartner must be persisted (and generateValue() must not recurse)",
        bp.getId());
  }
}
