package com.etendoerp.sequences.transactional;

import com.etendoerp.WeldBenchmark;
import com.etendoerp.legacy.advancedpaymentmngt.Fin_UtilityLegacy;
import com.etendoerp.legacy.utilitySequence.UtilitySequenceLegacy;
import com.etendoerp.sequences.SequenceMaskFormatter;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.service.db.DalConnectionProvider;
import org.openjdk.jmh.annotations.*;

import java.text.ParseException;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class TransactionalSequenceClassicVSNew extends WeldBenchmark {

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public String newSequenceFormatting() throws ParseException {
        return new SequenceMaskFormatter("y/#####").valueToString(1234);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public String newSequenceImplementation(TransactionalSequenceStates.NewSequenceState state) throws MaskValueGenerationException {
        return TransactionalSequenceUtils.getNextValueFromSequence(state.sequence, true);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public String classicSequenceImplementation(TransactionalSequenceStates.ClassicSequenceState state) {
        // FIX ME: this should be using FIN_Utility.getDocumentNo() directly, correct once weld initialization is in place.
        return new Fin_UtilityLegacy().getDocumentNo(OBDal.getInstance().get(DocumentType.class, state.defaultDocumentType),
                state.defaultTableName);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public String legacySequenceImplementation(TransactionalSequenceStates.ClassicSequenceState state) {
        VariablesSecureApp vars = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(),
                OBContext.getOBContext().getCurrentClient().getId(),
                OBContext.getOBContext().getCurrentOrganization().getId());
        // FIX ME: this should be using Utility.getDocumentNo() directly, correct once weld initialization is in place.
        return new UtilitySequenceLegacy().getDocumentNo(OBDal.getInstance().getConnection(false),
                new DalConnectionProvider(false), vars, "", state.defaultTableName, "", state.defaultDocumentType, false, true);
    }
}
