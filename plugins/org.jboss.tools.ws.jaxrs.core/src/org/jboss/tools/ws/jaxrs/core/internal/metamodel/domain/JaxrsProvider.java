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

import static org.eclipse.jdt.core.IJavaElementDelta.CHANGED;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_PROVIDER_HIERARCHY;
import static org.jboss.tools.ws.jaxrs.core.utils.Annotation.VALUE;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.CONSUMES;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.CONTAINER_REQUEST_FILTER;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.CONTAINER_RESPONSE_FILTER;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.CONTEXT_RESOLVER;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.ENTITY_READER_INTERCEPTOR;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.ENTITY_WRITER_INTERCEPTOR;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.EXCEPTION_MAPPER;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.MESSAGE_BODY_READER;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.MESSAGE_BODY_WRITER;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PRODUCES;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PROVIDER;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.Flags;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsProvider;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta;
import org.jboss.tools.ws.jaxrs.core.utils.Annotation;
import org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames;
import org.jboss.tools.ws.jaxrs.core.utils.JdtUtils;

/**
 * <p>
 * JAX-RS Providers fall into 5 categories:
 * <ul>
 * <li>Entity Providers: the class must implement
 * <code>javax.ws.rs.ext.MessageBodyReader</code> and/or
 * <code>javax.ws.rs.ext.MessageBodyWriter</code>. It MAY also declare media
 * type capabilities with <code>javax.ws.rs.Consume</code> and
 * <code>java.ws.rs.Produces</code> annotations.</li>
 * <li>Context Providers: the class must implement
 * <code>javax.ws.rs.ext.ContextResolver</code></li>
 * <li>Exception Providers: the class must be annotated with
 * <code>javax.ws.rs.ext.ExceptionMapper</code></li>
 * <li>Request/Response Filters: the class must implement <code>javax.ws.rs.container.ContainerRequestFilter</code> and/or <code>javax.ws.rs.container.ContainerResponseFilter</code></li>
 * <li>Entity Interceptor: the class must implement <code>javax.ws.rs.ext.ReaderInterceptor</code> and/or <code>javax.ws.rs.ext.WriterInterceptor</code></li>
 * </ul>
 * </p>
 * <p>
 * In all cases, the class is annotated with
 * <code>javax.ws.rs.ext.Provider</code> annotation.
 * </p>
 * 
 * @author xcoulon
 */
public class JaxrsProvider extends AbstractJaxrsJavaTypeElement implements IJaxrsProvider {

	private final Map<EnumElementKind, IType> providedTypes;

	/**
	 * Builder initializer
	 * 
	 * @param javaElement
	 *            the underlying {@link IJavaElement} that on which this JAX-RS
	 *            Element will be built.
	 * @return the Builder
	 * @throws JavaModelException
	 */
	public static Builder from(final IJavaElement javaElement) throws JavaModelException {
		final CompilationUnit ast = JdtUtils.parse(javaElement, new NullProgressMonitor());
		switch (javaElement.getElementType()) {
		case IJavaElement.COMPILATION_UNIT:
			return new Builder(((ICompilationUnit) javaElement).findPrimaryType(), ast);
		case IJavaElement.TYPE:
			return new Builder((IType) javaElement, ast);
		}
		return null;
	}

	/**
	 * Builder initializer
	 * 
	 * @param javaElement
	 *            the underlying {@link IJavaElement} that on which this JAX-RS
	 *            Element will be built.
	 * @param ast
	 *            the associated AST
	 * @return the Builder
	 * @throws JavaModelException
	 */
	public static Builder from(final IJavaElement javaElement, final CompilationUnit ast) {
		switch (javaElement.getElementType()) {
		case IJavaElement.COMPILATION_UNIT:
			return new Builder(((ICompilationUnit) javaElement).findPrimaryType(), ast);
		case IJavaElement.TYPE:
			return new Builder((IType) javaElement, ast);
		}
		return null;
	}

	/**
	 * {@link JaxrsProvider} Fluent Builder
	 * 
	 * @author xcoulon
	 * 
	 */
	public static class Builder {

		private final IType javaType;
		private final CompilationUnit ast;
		private Map<String, Annotation> annotations;
		private Map<EnumElementKind, IType> providedKinds;
		private JaxrsMetamodel metamodel;

		private Builder(final IType javaType, final CompilationUnit ast) {
			this.javaType = javaType;
			this.ast = ast;
		}

		public Builder withMetamodel(final JaxrsMetamodel metamodel) {
			this.metamodel = metamodel;
			return this;
		}

