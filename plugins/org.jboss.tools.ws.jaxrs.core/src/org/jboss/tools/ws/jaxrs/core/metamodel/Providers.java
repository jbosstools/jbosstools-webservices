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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.internal.builder.JAXRSAnnotationsScanner;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.metamodel.Provider.EnumProviderKind;
import org.jboss.tools.ws.jaxrs.core.utils.JdtUtils;

/**
 * Container for JAX-RS Providers elements
 * 
 * @author xcoulon
 * 
 */
public class Providers extends BaseElementContainer<Provider> {

	/** The interfaces that a provider can implement, indexed by kind */
	private final Map<EnumProviderKind, IType> providerInterfaces;

	public static class Builder {

		private final IJavaProject javaProject;
		private final Metamodel metamodel;

		public Builder(final IJavaProject javaProject, final Metamodel metamodel) {
			this.javaProject = javaProject;
			this.metamodel = metamodel;
		}

		public Providers build() throws CoreException {
			Map<EnumProviderKind, IType> providerInterfaces = new HashMap<EnumProviderKind, IType>();
			providerInterfaces.put(EnumProviderKind.CONSUMER,
					JdtUtils.resolveType("javax.ws.rs.ext.MessageBodyReader", javaProject.getJavaProject(), null));
			providerInterfaces.put(EnumProviderKind.PRODUCER,
					JdtUtils.resolveType("javax.ws.rs.ext.MessageBodyWriter", javaProject.getJavaProject(), null));
			providerInterfaces.put(EnumProviderKind.EXCEPTION_MAPPER,
					JdtUtils.resolveType("javax.ws.rs.ext.ExceptionMapper", javaProject.getJavaProject(), null));
			return new Providers(metamodel, providerInterfaces);
		}

	}

	/**
	 * Full constructor
	 * 
	 * @param javaProject
	 * @param providerInterfaces
	 */
	private Providers(final Metamodel metamodel, final Map<EnumProviderKind, IType> providerInterfaces) {
		super(metamodel);
		this.providerInterfaces = providerInterfaces;
	}

	public final Map<EnumProviderKind, IType> getProviderInterfaces() {
		return providerInterfaces;
	}

	/**
	 * The JAX-RS spec writes: "Message body readers and writers MAY restrict
	 * the media types they support using the @Consumes and @Produces
	 * annotations respectively. The absence of these annotations is equivalent
	 * to their inclusion with media type ("* / *"), i.e. absence implies that
	 * any media type is supported. An implementation MUST NOT use an entity
	 * provider for a media type that is not supported by that provider. When
	 * choosing an entity provider an implementation sorts the available
	 * providers according to the media types they declare support for. Sorting
	 * of media types follows the general rule: x/y < x/ * < * / *, i.e. a
	 * provider that explicitly lists a media types is sorted before a provider
	 * that lists * / *".
	 * 
	 * From this eclipse plugin point-of-view, the entity providers are concrete
	 * classes with no subclass and which implement the MessageBodyReader and/or
	 * MessageBodyWriter interfaces. If no annotation is provided, the supported
	 * mime-type is "* / *".
	 * 
	 * The following method also tries to identify the parameter type used to
	 * implement the interface(s). (Yep, this is the hardest part of the thing,
	 * but it provides added-value to the end-developper...)
	 * 
	 * @param scope
	 *            the scope from which elements should be added
	 * 
	 * @param progressMonitor
	 * @throws CoreException
	 */
	@Override
	public final List<Provider> addFrom(final IJavaElement scope, final IProgressMonitor progressMonitor)
			throws CoreException {
		progressMonitor.beginTask("Adding providers", 1);
		try {
			// FIXME : add support for javax.ws.rs.ext.ContextResolver(s) (most
			// often framework classes)
			// Consumers, Producers and ExceptionMappers are interfaces that are
			// implemented by Providers
			List<IType> providerTypes = JAXRSAnnotationsScanner.findProviderTypes(scope, false, progressMonitor);
			// FIXME : should check type is a top level and annotation binding
			// exists. Throw an exception in constructor ?
			List<Provider> addedProviders = new ArrayList<Provider>();
			for (IType providerType : providerTypes) {
				try {
					Provider provider = new Provider.Builder(providerType, metamodel, this).build(progressMonitor);
					elements.put(providerType.getFullyQualifiedName(), provider);
					addedProviders.add(provider);
				} catch (InvalidModelElementException e) {
					Logger.warn("Type '" + providerType.getFullyQualifiedName() + "' is not a valid JAX-RS Provider : "
							+ e.getMessage());
				}
			}
			progressMonitor.worked(1);
			return addedProviders;
		} finally {
			progressMonitor.done();
		}
	}

	/**
	 * Return the provider for the given parameterized type, or null if not
	 * found.
	 * 
	 * @param type
	 *            the parameterized type, as in
	 *            <code>ExceptionMapper&lt;type&gt;</code>
	 * @return a provider, or null
	 */
	public final Provider getFor(final String type) {
		for (Entry<String, Provider> providerEntry : elements.entrySet()) {
			Provider p = providerEntry.getValue();
			for (Entry<EnumProviderKind, IType> kindEntry : p.getProvidedKinds().entrySet()) {
				if (type.equals(kindEntry.getValue().getFullyQualifiedName())) {
					return p;
				}
			}
		}
		return null;
	}

	/**
	 * Return the provider for the given parameterized type, or null if not
	 * found.
	 * 
	 * @param type
	 *            the parameterized type, as in
	 *            <code>ExceptionMapper&lt;type&gt;</code>
	 * @return a provider, or null
	 */
	public final Provider getFor(final IType providedType) {
		for (Entry<String, Provider> providerEntry : elements.entrySet()) {
			Provider p = providerEntry.getValue();
			for (Entry<EnumProviderKind, IType> kindEntry : p.getProvidedKinds().entrySet()) {
				if (providedType.equals(kindEntry.getValue())) {
					return p;
				}
			}
		}
		return null;
	}

	/**
	 * Returns the producers from the providers list
	 * 
	 * @return the producers
	 */
	public final List<Provider> getProducers() {
		return filterProvidersByKind(EnumProviderKind.PRODUCER);
	}

	/**
	 * Returns the consumers from the providers list
	 * 
	 * @return the consumers
	 */
	public final List<Provider> getConsumers() {
		return filterProvidersByKind(EnumProviderKind.CONSUMER);
	}

	/**
	 * Returns the exceptionMappers from the providers list
	 * 
	 * @return the exceptionMappers
	 */
	public final List<Provider> getExceptionMappers() {
		return filterProvidersByKind(EnumProviderKind.EXCEPTION_MAPPER);
	}

	private List<Provider> filterProvidersByKind(final EnumProviderKind providerKind) {
		List<Provider> matches = new ArrayList<Provider>();
		for (Entry<String, Provider> entry : elements.entrySet()) {
			Provider p = entry.getValue();
			if (p.getProvidedKinds().containsKey(providerKind)) {
				matches.add(p);
			}
		}
		return Collections.unmodifiableList(matches);
	}
}
