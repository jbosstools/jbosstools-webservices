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

import java.util.Arrays;

import org.eclipse.core.resources.IResource;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;

/**
 * Built-in Http Methods as part of the JAX-RS 1.1 specification
 * @author Xavier Coulon
 *
 */
public class JaxrsBuiltinHttpMethod extends JaxrsHttpMethod {

	public static final JaxrsHttpMethod GET = new JaxrsBuiltinHttpMethod("javax.ws.rs.GET", "GET"); 
	public static final JaxrsHttpMethod POST = new JaxrsBuiltinHttpMethod("javax.ws.rs.POST", "POST"); 
	public static final JaxrsHttpMethod PUT = new JaxrsBuiltinHttpMethod("javax.ws.rs.PUT", "PUT"); 
	public static final JaxrsHttpMethod DELETE = new JaxrsBuiltinHttpMethod("javax.ws.rs.DELETE", "DELETE"); 
	public static final JaxrsHttpMethod OPTIONS = new JaxrsBuiltinHttpMethod("javax.ws.rs.OPTIONS", "OPTIONS"); 
	public static final JaxrsHttpMethod HEAD = new JaxrsBuiltinHttpMethod("javax.ws.rs.HEAD", "HEAD"); 

	private final String annotationName;
	private final String httpVerb;
	
	public JaxrsBuiltinHttpMethod(String annotationName, String annotationValue) {
		super(null, Arrays.asList(new Annotation(null, annotationName, annotationValue, null)), null);
		this.annotationName = annotationName;
		this.httpVerb = annotationValue;
	} 

	@Override
	public boolean isBuiltIn() {
		return true;
	}

	/**
	 * There is no resource associated with this built-in HTTP Method. Overriding this method prevents NPE (since <code>javaElement<code> attribute is null).
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
	public String getJavaClassName() {
		return annotationName;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((annotationName == null) ? 0 : annotationName.hashCode());
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
	
	
	
}
