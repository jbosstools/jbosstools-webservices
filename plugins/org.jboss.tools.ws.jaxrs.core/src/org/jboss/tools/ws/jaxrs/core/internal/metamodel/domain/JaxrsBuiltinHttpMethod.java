/******************************************************************************* 
 * Copyright (c) 2012 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain;

import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;

/**
 * Built-in Http Methods as part of the JAX-RS 1.1 specification
 * 
 * @author Xavier Coulon
 * 
 */
public class JaxrsBuiltinHttpMethod extends JaxrsHttpMethod {

	private final String annotationName;
	private final String httpVerb;

	public static Builder from(final String annotationName, final String httpVerb) {
		return new Builder(annotationName, httpVerb);
	}

	static class Builder {

		private final String annotationName;
		private final String httpVerb;
		private JaxrsMetamodel metamodel;
		private Map<String, Annotation> annotations;

		private Builder(final String annotationName, final String httpVerb) {
			this.annotationName = annotationName;
			this.httpVerb = httpVerb;
		}

		public JaxrsBuiltinHttpMethod buildIn(final JaxrsMetamodel metamodel) throws CoreException {
			annotations = CollectionUtils.toMap(annotationName, new Annotation(null, annotationName, httpVerb));
			this.metamodel = metamodel;
			final JaxrsBuiltinHttpMethod builtinHttpMethod = new JaxrsBuiltinHttpMethod(this);
			// this operation is only performed after creation
			builtinHttpMethod.joinMetamodel();
			return builtinHttpMethod;
		}
	}

	/**
	 * Full constructor.
	 * 
	 * @param annotationName
	 * @param httpVerb
	 * @param metamodel
	 */
	private JaxrsBuiltinHttpMethod(final Builder builder) {
		super(null, builder.annotations, builder.metamodel);
		this.annotationName = builder.annotationName;
		this.httpVerb = builder.httpVerb;
	}

	@Override
	public boolean isBuiltIn() {
		return true;
	}

	/**
	 * There is no resource associated with this built-in HTTP Method.
	 * Overriding this method prevents NPE (since
	 * <code>javaElement<code> attribute is null).
	 * 
	 * @see org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsJavaElement#getResource()
	 */
	@Override
	public IResource getResource() {
		return null;
	}

	@Override
	public String getHttpVerb() {
		return this.httpVerb;
	}

	@Override
	public String getName() {
		return this.annotationName;
	}

	/**
	 * @see org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsJavaElement#getIdentifier()
	 */
	@Override
	public String getIdentifier() {
		return this.annotationName;
	}

	@Override
	public String getJavaClassName() {
		return annotationName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((annotationName == null) ? 0 : annotationName.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		JaxrsBuiltinHttpMethod other = (JaxrsBuiltinHttpMethod) obj;
		if (annotationName == null) {
			if (other.annotationName != null) {
				return false;
			}
		} else if (!annotationName.equals(other.annotationName)) {
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
	public String toString() {
		return "HttpMethod (built-in)[@" + getJavaClassName() + ":" + httpVerb + "]";
	}

}
