package com.smf.securewebservices;

import org.openbravo.client.kernel.event.PersistenceEventOBInterceptor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Specializes;
import jakarta.inject.Inject;
import java.util.Iterator;

@ApplicationScoped
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

