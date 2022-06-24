package com.etendoerp.base.weld

import com.etendoerp.base.EBaseSpecification
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.hibernate.dialect.function.SQLFunction
import org.jboss.arquillian.container.test.api.Deployment
import org.jboss.arquillian.junit.Arquillian
import org.jboss.shrinkwrap.api.Filters
import org.jboss.shrinkwrap.api.ShrinkWrap
import org.jboss.shrinkwrap.api.asset.EmptyAsset
import org.jboss.shrinkwrap.api.importer.ExplodedImporter
import org.jboss.shrinkwrap.api.spec.JavaArchive
import org.junit.runner.RunWith
import org.openbravo.base.session.OBPropertiesProvider
import org.openbravo.base.session.SessionFactoryController
import org.openbravo.base.weld.WeldUtils
import org.openbravo.client.kernel.KernelInitializer
import org.openbravo.dal.core.OBInterceptor
import org.openbravo.dal.core.SQLFunctionRegister

import javax.enterprise.inject.Any
import javax.enterprise.inject.Instance
import javax.enterprise.inject.spi.Bean
import javax.enterprise.inject.spi.BeanManager
import javax.enterprise.util.AnnotationLiteral
import javax.inject.Inject

//@RunWith(Arquillian.class)
class EWeldSpecification extends EBaseSpecification {
    private static final Logger log = LogManager.getLogger();

    private static boolean initialized = false;
    private static JavaArchive archive = null;

    @Deployment
    def createTestArchive() {
        if (archive == null) {

            // Setting this property to "true" is avoided throwing an error when this class is used with
            // analytics module. It is a workaround for Weld proxy bug. See issue:
            // https://issues.openbravo.com/view.php?id=32704
            System.setProperty("com.sun.jersey.server.impl.cdi.lookupExtensionInBeanManager", "true");

            log.info("Creating cdi archive...");
            final String sourcePath = OBPropertiesProvider.getInstance()
                    .getOpenbravoProperties()
                    .getProperty("source.path");
            archive = ShrinkWrap.create(JavaArchive.class);

            // add all beans without exclusions so cdi can also be used for *test* packages
            archive.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");

            // include all classes deployed in webapp container
            archive.as(ExplodedImporter.class).importDirectory(sourcePath + "/build/classes/", Filters.exclude("/groovy/*"));

            // ...and all the jUnit ones
            archive.as(ExplodedImporter.class).importDirectory(sourcePath + "/src-test/build/classes/", Filters.exclude("/groovy/*"));

            // include all libraries deployed in webapp container
            archive.addAsDirectory(sourcePath + "/WebContent/WEB-INF/lib");

            log.debug(archive.toString(true));
            log.info("... cdi archive created");
        }
        return archive;
    }

    @SuppressWarnings("serial")
    private static final AnnotationLiteral<Any> ANY = new AnnotationLiteral<Any>() {
    };

    @Inject
    private BeanManager beanManager;

    @Inject
    private WeldUtils weldUtils;

    @Inject
    private KernelInitializer kernelInitializer;

    @Inject
    @Any
    private Instance<SQLFunctionRegister> sqlFunctionRegisters;

    /**
     * Sets static instance bean manager in WeldUtils so it is globally accessible and initializes
     * kernel.
     *
     * Arquillian creates a new cdi container for each test class but keeps existent one for all tests
     * within same class, let's initialize it once per class but we cannot use setupSpec() at this
     * point because we require of beanManager to be injected.
     */
    def setup() throws Exception {
        if (!initialized) {
            initializeDalLayer(getSqlFunctions());
            WeldUtils.setStaticInstanceBeanManager(beanManager);
            kernelInitializer.setInterceptor();
            weldUtils.setBeanManager(beanManager);
            initialized = true;
        }
    }

    private Map<String, SQLFunction> getSqlFunctions() {
        Map<String, SQLFunction> sqlFunctions = new HashMap<>();
        if (sqlFunctionRegisters == null) {
            return sqlFunctions;
        }
        for (SQLFunctionRegister register : sqlFunctionRegisters) {
            Map<String, SQLFunction> registeredSqlFunctions = register.getSQLFunctions();
            if (registeredSqlFunctions == null) {
                continue;
            }
            sqlFunctions.putAll(registeredSqlFunctions);
        }
        return sqlFunctions;
    }

    /**
     * Once we are done with the class execution, OBInterceptor needs to be reset other case when
     * executing a suite it will reuse the container created for the previous classes instead of the
     * new one.
     */
    def cleanupSpec() {
        if (SessionFactoryController.getInstance() != null) {
            final OBInterceptor interceptor = (OBInterceptor) SessionFactoryController.getInstance()
                    .getConfiguration()
                    .getInterceptor();
            interceptor.setInterceptorListener(null);
        }
        initialized = false;
    }

    @SuppressWarnings("unchecked")
    protected <U extends Object> U getWeldComponent(Class<U> clz) {

        final Bean<?> bean = beanManager.getBeans(clz, ANY).iterator().next();

        return (U) beanManager.getReference(bean, clz, beanManager.createCreationalContext(bean));
    }
}

