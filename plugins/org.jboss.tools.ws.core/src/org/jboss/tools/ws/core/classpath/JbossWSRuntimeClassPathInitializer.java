package org.jboss.tools.ws.core.classpath;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.jboss.tools.ws.core.utils.JbossWSCoreUtils;

public class JbossWSRuntimeClassPathInitializer extends
		ClasspathContainerInitializer {

	public static final String ID = "JbossWSRuntimeLib";

	public JbossWSRuntimeClassPathInitializer() {
	}

	@Override
	public void initialize(IPath containerPath, IJavaProject project)
			throws CoreException {

		if (containerPath.segment(0).equals(ID)) {
			JbossWSRuntimeClasspathContainer container = new JbossWSRuntimeClasspathContainer(
					containerPath);
			System.out.println(project.getElementName()+ " before "+project.getRawClasspath().length);
			for(int i=0;i<project.getRawClasspath().length;i++){
				System.out.println(i+" classpath entry = " + project.getRawClasspath()[i]);
			}
			JavaCore.setClasspathContainer(containerPath,
					new IJavaProject[] {project},
					new IClasspathContainer[] {container}, null);
			System.out.println(project.getElementName()+" after "+project.getRawClasspath().length);
			for(int i=0;i<project.getRawClasspath().length;i++){
				System.out.println(i+" classpath entry = " + project.getRawClasspath()[i]);
			}
		}
	}

	public IClasspathEntry[] getEntries(IPath path) {
		return new JbossWSRuntimeClasspathContainer(path).getClasspathEntries();
	}

	public class JbossWSRuntimeClasspathContainer implements
			IClasspathContainer {
		private IPath path;
		private IClasspathEntry[] entries = null;

		public JbossWSRuntimeClasspathContainer(IPath path) {
			this.path = path;
		}

		public String getDescription() {
				return "JBoss WS Runtime";
		}

		public int getKind() {
			return IClasspathContainer.K_APPLICATION;
		}

		public IPath getPath() {
			return path;
		}

		public IClasspathEntry[] getClasspathEntries() {
			if (entries == null) {
				loadClasspathEntries();
				if (entries == null)
					return new IClasspathEntry[0];
			}
			return entries;
		}

		private void loadClasspathEntries() {
			ArrayList<IClasspathEntry> list = new ArrayList<IClasspathEntry>();
			IPath libpath = JbossWSCoreUtils.getJbossLibPath();
			list.addAll(Arrays.asList(getEntries(libpath)));
			libpath = JbossWSCoreUtils.getJbossClientPath();
			
			list.addAll(Arrays.asList(getEntries(libpath)));

			entries = list.toArray(new IClasspathEntry[list.size()]);
		}

		protected IClasspathEntry getEntry(IPath path) {
			return JavaRuntime.newArchiveRuntimeClasspathEntry(path)
					.getClasspathEntry();
		}

		protected IClasspathEntry[] getEntries(IPath folder) {
			String[] files = folder.toFile().list();
			ArrayList<IClasspathEntry> list = new ArrayList<IClasspathEntry>();
			for (int i = 0; i < files.length; i++) {
				if (files[i].endsWith(".jar")) {
					list.add(getEntry(folder.append(files[i])));
				}//else if(new File(files[i] instanceof Folder)){}
			}
			return list.toArray(new IClasspathEntry[list.size()]);
		}

	}

}
