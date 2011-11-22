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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsProvider;
import org.jboss.tools.ws.jaxrs.core.metamodel.InvalidModelElementException;

/** JAX-RS Provider class Providers *must* implement MessageBodyReader,
 * MessageBodyWriter or ExceptionMapper Providers *may* be annotated with
 * <code>javax.ws.rs.ext.Provider</code> annotation.
 * 
 * @author xcoulon */
public class JaxrsProvider extends JaxrsElement<IType> implements IJaxrsProvider {

	/** Internal 'Provider' element builder.
	 * 
	 * @author xcoulon */
	public static class Builder {

		private final JaxrsMetamodel metamodel;
		private final IType javaType;

		/** Mandatory attributes of the enclosing 'Provider' element.
		 * 
		 * @param javaType
		 * @param metamodel */
		public Builder(final IType javaType, final JaxrsMetamodel metamodel) {
			this.javaType = javaType;
			this.metamodel = metamodel;
		}

		/** Builds and returns the elements. Internally calls the merge() method.
		 * 
		 * @param progressMonitor
		 * @return
		 * @throws InvalidModelElementException
		 * @throws CoreException */
		public JaxrsProvider build(IProgressMonitor progressMonitor) throws InvalidModelElementException, CoreException {
			JaxrsProvider provider = new JaxrsProvider(this);
			// provider.merge(javaType, progressMonitor);
			return provider;
		}
	}

	/** Full constructor using the inner 'MediaTypeCapabilitiesBuilder' static
	 * class.
	 * 
	 * @param builder */
	private JaxrsProvider(Builder builder) {
		super(builder.javaType, (Annotation) null, builder.metamodel);
	}

	@Override
	public EnumElementKind getElementKind() {
		return EnumElementKind.PROVIDER;
	}

	/** @param javaType
	 * @return
	 * @throws InvalidModelElementException
	 * @throws CoreException
	 * @Override public final Set<EnumElementChange> merge(final IType javaType,
	 *           final IProgressMonitor progressMonitor) throws
	 *           InvalidModelElementException, CoreException { if
	 *           (!JdtUtils.isTopLevelType(javaType)) { throw new
	 *           InvalidModelElementException("Type is not a top-level type"); }
	 *           Set<EnumElementChange> changes = new
	 *           HashSet<EnumElementChange>();
	 * 
	 *           CompilationUnit compilationUnit =
	 *           getCompilationUnit(progressMonitor); Set<IProblem> problems =
	 *           JdtUtils.resolveErrors(javaType, compilationUnit); if (problems
	 *           != null && problems.size() > 0) { //
	 *           metamodel.reportErrors(javaType, problems); return changes; }
	 *           IAnnotationBinding annotationBinding =
	 *           JdtUtils.resolveAnnotationBinding(javaType, compilationUnit,
	 *           javax.ws.rs.ext.Provider.class); // annotation was removed, or
	 *           import was removed if (annotationBinding == null) { throw new
	 *           InvalidModelElementException
	 *           (
	 *           "SimpleAnnotation binding not found : missing 'import' statement ?"
	 *           ); } ITypeHierarchy providerTypeHierarchy =
	 *           JdtUtils.resolveTypeHierarchy(javaType, false,
	 *           progressMonitor); IType[] subtypes =
	 *           providerTypeHierarchy.getSubtypes(javaType); // assert that the
	 *           class is not abstract and has no // sub-type, or continue; if
	 *           (JdtUtils.isAbstractType(javaType) || (subtypes != null &&
	 *           subtypes.length > 0)) { throw new
	 *           InvalidModelElementException(
	 *           "Type is an abstract type or has subtypes"
	 *           ); } Map<EnumKind, IType> providerKinds =
	 *           getProviderKinds(javaType, compilationUnit,
	 *           providerTypeHierarchy, container.getProviderInterfaces(),
	 *           null); // removes previous kinds and capabilities for
	 *           (Iterator<EnumKind> iterator =
	 *           this.getProvidedKinds().keySet().iterator();
	 *           iterator.hasNext();) { EnumKind kind = iterator.next(); if
	 *           (providerKinds == null || !providerKinds.containsKey(kind)) {
	 *           iterator.remove(); } } // add new kind and capabilities based
	 *           on resolved types and // annotations if (providerKinds != null)
	 *           { for (Entry<EnumKind, IType> entry : providerKinds.entrySet())
	 *           { JaxrsMediaTypeCapabilities mediaTypes =
	 *           resolveMediaTypeCapabilities(getJavaElement(), compilationUnit,
	 *           entry.getKey()); addProviderKind(entry.getKey(),
	 *           entry.getValue(), mediaTypes); } } return changes; } */

	/** {@inheritDoc} */
	@Override
	public void validate(IProgressMonitor progressMonitor) {

	}

	@Override
	public EnumKind getKind() {
		// TODO Auto-generated method stub
		return null;
	}

}
