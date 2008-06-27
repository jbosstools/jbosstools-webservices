package org.jboss.tools.ws.core.command;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.jboss.tools.ws.core.facet.delegate.IJBossWSFacetDataModelProperties;
import org.jboss.tools.ws.core.messages.JBossWSCoreMessages;
import org.jboss.tools.ws.core.utils.StatusUtils;

public class UninstallJBossWSClassPathCommand {
	IProject project;

	public UninstallJBossWSClassPathCommand(IProject project, IDataModel model) {
		this.project = project;
	}

	public IStatus execute(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {
		return executeOverride(monitor);
	}

	public IStatus executeOverride(IProgressMonitor monitor) {
		IStatus status = Status.OK_STATUS;
		try {
			boolean isServerSupplied = Boolean
					.getBoolean(project
							.getPersistentProperty(IJBossWSFacetDataModelProperties.PERSISTENCE_PROPERTY_SERVER_SUPPLIED_RUNTIME));
			if (isServerSupplied) {
//				project
//						.getPersistentProperties()
//						.remove(
//								IJBossWSFacetDataModelProperties.PERSISTENCE_PROPERTY_SERVER_SUPPLIED_RUNTIME);
			} else {
//				project
//						.getPersistentProperties()
//						.remove(
//								IJBossWSFacetDataModelProperties.PERSISTENCE_PROPERTY_QNAME_RUNTIME_NAME);
//				project
//						.getPersistentProperties()
//						.remove(
//								IJBossWSFacetDataModelProperties.PERSISTENCE_PROPERTY_RNTIME_LOCATION);
				String runtimeName = project
						.getPersistentProperty(IJBossWSFacetDataModelProperties.PERSISTENCE_PROPERTY_QNAME_RUNTIME_NAME);

				status = removeClassPath(project, runtimeName);
			}

		} catch (CoreException e) {
			status = StatusUtils.errorStatus(
					JBossWSCoreMessages.Error_Remove_Facet_JBossWS, e);
		}
		return status;
	}

	public IStatus removeClassPath(IProject project, String segment) {
		IStatus status = Status.OK_STATUS;
		try {
			IJavaProject javaProject = JavaCore.create(project);
			IClasspathEntry[] oldClasspathEntries = javaProject
					.readRawClasspath();

			boolean isFolderInClassPathAlready = false;
			List<IClasspathEntry> classpathEntries = new ArrayList<IClasspathEntry>();
			for (int i = 0; i < oldClasspathEntries.length
					&& !isFolderInClassPathAlready; i++) {
				if (!oldClasspathEntries[i].getPath().equals(
						new Path(JBossWSCoreMessages.JBossWS_Runtime_Lib)
								.append(segment))) {
					classpathEntries.add(oldClasspathEntries[i]);
				}
			}
			if (classpathEntries.size() < oldClasspathEntries.length) {
				javaProject.setRawClasspath(classpathEntries
						.toArray(new IClasspathEntry[classpathEntries.size()]),
						new NullProgressMonitor());
			}
		} catch (JavaModelException e) {
			status = StatusUtils.errorStatus(NLS.bind(
					JBossWSCoreMessages.Error_Remove_Facet_JBossWS,
					new String[] { e.getLocalizedMessage() }), e);
			return status;
		}

		return status;
	}

}
