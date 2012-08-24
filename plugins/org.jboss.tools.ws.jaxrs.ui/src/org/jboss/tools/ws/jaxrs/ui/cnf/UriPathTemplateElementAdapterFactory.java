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
