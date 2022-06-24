package com.etendoerp.sequences.services;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.spi.ServiceContributor;

public class NonTRXServiceContributor implements ServiceContributor {
    @Override
    public void contribute(StandardServiceRegistryBuilder standardServiceRegistryBuilder) {
        standardServiceRegistryBuilder.addInitiator(NonTransactionalSequenceInitiatior.INSTANCE);
    }
}
