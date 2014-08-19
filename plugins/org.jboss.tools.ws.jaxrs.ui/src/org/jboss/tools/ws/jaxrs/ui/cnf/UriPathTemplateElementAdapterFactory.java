/******************************************************************************* 
 * Copyright (c) 2011 - 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.jaxrs.ui.cnf;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.debug.ui.actions.ILaunchable;

public class UriPathTemplateElementAdapterFactory implements IAdapterFactory {

	@Override
	@SuppressWarnings("rawtypes") 
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if( adapterType.equals(ILaunchable.class)) {
			if( adaptableObject instanceof UriPathTemplateElement ) {
				return ((UriPathTemplateElement)adaptableObject);
			}
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class[] getAdapterList() {
		return new Class[]{ILaunchable.class};
	}

}
