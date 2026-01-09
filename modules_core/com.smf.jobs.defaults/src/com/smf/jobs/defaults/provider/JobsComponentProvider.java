package com.smf.jobs.defaults.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ComponentProvider.Qualifier(com.smf.jobs.defaults.provider.JobsComponentProvider.COMPONENT_TYPE)
public class JobsComponentProvider extends BaseComponentProvider {

    public static final String COMPONENT_TYPE = "JOBSPR_CompProv";

    @Override
    public Component getComponent(String componentId, Map<String, Object> parameters) {
        throw new IllegalArgumentException("Component id " + componentId + " not supported.");
    }

    @Override
    public List<ComponentResource> getGlobalComponentResources() {
        final List<ComponentResource> globalResources = new ArrayList<>();
        globalResources.add(createStaticResource("web/com.smf.jobs.defaults/processRecords.js", false));
        globalResources.add(createStaticResource("web/com.smf.jobs.defaults/ob-clone-record.js", false));
        globalResources.add(createStaticResource("web/com.smf.jobs.defaults/createFromOrders.js", false));

        return globalResources;
    }
}
