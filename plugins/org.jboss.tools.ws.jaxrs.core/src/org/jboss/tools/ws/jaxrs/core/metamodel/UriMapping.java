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
import org.jboss.tools.ws.jaxrs.core.internal.utils.ValidationMessages;
import org.jboss.tools.ws.jaxrs.core.utils.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.utils.ResourceMethodAnnotatedParameter;

public class UriMapping {

	private final IMethod javaMethod;

	private HTTPMethod httpMethod = null;

	private final MediaTypeCapabilities mediaTypeCapabilities = new MediaTypeCapabilities();

	private String uriPathTemplateFragment = null;

	// private Map<String, String> matrixParams = null;

	private final List<ResourceMethodAnnotatedParameter> pathParams = new ArrayList<ResourceMethodAnnotatedParameter>();

	private final List<ResourceMethodAnnotatedParameter> queryParams = new ArrayList<ResourceMethodAnnotatedParameter>();

	private final Metamodel metamodel;

	/**
	 * Internal 'Resource' element builder.
	 * 
	 * @author xcoulon
	 * 
	 */
	public static class Builder {

		private final IMethod javaMethod;
		private final Metamodel metamodel;

		/**
		 * Mandatory attributes of the enclosing 'ResourceMethod' element.
		 * 
		 * @param javaMethod
		 * @param metamodel
		 * @param parentResource
		 */
		public Builder(final IMethod javaMethod, final Metamodel metamodel) {
			this.javaMethod = javaMethod;
			this.metamodel = metamodel;
		}

		/**
		 * Builds and returns the elements. Internally calls the merge() method.
		 * 
		 * @param progressMonitor
		 * @return
		 * @throws InvalidModelElementException
		 * @throws CoreException
		 */
		public UriMapping build(final CompilationUnit compilationUnit) throws InvalidModelElementException,
				CoreException {
			UriMapping resourceMethod = new UriMapping(this);
			resourceMethod.merge(compilationUnit);
			return resourceMethod;
		}
	}

	/**
	 * Full constructor using the inner 'Builder' static class.
	 * 
	 * @param builder
	 */
	private UriMapping(Builder builder) {
		this.javaMethod = builder.javaMethod;
		this.metamodel = builder.metamodel;
	}

	/**
	 * @param javaMethod
	 * @param compilationUnit
	 * @throws JavaModelException
	 * @throws CoreException
	 */
	public void merge(CompilationUnit compilationUnit) throws JavaModelException, CoreException {
		HTTPMethod httpMethod = null;
		for (String httpMethodName : metamodel.getHttpMethods().getTypeNames()) {
			IAnnotationBinding httpMethodAnnotationBinding = JdtUtils.resolveAnnotationBinding(javaMethod,
					compilationUnit, httpMethodName);
			if (httpMethodAnnotationBinding != null) {
				// String qualifiedName =
				// JdtUtils.resolveAnnotationFullyQualifiedName(httpMethodAnnotationBinding);
				// httpMethod =
				// metamodel.getHttpMethods().getByTypeName(qualifiedName);
				httpMethod = metamodel.getHttpMethods().getByTypeName(httpMethodName);
				// stop iterating
				break;
			}
		}
		// resource method
		String uriPathTemplateFragment = (String) JdtUtils.resolveAnnotationAttributeValue(javaMethod, compilationUnit,
				Path.class, "value");
		List<ResourceMethodAnnotatedParameter> pathParams = JdtUtils.resolveMethodParameters(javaMethod,
				compilationUnit, PathParam.class);
		List<ResourceMethodAnnotatedParameter> queryParams = JdtUtils.resolveMethodParameters(javaMethod,
				compilationUnit, QueryParam.class);
		setHTTPMethod(httpMethod);
		setUriPathTemplateFragment(uriPathTemplateFragment);
		setPathParams(pathParams);
		setQueryParams(queryParams);
		MediaTypeCapabilities mediaTypeCapabilities = new MediaTypeCapabilities(
				JAXRSAnnotationsScanner.resolveMediaTypeCapabilities(javaMethod, compilationUnit, Consumes.class),
				JAXRSAnnotationsScanner.resolveMediaTypeCapabilities(javaMethod, compilationUnit, Produces.class));
		setMediaTypeCapabilities(mediaTypeCapabilities);
	}

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
					IMarker marker = this.javaMethod.getResource().createMarker(JaxrsMetamodelBuilder.JAXRS_PROBLEM);
					marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
					String message = NLS.bind(ValidationMessages.unbound_parameter, "'" + param + "'"); //$NON-NLS-1$
					marker.setAttribute(IMarker.MESSAGE, message);
					marker.setAttribute(IMarker.LINE_NUMBER, pathParam.getLineNumber());
					marker.setAttribute(IMarker.CHAR_START, pathParam.getCharStart());
					marker.setAttribute(IMarker.CHAR_END, pathParam.getCharEnd());
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
	 * @return the mediaTypeCapabilities
	 */
	public final MediaTypeCapabilities getMediaTypeCapabilities() {
		return mediaTypeCapabilities;
	}

