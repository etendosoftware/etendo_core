package com.etendoerp;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.openbravo.base.session.SessionFactoryController;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.kernel.KernelInitializer;
import org.openbravo.dal.core.OBInterceptor;

import javax.inject.Inject;

/**
 * Base class for benchmark suites that need Weld to run properly.
 * Extend this class and implement your own benchmark following the JMH documentation.
 * The weld context will be handled automatically.
 */
public abstract class WeldBenchmark extends BaseBenchmark {
    private WeldContainer container;

    @Inject
    private KernelInitializer kernelInitializer;

    /**
     * Sets static instance bean manager in WeldUtils so it is globally accessible and initializes
     * kernel.
     */
    @Override
    public void prepare() {
        super.prepare();

        Weld weld = new Weld();
        container = weld.initialize();

        WeldUtils.setStaticInstanceBeanManager(container.getBeanManager());

        // FIXME: the default weld initialization does not load all the beans that we would expect.
        // More work is needed to load all classes in the same way that the WeldBaseTest class does using Archillian.
        // For now the workaround is to instantiate the desired classes directly, when possible.

        //TODO: call setInterceptor() on kernelInitializer once the weld injection works properly
    }

    /**
     * Once we are done with the class execution, OBInterceptor needs to be reset other case when
     * executing a suite it will reuse the container created for the previous classes instead of the
     * new one.
     */
    @Override
    public void cleanUp() {
        super.cleanUp();
        if (SessionFactoryController.getInstance() != null) {
            final OBInterceptor interceptor = (OBInterceptor) SessionFactoryController.getInstance()
                    .getConfiguration()
                    .getInterceptor();
            interceptor.setInterceptorListener(null);
        }
        container.shutdown();
    }
}
