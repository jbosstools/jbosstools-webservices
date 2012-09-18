package org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.jboss.tools.common.validation.TempMarkerManager;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResourceMethod;

public class JaxrsResourceValidatorDelegate extends AbstractJaxrsElementValidatorDelegate<JaxrsResource> {

	public JaxrsResourceValidatorDelegate(TempMarkerManager markerManager, JaxrsResource element) {
		super(markerManager, element);
	}

	@Override
	public void validate() throws CoreException {
		final JaxrsResource resource = getElement();
		MarkerUtils.clearMarkers(resource.getResource());
		for(IJaxrsResourceMethod resourceMethod : resource.getAllMethods()) {
			new JaxrsResourceMethodValidatorDelegate(getMarkerManager(), (JaxrsResourceMethod) resourceMethod).validate();
		}
	}
	
	private void validateConstructorParameters() {
		//TODO...
	}

}
