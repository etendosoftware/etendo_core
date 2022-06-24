package org.openbravo.client.application.eventSequence;

import org.openbravo.base.weld.WeldUtils;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

public class EventSequence {
    @Inject
    @Any
    Instance<DocumentNoHandlerSequenceActionInterface> sequenceActionHandler;

    public static EventSequence getInstance() {
        return WeldUtils.getInstanceFromStaticBeanManager(EventSequence.class);
    }

    public Instance<DocumentNoHandlerSequenceActionInterface> getSequenceActionHandler() {
        return sequenceActionHandler;
    }
}
