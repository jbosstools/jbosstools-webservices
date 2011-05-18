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

import javax.ws.rs.HttpMethod;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.internal.core.BinaryType;
import org.jboss.tools.ws.jaxrs.core.utils.JdtUtils;

/**
 * A request method designator is a runtime annotation that is annotated with
 * the @HttpMethod annotation. JAX-RS defines a set of request method
 * designators for the common HTTP resourceMethods: @GET, @POST, @PUT, @DELETE,
 * 
 * @HEAD. Users may define their own custom request method designators including
 *        alternate designators for the common HTTP resourceMethods.
 * 
 * @author xcoulon
 * 
 */
@SuppressWarnings("restriction")
public class HTTPMethod extends BaseElement<IType> implements Comparable<HTTPMethod> {

	/** The HTTP Verb used with this Request method designator. */
	private String httpVerb = null;

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

		/**
		 * @return the rank
		 */
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

	/**
	 * Full constructor.
	 * 
	 * @param javaType
	 *            the underlying java type
	 * @param metamodel
	 *            the associated parent metamodel
	 * @param progressMonitor
	 *            the progress monitor
	 * @throws CoreException
	 *             in case of underlying exception
	 * @throws InvalidModelElementException
	 *             in case of underlying exception
	 */
	public HTTPMethod(final IType javaType, final Metamodel metamodel, final IProgressMonitor progressMonitor)
			throws CoreException, InvalidModelElementException {
		super(metamodel, javaType);
		// this.state = EnumState.CREATING;
		merge(javaType, progressMonitor);
		// this.state = EnumState.CREATED;
	}

	@Override
	public final void merge(final IType javaType, final IProgressMonitor progressMonitor)
			throws InvalidModelElementException, CoreException {

		// not much to validate on binary types..
		if (javaType instanceof BinaryType) {
			this.httpVerb = (String) javaType.getAnnotation(HttpMethod.class.getName()).getMemberValuePairs()[0]
					.getValue();
			return;
		}

		if (!JdtUtils.isTopLevelType(javaType)) {
			throw new InvalidModelElementException("Type is not a top-level type");
		}
		CompilationUnit compilationUnit = getCompilationUnit(javaType, progressMonitor);
		/*
		 * if (state == EnumState.CREATED) { Set<IProblem> problems =
		 * JdtUtils.resolveErrors(javaType, compilationUnit); if (problems !=
		 * null && problems.size() > 0) { //metamodel.reportErrors(javaType,
		 * problems); return; } }
		 */
		IAnnotationBinding annotationBinding = JdtUtils.resolveAnnotationBinding(getJavaElement(), compilationUnit,
				HttpMethod.class);
		if (annotationBinding == null) {
			throw new InvalidModelElementException("Annotation binding not found : missing 'import' statement ?");
		} else {
			this.httpVerb = (String) JdtUtils.resolveAnnotationAttributeValue(annotationBinding, "value");
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void validate(IProgressMonitor progressMonitor) {

	}

	@Override
	public final BaseElement.EnumType getKind() {
		return BaseElement.EnumType.HTTP_METHOD;
	}

	/**
	 * @return the httpVerb
	 */
	public final String getHttpVerb() {
		return httpVerb;
	}

	/**
	 * @return the name
	 */
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
		return "HTTPMethod [@" + getSimpleName() + ":" + httpVerb + "]";
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
		HTTPMethod other = (HTTPMethod) obj;
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
	public final int compareTo(final HTTPMethod other) {
		if (this.httpVerb.equals(other.getHttpVerb())) {
			return 0;
		}
		HttpVerbSortEnum enum1 = HttpVerbSortEnum.from(httpVerb);
		HttpVerbSortEnum enum2 = HttpVerbSortEnum.from(other.getHttpVerb());
		return enum1.getRank() - enum2.getRank();
	}

}
