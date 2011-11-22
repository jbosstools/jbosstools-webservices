package org.jboss.tools.ws.jaxrs.core.metamodel;

import java.util.List;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;

public interface IJaxrsResource extends IJaxrsElement<IType> {

	boolean isRootResource();

	boolean isSubresource();

	String getName();

	IJaxrsResourceMethod getByJavaMethod(final IMethod javaMethod) throws JavaModelException;

	IJaxrsApplication getApplication();

	List<IJaxrsResourceMethod> getAllMethods();

	List<IJaxrsResourceMethod> getResourceMethods();

	List<IJaxrsResourceMethod> getSubresourceMethods();

	List<IJaxrsResourceMethod> getSubresourceLocators();

	String getPathTemplate();

	Annotation getPathAnnotation();

	List<String> getConsumedMediaTypes();

	Annotation getConsumesAnnotation();

	List<String> getProducedMediaTypes();

	Annotation getProducesAnnotation();

}