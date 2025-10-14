package org.openbravo.erpCommon.utilitySequence;

import org.openbravo.base.weld.WeldUtils;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

public class UtilitySequence {
    @Inject
    @Any
    Instance<UtilitySequenceActionInterface> sequenceAction;

    public static UtilitySequence getInstance() {
        return WeldUtils.getInstanceFromStaticBeanManager(UtilitySequence.class);
    }

    public Instance<UtilitySequenceActionInterface> getSequenceAction() {
        return sequenceAction;
    }
}
