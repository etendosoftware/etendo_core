/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2011 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package com.smf.securewebservices.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;

/**
 * 
 * @author androettop
 */
@ApplicationScoped
@ComponentProvider.Qualifier(SWSComponentProvider.COMPONENT_TYPE)
public class SWSComponentProvider extends BaseComponentProvider {
	public static final String COMPONENT_TYPE = "SMFSWS_CompProv";

	@Override
	public Component getComponent(String componentId, Map<String, Object> parameters) {
		throw new IllegalArgumentException("Component id " + componentId + " not supported.");
	}

	@Override
	public List<ComponentResource> getGlobalComponentResources() {
		final List<ComponentResource> globalResources = new ArrayList<ComponentResource>();
		globalResources.add(createStaticResource("web/com.smf.securewebservices/js/generateprivatekey.js", false));
		return globalResources;
	}
}
