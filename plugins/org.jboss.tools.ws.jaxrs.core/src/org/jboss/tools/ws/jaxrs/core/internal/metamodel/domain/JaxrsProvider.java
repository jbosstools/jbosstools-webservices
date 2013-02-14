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

import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementDelta.F_NONE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementDelta.F_PROVIDER_HIERARCHY;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.CONSUMES;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PRODUCES;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PROVIDER;

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
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

	private final Map<EnumElementKind, IType> providedTypes;

	/**
	 * Full constructor using the inner 'MediaTypeCapabilitiesBuilder' static
	 * class.
	 * 
	 * @param providedKinds
	 * 
	 * @param builder
	 */
	public JaxrsProvider(final IType javaType, final Map<String, Annotation> annotations,
			final Map<EnumElementKind, IType> providedKinds, final JaxrsMetamodel metamodel) {
		super(javaType, annotations, metamodel);
		this.providedTypes = providedKinds;
	}

	@Override
	public EnumElementCategory getElementCategory() {
		return EnumElementCategory.PROVIDER;
	}
	
	@Override
	public boolean isMarkedForRemoval() {
		final boolean isMessageBodyReader = providedTypes.get(EnumElementKind.MESSAGE_BODY_READER) != null;
		final boolean isMessageBodyWriter = providedTypes.get(EnumElementKind.MESSAGE_BODY_WRITER) != null;
		final boolean isExceptionMapper = providedTypes.get(EnumElementKind.EXCEPTION_MAPPER) != null;
		final boolean isContextProvider = providedTypes.get(EnumElementKind.CONTEXT_RESOLVER) != null;
		final boolean hasProviderAnnotation = hasAnnotation(PROVIDER.qualifiedName);
		// element should be removed if it has no @Provider annotation or it does not implement any of the provider interfaces
		// (missing annotation is acceptable by some JAX-RS implementation, and Provider can be registered in the JAX-RS application or in the web.xml)
		return !(hasProviderAnnotation || (isMessageBodyReader || isMessageBodyWriter || isContextProvider || isExceptionMapper));
	}

	@Override
	public EnumElementKind getElementKind() {
		final boolean isMessageBodyReader = providedTypes.get(EnumElementKind.MESSAGE_BODY_READER) != null;
		final boolean isMessageBodyWriter = providedTypes.get(EnumElementKind.MESSAGE_BODY_WRITER) != null;
		final boolean isExceptionMapper = providedTypes.get(EnumElementKind.EXCEPTION_MAPPER) != null;
		final boolean isContextProvider = providedTypes.get(EnumElementKind.CONTEXT_RESOLVER) != null;
		if (isMessageBodyReader && isMessageBodyWriter) {
			return EnumElementKind.ENTITY_MAPPER;
		} else if (isMessageBodyReader) {
			return EnumElementKind.MESSAGE_BODY_READER;
		} else if (isMessageBodyWriter) {
			return EnumElementKind.MESSAGE_BODY_WRITER;
		} else if (isExceptionMapper) {
			return EnumElementKind.EXCEPTION_MAPPER;
		} else if (isContextProvider) {
			return EnumElementKind.CONTEXT_RESOLVER;
		}
		return EnumElementKind.UNDEFINED;
	}

	@Override
	public IType getProvidedType(EnumElementKind providerKind) {
		return providedTypes.get(providerKind);
	}

	public Map<EnumElementKind, IType> getProvidedTypes() {
		return providedTypes;
	}
	
	public Annotation getConsumesAnnotation() {
		return getAnnotation(CONSUMES.qualifiedName);
	}

	@Override
	public List<String> getConsumedMediaTypes() {
		final Annotation consumesAnnotation = getConsumesAnnotation();
		if (consumesAnnotation == null) {
			return null;
		}
		return consumesAnnotation.getValues("value");
	}

	public Annotation getProducesAnnotation() {
		return getAnnotation(PRODUCES.qualifiedName);
	}

	@Override
	public List<String> getProducedMediaTypes() {
		final Annotation producesAnnotation = getProducesAnnotation();
		if (producesAnnotation == null) {
			return null;
		}
		return producesAnnotation.getValues("value");
	}

	/**
	 * Update this provider from the given provider.
	 * 
	 * @param transientProvider
	 * @return flags indicating the nature of the changes
	 * @throws CoreException
	 */
	public int update(final JaxrsProvider transientProvider) throws CoreException {
		int flags = F_NONE;
		if (transientProvider != null) {
			if (!this.getProvidedTypes().equals(transientProvider.getProvidedTypes())) {
				this.providedTypes.clear();
				this.providedTypes.putAll(transientProvider.getProvidedTypes());
				flags += F_PROVIDER_HIERARCHY;
			}
			flags += updateAnnotations(transientProvider.getAnnotations());
		}
		return flags;
	}

	/**
	 * Return {@link JaxrsJavaElement#hashCode()} result based on underlying Java Type. Thus, it does not take the Provider's Type Parameter(s) into account here.
	 */
	@Override
	public int hashCode() {
		return super.hashCode();
	}

	/**
	 * Return {@link JaxrsJavaElement#equals(Object)} result based on underlying Java Type. Thus, it does not take the Provider's Type Parameter(s) into account here.
	 */
	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "JaxrsProvider " + getJavaElement().getElementName() + " [" + providedTypes + "]";
	}

}
