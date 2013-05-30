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
package org.jboss.tools.ws.jaxrs.core.metamodel.domain;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;

public class JaxrsMetamodelLocator {

	/** Singleton constructor */
	private JaxrsMetamodelLocator() {

	}

	/**
	 * Accessor to the metamodel from the given project's session properties.
	 * 
	 * @param javaProject
	 *            the java project
	 * @return the metamodel or null if none was found
	 * @throws CoreException
	 *             in case of underlying exception
	 */
	public static JaxrsMetamodel get(final IJavaProject javaProject) throws CoreException {
		return get(javaProject, false);
	}

	/**
	 * Accessor to the metamodel from the given project's session properties.
	 * 
	 * @param javaProject
	 *            the java project
	 * @param createIfMissing
	 *            create the JAX-RS Metamodel if none already exists for the
	 *            given JavaProject
	 * @return the metamodel or null if none was found
	 * @throws CoreException
	 *             in case of underlying exception
	 */
	public static JaxrsMetamodel get(final IJavaProject javaProject, final boolean createIfMissing)
			throws CoreException {
		if (javaProject == null || javaProject.getProject() == null) {
			return null;
		}
		return get(javaProject.getProject(), createIfMissing);
	}

	/**
	 * Accessor to the metamodel from the given project's session properties.
	 * 
	 * @param project
	 *            the project
	 * @return the metamodel or null if none was found
	 * @throws CoreException
	 *             in case of underlying exception
	 */
	public static JaxrsMetamodel get(final IProject project) throws CoreException {
		if (project == null || !project.isOpen()) {
			return null;
		}
		return get(project, false);
	}

	/**
	 * Accessor to the metamodel from the given project's session properties.
	 * 
	 * @param project
	 *            the project
	 * @param createIfMissing
	 *            create the JAX-RS Metamodel if none already exists for the
	 *            given JavaProject
	 * @return the metamodel or null if none was found
	 * @throws CoreException
	 *             in case of underlying exception
	 */
	public static JaxrsMetamodel get(final IProject project, final boolean createIfMissing) throws CoreException {
		if (project == null || !project.isOpen()) {
			return null;
		}
		final JaxrsMetamodel metamodel = (JaxrsMetamodel) project.getSessionProperty(
				JaxrsMetamodel.METAMODEL_QUALIFIED_NAME);
		if (metamodel == null && createIfMissing) {
			return JaxrsMetamodel.create(JavaCore.create(project));
		}
		return metamodel;
	}

}
