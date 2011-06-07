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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.utils.JdtUtils;

/**
 * The optional '@Application' annotation, used to designate the base context
 * URI of the root resources.
 * 
 * 
 * @author xcoulon
 * 
 */
public class Application extends BaseElement<IType> {

	/**
	 * Internal 'HTTPMethod' element builder.
	 * 
	 * @author xcoulon
	 * 
	 */
	public static class Builder {

		private final Metamodel metamodel;
		private final IType javaType;

		/**
		 * Mandatory attributes of the enclosing 'HTTPMethod' element.
		 * 
		 * @param javaType
		 * @param metamodel
		 */
		public Builder(final IType javaType, final Metamodel metamodel) {
			this.javaType = javaType;
			this.metamodel = metamodel;
		}

		/**
		 * Builds and returns the elements. Internally calls the merge() method.
		 * 
		 * @param progressMonitor
		 * @return
		 * @throws InvalidModelElementException
		 * @throws CoreException
		 */
		public Application build(IProgressMonitor progressMonitor) throws CoreException {
			Application app = new Application(this);
			app.merge(javaType, progressMonitor);
			return app;
		}
	}

	/**
	 * Full constructor using the inner 'MediaTypeCapabilitiesBuilder' static
	 * class.
	 * 
	 * @param builder
	 */
	private Application(Builder builder) {
		super(builder.javaType, builder.metamodel);
	}

	@Override
	public Set<EnumElementChange> merge(IType element, IProgressMonitor progressMonitor) throws CoreException {
		Set<EnumElementChange> changes = new HashSet<EnumElementChange>();
		if (getJavaElement() != null) {
			CompilationUnit compilationUnit = getCompilationUnit(progressMonitor);
			String appPath = (String) JdtUtils.resolveAnnotationAttributeValue(getJavaElement(), compilationUnit,
					javax.ws.rs.ApplicationPath.class, "value");
			if (appPath != null) {
				getMetamodel().setServiceUri(appPath);
			} else {
				getMetamodel().setServiceUri("/");
			}
		} else {
			getMetamodel().setServiceUri("/");
		}
		return changes;
	}

	@Override
	public void validate(IProgressMonitor progressMonitor) throws CoreException {
		// TODO Auto-generated method stub

	}

	@Override
	public org.jboss.tools.ws.jaxrs.core.metamodel.BaseElement.EnumKind getKind() {
		// TODO Auto-generated method stub
		return null;
	}

}
