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
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;

public class ResourceChangedEventFilter {

	public boolean applyRules(ResourceChangedEvent event) throws JavaModelException {
		final IResource resource = event.getResource();
		// prevent processing resources in a closed project
		if (!resource.getProject().isOpen()) {
			return false;
		}

		final IJavaProject javaProject = JavaCore.create(resource.getProject());
		// check if the resource if a .java file in an existing Package Fragment
		// Root
		if ("java".equals(resource.getFileExtension())) {
			final IPath resourcePath = resource.getFullPath();
			for (IPackageFragmentRoot fragment : javaProject.getPackageFragmentRoots()) {
				if (fragment.getPath().isPrefixOf(resourcePath)) {
					Logger.debug("**accepted** {}", event);
					return true;
				}
			}
		}
		Logger.debug("**rejected** {}", event);
		return false;
	}

}
