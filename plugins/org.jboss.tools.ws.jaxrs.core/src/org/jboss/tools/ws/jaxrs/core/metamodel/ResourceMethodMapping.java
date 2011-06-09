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

package org.jboss.tools.ws.jaxrs.core.metamodel;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.osgi.util.NLS;
import org.jboss.tools.ws.jaxrs.core.internal.builder.JAXRSAnnotationsScanner;
import org.jboss.tools.ws.jaxrs.core.internal.builder.JaxrsMetamodelBuilder;
import org.jboss.tools.ws.jaxrs.core.internal.utils.ObjectUtils;
import org.jboss.tools.ws.jaxrs.core.internal.utils.ValidationMessages;
import org.jboss.tools.ws.jaxrs.core.utils.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.utils.ResourceMethodAnnotatedParameter;

public class ResourceMethodMapping {

	private final ResourceMethod resourceMethod;

	private HTTPMethod httpMethod;

	private final MediaTypeCapabilities consumedMediaTypes;

	private final MediaTypeCapabilities producedMediaTypes;

	private String uriPathTemplateFragment = null;

	private List<ResourceMethodAnnotatedParameter> pathParams = null;

	private List<ResourceMethodAnnotatedParameter> queryParams = null;

	/**
	 * Full constructor using the inner 'MediaTypeCapabilitiesBuilder' static
	 * class.
	 * 
	 * @param builder
	 */
	public ResourceMethodMapping(final ResourceMethod resourceMethod) {
		this.resourceMethod = resourceMethod;
		this.consumedMediaTypes = new MediaTypeCapabilities(resourceMethod.getJavaElement());
		this.producedMediaTypes = new MediaTypeCapabilities(resourceMethod.getJavaElement());
	}

	/**
	 * @return the resourceMethod
	 */
	public ResourceMethod getResourceMethod() {
		return resourceMethod;
	}

	/**
	 * @param javaMethod
	 * @param compilationUnit
	 * @throws JavaModelException
	 * @throws CoreException
	 * @throws InvalidModelElementException
	 */
	public boolean merge(CompilationUnit compilationUnit) throws JavaModelException, CoreException,
			InvalidModelElementException {
		boolean changed = false;
		IMethod javaMethod = resourceMethod.getJavaElement();
		HTTPMethod nextHTTPMethod = resolveHTTPMethod(compilationUnit);
		if (ObjectUtils.compare(this.httpMethod, nextHTTPMethod)) {
			this.httpMethod = nextHTTPMethod;
			changed = true;
		}
		// resource method
		String nextValue = (String) JdtUtils.resolveAnnotationAttributeValue(javaMethod, compilationUnit, Path.class,
				"value");
		if (ObjectUtils.compare(this.uriPathTemplateFragment, nextValue)) {
			this.uriPathTemplateFragment = nextValue;
			changed = true;
		}
		List<ResourceMethodAnnotatedParameter> nextPathParams = JdtUtils.resolveMethodParameters(javaMethod,
				compilationUnit, PathParam.class);
		if (ObjectUtils.compare(this.pathParams, nextPathParams)) {
			this.pathParams = nextPathParams;
			changed = true;
		}
		List<ResourceMethodAnnotatedParameter> nextQueryParams = JdtUtils.resolveMethodParameters(javaMethod,
				compilationUnit, QueryParam.class);
		if (ObjectUtils.compare(this.queryParams, nextQueryParams)) {
			this.queryParams = nextQueryParams;
			changed = true;
		}
		changed = (changed | this.consumedMediaTypes.merge(JAXRSAnnotationsScanner.resolveMediaTypeCapabilities(
				javaMethod, compilationUnit, Consumes.class)));
		changed = (changed | this.producedMediaTypes.merge(JAXRSAnnotationsScanner.resolveMediaTypeCapabilities(
				javaMethod, compilationUnit, Produces.class)));
		return changed;
	}

