package org.openbravo.erpCommon.calloutsSequence;

import org.openbravo.base.weld.WeldUtils;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

public class CalloutSequence {

    public static CalloutSequence getInstance() {
        return WeldUtils.getInstanceFromStaticBeanManager(CalloutSequence.class);
    }

    @Inject
    @Any
    Instance<SL_Order_SequenceActionInterface> SL_Order_SequenceAction;

    @Inject
    @Any
    Instance<SE_InOut_SequenceActionInterface> SE_InOut_SequenceAction;

    @Inject
    @Any
    Instance<SL_Invoice_SequenceActionInterface> SL_Invoice_SequenceAction;

    public Instance<SL_Order_SequenceActionInterface> getSL_Order_SequenceAction() {
        return SL_Order_SequenceAction;
    }

    public Instance<SE_InOut_SequenceActionInterface> getSE_InOut_SequenceAction() {
        return SE_InOut_SequenceAction;
    }

    public Instance<SL_Invoice_SequenceActionInterface> getSL_Invoice_SequenceAction() {
        return SL_Invoice_SequenceAction;
    }
}
