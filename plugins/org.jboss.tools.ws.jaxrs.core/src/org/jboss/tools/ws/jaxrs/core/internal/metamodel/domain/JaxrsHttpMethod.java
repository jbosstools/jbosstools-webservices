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

import javax.ws.rs.HttpMethod;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsHttpMethod;

/** A request method designator is a runtime annotation that is annotated with
 * the @HttpMethod annotation. JAX-RS defines a set of request method
 * designators for the common HTTP resourceMethods: @GET, @POST, @PUT, @DELETE,
 * 
 * @HEAD. Users may define their own custom request method designators including
 *        alternate designators for the common HTTP resourceMethods.
 * 
 * @author xcoulon */
public class JaxrsHttpMethod extends JaxrsElement<IType> implements IJaxrsHttpMethod {

	/** A Simple sorter for HTTP Verbs: the preferred order is
	 * GET/POST/PUT/DELETE/HEAD/OPTION. Other/custom Verbs come after. */
	private enum HttpVerbSortEnum {
		/** GET Verb. */
		GET(0),
		/** POST Verb. */
		POST(1),
		/** PUT Verb. */
		PUT(2),
		/** DELETE Verb. */
		DELETE(3),
		/** HEAD Verb. */
		HEAD(5),
		/** OPTIONS Verb. */
		OPTIONS(6),
		/** Any other HTTP Verb. */
		OTHER(7);

		/** the order rank. */
		private int rank = 0;

		/** The constructor.
		 * 
		 * @param r
		 *            the rank */
		HttpVerbSortEnum(final int r) {
			this.rank = r;
		}

		/** @return the rank */
		int getRank() {
			return rank;
		}

		/** Converter from literal to enum value. If no direct match, 'OTHER' is
		 * returned.
		 * 
		 * @param verb
		 *            the HTTP verb
		 * @return the corresponding value in the enum. */
		static HttpVerbSortEnum from(final String verb) {
			for (HttpVerbSortEnum e : HttpVerbSortEnum.values()) {
				if (verb.equals(e.toString())) {
					return e;
				}
			}
			return OTHER;
		}
	}

	public JaxrsHttpMethod(IType javaType, Annotation httpMehodAnnotation, JaxrsMetamodel metamodel) {
		super(javaType, httpMehodAnnotation, metamodel);
	}

	@Override
	public EnumElementKind getElementKind() {
		return EnumElementKind.HTTP_METHOD;
	}

	/** {@inheritDoc} */
	@Override
	public void validate(IProgressMonitor progressMonitor) {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jboss.tools.ws.jaxrs.core.internal.metamodel.IHttpMethod#getHttpVerb
	 * ()
	 */
	@Override
	public final String getHttpVerb() {
		final Annotation httpVerbAnnotation = getHttpMethodAnnotation();
		if (httpVerbAnnotation != null) {
			return httpVerbAnnotation.getValue("value");
		}
		return null;
	}

	/** @param httpVerb
	 *            the httpVerb to set
	 * @return */
	public boolean setHttpMethodAnnotation(final Annotation httpVerbAnnotation) {
		return getHttpMethodAnnotation().update(httpVerbAnnotation);
	}

	/** @return the httpVerbAnnotation */
	@Override
	public Annotation getHttpMethodAnnotation() {
		return getAnnotation(HttpMethod.class.getName());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jboss.tools.ws.jaxrs.core.internal.metamodel.IHttpMethod#getSimpleName
	 * ()
	 */
	@Override
	public final String getSimpleName() {
		return getJavaElement().getElementName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public final String toString() {
		return "HttpMethod [@" + getSimpleName() + ":" + getHttpMethodAnnotation() + "]";
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
		result = prime * result + ((getJavaElement() == null) ? 0 : getJavaElement().hashCode());
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
		JaxrsHttpMethod other = (JaxrsHttpMethod) obj;
		if (getJavaElement() == null) {
			if (other.getJavaElement() != null) {
				return false;
			}
		} else if (!getJavaElement().equals(other.getJavaElement())) {
			return false;
		}
		return true;
	}

	@Override
	public final int compareTo(final IJaxrsHttpMethod other) {
		if (this.getHttpVerb().equals(other.getHttpVerb())) {
			return 0;
		}
		HttpVerbSortEnum enum1 = HttpVerbSortEnum.from(this.getHttpVerb());
		HttpVerbSortEnum enum2 = HttpVerbSortEnum.from(other.getHttpVerb());
		return enum1.getRank() - enum2.getRank();
	}

	@Override
	public EnumKind getKind() {
		if (getHttpMethodAnnotation() != null) {
			return EnumKind.HTTP_METHOD;
		}
		return EnumKind.UNDEFINED;
	}

}
