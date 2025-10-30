package org.openbravo.client.application.eventSequence;

import org.openbravo.base.weld.WeldUtils;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@Dependent
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
