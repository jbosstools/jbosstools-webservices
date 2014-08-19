/******************************************************************************* 
 * Copyright (c) 2008 - 2014 Red Hat, Inc. and others. 
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
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;

/**
 * Utility class to retrieve the {@link JaxrsMetamodel} of a given {@link IProject} or {@link IJavaProject}
 * 
 * @author xcoulon
 *
 */
public class JaxrsMetamodelLocator {

	/** Singleton constructor */
	private JaxrsMetamodelLocator() {

	}

	/**
	 * Accessor to the metamodel from the given project's session properties.
	 * 
	 * @param javaProject
	 *            the java project
	 * @return the metamodel or null if none was found or if the given javaProject was null or closed
	 * @throws CoreException
	 *             in case of underlying exception
	 */
	public static JaxrsMetamodel get(final IJavaProject javaProject) throws CoreException {
		return get(javaProject, false);
	}

	/**
	 * Accessor to the metamodel from the given project's session properties.
	 * 
	 * @param project
	 *            the project
	 * @return the metamodel or null if none was found or if the given project was null or closed
	 * @throws CoreException
	 *             in case of underlying exception
	 */
	public static JaxrsMetamodel get(final IProject project) throws CoreException {
		return get(project, false);
	}
	
	/**
	 * Accessor to the metamodel from the given project's session properties.
	 * 
	 * @param project
	 *            the project
	 * @param force
	 *            create the JAX-RS Metamodel if none already exists for the
	 *            given {@link IProject}, even if the given it is not yet open.
	 * @return the metamodel or null if none was found or if the given project
	 *         was null or closed
	 * @throws CoreException
	 *             in case of underlying exception
	 */
	public static JaxrsMetamodel get(IProject project, boolean force) throws CoreException {
		if (project == null) {
			return null;
		}
		if (!project.isOpen()) {
			Logger.debug("*** Returning a null Metamodel because the given project '{}' is closed (or not opened yet) ***", project.getName());
			return null;
		}
		
		return get(JavaCore.create(project), force);
	}

	
	/**
	 * Accessor to the {@link JaxrsMetamodel} from the given project's session
	 * properties.
	 * 
	 * @param javaProject
	 *            the java project
	 * @param force
	 *            create the JAX-RS Metamodel if none already exists for the
	 *            given {@link IJavaProject}, even if it is not yet open.
	 * @return the metamodel or null if none was found or the given javaProject
	 *         was null or closed and the {@code force} arg was set to
	 *         {@code false}
	 * @throws CoreException
	 *             in case of underlying exception
	 */
	public static JaxrsMetamodel get(final IJavaProject javaProject, final boolean force)
			throws CoreException {
		if (javaProject == null) {
			return null;
		}
		if (!force && !javaProject.isOpen()) {
			Logger.debug("*** Returning a null Metamodel because the javaProject '{}' is closed (or not opened yet) ***", javaProject.getElementName());
			return null;
		}

		final IProject project = javaProject.getProject();
		final JaxrsMetamodel metamodel = (JaxrsMetamodel) project.getSessionProperty(
				JaxrsMetamodel.METAMODEL_QUALIFIED_NAME);
		if (metamodel == null && force) {
			return JaxrsMetamodel.create(javaProject);
		}
		return metamodel;	
	}

}
