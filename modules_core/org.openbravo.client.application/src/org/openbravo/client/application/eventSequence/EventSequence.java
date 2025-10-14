package org.openbravo.client.application.eventSequence;

import org.openbravo.base.weld.WeldUtils;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

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
