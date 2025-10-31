package com.etendoerp.advpaymentmngt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;

import jakarta.enterprise.context.Dependent;

@Dependent
@ComponentProvider.Qualifier(EtendoComponentProvider.ETENDO_VIEW_COMPONENT_TYPE)
public class EtendoComponentProvider extends BaseComponentProvider {
    public static final String ETENDO_VIEW_COMPONENT_TYPE = "EAPM_EtendoViewType";

    @Override
    public Component getComponent(String componentId, Map<String, Object> parameters) {
        throw new IllegalArgumentException("Component id " + componentId + " not supported.");
    }

    @Override
    public List<ComponentResource> getGlobalComponentResources() {
        final List<ComponentResource> globalResources = new ArrayList<ComponentResource>();
        globalResources.add(createStaticResource(
                "web/com.etendoerp.advpaymentmngt/js/received_in-paid_out-onchange.js", false));
        globalResources.add(createStaticResource(
                "web/com.etendoerp.advpaymentmngt/js/payment-action-popup.js", false));
        return globalResources;
    }
}
