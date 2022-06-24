package com.etendoerp.sequences.services;

import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataContributor;
import org.jboss.jandex.IndexView;

public class NonTRXMetadataContributor implements MetadataContributor {

    @Override
    public void contribute(InFlightMetadataCollector inFlightMetadataCollector, IndexView indexView) {
        if (NonTransactionalSequenceServiceImpl.INSTANCE.getDatabase() == null) {
            NonTransactionalSequenceServiceImpl.INSTANCE.setDatabase(inFlightMetadataCollector.getDatabase());
        }
    }
}
