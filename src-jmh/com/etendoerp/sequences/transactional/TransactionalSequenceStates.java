package com.etendoerp.sequences.transactional;

import com.etendoerp.RequiresAdminState;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.utility.Sequence;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

public class TransactionalSequenceStates {
    private TransactionalSequenceStates() {}

    @State(Scope.Benchmark)
    public static class ClassicSequenceState extends RequiresAdminState {
        String defaultTableName = "C_Order";
        String defaultDocumentType = "466AF4B0136A4A3F9F84129711DA8BD3";
    }

    @State(Scope.Benchmark)
    public static class NewSequenceState extends RequiresAdminState {
        String sequenceId = null;
        Sequence sequence = null;

        @Setup
        public void createSequence() {
            sequence = OBProvider.getInstance().get(Sequence.class);
            sequence.setNewOBObject(true);
            sequence.setAutoNumbering(true);
            sequence.setName("benchmark-sequence");
            sequence.setMask("###");
            sequence.setStartingNo(100L);
            sequence.setNextAssignedNumber(100L);
            OBDal.getInstance().save(sequence);
            OBDal.getInstance().flush();
            sequenceId = sequence.getId();
        }

        @TearDown
        public void deleteSequence() {
            OBDal.getInstance().remove(sequence);
        }
    }
}
