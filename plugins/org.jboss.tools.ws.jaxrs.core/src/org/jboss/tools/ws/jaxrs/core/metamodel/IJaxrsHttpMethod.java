package org.jboss.tools.ws.jaxrs.core.metamodel;

import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;

public interface IJaxrsHttpMethod extends IJaxrsElement<IType>, Comparable<IJaxrsHttpMethod> {

	/** @return the httpVerb */
	String getHttpVerb();

	/** @return the name */
	String getSimpleName();

	Annotation getHttpMethodAnnotation();

}