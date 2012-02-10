package org.jboss.tools.ws.jaxrs.ui.cnf;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.debug.ui.actions.ILaunchable;

public class UriPathTemplateElementAdapterFactory implements IAdapterFactory {

	@Override
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if( adapterType.equals(ILaunchable.class)) {
			if( adaptableObject instanceof UriPathTemplateElement ) {
				return ((UriPathTemplateElement)adaptableObject);
			}
		}
		return null;
	}

	@Override
	public Class[] getAdapterList() {
		return new Class[]{ILaunchable.class};
	}

}
