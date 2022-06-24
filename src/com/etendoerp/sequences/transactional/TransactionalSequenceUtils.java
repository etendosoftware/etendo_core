package com.etendoerp.sequences.transactional;

import com.etendoerp.sequences.SequenceMaskFormatter;
import com.etendoerp.sequences.SequenceDatabaseUtils;
import com.etendoerp.sequences.parameters.SequenceParameterList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.query.Query;
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

    public static Sequence lockSequence(String sequenceId) {
        final String where = "" +
                "select s " +
                "from ADSequence s " +
                "where id = :id";
        final Session session = OBDal.getInstance().getSession();
        final Query<Sequence> query = session.createQuery(where, Sequence.class);
        query.setParameter("id", sequenceId);
        query.setMaxResults(1);
        query.setLockOptions(LockOptions.UPGRADE);
        return query.uniqueResult();
    }

    public static Sequence getSequenceFromParameters(SequenceParameterList sequenceParameterList) throws RequiredDimensionException, NotFoundSequenceException {
        var sequence = (Sequence) SequenceDatabaseUtils.getFromClass(Sequence.class, sequenceParameterList);
        if (sequence == null) {
            throw new NotFoundSequenceException();
        }
        return sequence;
    }

}
