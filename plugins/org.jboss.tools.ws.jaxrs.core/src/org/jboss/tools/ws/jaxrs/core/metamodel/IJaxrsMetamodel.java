package org.jboss.tools.ws.jaxrs.core.metamodel;

import java.util.List;

import org.eclipse.jdt.core.IJavaElement;

public interface IJaxrsMetamodel {

	/** @return the JAX-RS Providers */
	public abstract List<IJaxrsProvider> getAllProviders();

	/** @return the JAX-RS HTTP Methods */
	public abstract List<IJaxrsHttpMethod> getAllHttpMethods();

	/** @return the serviceUri */
	public abstract String getServiceUri();

	/** @return the JAX-RS Ednpoints */
	public abstract List<IJaxrsEndpoint> getAllEndpoints();

	IJaxrsElement<?> getElement(IJavaElement element);

}