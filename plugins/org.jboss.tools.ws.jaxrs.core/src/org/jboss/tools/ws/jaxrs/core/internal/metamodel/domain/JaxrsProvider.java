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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.IType;
import org.eclipse.wst.validation.ValidatorMessage;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsProvider;

/**
 * JAX-RS Provider class Providers <strong>must</strong> implement MessageBodyReader, MessageBodyWriter or
 * ExceptionMapper Providers *may* be annotated with <code>javax.ws.rs.ext.Provider</code> annotation.
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
		private Annotation consumesAnnotation;
		private Annotation producesAnnotation;
		private Annotation providerAnnotation;
		private Map<EnumElementKind, IType> providedKinds;

		public Builder(final IType javaType, final JaxrsMetamodel metamodel) {
			this.javaType = javaType;
			this.metamodel = metamodel;
		}

		public Builder withProviderAnnotation(final Annotation providerAnnotation) {
			this.providerAnnotation = providerAnnotation;
			return this;
		}

		public Builder consumes(final Annotation consumesAnnotation) {
			this.consumesAnnotation = consumesAnnotation;
			return this;
		}

		public Builder produces(final Annotation producesAnnotation) {
			this.producesAnnotation = producesAnnotation;
			return this;
		}

		public JaxrsProvider build() {
			List<Annotation> annotations = new ArrayList<Annotation>();
			if (providerAnnotation != null) {
				annotations.add(providerAnnotation);
			}
			if (consumesAnnotation != null) {
				annotations.add(consumesAnnotation);
			}
			if (producesAnnotation != null) {
				annotations.add(producesAnnotation);
			}
			return new JaxrsProvider(javaType, annotations, providedKinds, metamodel);
		}

		public Builder providing(final Annotation providerAnnotation, final Map<EnumElementKind, IType> providedKinds) {
			this.providerAnnotation = providerAnnotation;
			this.providedKinds = providedKinds;
			return this;
		}
	}

	private final Map<EnumElementKind, IType> providedKinds;
	/**
	 * Full constructor using the inner 'MediaTypeCapabilitiesBuilder' static class.
	 * @param providedKinds 
	 * 
	 * @param builder
	 */
	private JaxrsProvider(final IType javaType, final List<Annotation> annotations, final Map<EnumElementKind, IType> providedKinds, final JaxrsMetamodel metamodel) {
		super(javaType, annotations, metamodel);
		this.providedKinds = providedKinds;
	}

	@Override
	public EnumElementCategory getElementCategory() {
		return EnumElementCategory.PROVIDER;
	}

	@Override
	public List<ValidatorMessage> validate() {
		List<ValidatorMessage> messages = new ArrayList<ValidatorMessage>();
		return messages;
	}

	@Override
	public EnumElementKind getElementKind() {
		final boolean isMessageBodyReader = providedKinds.get(EnumElementKind.MESSAGE_BODY_READER) != null;
		final boolean isMessageBodyWriter = providedKinds.get(EnumElementKind.MESSAGE_BODY_WRITER) != null;
		final boolean isExceptionMapper = providedKinds.get(EnumElementKind.EXCEPTION_MAPPER) != null;
		if(isMessageBodyReader && isMessageBodyWriter) {
			return EnumElementKind.ENTITY_MAPPER;
		} else if(isMessageBodyReader) {
			return EnumElementKind.MESSAGE_BODY_READER;
		} else if(isMessageBodyWriter) {
			return EnumElementKind.MESSAGE_BODY_WRITER;
		} else if(isExceptionMapper) {
			return EnumElementKind.EXCEPTION_MAPPER;
		}
		return null;
	}

	@Override
	public IType getProvidedType(EnumElementKind providerKind) {
		return providedKinds.get(providerKind);
	}

}
