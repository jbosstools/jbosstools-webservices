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
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_CONSUMES_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_DEFAULT_VALUE_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_HTTP_METHOD_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_MATRIX_PARAM_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_METHOD_PARAMETERS;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_PATH_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_PRODUCES_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_QUERY_PARAM_ANNOTATION;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.jboss.tools.ws.jaxrs.core.internal.utils.ObjectUtils;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Pair;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsApplication;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResource;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceMethod;

public class JaxrsEndpoint implements IJaxrsEndpoint {

	/** Unique identifier. */
	private final String identifier;

	private final JaxrsMetamodel metamodel;

	private final LinkedList<JaxrsResourceMethod> resourceMethods;

	private IJaxrsHttpMethod httpMethod;

	private IJaxrsApplication application = null;

	private String uriPathTemplate = null;

	private List<String> consumedMediaTypes = null;

	private List<String> producedMediaTypes = null;

	public JaxrsEndpoint(final JaxrsMetamodel metamodel, final IJaxrsHttpMethod httpMethod,
			final LinkedList<JaxrsResourceMethod> resourceMethods) {
		this.identifier = UUID.randomUUID().toString();
		this.metamodel = metamodel;
		this.application = (metamodel != null ? metamodel.getApplication() : null);
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

	/*
	 * (non-Javadoc)
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

	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
		return result;
	}

	/* (non-Javadoc)
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
		if (application.equals(metamodel.getApplication())) {
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
			this.application = metamodel.getApplication();
			refreshUriPathTemplate();
			metamodel.update(this);
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
	public void update(final int flags) throws CoreException {
		boolean changed = false;
		if ((flags & F_HTTP_METHOD_ANNOTATION) > 0) {
			changed = changed || refreshHttpMethod();
		}
		if ((flags & F_PATH_ANNOTATION) > 0 || (flags & F_QUERY_PARAM_ANNOTATION) > 0
				|| (flags & F_MATRIX_PARAM_ANNOTATION) > 0 || (flags & F_DEFAULT_VALUE_ANNOTATION) > 0
				|| (flags & F_METHOD_PARAMETERS) > 0) {
			changed = changed || refreshUriPathTemplate();
		}
		// look for mediatype capabilities at the method level, then fall back
		// at the type level, then "any" otherwise
		if ((flags & F_CONSUMES_ANNOTATION) > 0 || (flags & F_PRODUCES_ANNOTATION) > 0) {
			if ((flags & F_CONSUMES_ANNOTATION) > 0) {
				changed = changed || refreshConsumedMediaTypes();
			}
			if ((flags & F_PRODUCES_ANNOTATION) > 0) {
				changed = changed || refreshProducedMediaTypes();
			}
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
	 * @return
	 */
	private boolean refreshUriPathTemplate() {
		// compute the URI Path Template from the chain of Methods/Resources
		StringBuilder uriPathTemplateBuilder = new StringBuilder();
		if (application != null && application.getApplicationPath() != null) {
			uriPathTemplateBuilder.append(application.getApplicationPath());
		}
		final List<String> queryParams = new ArrayList<String>();
		for (JaxrsResourceMethod resourceMethod : resourceMethods) {
			final Pair<String, List<String>> displayableResourcePathTemplate = resourceMethod.getParentResource().getDisplayablePathTemplate(resourceMethod);
			if(!displayableResourcePathTemplate.left.isEmpty()) {
				uriPathTemplateBuilder.append("/").append(displayableResourcePathTemplate.left);
			}
			queryParams.addAll(displayableResourcePathTemplate.right);
			final Pair<String, List<String>> displayableResourceMethodPathTemplate = resourceMethod.getDisplayablePathTemplate();
			if(!displayableResourceMethodPathTemplate.left.isEmpty()) {
				uriPathTemplateBuilder.append("/").append(displayableResourceMethodPathTemplate.left);
			}
			queryParams.addAll(displayableResourceMethodPathTemplate.right);
		}
		if(!queryParams.isEmpty()) {
			uriPathTemplateBuilder.append('?');
			for(Iterator<String> iterator = queryParams.iterator(); iterator.hasNext();) {
				uriPathTemplateBuilder.append(iterator.next());
				if(iterator.hasNext()) {
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
		int level = 0; // Severity NONE
		for (IJaxrsResourceMethod resourceMethod : getResourceMethods()) {
			level = Math.max(level, resourceMethod.getProblemLevel());
		}
		level = Math.max(level, httpMethod.getProblemLevel());
		if(application != null) {
			level = Math.max(level, application.getProblemLevel());
		}
		return level;
	}

	public void remove() {
		metamodel.remove(this);

	}

}
