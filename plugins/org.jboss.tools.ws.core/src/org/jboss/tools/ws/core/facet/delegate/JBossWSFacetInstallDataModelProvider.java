/******************************************************************************* 
 * Copyright (c) 2007 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.core.facet.delegate;

import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.common.componentcore.datamodel.FacetInstallDataModelProvider;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.jboss.tools.ws.core.utils.StatusUtils;


public class JBossWSFacetInstallDataModelProvider extends
		FacetInstallDataModelProvider implements IJBossWSFacetDataModelProperties {


	/**
	 * Returns set of facet properties for facet wizard page
	 * 
	 * @return set of property names
	 */
	@SuppressWarnings("unchecked")
	public Set<String> getPropertyNames() {
		Set<String> names = super.getPropertyNames();

		names.add(JBOSS_WS_DEPLOY);
		names.add(JBOSS_WS_RUNTIME_ID);
		names.add(JBOSS_WS_RUNTIME_IS_SERVER_SUPPLIED);
		names.add(JBOSS_WS_RUNTIME_HOME);
		

		return names;
	}

	/**
	 * Returns default value for a given property
	 * 
	 * @param propertyName name of property which default value requested
	 * @return default value 
	 */
	public Object getDefaultProperty(String propertyName) {
		if(propertyName.equals(JBOSS_WS_DEPLOY)){
			return false;
		}else if(propertyName.equals(JBOSS_WS_RUNTIME_ID)){
			return "";
		}else if(propertyName.equals(FACET_ID)){
			return IJBossWSFacetDataModelProperties.JBOSS_WS_FACET_ID;
		}
		return super.getDefaultProperty(propertyName);
	}

	public Object create() {
		return DataModelFactory.createDataModel(this);
	}
	

	@Override
	public IStatus validate(String name) {
		boolean serverSupplied = getBooleanProperty(JBOSS_WS_RUNTIME_IS_SERVER_SUPPLIED);
		String runtimeName = getStringProperty(JBOSS_WS_RUNTIME_ID);
		if (!serverSupplied 
				&& (runtimeName == null || runtimeName.equals(""))) {
			return StatusUtils.errorStatus("");
		}
		return super.validate(name);
	}
}