		/**
		 * Creates a <strong>transient</strong> JAX-RS Provider from the given
		 * Type. A valid Provider must be annotated with
		 * <ul>
		 * <li><code>javax.ws.rs.ext.MessageBodyReader</code></li>
		 * <li><code>javax.ws.rs.ext.MessageBodyWriter</code></li>
		 * <li><code>javax.ws.rs.ext.ExceptionMapper</code></li>
		 * <li><code>javax.ws.rs.container.ContainerRequestFilter</code></li>
		 * <li><code>javax.ws.rs.container.ContainerResponseFilter</code></li>
		 * <li><code>javax.ws.rs.ext.ReaderInterceptor</code></li>
		 * <li><code>javax.ws.rs.ext.WriterInterceptor</code></li>
		 * </ul> 
		 * 
		 * @param javaType
		 * @throws CoreException
		 *             in case of underlying exception
		 * @return a representation of the given provider or null in case of
		 *         invalid type (ie, not a valid JAX-RS Provider)
		 */
		public JaxrsProvider build() throws CoreException {
			return build(true);
		}
		
		JaxrsProvider build(final boolean joinMetamodel) throws CoreException {
			final long start = System.currentTimeMillis();
			try {
				// skip if element does not exist or if it has compilation errors
				if (javaType == null || !javaType.exists() || !javaType.isStructureKnown()) {
					return null;
				}
				// assert that given java type is not abstract
				if (JdtUtils.isAbstractType(javaType)) {
					return null;
				}
				final ITypeHierarchy providerTypeHierarchy = JdtUtils.resolveTypeHierarchy(javaType,
						javaType.getJavaProject(), false, new NullProgressMonitor());
				final IType[] subtypes = providerTypeHierarchy.getSubtypes(javaType);
				// assert that given java type has no sub-type, or continue;
				if (subtypes != null && subtypes.length > 0) {
					return null;
				}
				// skip if type implements ParamConverterProvider
				final IType paramConverterProviderSupertype = JdtUtils.resolveType(JaxrsClassnames.PARAM_CONVERTER_PROVIDER,
						javaType.getJavaProject(), new NullProgressMonitor());
				if(JdtUtils.isTypeOrSuperType(paramConverterProviderSupertype, javaType)) {
					return null;
				}
				
				// retrieve the kind of provider from the implemented interfaces of the given java type
				this.providedKinds = getProvidedKinds(javaType, ast, providerTypeHierarchy, new NullProgressMonitor());
				// retrieve all annotations, including NameBinginds
				this.annotations = JdtUtils.resolveAllAnnotations(javaType, ast);
				if (annotations.get(PROVIDER) != null || !providedKinds.isEmpty()) {
					final JaxrsProvider provider = new JaxrsProvider(this);
					// this operation is only performed after creation
					if(joinMetamodel) {
						provider.joinMetamodel();
					}
					return provider;
				}
				return null;
			} finally {
				final long end = System.currentTimeMillis();
				Logger.tracePerf("Built JAX-RS Provider in {}ms", (end - start));
			}
		}

		/**
		 * 
		 * @param providerType the underlying {@link IType} to analyse 
		 * @param compilationUnit the associated {@link ICompilationUnit}
		 * @param providerTypeHierarchy 
		 * @param progressMonitor
		 * @return
		 * @throws CoreException
		 * @throws JavaModelException
		 */
		// FIXME: [Perf] cache this method call's result, should not happen each
		// time
		private static Map<EnumElementKind, IType> getProvidedKinds(final IType providerType,
				final CompilationUnit compilationUnit, final ITypeHierarchy providerTypeHierarchy,
				final IProgressMonitor progressMonitor) throws CoreException, JavaModelException {
			final Map<EnumElementKind, IType> implementedProviderKinds = new HashMap<EnumElementKind, IType>();
			final Map<String, EnumElementKind> providerInterfaces = new HashMap<String, EnumElementKind>();
			providerInterfaces.put(MESSAGE_BODY_READER, EnumElementKind.MESSAGE_BODY_READER);
			providerInterfaces.put(MESSAGE_BODY_WRITER, EnumElementKind.MESSAGE_BODY_WRITER);
			providerInterfaces.put(EXCEPTION_MAPPER, EnumElementKind.EXCEPTION_MAPPER);
			providerInterfaces.put(CONTEXT_RESOLVER, EnumElementKind.CONTEXT_RESOLVER);
			providerInterfaces.put(CONTAINER_REQUEST_FILTER, EnumElementKind.CONTAINER_REQUEST_FILTER);
			providerInterfaces.put(CONTAINER_RESPONSE_FILTER, EnumElementKind.CONTAINER_RESPONSE_FILTER);
			providerInterfaces.put(ENTITY_READER_INTERCEPTOR, EnumElementKind.ENTITY_READER_INTERCEPTOR);
			providerInterfaces.put(ENTITY_WRITER_INTERCEPTOR, EnumElementKind.ENTITY_WRITER_INTERCEPTOR);

			final List<IType> providerTypeSuperInterfaces = Arrays.asList(providerTypeHierarchy.getAllSuperInterfaces(providerType));
			for (Entry<String, EnumElementKind> entry : providerInterfaces.entrySet()) {
				final String interfaceName = entry.getKey();
				final EnumElementKind providerKind = entry.getValue();
				final IType matchingGenericType = JdtUtils.resolveType(interfaceName,
						providerType.getJavaProject(), progressMonitor);
				if(providerTypeSuperInterfaces.contains(matchingGenericType)) {
					final List<IType> argumentTypes = JdtUtils.resolveTypeArguments(providerType, compilationUnit,
							matchingGenericType, providerTypeHierarchy, progressMonitor);
					if (argumentTypes == null || argumentTypes.isEmpty()) {
						implementedProviderKinds.put(providerKind, null);
					} else {
						implementedProviderKinds.put(providerKind, argumentTypes.get(0));
					}
				}
			}
			return implementedProviderKinds;
		}
	}

