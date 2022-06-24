package com.smf.smartclient.debugtools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;

/**
 * 
 * @author androettop
 */
@ApplicationScoped
@ComponentProvider.Qualifier(DebugToolsComponentProvider.COMPONENT_TYPE)
public class DebugToolsComponentProvider extends BaseComponentProvider {
	public static final String COMPONENT_TYPE = "SMFSCDT_CompProvider";

	@Override
	public Component getComponent(String componentId, Map<String, Object> parameters) {
		throw new IllegalArgumentException("Component id " + componentId + " not supported.");
	}

	@Override
	public List<ComponentResource> getGlobalComponentResources() {
		final List<ComponentResource> globalResources = new ArrayList<ComponentResource>();
		globalResources.add(createStaticResource("web/com.smf.smartclient.debugtools/js/egg.min.js", false));
		globalResources.add(createStaticResource("web/com.smf.smartclient.debugtools/js/debugtools.js", false));
		globalResources.add(createStaticResource("web/com.smf.smartclient.debugtools/js/initialtoolspack.js", false));
		globalResources.add(createStyleSheetResource("web/com.smf.smartclient.debugtools/css/debugtools.css", false));
		return globalResources;
	}
}