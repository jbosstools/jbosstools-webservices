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

import java.util.Iterator;
import java.util.List;

public class ResolvedUriMapping implements Comparable<ResolvedUriMapping> {

	private HTTPMethod httpMethod = null;

	private final String fullUriPathTemplate;

	private final String baseUriPathTemplate;
	
	private final MediaTypeCapabilities mediaTypeCapabilities = new MediaTypeCapabilities();

	/**
	 * Full constructor
	 * 
	 * @param httpMethod
	 *            optional http method
	 * @param consumes
	 *            optional single consumed media type
	 * @param produces
	 *            optional single produced media type
	 * @param fullUriPathTemplate
	 *            optional URI path template fragment
	 */
	public ResolvedUriMapping(HTTPMethod httpMethod, String uriPathTemplate, List<ResourceMethodAnnotatedParameter> queryParams,
			MediaTypeCapabilities mediaTypeCapabilities) {
		super();
		this.httpMethod = httpMethod;
		this.baseUriPathTemplate = uriPathTemplate;
		this.fullUriPathTemplate = computeUriPathTemplate(uriPathTemplate, queryParams);
		this.mediaTypeCapabilities.merge(mediaTypeCapabilities);
	}

	private static String computeUriPathTemplate(String uriPathTemplate,  List<ResourceMethodAnnotatedParameter> queryParams) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(uriPathTemplate);
		if (queryParams != null && !queryParams.isEmpty()) {
			buffer.append("?");
			for (Iterator<ResourceMethodAnnotatedParameter> queryParamIterator = queryParams.iterator(); queryParamIterator
					.hasNext();) {
				ResourceMethodAnnotatedParameter queryParam = queryParamIterator.next();
				buffer.append(queryParam.getAnnotationValue()).append("={").append(queryParam.getParameterType()).append("}");
				if (queryParamIterator.hasNext()) {
					buffer.append("&");
				}

			}

		}
		return buffer.toString();
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
	 * {inheritDoc
	 */
	@Override
	public final String toString() {
		StringBuffer buffer = new StringBuffer();
		if (httpMethod != null) {
			buffer.append(httpMethod.getHttpVerb());
			buffer.append(" ");
		}
		String uriPathTemplate = getFullUriPathTemplate();
		if (uriPathTemplate != null) {
			buffer.append(uriPathTemplate);
			buffer.append(" ");
		}
		buffer.append("{Accept:").append(mediaTypeCapabilities.getConsumedMimeTypes()).append(" Content-type: ")
				.append(mediaTypeCapabilities.getProducedMimeTypes()).append("}");
		return buffer.toString();
	}

	/**
	 * @return the fullUriPathTemplate
	 */
	public final String getFullUriPathTemplate() {
		return fullUriPathTemplate;
	}

	/**
	 * @return the baseUriPathTemplate
	 */
	public String getBaseUriPathTemplate() {
		return baseUriPathTemplate;
	}

	/**
	 * Compares ResolvedURIMapping together. Comparison is first based on the
	 * URI Path Template *excluding* the query params, then on the associated
	 * HTTP Method for which a special order is used (GET first, etc.).
	 */
	@Override
	public final int compareTo(final ResolvedUriMapping other) {
		int u = baseUriPathTemplate.compareTo(other.getBaseUriPathTemplate());
		if (u != 0) {
			return u;
		}
		int h = httpMethod.compareTo(other.getHTTPMethod());
		if (h != 0) {
			return h;
		}
		return mediaTypeCapabilities.compareTo(other.getMediaTypeCapabilities());
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
		result = prime * result + ((fullUriPathTemplate == null) ? 0 : fullUriPathTemplate.hashCode());
		result = prime * result + ((mediaTypeCapabilities == null) ? 0 : mediaTypeCapabilities.hashCode());
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
		ResolvedUriMapping other = (ResolvedUriMapping) obj;
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
		if (fullUriPathTemplate == null) {
			if (other.fullUriPathTemplate != null) {
				return false;
			}
		} else if (!fullUriPathTemplate.equals(other.fullUriPathTemplate)) {
			return false;
		}
		return true;
	}

}