	/**
	 * @return the httpMethod
	 */
	public final HTTPMethod getHTTPMethod() {
		return httpMethod;
	}

	/**
	 * @param httpMethod
	 *            the httpMethod to set
	 */
	public final void setHTTPMethod(final HTTPMethod httpMethod) {
		this.httpMethod = httpMethod;
	}

	/**
	 * @return the uriPathTemplateFragment
	 */
	public final String getUriPathTemplateFragment() {
		return uriPathTemplateFragment;
	}

	/**
	 * @param uriPathTemplateFragment
	 *            the uriPathTemplateFragment to set
	 */
	public final void setUriPathTemplateFragment(final String uriPathTemplateFragment) {
		this.uriPathTemplateFragment = uriPathTemplateFragment;
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
		if (consumes != null && !this.mediaTypeCapabilities.getConsumedMimeTypes().contains(consumes)) {
			return false;
		}
		if (produces != null && !this.mediaTypeCapabilities.getProducedMimeTypes().contains(produces)) {
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
		buffer.append("{Accept:").append(mediaTypeCapabilities.getConsumedMimeTypes()).append(" Content-type: ")
				.append(mediaTypeCapabilities.getProducedMimeTypes()).append("}");
		return buffer.toString();
	}

	/**
	 * @return the queryParams
	 */
	public final List<ResourceMethodAnnotatedParameter> getQueryParams() {
		return queryParams;
	}

	/**
	 * @param queryParams
	 *            the queryParams to set
	 */
	public final void setQueryParams(final List<ResourceMethodAnnotatedParameter> queryParams) {
		this.queryParams.clear();
		this.queryParams.addAll(queryParams);
	}

	/**
	 * @return the pathParams
	 */
	public final List<ResourceMethodAnnotatedParameter> getPathParams() {
		return pathParams;
	}

	/**
	 * @param pathParams
	 *            the pathParams to set
	 */
	public final void setPathParams(final List<ResourceMethodAnnotatedParameter> pathParams) {
		this.pathParams.clear();
		this.pathParams.addAll(pathParams);
	}

	public final void setMediaTypeCapabilities(final MediaTypeCapabilities mediaTypeCapabilities) {
		this.mediaTypeCapabilities.setConsumedMimeTypes(mediaTypeCapabilities.getConsumedMimeTypes());
		this.mediaTypeCapabilities.setProducedMimeTypes(mediaTypeCapabilities.getProducedMimeTypes());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((httpMethod == null) ? 0 : httpMethod.hashCode());
		result = prime * result + ((mediaTypeCapabilities == null) ? 0 : mediaTypeCapabilities.hashCode());
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
		UriMapping other = (UriMapping) obj;
		if (httpMethod == null) {
			if (other.httpMethod != null) {
				return false;
			}
		} else if (!httpMethod.equals(other.httpMethod)) {
			return false;
		}
		if (mediaTypeCapabilities == null) {
			if (other.mediaTypeCapabilities != null) {
				return false;
			}
		} else if (!mediaTypeCapabilities.equals(other.mediaTypeCapabilities)) {
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
