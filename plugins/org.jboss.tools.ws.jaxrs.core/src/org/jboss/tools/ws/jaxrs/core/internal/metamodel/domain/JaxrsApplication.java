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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.wst.validation.ValidatorMessage;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsApplication;
import org.jboss.tools.ws.jaxrs.core.metamodel.InvalidModelElementException;

/** The optional '@Application' annotation, used to designate the base context
 * URI of the root resources.
 * 
 * 
 * @author xcoulon */
public class JaxrsApplication extends JaxrsElement<IType> implements IJaxrsApplication {

	/** Internal 'HttpMethod' element builder.
	 * 
	 * @author xcoulon */
	public static class Builder {

		private final JaxrsMetamodel metamodel;
		private final IType javaType;

		/** Mandatory attributes of the enclosing 'HttpMethod' element.
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
		public JaxrsApplication build(IProgressMonitor progressMonitor) throws CoreException {
			JaxrsApplication app = new JaxrsApplication(this);
			// app.merge(javaType, progressMonitor);
			return app;
		}
	}

	/** Full constructor using the inner 'Builder' static
	 * class.
	 * 
	 * @param builder */
	private JaxrsApplication(Builder builder) {
		super(builder.javaType, (Annotation) null, builder.metamodel);
	}

	@Override
	public EnumElementKind getElementKind() {
		return EnumElementKind.APPLICATION;
	}

	/*
	 * @Override public Set<EnumElementChange> merge(IType element,
	 * IProgressMonitor progressMonitor) throws CoreException {
	 * Set<EnumElementChange> changes = new HashSet<EnumElementChange>(); if
	 * (getJavaElement() != null) { CompilationUnit compilationUnit =
	 * getCompilationUnit(progressMonitor); String appPath = (String)
	 * JdtUtils.resolveAnnotationAttributeValue(getJavaElement(),
	 * compilationUnit, javax.ws.rs.ApplicationPath.class, "value"); if (appPath
	 * != null) { getMetamodel().setServiceUri(appPath); } else {
	 * getMetamodel().setServiceUri("/"); } } else {
	 * getMetamodel().setServiceUri("/"); } return changes; }
	 */

	@Override
	public EnumKind getKind() {
		return EnumKind.APPLICATION;
	}

	@Override
	public List<ValidatorMessage> validate() {
		List<ValidatorMessage> messages = new ArrayList<ValidatorMessage>();
		return messages;
	}

}
