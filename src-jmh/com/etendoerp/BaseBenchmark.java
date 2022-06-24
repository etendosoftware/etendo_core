package com.etendoerp;

import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.database.ExternalConnectionPool;
import org.openjdk.jmh.Main;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.io.IOException;

import static com.etendoerp.utilities.DalUtils.initializeDalLayer;

/**
 * Base class for benchmark suites.
 * Extend this class and create your own benchmarks following the JMH documentation.
 * The Dal context will be handled automatically.
 */
@State(Scope.Benchmark)
public class BaseBenchmark {
    @Setup()
    public void prepare() {
        initializeDalLayer(null);
    }

    @TearDown
    public void cleanUp() {
        // if not an administrator but still admin mode set throw an exception
        if (OBContext.getOBContext() != null
                && !OBContext.getOBContext().getUser().getId().equals("0")
                && !OBContext.getOBContext().getRole().getId().equals("0")
                && OBContext.getOBContext().isInAdministratorMode()) {
            OBContext.restorePreviousMode();
            throw new IllegalStateException(
                    "Each benchmark should take care of reseting admin mode correctly in a finally block, use OBContext.restorePreviousMode");
        }
        try {
            if (SessionHandler.isSessionHandlerPresent()) {
                if (SessionHandler.getInstance().getDoRollback()) {
                    SessionHandler.getInstance().rollback();
                } else if (SessionHandler.getInstance().getSession().getTransaction().isActive()) {
                    SessionHandler.getInstance().commitAndClose();
                } else {
                    SessionHandler.getInstance().getSession().close();
                }
            }
        } catch (final Exception e) {
            throw new OBException(e);
        } finally {
            if (SessionHandler.isSessionHandlerPresent(ExternalConnectionPool.READONLY_POOL)) {
                SessionHandler.getInstance().commitAndClose(ExternalConnectionPool.READONLY_POOL);
            }
            SessionHandler.deleteSessionHandler();
            OBContext.setOBContext((OBContext) null);
        }
    }

    public static void main(String[] args) throws IOException {
        Main.main(args);
    }
}
