/******************************************************************************* 
 * Copyright (c) 2008 - 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.ws.jaxws.core.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jst.j2ee.internal.project.J2EEProjectUtilities;
import org.eclipse.jst.ws.internal.common.J2EEUtils;
import org.eclipse.jst.ws.internal.common.ResourceUtils;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.componentcore.resources.IVirtualResource;

@SuppressWarnings("restriction")
public class ClasspathParser {

	private static ClasspathParser instance;

	private ClasspathParser() {
	}

	public static ClasspathParser getInstance() {
		if (instance == null)
			instance = new ClasspathParser();
		return instance;
	}

	public String getClasspathString(IProject project) {
		StringBuffer classpath = new StringBuffer();
		String[] classpathEntries = getClasspath(project, false);

		Vector<String> classpathVector = new Vector<String>();
		for (int i = 0; i < classpathEntries.length; i++) {
			if (!classpathVector.contains(classpathEntries[i])) {
				classpathVector.add(classpathEntries[i]);
				classpath.append(classpathEntries[i]);
				classpath.append(";"); //$NON-NLS-1$
			}
		}
		return classpath.toString();
	}

	private String[] getClasspath(IProject project, boolean isDependent) {
		String[] cClasspath = new String[0];
		ArrayList<String> projectClasspath = new ArrayList<String>();
		boolean needJavaClasspath = false;

		IVirtualComponent comp = ComponentCore.createComponent(project);
		if (comp != null) {

			// get a ear component classpath
			if (J2EEUtils.isEARComponent(comp)) {
				cClasspath = getClasspathOfEARProject(project, comp
						.getName());

				// add component classpath to the project classpath
				for (int j = 0; j < cClasspath.length; j++) {
					projectClasspath.add(cClasspath[j]);
				}
			} else if (J2EEUtils.isWebComponent(comp)
					|| J2EEUtils.isJavaComponent(comp)) {
				needJavaClasspath = true;

				IContainer outputContainer = null;
				IResource fragmentRoot = null;
				IPackageFragmentRoot[] pkgFragmentRoot = ResourceUtils
						.getJavaPackageFragmentRoots(project);
				ArrayList<String> webComponentClasspath = new ArrayList<String>();
				try {
					for (int i = 0; i < pkgFragmentRoot.length; i++) {
						fragmentRoot = pkgFragmentRoot[i]
								.getCorrespondingResource();
						if (fragmentRoot != null
								&& (fragmentRoot.getProject().equals(project))
								&& (fragmentRoot.getType() != IResource.FILE)) {
							outputContainer = J2EEProjectUtilities
									.getOutputContainer(project,
											pkgFragmentRoot[i]);
							if (outputContainer != null) {
								webComponentClasspath.add(outputContainer
										.getLocation().toOSString());
							}
						}
					}
				} catch (JavaModelException e) {
				}

				// add Web component classpath to the project classpath
				Iterator<String> iter = webComponentClasspath.iterator();
				while (iter.hasNext()) {
					projectClasspath.add((String) iter.next());
				}
			}

			if (!isDependent) {
				if (J2EEUtils.isWebComponent(comp)) {
					needJavaClasspath = true;
					cClasspath = getWebInfLib(project);
					for (int j = 0; j < cClasspath.length; j++) {
						projectClasspath.add(cClasspath[j]);
					}
				}
			}

		} else {
			needJavaClasspath = true;
		}

		// a Web or Java component in the project, get the project's Java classpath
		if (needJavaClasspath) {
			String[] javaClasspath;
			try {
				IJavaProject javaProj = (IJavaProject) project
						.getNature(JavaCore.NATURE_ID);
				if (javaProj != null) {
					javaClasspath = getClasspathForJavaProject(javaProj);
					for (int j = 0; j < javaClasspath.length; j++) {
						projectClasspath.add(javaClasspath[j]);
					}
				}
			} catch (CoreException e) {
				// can't get Java classpath, ignore
			}
		}

		return (String[]) projectClasspath.toArray(new String[projectClasspath
				.size()]);
	}

	// Need to get all components in the project. If there's a EAR component, get the
	// utility Jars
	private String[] getUtilityJarClasspath(IProject project) {
		String[] utilityJarString = new String[0];
		String[] cClasspath = new String[0];
		ArrayList<String> utilityJarsClasspath = new ArrayList<String>();

		String component;
		IVirtualComponent comp = ComponentCore.createComponent(project);
		if (comp != null) {
			component = comp.getName();
			if (J2EEUtils.isEARComponent(comp)) {
				cClasspath = getClasspathOfEARProject(project, component);
				for (int j = 0; j < cClasspath.length; j++) {
					utilityJarsClasspath.add(cClasspath[j]);
				}
				utilityJarString = (String[]) utilityJarsClasspath
						.toArray(new String[utilityJarsClasspath.size()]);
			}
		}

		return utilityJarString;
	}

	private String[] getClasspathOfEARProject(IProject project, String component) {
		IPath projectPath = project.getProject().getLocation()
				.addTrailingSeparator().append(component).addTrailingSeparator();
		return getDirectoryJarFiles(projectPath);
	}

	private String[] getDirectoryJarFiles(IPath iPath) {
		File dir = iPath.toFile();
		Vector<Object> jarsVector = new Vector<Object>();
		if (dir.exists() && dir.isDirectory()) {
			String[] filenames = dir.list();
			for (int i = 0; i < filenames.length; i++) {
				if (filenames[i].endsWith(".jar")) //$NON-NLS-1$
					jarsVector.add(pathToString(iPath) + filenames[i]);
			}
		}
		String[] jars = new String[jarsVector.size()];
		jarsVector.copyInto(jars);
		return jars;
	}

	private String[] getWebInfLib(IProject project) {
		String[] webinfLibJars = new String[0];
		ArrayList<String> anArrayList = new ArrayList<String>();
		try {
			IVirtualComponent component = ComponentCore
					.createComponent(project);
			if (component != null) {

				IVirtualFolder webInfLib = component.getRootFolder().getFolder(
						new Path("/WEB-INF/lib")); //$NON-NLS-1$
				if (webInfLib != null) {
					IVirtualResource[] resources = webInfLib.members();
					IResource aResource = null;
					for (int i = 0; i < resources.length; i++) {
						aResource = resources[i].getUnderlyingResource();
						if ("jar".equalsIgnoreCase(aResource.getFileExtension())) //$NON-NLS-1$
							anArrayList.add(aResource.getLocation()
									.toOSString());
					}
					if (anArrayList.size() != 0)
						webinfLibJars = (String[]) anArrayList
								.toArray(new String[anArrayList.size()]);
				}
			}
		} catch (CoreException e) {
		}
		return webinfLibJars;
	}

	private String[] getClasspathForJavaProject(IJavaProject javaProject) {
		ArrayList<String> projectClasspath = new ArrayList<String>();
		try {
			IClasspathEntry[] buildPath = javaProject
					.getResolvedClasspath(true);
			for (int i = 0; i < buildPath.length; i++) {
				String[] buildPathString = classpathEntryToString(buildPath[i],
						javaProject.getProject());
				for (int j = 0; j < buildPathString.length; j++) {
					projectClasspath.add(buildPathString[j]);
				}
			}
		} catch (JavaModelException jme) {
		}

		String[] utilityJarsClasspath;
		IProject project = javaProject.getProject();
		IProject[] referencingProjects = project.getReferencingProjects();
		for (int i = 0; i < referencingProjects.length; i++) {
			utilityJarsClasspath = getUtilityJarClasspath(referencingProjects[i]);
			for (int j = 0; j < utilityJarsClasspath.length; j++) {
				projectClasspath.add(utilityJarsClasspath[j]);
			}
		}

		return (String[]) projectClasspath.toArray(new String[projectClasspath
				.size()]);
	}

	private String[] classpathEntryToString(IClasspathEntry entry,
			IProject project) {
		switch (entry.getEntryKind()) {
		case IClasspathEntry.CPE_LIBRARY: {
			return new String[] { pathToString(entry.getPath()) };
		}
		case IClasspathEntry.CPE_PROJECT: {
			return getClasspath(ResourcesPlugin.getWorkspace().getRoot()
					.getProject(entry.getPath().lastSegment()), true);
		}
		case IClasspathEntry.CPE_SOURCE: {
			IPath path = entry.getPath();
			if (path.segment(0).equals(project.getName()))
				path = path.removeFirstSegments(1);
			return new String[] { pathToString(project.getLocation()
					.addTrailingSeparator().append(path)) };
		}
		case IClasspathEntry.CPE_VARIABLE: {
			return classpathEntryToString(JavaCore
					.getResolvedClasspathEntry(entry), project);
		}
		default: {
			return new String[] { pathToString(entry.getPath()) };
		}
		}
	}

	private String pathToString(IPath path) {
		return path.toOSString();
	}
}
