package com.etendoerp.sequences.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import java.util.Map;

public class NonTransactionalSequenceInitiatior implements StandardServiceInitiator<NonTransactionalSequenceService> {
    private static final Logger log = LogManager.getLogger();
    public static final NonTransactionalSequenceInitiatior INSTANCE = new NonTransactionalSequenceInitiatior();

    private NonTransactionalSequenceInitiatior() { }

    @Override
    public Class<NonTransactionalSequenceService> getServiceInitiated() {
        return NonTransactionalSequenceService.class;
    }

    @Override
    public NonTransactionalSequenceService initiateService(Map map, ServiceRegistryImplementor serviceRegistryImplementor) {
        log.debug("Initiating Non Transactional Sequence Service");
        return NonTransactionalSequenceServiceImpl.INSTANCE;
    }
}
