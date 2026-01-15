package com.smf.smartclient.boostedui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * 
 * @author androettop
 */
@ApplicationScoped
@ComponentProvider.Qualifier(SCFeaturesComponentProvider.COMPONENT_TYPE)
public class SCFeaturesComponentProvider extends BaseComponentProvider {
	public static final String COMPONENT_TYPE = "SMFSCDT_CompProvider";

	@Override
	public Component getComponent(String componentId, Map<String, Object> parameters) {
		throw new IllegalArgumentException("Component id " + componentId + " not supported.");
	}

	@Override
	public List<ComponentResource> getGlobalComponentResources() {
		final List<ComponentResource> globalResources = new ArrayList<ComponentResource>();
		globalResources.add(createStaticResource("web/com.smf.smartclient.boostedui/js/boostedui.js", false));
		globalResources.add(createStyleSheetResource("web/com.smf.smartclient.boostedui/css/boostedui.css", false));
		return globalResources;
	}
}
