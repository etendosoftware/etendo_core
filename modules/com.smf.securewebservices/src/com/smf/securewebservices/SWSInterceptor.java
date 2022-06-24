package com.smf.securewebservices;

import org.openbravo.client.kernel.event.PersistenceEventOBInterceptor;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Specializes;
import javax.inject.Inject;
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

