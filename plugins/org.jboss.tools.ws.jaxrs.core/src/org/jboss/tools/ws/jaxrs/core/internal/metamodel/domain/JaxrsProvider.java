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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsProvider;

/**
 * <p>
 * JAX-RS Provider class <strong>Providers</strong> fall into 3 categories:
 * <ul>
 * <li>Entity Providers: the class MUST implement
 * <code>javax.ws.rs.ext.MessageBodyReader</code> and/or
 * <code>javax.ws.rs.ext.MessageBodyWriter</code>. It MAY also declare media
 * type capabilities with <code>javax.ws.rs.Consume</code> and
 * <code>java.ws.rs.Produces</code> annotations.</li>
 * <li>Context Providers: the class must implement
 * <code>javax.ws.rs.ext.ContextResolver</code></li>
 * <li>Exception Providers: the class must be annotated with
 * <code>javax.ws.rs.ext.ExceptionMapper</code></li>
 * </ul>
 * </p>
 * <p>
 * In all cases, the class is annotated with
 * <code>javax.ws.rs.ext.Provider</code> annotation.
 * </p>
 * 
 * @author xcoulon
 */
public class JaxrsProvider extends JaxrsJavaElement<IType> implements IJaxrsProvider {

	/**
	 * Internal 'Provider' element builder.
	 * 
	 * @author xcoulon
	 */
	public static class Builder {
		final IType javaType;
		final JaxrsMetamodel metamodel;
		private final Map<String, Annotation> annotations = new HashMap<String, Annotation>();
		private final Map<EnumElementKind, IType> providedTypes = new HashMap<EnumElementKind, IType>();

		public Builder(final IType javaType, final JaxrsMetamodel metamodel) {
			this.javaType = javaType;
			this.metamodel = metamodel;
		}

		public Builder providedTypes(final Map<EnumElementKind, IType> providedTypes) {
			this.providedTypes.putAll(providedTypes);
			return this;
		}

		public Builder annotations(final Map<String, Annotation> annotations) {
			this.annotations.putAll(annotations);
			return this;
		}

		public JaxrsProvider build() {
			return new JaxrsProvider(javaType, annotations, providedTypes, metamodel);
		}

	}

	private final Map<EnumElementKind, IType> providedKinds;

	/**
	 * Full constructor using the inner 'MediaTypeCapabilitiesBuilder' static
	 * class.
	 * 
	 * @param providedKinds
	 * 
	 * @param builder
	 */
	private JaxrsProvider(final IType javaType, final Map<String, Annotation> annotations,
			final Map<EnumElementKind, IType> providedKinds, final JaxrsMetamodel metamodel) {
		super(javaType, annotations, metamodel);
		this.providedKinds = providedKinds;
	}

	@Override
	public EnumElementCategory getElementCategory() {
		return EnumElementCategory.PROVIDER;
	}

	@Override
	public EnumElementKind getElementKind() {
		final boolean isMessageBodyReader = providedKinds.get(EnumElementKind.MESSAGE_BODY_READER) != null;
		final boolean isMessageBodyWriter = providedKinds.get(EnumElementKind.MESSAGE_BODY_WRITER) != null;
		final boolean isExceptionMapper = providedKinds.get(EnumElementKind.EXCEPTION_MAPPER) != null;
		final boolean isContextProvider = providedKinds.get(EnumElementKind.CONTEXT_PROVIDER) != null;
		if (isMessageBodyReader && isMessageBodyWriter) {
			return EnumElementKind.ENTITY_MAPPER;
		} else if (isMessageBodyReader) {
			return EnumElementKind.MESSAGE_BODY_READER;
		} else if (isMessageBodyWriter) {
			return EnumElementKind.MESSAGE_BODY_WRITER;
		} else if (isExceptionMapper) {
			return EnumElementKind.EXCEPTION_MAPPER;
		} else if (isContextProvider) {
			return EnumElementKind.CONTEXT_PROVIDER;
		}
		return null;
	}

	@Override
	public IType getProvidedType(EnumElementKind providerKind) {
		return providedKinds.get(providerKind);
	}

}
