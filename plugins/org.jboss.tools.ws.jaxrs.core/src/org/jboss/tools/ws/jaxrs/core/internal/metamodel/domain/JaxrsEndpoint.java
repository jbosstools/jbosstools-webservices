/******************************************************************************* 
 * Copyright (c) 2008 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain;

import static org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils.notNullNorEmpty;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_BEAN_PARAM_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_CONSUMES_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_DEFAULT_VALUE_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_HTTP_METHOD_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_MATRIX_PARAM_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_METHOD_PARAMETERS;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_PATH_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_PATH_PARAM_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_PRODUCES_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_QUERY_PARAM_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.DEFAULT_VALUE;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.MATRIX_PARAM;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PATH_PARAM;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.QUERY_PARAM;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.Flags;
import org.jboss.tools.ws.jaxrs.core.internal.utils.ObjectUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.AnnotationUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.SourceType;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IAnnotatedSourceType;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsApplication;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResource;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceMethod;

public class JaxrsEndpoint implements IJaxrsEndpoint {

	/** Unique identifier. */
	private final String identifier;

	/** The parent JAX-RS Metamodel.*/
	private final JaxrsMetamodel metamodel;

	/** The chain of JAX-RS Resource Methods that map to this endpoint. */
	private final LinkedList<JaxrsResourceMethod> resourceMethods;

	/** The HTTP Method to invoke when calling this endpoint. */
	private IJaxrsHttpMethod httpMethod;

	/** The JAX-RS Application. */
	private IJaxrsApplication application = null;

	/**
	 * The URI Path Template (generated from the Application and the chain of
	 * JAX-RS Resource methods.
	 */
	private String uriPathTemplate = null;

	/** The media-types consumed by this endpoint.*/
	private List<String> consumedMediaTypes = null;

	/** The media-types produced by this endpoint.*/
	private List<String> producedMediaTypes = null;

	/**
	 * Full constructor
	 * @param metamodel
	 * @param httpMethod
	 * @param resourceMethods
	 */
	public JaxrsEndpoint(final JaxrsMetamodel metamodel, final IJaxrsHttpMethod httpMethod,
			final LinkedList<JaxrsResourceMethod> resourceMethods) {
		this.identifier = UUID.randomUUID().toString();
		this.metamodel = metamodel;
		this.application = (metamodel != null ? metamodel.findApplication() : null);
		this.httpMethod = httpMethod;
		this.resourceMethods = resourceMethods;
		refreshUriPathTemplate();
		refreshConsumedMediaTypes();
		refreshProducedMediaTypes();
	}

	public void joinMetamodel() {
		if (metamodel != null) {
			metamodel.add(this);
		}
	}

	/**
	 * @return the identifier
	 */
	public String getIdentifier() {
		return identifier;
	}

	@Override
	public EnumElementCategory getElementCategory() {
		return EnumElementCategory.ENDPOINT;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final IMethod javaMethod = resourceMethods.getLast().getJavaElement();
		return (httpMethod != null ? httpMethod.getHttpVerb() : null) + " " + uriPathTemplate + " | consumes:"
				+ consumedMediaTypes + " | produces=" + producedMediaTypes + " in method "
				+ javaMethod.getParent().getElementName() + "." + javaMethod.getElementName() + "(...)";
	}

	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		JaxrsEndpoint other = (JaxrsEndpoint) obj;
		if (identifier == null) {
			if (other.identifier != null) {
				return false;
			}
		} else if (!identifier.equals(other.identifier)) {
			return false;
		}
		return true;
	}

	/**
	 * Triggers a refresh when the given HTTP Method element changed
	 * 
	 * @return true if the endpoint is still valid, false otherwise (it should
	 *         be removed from the metamodel)
	 */
	public boolean update(final IJaxrsHttpMethod httpMethod) {
		metamodel.update(this);
		return true;
	}

	/**
	 * Triggers a refresh when the given application element changed
	 * 
	 * @return true if the endpoint is still valid, false otherwise (it should
	 *         be removed from the metamodel)
	 */
	public boolean update(final IJaxrsApplication application) {
		if (application.equals(metamodel.findApplication())) {
			this.application = application;
			refreshUriPathTemplate();
			metamodel.update(this);
			return true;
		}
		return false;
	}

	/**
	 * Triggers a refresh when the given application element has been removed.
	 * 
	 * @return true if the endpoint is still valid, false otherwise (it should
	 *         be removed from the metamodel)
	 */
	public boolean remove(final IJaxrsApplication application) {
		// replace the current application with the (new) default one from the
		// metamodel
		if (this.application.equals(application)) {
			this.application = this.metamodel.findApplication();
			refreshUriPathTemplate();
			this.metamodel.update(this);
			return true;
		}
		return false;
	}

	/**
	 * Triggers a refresh when changes occurred on one or more elements
	 * (HttpMethod and/or ResourcMethods) of the endpoint.
	 * 
	 * @throws CoreException
	 */
	public void update(final Flags flags) throws CoreException {
		boolean changed = false;
		if (flags.hasValue(F_HTTP_METHOD_ANNOTATION)) {
			changed = changed || refreshHttpMethod();
		}

		if (flags.hasValue(F_PATH_ANNOTATION, F_PATH_PARAM_ANNOTATION, F_QUERY_PARAM_ANNOTATION, F_MATRIX_PARAM_ANNOTATION, F_BEAN_PARAM_ANNOTATION, 
				F_DEFAULT_VALUE_ANNOTATION, F_METHOD_PARAMETERS)) {
			changed = changed || refreshUriPathTemplate();
		}

		// look for mediatype capabilities at the method level, then fall back
		// at the type level, then "any" otherwise
		if (flags.hasValue(F_CONSUMES_ANNOTATION)) {
			changed = changed || refreshConsumedMediaTypes();
		}
		if (flags.hasValue(F_PRODUCES_ANNOTATION)) {
			changed = changed || refreshProducedMediaTypes();
		}
		if (changed) {
			metamodel.update(this);
		}
	}

	private boolean refreshHttpMethod() throws CoreException {
		final IJaxrsResourceMethod resourceMethod = resourceMethods.getLast();
		final String httpMethodClassName = resourceMethod.getHttpMethodClassName();
		if (httpMethodClassName != null) {
			return setHttpMethod(metamodel.findHttpMethodByTypeName(httpMethodClassName));
		}
		return false;
	}

	private boolean setHttpMethod(final JaxrsHttpMethod newHttpMethod) {
		if (!ObjectUtils.nullSafeEquals(this.httpMethod, newHttpMethod)) {
			this.httpMethod = newHttpMethod;
			return true;
		}
		return false;
	}

	private boolean refreshProducedMediaTypes() {
		final JaxrsResourceMethod resourceMethod = (JaxrsResourceMethod) resourceMethods.getLast();
		final JaxrsResource resource = resourceMethod.getParentResource();
		if (notNullNorEmpty(resourceMethod.getProducedMediaTypes())) {
			return setProducedMediaTypes(resourceMethod.getProducedMediaTypes());
		} else if (notNullNorEmpty(resourceMethod.getParentResource().getProducedMediaTypes())) {
			return setProducedMediaTypes(resource.getProducedMediaTypes());
		} else {
			return setProducedMediaTypes(Arrays.asList("*/*"));
		}
	}

	private boolean setProducedMediaTypes(final List<String> newProducedMediaTypes) {
		if (!ObjectUtils.nullSafeEquals(this.producedMediaTypes, newProducedMediaTypes)) {
			this.producedMediaTypes = newProducedMediaTypes;
			return true;
		}
		return false;
	}

	private boolean refreshConsumedMediaTypes() {
		final IJaxrsResourceMethod resourceMethod = resourceMethods.getLast();
		final IJaxrsResource resource = resourceMethod.getParentResource();
		if (notNullNorEmpty(resourceMethod.getConsumedMediaTypes())) {
			return setConsumedMediaTypes(resourceMethod.getConsumedMediaTypes());
		} else if (notNullNorEmpty(resourceMethod.getParentResource().getConsumedMediaTypes())) {
			return setConsumedMediaTypes(resource.getConsumedMediaTypes());
		} else {
			return setConsumedMediaTypes(Arrays.asList("*/*"));
		}
	}

	private boolean setConsumedMediaTypes(final List<String> newConsumedMediaTypes) {
		if (!ObjectUtils.nullSafeEquals(this.consumedMediaTypes, newConsumedMediaTypes)) {
			this.consumedMediaTypes = newConsumedMediaTypes;
			return true;
		}
		return false;
	}

	/**
	 * Refresh the URI Path Template
	 * 
	 * @return {@code true} if the internal URI Path Template was modified,
	 *         {@code false} otherwise.
	 */
	private boolean refreshUriPathTemplate() {
		// compute the URI Path Template from the chain of Methods/Resources
		final StringBuilder uriPathTemplateBuilder = new StringBuilder();
		final List<String> queryParams = new ArrayList<String>();
		if (application != null && application.getApplicationPath() != null) {
			uriPathTemplateBuilder.append(application.getApplicationPath());
		}
		// first resource method's parent resource has a @Path annotation, too
		final JaxrsResourceMethod firstResourceMethod = resourceMethods.get(0);
		final String displayableResourcePathTemplate = getDisplayablePathTemplate(firstResourceMethod.getParentResource(), firstResourceMethod);
		if (!displayableResourcePathTemplate.isEmpty()) {
			uriPathTemplateBuilder.append(displayableResourcePathTemplate);
		}
		for (JaxrsResourceMethod resourceMethod : resourceMethods) {
			final String displayableResourceMethodPathTemplate = getDisplayablePathTemplate(resourceMethod);
			if (!displayableResourceMethodPathTemplate.isEmpty()) {
				uriPathTemplateBuilder.append(displayableResourceMethodPathTemplate);
			}
			final List<String> displayableResourceMethodQueryParameters = getDisplayableQueryParameters(resourceMethod);
			queryParams.addAll(displayableResourceMethodQueryParameters);
		}
		if (!queryParams.isEmpty()) {
			uriPathTemplateBuilder.append('?');
			for (Iterator<String> iterator = queryParams.iterator(); iterator.hasNext();) {
				uriPathTemplateBuilder.append(iterator.next());
				if (iterator.hasNext()) {
					uriPathTemplateBuilder.append('&');
				}
			}
		}
		String template = uriPathTemplateBuilder.toString();
		while (template.indexOf("//") > -1) {
			template = template.replace("//", "/");
		}
		return setUriPathTemplate(template);
	}
	
	/**
	 * Generates and returns a displayable path template (including Matrix Parameters) in the context of the
	 * given {@link JaxrsResourceMethod}.
	 *
	 * @param resource the parent resource
	 * @param resourceMethod
	 *            the JAX-RS Resource Method
	 * @return the displayable URI Path Template as a {@link String}
	 */
	private static String getDisplayablePathTemplate(final JaxrsResource resource, final JaxrsResourceMethod resourceMethod) {
		// skip if the resource's path annotation value is invalid
		if(!AnnotationUtils.isValidAnnotationValue(resource.getPathTemplate())) {
			return "";
		}
		final StringBuilder pathTemplateBuilder = new StringBuilder();
		int index = 0;
		if(resource.getPathTemplate() != null) {
			while (index < resource.getPathTemplate().length()) {
				// make sure the path template starts with a '/'. 
				if(!resource.getPathTemplate().startsWith("/")) {
					pathTemplateBuilder.append('/');
				}  
				final int beginIndex = resource.getPathTemplate().indexOf('{', index);
				final int endIndex = resource.getPathTemplate().indexOf('}', beginIndex + 1);
				// let's keep everything in between the current index and the
				// next path arg to process
				if (beginIndex > index) {
					pathTemplateBuilder.append(resource.getPathTemplate().substring(index, beginIndex));
				} else if (beginIndex == -1) {
					pathTemplateBuilder.append(resource.getPathTemplate().substring(index));
					break;
				}
				// retrieve path arg without surrounding curly brackets
				final String pathArg = resource.getPathTemplate().substring(beginIndex + 1, endIndex).replace(" ", "");
				// path arg contains some regexp, let's keep it
				if (pathArg.contains(":")) {
					pathTemplateBuilder.append('{').append(pathArg).append('}');
				}
				// TODO: implement a preference to let the user decide if she wants to 
				// have the type of the associated PathParam
				else {
					final IAnnotatedSourceType parameterType = ((JaxrsResourceMethod) resourceMethod).getRelatedTypeAnnotatedWith(PATH_PARAM, pathArg);
					if (parameterType != null) {
						pathTemplateBuilder.append('{').append(pathArg);
						if (parameterType.getType() != null) {
							pathTemplateBuilder.append(":").append(parameterType.getType().getDisplayableTypeName());
						}
						pathTemplateBuilder.append('}');
					} else {
						pathTemplateBuilder.append('{').append(pathArg).append(":.*").append('}');
					}
				}
				index = endIndex + 1;
			}
		}
		return pathTemplateBuilder.toString();
	}

	/**
	 * Substitute the given Path Template parameters with a syntax that reveals
	 * their associated java types in the displayable form.
	 * 
	 * @param pathTemplate
	 * @param resourceMethod
	 */
	private String getDisplayablePathTemplate(final JaxrsResourceMethod resourceMethod) {
		// skip if the resource method's path annotation value is invalid
		if(!AnnotationUtils.isValidAnnotationValue(resourceMethod.getPathTemplate())) {
			return "";
		}

		final StringBuilder pathTemplateBuilder = new StringBuilder();
		int index = 0;
		if (resourceMethod.getPathTemplate() != null) {
			// make sure the path template starts with a '/'. 
			if(!resourceMethod.getPathTemplate().startsWith("/")) {
				pathTemplateBuilder.append('/');
			}
			while (index < resourceMethod.getPathTemplate().length()) {
				final int beginIndex = resourceMethod.getPathTemplate().indexOf('{', index);
				final int endIndex = resourceMethod.getPathTemplate().indexOf('}', beginIndex + 1);
				
				// let's keep everything in between the current index and the
				// next path arg to process
				if (beginIndex > index) {
					pathTemplateBuilder.append(resourceMethod.getPathTemplate().substring(index, beginIndex));
				} else if (beginIndex == -1) {
					pathTemplateBuilder.append(resourceMethod.getPathTemplate().substring(index));
					break;
				} else if(beginIndex != -1 && endIndex == -1) {
					// missing end bracket.
					break;
				}
				
				// retrieve path arg without surrounding curly brackets
				final String pathArg = resourceMethod.getPathTemplate().substring(beginIndex + 1, endIndex)
						.replace(" ", "");
				// path arg contains some regexp, let's keep it
				if (pathArg.contains(":")) {
					pathTemplateBuilder.append('{').append(pathArg).append('}');
				}
				// otherwise, let's use the type of the associated PathParam of
				// the first resource methods
				// which provides it
				else {
					final IAnnotatedSourceType parameterType = ((JaxrsResourceMethod) resourceMethod).getRelatedTypeAnnotatedWith(PATH_PARAM, pathArg);
					if (parameterType != null) {
						pathTemplateBuilder.append('{').append(pathArg);
						if (parameterType.getType() != null) {
							pathTemplateBuilder.append(":").append(parameterType.getType().getDisplayableTypeName());
						}
						pathTemplateBuilder.append('}');
					} else {
						pathTemplateBuilder.append('{').append(pathArg).append(":.*").append('}');
					}
				}
				index = endIndex + 1;
			}
		}
		final List<String> matrixParamFieldAnnotationValues = new ArrayList<String>();
		final List<IAnnotatedSourceType> matrixParams = resourceMethod.getRelatedTypesAnnotatedWith(MATRIX_PARAM);
		
		for (IAnnotatedSourceType matrixParam : matrixParams) {
			pathTemplateBuilder.append(';');
			final String matrixParamFieldAnnotationValue = matrixParam.getAnnotation(
					MATRIX_PARAM).getValue();
			matrixParamFieldAnnotationValues.add(matrixParamFieldAnnotationValue);
			pathTemplateBuilder.append(matrixParamFieldAnnotationValue).append("={")
					.append(matrixParam.getType().getDisplayableTypeName());
			if(matrixParam.hasAnnotation(DEFAULT_VALUE)) {
				pathTemplateBuilder.append(':').append('\"').append(matrixParam.getAnnotation(DEFAULT_VALUE).getValue()).append('\"');
			}
			pathTemplateBuilder.append('}');
		}
		return pathTemplateBuilder.toString();
	}

	
	/**
	 * Generates and returns a list of displayable path template fragment for each Query Parameter Fields in this given {@link JaxrsResourceMethod}.
	 *
	 * @param resourceMethod
	 *            the JAX-RS Resource Method
	 * @return the ordered list of query parameters as they should appear in the UI.
	 */
	private List<String> getDisplayableQueryParameters(final JaxrsResourceMethod resourceMethod) {
		final Set<String> queryParams = new HashSet<String>();
		final List<String> displayableQueryParams = new ArrayList<String>();
		final List<IAnnotatedSourceType> queryParamTypes = resourceMethod.getRelatedTypesAnnotatedWith(QUERY_PARAM);
		for(IAnnotatedSourceType annotatedSourceType : queryParamTypes) {
			final String annotationValue = annotatedSourceType.getAnnotation(QUERY_PARAM).getValue();
			final String annotationDisplayableValue = createDisplayableQueryParam(annotatedSourceType.getType(), annotatedSourceType.getAnnotations());
			// skip if this query param was already known
			if(queryParams.contains(annotationValue)) {
				continue;
			}
			queryParams.add(annotationValue);
			displayableQueryParams.add(annotationDisplayableValue);
		}
		return displayableQueryParams;
	}

	/**
	 * Builds a displayable form for the Query Param, based on its type and {@code @QueryParam} and {@code @DefaultValue} annotation values.
	 * @param annotations
	 * @param queryParamField
	 * @return
	 */
	public String createDisplayableQueryParam(final SourceType type, final Map<String, Annotation> annotations) {
		final StringBuilder queryParamBuilder = new StringBuilder();
		final String queryParamAnnotationValue = annotations.get(QUERY_PARAM).getValue();
		queryParamBuilder.append(queryParamAnnotationValue).append("={");
		if(type != null) {
			queryParamBuilder.append(type.getDisplayableTypeName());
		} else {
			queryParamBuilder.append("unknown");
		}
		if(annotations.containsKey(DEFAULT_VALUE)) {
			queryParamBuilder.append(':').append('\"').append(annotations.get(DEFAULT_VALUE).getValue()).append('\"');
		}
		final String queryParam = queryParamBuilder.append('}').toString();
		return queryParam;
	}

	private boolean setUriPathTemplate(final String template) {
		if (!ObjectUtils.nullSafeEquals(this.uriPathTemplate, template)) {
			this.uriPathTemplate = template;
			return true;
		}
		return false;
	}

	@Override
	public int compareTo(IJaxrsEndpoint other) {
		int uriPathTemplateComparison = this.uriPathTemplate.compareTo(other.getUriPathTemplate());
		if (uriPathTemplateComparison != 0) {
			return uriPathTemplateComparison;
		}
		return this.httpMethod.compareTo(other.getHttpMethod());
	}

	@Override
	public IJaxrsHttpMethod getHttpMethod() {
		return httpMethod;
	}

	@Override
	public IJaxrsApplication getApplication() {
		return application;
	}

	/** @return the resourceMethods */
	@Override
	public LinkedList<IJaxrsResourceMethod> getResourceMethods() {
		return new LinkedList<IJaxrsResourceMethod>(resourceMethods);
	}

	@Override
	public String getUriPathTemplate() {
		return uriPathTemplate;
	}

	@Override
	public List<String> getConsumedMediaTypes() {
		return consumedMediaTypes;
	}

	@Override
	public List<String> getProducedMediaTypes() {
		return producedMediaTypes;
	}

	@Override
	public IJavaProject getJavaProject() {
		return this.metamodel.getJavaProject();
	}

	@Override
	public IProject getProject() {
		return this.metamodel.getProject();
	}
	
	/**
	 * @return the problem level for this given endpoint. The returned problem
	 *         level is the highest value from all the resource methods this
	 *         endpoint is made of.
	 */
	@Override
	public int getProblemLevel() {
		int level = IMarker.SEVERITY_INFO; // Severity NONE
		for (IJaxrsResourceMethod resourceMethod : getResourceMethods()) {
			level = Math.max(level, resourceMethod.getMarkerSeverity());
		}
		/*level = Math.max(level, httpMethod.getProblemLevel());
		if(application != null) {
			level = Math.max(level, application.getProblemLevel());
		}*/
		return level;
	}

	public void remove() {
		metamodel.remove(this);

	}

}
