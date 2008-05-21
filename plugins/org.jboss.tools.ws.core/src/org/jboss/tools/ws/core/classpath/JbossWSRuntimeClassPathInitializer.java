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
import org.jboss.tools.ws.core.messages.JbossWSCoreMessages;
import org.jboss.tools.ws.core.utils.JbossWSCoreUtils;

/**
 * @author Grid Qian
 */
public class JbossWSRuntimeClassPathInitializer extends
		ClasspathContainerInitializer {

	public JbossWSRuntimeClassPathInitializer() {
	}

	@Override
	public void initialize(IPath containerPath, IJavaProject project)
			throws CoreException {

		if (containerPath.segment(0).equals(
				JbossWSCoreMessages.JBOSSWS_RUNTIME_LIB)) {
			JbossWSRuntimeClasspathContainer container = new JbossWSRuntimeClasspathContainer(
					containerPath);
			JavaCore.setClasspathContainer(containerPath,
					new IJavaProject[] { project },
					new IClasspathContainer[] { container }, null);
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
			return JbossWSCoreMessages.JBOSSWS_RUNTIME;
		}

		public int getKind() {
			return IClasspathContainer.K_APPLICATION;
		}

		public IPath getPath() {
			return path;
		}

		public IClasspathEntry[] getClasspathEntries() {
			if (entries == null) {
				ArrayList<IClasspathEntry> list = new ArrayList<IClasspathEntry>();
				IPath wsPath = JbossWSCoreUtils.getJbossWSRuntimePath();
				if (wsPath != null) {
					IPath libPath = wsPath.append(JbossWSCoreMessages.DIR_LIB);
					list.addAll(Arrays.asList(getEntries(libPath)));
					libPath = wsPath.append(JbossWSCoreMessages.DIR_CLIENT);
					list.addAll(Arrays.asList(getEntries(libPath)));
					entries = list.toArray(new IClasspathEntry[list.size()]);
				}
				if (entries == null)
					return new IClasspathEntry[0];
			}
			return entries;
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
				} else if (folder.append(files[i]).toFile().isDirectory()) {
					list.addAll(Arrays.asList(getEntries(folder
							.append(files[i]))));
				}
			}
			return list.toArray(new IClasspathEntry[list.size()]);
		}

	}

}