	/*
	 * private static final String resolveBaseURIPathTemplate(Resource resource)
	 * { String resourceUriPathTemplate =
	 * resource.getMapping().getUriPathTemplateFragment(); if
	 * (resource.isRootResource()) { return resourceUriPathTemplate; }
	 * StringBuffer uriPathTemplateBuffer = new
	 * StringBuffer(resolveBaseURIPathTemplate());
	 * uriPathTemplateBuffer.append("/").append(resourceUriPathTemplate); return
	 * uriPathTemplateBuffer.toString(); }
	 * 
	 * private static String computeFullUriPathTemplate(ResourceMethodMapping
	 * mapping) { String uriPathTemplate = mapping.getUriPathTemplateFragment();
	 * List<ResourceMethodAnnotatedParameter> queryParams =
	 * mapping.getQueryParams(); String baseURIPathTemplate =
	 * resolveBaseURIPathTemplate
	 * (mapping.getResourceMethod().getParentResource()); StringBuffer
	 * uriPathTemplateBuffer = new StringBuffer(baseURIPathTemplate);
	 * uriPathTemplateBuffer.append(uriPathTemplate); if (queryParams != null &&
	 * !queryParams.isEmpty()) { uriPathTemplateBuffer.append("?"); for
	 * (Iterator<ResourceMethodAnnotatedParameter> queryParamIterator =
	 * queryParams.iterator(); queryParamIterator .hasNext();) {
	 * ResourceMethodAnnotatedParameter queryParam = queryParamIterator.next();
	 * uriPathTemplateBuffer
	 * .append(queryParam.getAnnotationValue()).append("={")
	 * .append(queryParam.getParameterType()).append("}"); if
	 * (queryParamIterator.hasNext()) { uriPathTemplateBuffer.append("&"); }
	 * 
	 * }
	 * 
	 * } return uriPathTemplateBuffer.toString().replaceAll("/\\*",
	 * "/").replaceAll("///", "/").replaceAll("//", "/"); }
	 */
	private HTTPMethod resolveHTTPMethod(CompilationUnit compilationUnit) {
		for (String httpMethodName : resourceMethod.getMetamodel().getHttpMethods().getTypeNames()) {
			IAnnotationBinding httpMethodAnnotationBinding = JdtUtils.resolveAnnotationBinding(
					resourceMethod.getJavaElement(), compilationUnit, httpMethodName);
			if (httpMethodAnnotationBinding != null) {
				// stop iterating
				return resourceMethod.getMetamodel().getHttpMethods().getByTypeName(httpMethodName);
			}
		}
		return null;
	}

	/*
	 * private static final MediaTypeCapabilities resolveMediaTypes(final
	 * MediaTypeCapabilities resourceMediaTypes, final MediaTypeCapabilities
	 * methodMediaTypes) { if (!methodMediaTypes.isEmpty()) { return
	 * methodMediaTypes; } else if (resourceMediaTypes != null) { return
	 * resourceMediaTypes; } return null; }
	 */

	/**
	 * Validates the URI Mapping by checking that all
	 * <code>javax.ws.rs.PathParam</code> annotation values match a parameter in
	 * the URI Path Template fragment defined by the value of the
	 * <code>java.ws.rs.Path</code> annotation value.
	 * 
	 * @throws CoreException
	 * 
	 */
	public void validate() throws CoreException {
		if (uriPathTemplateFragment != null) {
			List<String> uriTemplateParams = extractParamsFromUriTemplateFragment(uriPathTemplateFragment);
			for (ResourceMethodAnnotatedParameter pathParam : pathParams) {
				String param = pathParam.getAnnotationValue();
				if (!uriTemplateParams.contains(param)) {
					IMarker marker = resourceMethod.getJavaElement().getResource()
							.createMarker(JaxrsMetamodelBuilder.JAXRS_PROBLEM);
					marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
					String message = NLS.bind(ValidationMessages.unbound_parameter, "'" + param + "'"); //$NON-NLS-1$
					marker.setAttribute(IMarker.MESSAGE, message);
					marker.setAttribute(IMarker.LINE_NUMBER, pathParam.getLineNumber());
					marker.setAttribute(IMarker.CHAR_START, pathParam.getCharStart());
					marker.setAttribute(IMarker.CHAR_END, pathParam.getCharEnd());
					this.resourceMethod.hasErrors(true);
				}
			}
		}
	}

