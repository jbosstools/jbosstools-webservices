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

package org.jboss.tools.ws.core.classpath;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.jboss.tools.common.core.classpath.*;
import org.jboss.tools.ws.core.messages.JBossWSCoreMessages;

/**
 * @author Grid Qian
 */
public class JBossWSRuntimeClassPathInitializer extends
AbstractClasspathContainerInitializer {
	public final static String JBOSS_WS_RUNTIME_CLASSPATH_CONTAINER_ID = "org.jboss.ws.runtime.classpath"; //$NON-NLS-1$
	public JBossWSRuntimeClassPathInitializer() {
	}

	private String segment;

	@Override
	public void initialize(IPath containerPath, IJavaProject project)
			throws CoreException {
		this.javaProject = project;
		if (containerPath.segment(0).equals(
				JBossWSCoreMessages.JBossWS_Runtime_Lib)) {
			JBossWSRuntimeClasspathContainer container = new JBossWSRuntimeClasspathContainer(
					containerPath, project);
			segment = containerPath.segment(1);
			JavaCore.setClasspathContainer(containerPath,
					new IJavaProject[] { project },
					new IClasspathContainer[] { container }, null);
		}
	}

	public IClasspathEntry[] getEntries(IPath path) {
		return new JBossWSRuntimeClasspathContainer(path, javaProject).getClasspathEntries();
	}

	public class JBossWSRuntimeClasspathContainer extends
	AbstractClasspathContainer {
		private IPath path;
		private IClasspathEntry[] entries = null;

		public JBossWSRuntimeClasspathContainer(IPath path, IJavaProject javaProject) {
			super(path,JBossWSCoreMessages.JBossWS_Runtime_Lib,null, javaProject);
			this.path = path;
		}

		public String getDescription() {
			return JBossWSCoreMessages.JBossWS_Runtime;
		}

		public int getKind() {
			return IClasspathContainer.K_APPLICATION;
		}

		public IPath getPath() {
			return path;
		}
		
		public IClasspathEntry[] computeEntries() {
			ArrayList<IClasspathEntry> entryList = new ArrayList<IClasspathEntry>();
			JBossWSRuntime jbws = JBossWSRuntimeManager.getInstance()
					.findRuntimeByName(segment);
			
			List<String> jars = null;
			if (jbws != null) {
				jars = JBossWSRuntimeManager.getInstance().getAllRuntimeJars(jbws);
				for (String jar : jars) {
					entryList.add(getEntry(new Path(jar)));
				}
			}	
				if(jars == null){
					return new IClasspathEntry[0];
				}
				
				for (String jar : jars) {

					IPath entryPath = new Path(jar);

					IPath sourceAttachementPath = null;
					IPath sourceAttachementRootPath = null;

					final ClasspathDecorations dec = decorations.getDecorations(
							getDecorationManagerKey(getPath().toString()),
							entryPath.toString());

					IClasspathAttribute[] attrs = {};
					if (dec != null) {
						sourceAttachementPath = dec.getSourceAttachmentPath();
						sourceAttachementRootPath = dec
								.getSourceAttachmentRootPath();
						attrs = dec.getExtraAttributes();
					}

					IAccessRule[] access = {};
					IClasspathEntry entry = JavaCore.newLibraryEntry(entryPath,
							sourceAttachementPath, sourceAttachementRootPath,
							access, attrs, false);
					entryList.add(entry);
				}			
				entries = entryList.toArray(new IClasspathEntry[entryList.size()]);
			return entries;
		}

		public IClasspathEntry[] getClasspathEntries() {
			if (entries == null) {
				ArrayList<IClasspathEntry> entryList = new ArrayList<IClasspathEntry>();
				JBossWSRuntime jbws = JBossWSRuntimeManager.getInstance()
						.findRuntimeByName(segment);
				if (jbws != null) {

					List<String> jars = JBossWSRuntimeManager.getInstance()
							.getAllRuntimeJars(jbws);
					for (String jar : jars) {
						entryList.add(getEntry(new Path(jar)));
					}
					entries = entryList.toArray(new IClasspathEntry[entryList
							.size()]);
					if(entryList.size() == 0){
						return new IClasspathEntry[0];
					}
					
					for (String jar : jars) {

						IPath entryPath = new Path(jar);

						IPath sourceAttachementPath = null;
						IPath sourceAttachementRootPath = null;

						final ClasspathDecorations dec = decorations.getDecorations(
								getDecorationManagerKey(getPath().toString()),
								entryPath.toString());

						IClasspathAttribute[] attrs = {};
						if (dec != null) {
							sourceAttachementPath = dec.getSourceAttachmentPath();
							sourceAttachementRootPath = dec
									.getSourceAttachmentRootPath();
							attrs = dec.getExtraAttributes();
						}

						IAccessRule[] access = {};
						IClasspathEntry entry = JavaCore.newLibraryEntry(entryPath,
								sourceAttachementPath, sourceAttachementRootPath,
								access, attrs, false);
						entryList.add(entry);
					}
				}
			}
			return entries;
		}

		protected IClasspathEntry getEntry(IPath path) {
			return JavaRuntime.newArchiveRuntimeClasspathEntry(path)
					.getClasspathEntry();
		}

		public void removeEntry(String jarName) {
			if (entries == null) {
				return;
			}

			List<IClasspathEntry> entriesList = new ArrayList<IClasspathEntry>();
			for (IClasspathEntry entry : entries) {
				if (entry != null) {
					IPath path = entry.getPath();
					if (path != null) {
						if (path != null && path.lastSegment() != null
								&& path.lastSegment().equals(jarName)) {
							continue;
						}
					}
					entriesList.add(entry);
				}
			}
			entries = entriesList.toArray(new IClasspathEntry[0]);
		}

		@Override
		public void refresh() {
			new JBossWSRuntimeClasspathContainer(path,javaProject).install();
		}
	}

	public boolean filterJars(String jarName, ArrayList<IClasspathEntry> list) {
		for (IClasspathEntry entry : list) {
			if (entry.getPath().lastSegment().equals(jarName)) {
				return false;
			}
		}
		return true;
	}

	@Override
	protected AbstractClasspathContainer createClasspathContainer(IPath path) {
		return new JBossWSRuntimeClasspathContainer(path, javaProject);
	}

	@Override
	protected String getClasspathContainerID() {
		return JBOSS_WS_RUNTIME_CLASSPATH_CONTAINER_ID;
	}

}
