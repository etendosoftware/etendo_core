package com.etendoerp.sequences.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.type.StandardBasicTypes;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * Singleton class to generate sequence values using a Database Sequence
 * Implements a Hibernate Service to have the necessary data to configure a {@link SequenceStyleGenerator} per database sequence
 * Configuration is done automatically by generated classes (see entitySequenceContributor.ftl)
 */
public class NonTransactionalSequenceServiceImpl implements NonTransactionalSequenceService, ServiceRegistryAwareService {
    public static final NonTransactionalSequenceServiceImpl INSTANCE = new NonTransactionalSequenceServiceImpl();
    private static final long serialVersionUID = 875777113674534563L;
    private static final Logger log = LogManager.getLogger();

    private transient ServiceRegistry serviceRegistry;
    private transient Dialect dialect;
    private transient Database database;
    private final Map<String, SequenceStyleGenerator> generators = new HashMap<>();

    private NonTransactionalSequenceServiceImpl() { }

    /**
     * Hibernate's database model
     * Used to configure a {@link SequenceStyleGenerator}
     */
    public Database getDatabase() {
        return database;
    }

    /**
     * Used by {@link NonTRXMetadataContributor} to collect database information
     * @param database the hibernate Database reference
     */
    public void setDatabase(Database database) {
        this.database = database;
    }

    /**
     * Hibernate's service registry
     * Used to configure a {@link SequenceStyleGenerator}
     */
    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    /**
     * Hibernate's current dialect depending on RDBMS
     * Used to configure a {@link SequenceStyleGenerator}
     */
    public Dialect getDialect() {
        return dialect;
    }

    /**
     * Obtains the next value of a database sequence using its associated generator.
     * @param ofSequenceName the name of the database sequence
     * @return the next value of the desired sequence as a String
     * @throws IllegalArgumentException when the generator was not initalized for the sequence requested.
     */
    public String nextValue(String ofSequenceName) {
        var gen = this.getGenerator(ofSequenceName);
        if (gen == null) {
            throw new IllegalArgumentException("A generator was not initialized for the sequence: " + ofSequenceName);
        }
        return String.valueOf(gen.generate((SharedSessionContractImplementor) OBDal.getInstance().getSession(), null));
    }

    /**
     * @param sequenceName The name of the database sequence
     * @return a {@link SequenceStyleGenerator} for this particular sequence.
     * @see #registerGenerator(String, String, String)
     */
    public SequenceStyleGenerator getGenerator(String sequenceName) {
        return generators.get(sequenceName);
    }

    /**
     * Register a generator for a certain database sequence, using the default optimizer provided by Hibernate.
     * @param sequenceName the database sequence name
     * @param initialValue the initial value configured for that sequence in db
     * @param incrementSize the increment size configured for that sequence in db
     */
    public void registerGenerator(String sequenceName, String initialValue, String incrementSize) {
        registerGenerator(sequenceName, initialValue, incrementSize, null);
    }

    /**
     * Register a generator for a certain database sequence, using the default optimizer provided by Hibernate.
     * @param sequenceName the database sequence name
     * @param initialValue the initial value configured for that sequence in db
     * @param incrementSize the increment size configured for that sequence in db
     * @param optimizer the optimizer to use when generating values
     */
    public void registerGenerator(String sequenceName, String initialValue, String incrementSize, String optimizer) {
        SequenceStyleGenerator generator = new SequenceStyleGenerator();
        Properties params = new Properties();

        params.put("sequence_name", sequenceName);
        params.put("initial_value", initialValue);
        params.put("increment_size", incrementSize);
        Optional.ofNullable(optimizer).ifPresent(strategy -> params.put("optimizer", strategy));

        try {
            generator.configure(StandardBasicTypes.LONG, params, serviceRegistry);
            generator.registerExportables(database);
        } catch (MappingException e) {
            throw new OBException(e.getMessage(), e);
        }

        generators.put(sequenceName, generator);
    }

    /**
     * Callback to inject the registry.
     * This will be used by hibernate to fill the necessary information so that later we are able to configure a {@link SequenceStyleGenerator}
     * @param serviceRegistry The registry
     */
    @Override
    public void injectServices(ServiceRegistryImplementor serviceRegistry) {
        log.debug("Injecting service registry to Non Transactional Sequence Service");
        this.serviceRegistry = serviceRegistry;
        this.dialect = serviceRegistry.getService(JdbcEnvironment.class).getDialect();
    }
}
