package org.jboss.tools.ws.jaxrs.core.metamodel;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.jdt.core.IJavaProject;

public interface IJaxrsEndpoint extends Comparable<IJaxrsEndpoint> {

	/** @return the httpMethod */
	public abstract IJaxrsHttpMethod getHttpMethod();

	/** @return the uriPathTemplate */
	public abstract String getUriPathTemplate();

	/** @return the consumedMediaTypes */
	public abstract List<String> getConsumedMediaTypes();

	/** @return the producedMediaTypes */
	public abstract List<String> getProducedMediaTypes();

	public abstract LinkedList<IJaxrsResourceMethod> getResourceMethods();

	public abstract IJavaProject getJavaProject();

}