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

import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementDelta.F_HTTP_METHOD_VALUE;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsElements.HTTP_METHOD;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.core.IType;
import org.eclipse.wst.validation.ValidatorMessage;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsMetamodelBuilder;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsHttpMethod;

/**
 * A request method designator is a runtime annotation that is annotated with
 * the @HttpMethod annotation. JAX-RS defines a set of request method
 * designators for the common HTTP resourceMethods: @GET, @POST, @PUT, @DELETE,
 * 
 * @HEAD. Users may define their own custom request method designators including
 *        alternate designators for the common HTTP resourceMethods.
 * 
 * @author xcoulon
 */
public class JaxrsHttpMethod extends JaxrsJavaElement<IType> implements IJaxrsHttpMethod {

	/**
	 * A Simple sorter for HTTP Verbs: the preferred order is
	 * GET/POST/PUT/DELETE/HEAD/OPTION. Other/custom Verbs come after.
	 */
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

		/**
		 * The constructor.
		 * 
		 * @param r
		 *            the rank
		 */
		HttpVerbSortEnum(final int r) {
			this.rank = r;
		}

		/** @return the rank */
		int getRank() {
			return rank;
		}

		/**
		 * Converter from literal to enum value. If no direct match, 'OTHER' is
		 * returned.
		 * 
		 * @param verb
		 *            the HTTP verb
		 * @return the corresponding value in the enum.
		 */
		static HttpVerbSortEnum from(final String verb) {
			for (HttpVerbSortEnum e : HttpVerbSortEnum.values()) {
				if (verb.equals(e.toString())) {
					return e;
				}
			}
			return OTHER;
		}
	}
	
	public JaxrsHttpMethod(IType javaType, Annotation httpMethodAnnotation, JaxrsMetamodel metamodel) {
		super(javaType, httpMethodAnnotation, metamodel);
	}

	@Override
	public EnumElementKind getElementKind() {
		return EnumElementKind.HTTP_METHOD;
	}

	@Override
	public List<ValidatorMessage> validate() {
		List<ValidatorMessage> messages = new ArrayList<ValidatorMessage>();
		final Annotation annotation = getHttpMethodAnnotation();
		if (annotation == null) {

		} else {
			final String httpValue = annotation.getValue("value");
			if (httpValue == null || httpValue.isEmpty()) {
				final ValidatorMessage message = ValidatorMessage
						.create("HTTP Verb should not be empty", getResource());
				message.setAttribute(IMarker.MARKER, JaxrsMetamodelBuilder.JAXRS_PROBLEM);
				message.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
				message.setAttribute(IMarker.CHAR_START, annotation.getRegion().getOffset());
				message.setAttribute(IMarker.CHAR_END, annotation.getRegion().getOffset()
						+ annotation.getRegion().getLength());
				messages.add(message);
			}
		}
		return messages;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jboss.tools.ws.jaxrs.core.internal.metamodel.IHttpMethod#getHttpVerb
	 * ()
	 */
	@Override
	public String getHttpVerb() {
		final Annotation httpVerbAnnotation = getHttpMethodAnnotation();
		if (httpVerbAnnotation != null) {
			return httpVerbAnnotation.getValue("value");
		}
		return null;
	}

	/** @return the httpVerbAnnotation */
	@Override
	public Annotation getHttpMethodAnnotation() {
		return getAnnotation(HTTP_METHOD.qualifiedName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jboss.tools.ws.jaxrs.core.internal.metamodel.IHttpMethod#getSimpleName
	 * ()
	 */
	@Override
	public String getFullyQualifiedName() {
		return getJavaElement().getFullyQualifiedName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public final String toString() {
		return "HttpMethod [@" + getFullyQualifiedName() + ":" + getHttpMethodAnnotation() + "]";
	}

	@Override
	public final int compareTo(final IJaxrsHttpMethod other) {
		if(this.getHttpVerb() == null) {
			return 1;
		}
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

	/**
	 * Update this HttpMethod with the elements of the given httpMethod
	 * 
	 * @param httpMethod
	 * @return the flags indicating the kind of changes that occurred during the
	 *         update.
	 */
	public int update(JaxrsHttpMethod httpMethod) {
		int flags = 0;
		final Annotation annotation = this.getAnnotation(HTTP_METHOD.qualifiedName);
		final Annotation otherAnnotation = httpMethod.getAnnotation(HTTP_METHOD.qualifiedName);
		if (annotation != null && otherAnnotation != null && !annotation.equals(otherAnnotation)
				&& annotation.update(otherAnnotation)) {
			flags += F_HTTP_METHOD_VALUE;
		}
		return flags;
	}

}
