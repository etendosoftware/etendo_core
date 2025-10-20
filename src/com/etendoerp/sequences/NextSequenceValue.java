package com.etendoerp.sequences;

import com.etendoerp.sequences.annotations.SequenceQualifier;
import org.openbravo.base.weld.WeldUtils;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

public class NextSequenceValue {

    @Inject
    @Any
    Instance<UINextSequenceValueInterface> sequenceInstance;

    public static NextSequenceValue getInstance() {
        return WeldUtils.getInstanceFromStaticBeanManager(NextSequenceValue.class);
    }

    public UINextSequenceValueInterface getSequenceHandler(String referenceId) {
        var instance = getSequenceInstance(referenceId);
        if (instance != null && instance.isResolvable()) {
            return instance.get();
        }
        return null;
    }

    public Instance<UINextSequenceValueInterface> getSequenceInstance(String referenceId) {
        var qualifier = new SequenceQualifier(referenceId);
        return sequenceInstance.select(qualifier);
    }

}