	/**
	 * Full constructor.
	 * 
	 * @param builder
	 *            the fluent builder
	 */
	private JaxrsProvider(final Builder builder) {
		super(builder.javaType, builder.annotations, builder.metamodel);
		this.providedTypes = builder.providedKinds;
	}

	/**
	 * @return {@code true} if this element should be removed (ie, it does not meet the requirements to be a {@link JaxrsProvider} anymore) 
	 */
	@Override
	boolean isMarkedForRemoval() {
		final boolean hasProviderAnnotation = hasAnnotation(PROVIDER);
		final boolean hasProviderInterface = getElementKind() != EnumElementKind.UNDEFINED_PROVIDER;
		// element should be removed if it has no @Provider annotation or it
		// does not implement any of the provider interfaces
		// (missing annotation is acceptable by some JAX-RS implementation, and
		// Provider can be registered in the JAX-RS application or in the
		// web.xml)
		return !hasProviderAnnotation && !hasProviderInterface;
	}

	@Override
	public EnumElementKind getElementKind() {
		final boolean isMessageBodyReader = providedTypes.containsKey(EnumElementKind.MESSAGE_BODY_READER);
		final boolean isMessageBodyWriter = providedTypes.containsKey(EnumElementKind.MESSAGE_BODY_WRITER);
		final boolean isExceptionMapper = providedTypes.containsKey(EnumElementKind.EXCEPTION_MAPPER);
		final boolean isContextProvider = providedTypes.containsKey(EnumElementKind.CONTEXT_RESOLVER);
		final boolean isContainerRequestFilter = providedTypes.containsKey(EnumElementKind.CONTAINER_REQUEST_FILTER);
		final boolean isContainerResponseFilter = providedTypes.containsKey(EnumElementKind.CONTAINER_RESPONSE_FILTER);
		final boolean isEntityReaderInterceptor = providedTypes.containsKey(EnumElementKind.ENTITY_READER_INTERCEPTOR);
		final boolean isEntityWriterInterceptor = providedTypes.containsKey(EnumElementKind.ENTITY_WRITER_INTERCEPTOR);
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
		} else if (isContainerRequestFilter && isContainerResponseFilter) {
			return EnumElementKind.CONTAINER_FILTER;
		} else if (isContainerRequestFilter) {
			return EnumElementKind.CONTAINER_REQUEST_FILTER;
		} else if (isContainerResponseFilter) {
			return EnumElementKind.CONTAINER_RESPONSE_FILTER;
		} else if (isEntityReaderInterceptor && isEntityWriterInterceptor) {
			return EnumElementKind.ENTITY_INTERCEPTOR;
		} else if (isEntityReaderInterceptor) {
			return EnumElementKind.ENTITY_READER_INTERCEPTOR;
		} else if (isEntityWriterInterceptor) {
			return EnumElementKind.ENTITY_WRITER_INTERCEPTOR;
		}
		return EnumElementKind.UNDEFINED_PROVIDER;
	}

	@Override
	public IType getProvidedType(final EnumElementKind providerKind) {
		return providedTypes.get(providerKind);
	}

	/**
	 * @return the implemented interface {@link IType} indexed by their
	 *         associated {@link EnumElementKind}.
	 */
	public Map<EnumElementKind, IType> getProvidedTypes() {
		return providedTypes;
	}

	public Annotation getConsumesAnnotation() {
		return getAnnotation(CONSUMES);
	}

	@Override
	public List<String> getConsumedMediaTypes() {
		final Annotation consumesAnnotation = getConsumesAnnotation();
		if (consumesAnnotation != null) {
			return consumesAnnotation.getValues(VALUE);
		}
		return Collections.emptyList();
	}

	public Annotation getProducesAnnotation() {
		return getAnnotation(PRODUCES);
	}

	@Override
	public List<String> getProducedMediaTypes() {
		final Annotation producesAnnotation = getProducesAnnotation();
		if (producesAnnotation != null) {
			return producesAnnotation.getValues(VALUE);
		}
		return Collections.emptyList();
	}

	/**
	 * Update this provider from the given {@link IJavaElement} and its associated {@link CompilationUnit}.
	 * 
	 * @param javaElement the underlying {@link IJavaElement}
	 * @param ast the associated{@link CompilationUnit}
	 * 
	 * @return flags indicating the nature of the changes
	 * @throws CoreException
	 */
	@Override
	public void update(final IJavaElement javaElement, final CompilationUnit ast) throws CoreException {
		final JaxrsProvider transientProvider = JaxrsProvider.from(javaElement, ast).build(false);
		// clear this element if the given transient element is null
		if (transientProvider == null) {
			this.getProvidedTypes().clear();
			remove();
		} else {
			final Flags updateAnnotationsFlags = updateAnnotations(transientProvider.getAnnotations());
			final JaxrsElementDelta delta = new JaxrsElementDelta(this, CHANGED, updateAnnotationsFlags);
			if (!this.getProvidedTypes().equals(transientProvider.getProvidedTypes())) {
				this.getProvidedTypes().clear();
				this.getProvidedTypes().putAll(transientProvider.getProvidedTypes());
				delta.addFlag(F_PROVIDER_HIERARCHY);
			}
			if (isMarkedForRemoval()) {
				remove();
			}
			// update indexes for this element.
			else if(hasMetamodel()){
				getMetamodel().update(delta);
			}
		}
	}

	/**
	 * Return {@link AbstractJaxrsJavaElement#hashCode()} result based on underlying
	 * Java Type. Thus, it does not take the Provider's Type Parameter(s) into
	 * account here.
	 */
	@Override
	public int hashCode() {
		return super.hashCode();
	}

	/**
	 * Return {@link AbstractJaxrsJavaElement#equals(Object)} result based on underlying
	 * Java Type. Thus, it does not take the Provider's Type Parameter(s) into
	 * account here.
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
		final StringBuilder builder = new StringBuilder("JaxrsProvider ").append(getJavaElement().getElementName())
				.append("[");
		for (Iterator<Entry<EnumElementKind, IType>> iterator = this.providedTypes.entrySet().iterator(); iterator
				.hasNext();) {
			Entry<EnumElementKind, IType> entry = iterator.next();
			builder.append(entry.getKey().toString().toLowerCase()).append("->");
			if (entry.getValue() != null && entry.getValue().exists()) {
				builder.append(entry.getValue().getFullyQualifiedName());
			} else {
				builder.append("*unknown*");
			}
			if (iterator.hasNext()) {
				builder.append(", ");
			}
			builder.append("]");
		}
		return builder.toString();
	}

	/**
	 * Returns <code>true</code> if this provider and the given provider overlap on supported media types for the given element kind.
	 * @param otherProvider the provider to compare this one with.
	 * @param elementKind the kind to compare to.
	 * @return true if both providers overlap, false otherwise.
	 */
	@SuppressWarnings("incomplete-switch")
	public boolean collidesWith(final JaxrsProvider otherProvider, final EnumElementKind elementKind) {
		switch(elementKind) {
		case MESSAGE_BODY_READER:
			final List<String> otherConsumedMediaTypes = otherProvider.getConsumedMediaTypes();
			if(otherConsumedMediaTypes.isEmpty() || getConsumedMediaTypes().isEmpty()) {
				return true;
			}
			return CollectionUtils.hasIntersection(otherConsumedMediaTypes, getConsumedMediaTypes());
		case MESSAGE_BODY_WRITER:
		case EXCEPTION_MAPPER:
			final List<String> otherProducedMediaTypes = otherProvider.getProducedMediaTypes();
			if(otherProducedMediaTypes.isEmpty() || getProducedMediaTypes().isEmpty()) {
				return true;
			}
			return CollectionUtils.hasIntersection(otherProducedMediaTypes, getProducedMediaTypes());
		}
		
		return false;
	}

}
