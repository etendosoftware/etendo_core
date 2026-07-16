package com.etendoerp.sequences.transactional;

import com.etendoerp.sequences.SequenceMaskFormatter;
import com.etendoerp.sequences.SequenceDatabaseUtils;
import com.etendoerp.sequences.parameters.SequenceParameterList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.utility.Sequence;

public class TransactionalSequenceUtils {

    protected static final Logger log = LogManager.getLogger();

    public static final String TRANSACTIONAL_SEQUENCE_ID = "B82E1C56F57749AD97DD9924624F08D3";

    public static final String SEQUENCE_ERROR_PARCE_CODE = "SequenceParceError";

    private TransactionalSequenceUtils() {}

    public static String getNextValueFromSequence(Sequence sequence, final Boolean updateNext) throws MaskValueGenerationException {
        if (sequence != null) {
            if (updateNext) {
                sequence = lockSequence(sequence.getId());
            }

            String nextValue = processNextValueFromSequence(sequence);

            if (updateNext) {
                updateNextSequenceValue(sequence);
            }

            return nextValue;
        }
        return null;
    }

    public static String processNextValueFromSequence(Sequence sequence) throws MaskValueGenerationException {
        String defaultMask = sequence.getMask();
        String prefix = sequence.getPrefix();
        String suffix = sequence.getSuffix();
        Object currentNext = sequence.getNextAssignedNumber();

        final String defaultInput = "";
        StringBuilder nextVal = new StringBuilder();

        if (prefix != null) {
            nextVal.append(generateMaskValue(prefix,defaultInput));
        }
        nextVal.append(generateMaskValue(defaultMask, currentNext));
        if (suffix != null) {
            nextVal.append(generateMaskValue(suffix,defaultInput));
        }
        return nextVal.toString();
    }

    public static String generateMaskValue(String mask, Object value) throws MaskValueGenerationException {
        try {
            var formatter = new SequenceMaskFormatter(mask);
            return formatter.valueToString(value);
        } catch (Exception e) {
            throw new MaskValueGenerationException(e);
        }
    }

    public static void updateNextSequenceValue(Sequence sequence) {
        sequence.setNextAssignedNumber(sequence.getNextAssignedNumber() + sequence.getIncrementBy());
        OBDal.getInstance().save(sequence);
    }

    /**
     * Returns the {@link Sequence} holding a pessimistic row lock, guaranteeing its in-memory
     * state matches the locked row.
     *
     * <p>A locking HQL query is NOT enough here: when the entity is already present in the
     * Hibernate session (callers such as {@code DefaultTransactionalSequence.generateValue()}
     * always look the sequence up, unlocked, before asking for its next value), the query
     * result resolves to the cached instance and its stale {@code nextAssignedNumber} survives,
     * so two concurrent transactions can both compute the same value even though the SQL lock
     * itself worked. {@code refresh(entity, UPGRADE)} both acquires the {@code SELECT ... FOR
     * UPDATE} lock and rehydrates the entity from the locked row.</p>
     *
     * <p>Do NOT call {@code session.flush()} here. {@code generateValue()} — and therefore this
     * method — is invoked BY Hibernate itself from inside {@code AbstractEntityPersister.insert()}
     * while an outer flush is already iterating the action queue
     * ({@code preInsertInMemoryValueGeneration}). A nested {@code flush()} re-enters that same
     * iteration, which re-invokes {@code generateValue()} for the very insert still in progress,
     * recursing until the stack overflows. Any increment pending in this transaction is flushed
     * either by that outer, already-running flush or by the caller's normal per-record flush
     * (e.g. one row of a batch import), so a locked refresh alone is enough to see it.</p>
     */
    public static Sequence lockSequence(String sequenceId) {
        final Session session = OBDal.getInstance().getSession();
        final Sequence sequence = OBDal.getInstance().get(Sequence.class, sequenceId);
        if (sequence == null) {
            return null;
        }
        session.refresh(sequence, LockOptions.UPGRADE);
        return sequence;
    }

    public static Sequence getSequenceFromParameters(SequenceParameterList sequenceParameterList) throws RequiredDimensionException, NotFoundSequenceException {
        var sequence = (Sequence) SequenceDatabaseUtils.getFromClass(Sequence.class, sequenceParameterList);
        if (sequence == null) {
            throw new NotFoundSequenceException();
        }
        return sequence;
    }

}
