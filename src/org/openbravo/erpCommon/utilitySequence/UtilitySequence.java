package org.openbravo.erpCommon.utilitySequence;

import org.openbravo.base.weld.WeldUtils;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

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
