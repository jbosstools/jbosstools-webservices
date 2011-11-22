package org.jboss.tools.ws.jaxrs.core.metamodel;

import java.util.List;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.JavaMethodParameter;

public interface IJaxrsResourceMethod extends IJaxrsElement<IMethod> {

	/** Sets a flag of whether the underlying java method has compilation errors
	 * or not. If true, also marke the parent resource with errors flag.
	 * 
	 * @param h
	 *            : true if the java element has errors, false otherwise */
	public abstract void hasErrors(final boolean h);

	@Override
	public abstract EnumKind getKind();

	/** @return the parentResource */
	abstract IJaxrsResource getParentResource();

	/** @return the returnType */
	abstract IType getReturnType();

	abstract Annotation getPathAnnotation();

	abstract String getPathTemplate();

	abstract Annotation getHttpMethodAnnotation();

	abstract String getHttpMethod();

	abstract Annotation getConsumesAnnotation();

	abstract List<String> getConsumedMediaTypes();

	abstract Annotation getProducesAnnotation();

	abstract List<String> getProducedMediaTypes();

	List<JavaMethodParameter> getJavaMethodParameters();
	
	/**
	 * Determines the proposals for the PathParam annotated method parameters of the underlying Java Method.
	 * This list is based on the @Path annotation found on the Java Method and on the parent Java Type.
	 * @return
	 */
	List<String> getPathParamValueProposals();

}