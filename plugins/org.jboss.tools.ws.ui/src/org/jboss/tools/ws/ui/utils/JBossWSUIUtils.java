/******************************************************************************* 
 * Copyright (c) 2008 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.ws.ui.utils;

import java.io.File;
import java.net.URI;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.jboss.tools.ws.core.utils.StatusUtils;
import org.jboss.tools.ws.ui.JBossWSUIPlugin;
import org.jboss.tools.ws.ui.messages.JBossWSUIMessages;

/**
 * @author Grid Qian
 */
public class JBossWSUIUtils {

	public static String addAnotherNodeToPath(String currentPath, String newNode) {
		return currentPath + File.separator + newNode;
	}

	public static String addNodesToPath(String currentPath, String[] newNode) {
		String returnPath = currentPath;
		for (int i = 0; i < newNode.length; i++) {
			returnPath = returnPath + File.separator + newNode[i];
		}
		return returnPath;
	}

	public static IStatus validatePackageName(String name, IJavaElement context) {
		IStatus status = null;
		if (context == null || !context.exists()) {
			status = JavaConventions.validatePackageName(name,JavaCore.VERSION_1_3, JavaCore.VERSION_1_3);
			if (status != null && !status.isOK()) {
				return status;
			}
		}
		String[] sourceComplianceLevels = getSourceComplianceLevels(context);
		status = JavaConventions.validatePackageName(name,sourceComplianceLevels[0], sourceComplianceLevels[1]);
		if (status != null && status.getSeverity() == IStatus.ERROR) {
			return status;
		}

		IPackageFragmentRoot[] roots = null;
		try {
			roots = context.getJavaProject().getPackageFragmentRoots();
		} catch (JavaModelException e) {
			JBossWSUIPlugin.log(e);
		}
		for (IPackageFragmentRoot root : roots) {
			if (root != null) {
				IPackageFragment pack = root.getPackageFragment(name);
				try {
					IPath rootPath = root.getPath();
					IPath outputPath = root.getJavaProject()
							.getOutputLocation();
					if (rootPath.isPrefixOf(outputPath)
							&& !rootPath.equals(outputPath)) {
						// if the bin folder is inside of our root, don't allow
						// to name a package
						// like the bin folder
						IPath packagePath = pack.getPath();
						if (outputPath.isPrefixOf(packagePath)) {
							status = StatusUtils
									.warningStatus(JBossWSUIMessages.Error_JBossWS_GenerateWizard_IsOutputFolder);
							return status;
						}
					}
					if (pack.exists()) {
						if (pack.containsJavaResources()
								|| !pack.hasSubpackages()) {
							status = StatusUtils
									.warningStatus(JBossWSUIMessages.Error_JBossWS_GenerateWizard_PackageExists);
						} else {
							status = StatusUtils
									.warningStatus(JBossWSUIMessages.Error_JBossWS_GenerateWizard_PackageNotShown);
						}
						return status;
					} else {
						if(pack.getResource() == null){
							continue;
						}
						URI location = pack.getResource().getLocationURI();
						if (location != null) {
							IFileStore store = EFS.getStore(location);
							if (store.fetchInfo().exists()) {
								status = StatusUtils
										.warningStatus(JBossWSUIMessages.Error_JBossWS_GenerateWizard_PackageExistsDifferentCase);
								return status;
							}
						}
					}
				} catch (CoreException e) {
					JBossWSUIPlugin.log(e);
				}
			}
		}
		return status;
	}

	public static String[] getSourceComplianceLevels(IJavaElement context) {
		if (context != null) {
			IJavaProject javaProject = context.getJavaProject();
			if (javaProject != null) {
				return new String[] {
						javaProject.getOption(JavaCore.COMPILER_SOURCE, true),
						javaProject.getOption(JavaCore.COMPILER_COMPLIANCE,
								true) };
			}
		}
		return new String[] { JavaCore.getOption(JavaCore.COMPILER_SOURCE),
				JavaCore.getOption(JavaCore.COMPILER_COMPLIANCE) };
	}

}