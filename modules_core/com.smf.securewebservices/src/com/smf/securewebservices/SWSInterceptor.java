package com.smf.securewebservices;

import java.util.Iterator;

import org.openbravo.client.kernel.event.PersistenceEventOBInterceptor;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Specializes;
import jakarta.inject.Inject;

@Dependent
@Specializes
public class SWSInterceptor extends PersistenceEventOBInterceptor {

    @Inject
    private Event<EntityPreFlush> entityPostFlushEventProducer;

    @Override
    public void preFlush(Iterator entities) {
        final EntityPreFlush event = new EntityPreFlush();
        event.setBobs(entities);
        entityPostFlushEventProducer.fire(event);
    }
}

