/**
 * Copyright (c) 2011 - 2014 Red Hat, Inc. and others.  
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.tools.ws.creation.core.utils;

import java.io.File;
import java.io.FilenameFilter;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.jboss.tools.common.util.EclipseJavaUtil;
import org.jboss.tools.ws.core.utils.StatusUtils;
import org.jboss.tools.ws.creation.core.messages.JBossWSCreationCoreMessages;

/**
 * @author bfitzpat
 *
 */
public class RestEasyLibUtils {

	private static final String REST_EASY = "RestEasy"; //$NON-NLS-1$

	/**
	 * Simple check to see if the JBoss WS runtime associated with a project
	 * actually includes the RESTEasy jars. If so, returns Status.OK_STATUS.
	 * If not, returns null.
	 * 
	 * @param project
	 * @return
	 */
	public static IStatus doesRuntimeSupportRestEasy ( IProject project ) {
		
		try {
			IJavaProject javaProject = JavaCore.create(project);
			if (javaProject != null) {
				String[] classesToValidate = 
					{ "javax.ws.rs.GET",  //$NON-NLS-1$
					  "javax.ws.rs.Path",  //$NON-NLS-1$
					  "javax.ws.rs.core.Application"}; //$NON-NLS-1$
				for (int i = 0; i < classesToValidate.length; i++) {
					String classToTest = classesToValidate[i];
					IType rtnType1 = EclipseJavaUtil.findType(
							javaProject, classToTest);
					if (rtnType1 == null) {
						return StatusUtils.warningStatus(
								JBossWSCreationCoreMessages.AddRestEasyJarsCommand_RestEasy_JARS_Not_Found);
					}
				}
			}
		} catch (Exception e) {
			return StatusUtils.warningStatus(JBossWSCreationCoreMessages.RestEasyLibUtils_Error_UnableToFindRuntimeForProject);
		}
		return Status.OK_STATUS;
	}
	
	public static IStatus doesRuntimeHaveRootLevelRestEasyDir ( IProject project ) {
		try {
			IJavaProject javaProject = JavaCore.create(project);
			if (javaProject != null) {
				String path =
						JBossWSCreationUtils.getJBossWSRuntimeLocation(project.getProject());
				File runtime = new File(path);
				if (runtime.exists()) {
					File parent = runtime.getParentFile();
					if (parent.exists() && parent.isDirectory()) {
						File[] restEasyDir = parent.listFiles(new FilenameFilter() {
							public boolean accept(File dir, String name) {
								if (name.equalsIgnoreCase(REST_EASY)) {
									return true;
								}
								return false;
							}
						});
						if (restEasyDir != null && restEasyDir.length > 0) {
							return Status.OK_STATUS;
						} else {
							return StatusUtils.warningStatus("Root-level RESTeasy directory not found in runtime location");
						}
					}
				}
			}
		} catch (Exception e) {
			return StatusUtils.warningStatus("Root-level RESTeasy directory not found in runtime location");
		}
		return Status.OK_STATUS;
	}

}