	private static List<String> extractParamsFromUriTemplateFragment(String fragment) {
		List<String> params = new ArrayList<String>();
		int beginIndex = -1;
		while ((beginIndex = fragment.indexOf("{", beginIndex + 1)) != -1) {
			int semicolonIndex = fragment.indexOf(":", beginIndex);
			int closingCurlyBraketIndex = fragment.indexOf("}", beginIndex);
			int endIndex = semicolonIndex != -1 ? semicolonIndex : closingCurlyBraketIndex;
			params.add(fragment.substring(beginIndex + 1, endIndex).trim());
		}
		return params;
	}

	/**
	 * @return the httpMethod
	 */
	public final HTTPMethod getHTTPMethod() {
		return httpMethod;
	}

	/**
	 * @return the uriPathTemplateFragment
	 */
	public final String getUriPathTemplateFragment() {
		return uriPathTemplateFragment;
	}

	public boolean matches(HTTPMethod httpMethod, String uriPathTemplateFragment, String consumes, String produces) {
		if (httpMethod != null && !httpMethod.equals(this.httpMethod)) {
			return false;
		}
		if (this.httpMethod != null && !this.httpMethod.equals(httpMethod)) {
			return false;
		}
		if (uriPathTemplateFragment != null && !uriPathTemplateFragment.equals(this.uriPathTemplateFragment)) {
			return false;
		}
		if (this.uriPathTemplateFragment != null && !this.uriPathTemplateFragment.equals(uriPathTemplateFragment)) {
			return false;
		}
		if (consumedMediaTypes != null && consumes != null && !consumedMediaTypes.contains(consumes)) {
			return false;
		}
		if (producedMediaTypes != null && produces != null && !producedMediaTypes.contains(produces)) {
			return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public final String toString() {
		StringBuffer buffer = new StringBuffer();
		if (httpMethod != null) {
			buffer.append(httpMethod.getHttpVerb());
			buffer.append(" ");
		}
		String uriPathTemplate = getUriPathTemplateFragment();
		if (uriPathTemplate != null) {
			buffer.append(uriPathTemplate);
			buffer.append(" ");
		}
		buffer.append("{Accept:").append(consumedMediaTypes).append(" Content-type: ").append(producedMediaTypes)
				.append("}");
		return buffer.toString();
	}

	/**
	 * @return the Consumed MediaTypes
	 */
	public final MediaTypeCapabilities getConsumedMediaTypes() {
		return consumedMediaTypes;
	}

	/**
	 * @return the Produced MediaTypes
	 */
	public final MediaTypeCapabilities getProcucedMediaTypes() {
		return producedMediaTypes;
	}

	/**
	 * @return the queryParams
	 */
	public final List<ResourceMethodAnnotatedParameter> getQueryParams() {
		return queryParams;
	}

	/**
	 * @return the pathParams
	 */
	public final List<ResourceMethodAnnotatedParameter> getPathParams() {
		return pathParams;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((httpMethod == null) ? 0 : httpMethod.hashCode());
		result = prime * result + ((consumedMediaTypes == null) ? 0 : consumedMediaTypes.hashCode());
		result = prime * result + ((producedMediaTypes == null) ? 0 : producedMediaTypes.hashCode());
		result = prime * result + ((queryParams == null) ? 0 : queryParams.hashCode());
		result = prime * result + ((uriPathTemplateFragment == null) ? 0 : uriPathTemplateFragment.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public final boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ResourceMethodMapping other = (ResourceMethodMapping) obj;
		if (httpMethod == null) {
			if (other.httpMethod != null) {
				return false;
			}
		} else if (!httpMethod.equals(other.httpMethod)) {
			return false;
		}
		if (producedMediaTypes == null) {
			if (other.producedMediaTypes != null) {
				return false;
			}
		} else if (!producedMediaTypes.equals(other.producedMediaTypes)) {
			return false;
		}
		if (consumedMediaTypes == null) {
			if (other.consumedMediaTypes != null) {
				return false;
			}
		} else if (!consumedMediaTypes.equals(other.consumedMediaTypes)) {
			return false;
		}
		if (queryParams == null) {
			if (other.queryParams != null) {
				return false;
			}
		} else if (!queryParams.equals(other.queryParams)) {
			return false;
		}
		if (uriPathTemplateFragment == null) {
			if (other.uriPathTemplateFragment != null) {
				return false;
			}
		} else if (!uriPathTemplateFragment.equals(other.uriPathTemplateFragment)) {
			return false;
		}
		return true;
	}

}
