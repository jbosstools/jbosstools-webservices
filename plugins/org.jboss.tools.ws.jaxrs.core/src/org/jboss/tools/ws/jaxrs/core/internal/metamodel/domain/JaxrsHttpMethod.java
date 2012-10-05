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

import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.HTTP_METHOD;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
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
	
	public static class Builder {
		final IType javaType;
		final JaxrsMetamodel metamodel;
		final List<Annotation> annotations = new ArrayList<Annotation>();

		public Builder(final IType javaType, final JaxrsMetamodel metamodel) {
			this.javaType = javaType;
			this.metamodel = metamodel;
		}

		public Builder retention(final Annotation retentionAnnotation) {
			if(retentionAnnotation != null) {
				annotations.add(retentionAnnotation);
			}
			return this;
		}

		public Builder httpMethod(final Annotation httpMethodAnnotation) {
			if(httpMethodAnnotation != null) {
				annotations.add(httpMethodAnnotation);
			}
			return this;
		}

		public Builder target(final Annotation targetAnnotation) {
			if(targetAnnotation != null) {
				annotations.add(targetAnnotation);
			}
			return this;
		}

		public Builder annotations(final Collection<Annotation> annotations) {
			if(annotations != null) {
				this.annotations.addAll(annotations);
			}
			return this;
		}
		public JaxrsHttpMethod build() {
			JaxrsHttpMethod httpMethod = new JaxrsHttpMethod(javaType, annotations, metamodel);
			return httpMethod;
		}
	}

	/**
	 * Full constructor.
	 * 
	 * @param annotations
	 * 
	 */
	JaxrsHttpMethod(IType javaType, List<Annotation> annotations,
			final JaxrsMetamodel metamodel) {
		super(javaType, annotations, metamodel);
	}
	
	public boolean isBuiltIn() {
		return false;
	}

	@Override
	public EnumElementCategory getElementCategory() {
		return EnumElementCategory.HTTP_METHOD;
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

	/** @return the HttpMethod Annotation */
	public Annotation getHttpMethodAnnotation() {
		return getAnnotation(HTTP_METHOD.qualifiedName);
	}
	
	/** @return the Retention Annotation */
	public Annotation getRetentionAnnotation() {
		return getAnnotation(Retention.class.getName());
	}
	
	/** @return the Target Annotation */
	public Annotation getTargetAnnotation() {
		return getAnnotation(Target.class.getName());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jboss.tools.ws.jaxrs.core.internal.metamodel.IHttpMethod#getSimpleName
	 * ()
	 */
	@Override
	public String getJavaClassName() {
		return getJavaElement().getFullyQualifiedName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "HttpMethod [@" + getJavaClassName() + ":" + getHttpMethodAnnotation() + "]";
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
	public EnumElementKind getElementKind() {
		if (getHttpMethodAnnotation() != null) {
			return EnumElementKind.HTTP_METHOD;
		}
		return EnumElementKind.UNDEFINED;
	}

	/**
	 * Update this HttpMethod with the elements of the given httpMethod
	 * 
	 * @param httpMethod
	 * @return the flags indicating the kind of changes that occurred during the
	 *         update.
	 */
	public int update(final JaxrsHttpMethod httpMethod) {
		/*int flags = 0;
		final Annotation annotation = this.getAnnotation(HTTP_METHOD.qualifiedName);
		final Annotation otherAnnotation = httpMethod.getAnnotation(HTTP_METHOD.qualifiedName);
		if (annotation != null && otherAnnotation != null && !annotation.equals(otherAnnotation)
				&& annotation.update(otherAnnotation)) {
			flags += F_HTTP_METHOD_VALUE;
		}
		return flags;*/
		return updateAnnotations(httpMethod.getAnnotations());
	}

}
