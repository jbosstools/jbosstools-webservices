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

import java.lang.reflect.Field;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.QualifiedName;
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCorePlugin;
import org.jboss.tools.ws.jaxrs.core.configuration.ProjectNatureUtils;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;

/** The JAX-RS Metamodel builder. Invoked when a full build or an incremental
 * build is triggered on a project on which the JAX-RS nature is installed.
 * 
 * This builder is responsible of the creation and update of the JAX-RS
 * Metamodel which is kept in the project's session properties.
 * 
 * @author xcoulon */
public class JaxrsMetamodelBuilder extends IncrementalProjectBuilder {

	/** The number of steps to fully build the JAX-RS Metamodel. */
	private static final int FULL_BUILD_STEPS = 100;

	/** The standard 'Java type' marker type. */
	public static final String JAVA_PROBLEM = "org.eclipse.jdt.core.problem";

	/** The custom 'JAX-RS Problem' marker type. */
	public static final String JAXRS_PROBLEM = "org.jboss.tools.ws.jaxrs.metamodelMarker";

	/** The Java element change listener name. */
	public static final QualifiedName JAXRS_ELEMENT_CHANGE_LISTENER_NAME = new QualifiedName(
			JBossJaxrsCorePlugin.PLUGIN_ID, "jaxrsPostReconcileListener");

	// TODO : add support for cancellation
	// TODO : report build failed
	/** {@inheritDoc} */
	@Override
	protected final IProject[] build(final int kind, @SuppressWarnings("rawtypes") final Map args,
			final IProgressMonitor monitor) throws CoreException {
		IProject project = getProject();
		if (!ProjectNatureUtils.isProjectNatureInstalled(project, ProjectNatureUtils.JAXRS_NATURE_ID)) {
			Logger.warn("Project '" + project.getName() + "' is not a JAX-RS project.");
			return null;
		}
		logBuild(kind, args, project);
		switch (kind) {
		case CLEAN_BUILD:
		case FULL_BUILD:
			build(project, true, monitor);
			break;
		case AUTO_BUILD:
		case INCREMENTAL_BUILD:
			//if (JaxrsMetamodel.get(getProject()) == null) {
				build(getProject(), false, monitor);
			//}
			break;
		default:
			break;
		}
		return null;
	}

	/** Checks if the running operation was cancelled by the user, as reported by
	 * the progress monitor.
	 * 
	 * @param monitor
	 *            the progress monitor. */
	// @see http://www.eclipse.org/articles/Article-Builders/builders.html
	protected final void checkCancel(final IProgressMonitor monitor) {
		if (monitor.isCanceled()) {
			forgetLastBuiltState(); // not always necessary
			throw new OperationCanceledException();
		}
	}

	

	/** Performs a full build of the project's JAX-RS Metamodel. This method has
	 * a public visibility so that it can be called from other components
	 * 
	 * @param project
	 *            the project
	 * @param monitor
	 *            the progress monitor */
	private void build(final IProject project, final boolean requiresReset, final IProgressMonitor monitor) {
		long startTime = new Date().getTime();
		try {
			monitor.beginTask("Building JAX-RS metamodel...", FULL_BUILD_STEPS);
			JaxrsMetamodelBuildJob buildJob = new JaxrsMetamodelBuildJob(project, requiresReset);
			buildJob.schedule();
			// wait until the job is finished
			buildJob.join();
		} catch (InterruptedException e) {
			Logger.error("Error while building the JAX-RS Metamodel for project " + project.getName(), e);
		} catch (CoreException e) {
			Logger.error("Error while building the JAX-RS Metamodel for project " + project.getName(), e);
		} finally {
			long endTime = new Date().getTime();
			Logger.debug("JAX-RS Metamodel for project '" + project.getName() + "' fully built in "
					+ (endTime - startTime) + "ms.");
			monitor.done();
		}
	}

	/** Trace the kind of build in the log.
	 * 
	 * @param kind
	 *            the build kind
	 * @param args
	 * @param project
	 *            the project being built */
	private void logBuild(final int kind, @SuppressWarnings("rawtypes") final Map args, final IProject project) {
		StringBuilder sb = new StringBuilder("'");
		for (Field field : IncrementalProjectBuilder.class.getDeclaredFields()) {
			String name = field.getName();
			int value;
			try {
				value = field.getInt(this);
				if (value == kind) {
					sb.append(name.toLowerCase().replace('_', ' '));
				}
			} catch (IllegalArgumentException e) {
				sb.append("*Unknow build*");
			} catch (IllegalAccessException e) {
				sb.append("*Unknow build*");
			}
		}
		sb.append("' on project ").append(project.getName());
		if (args != null && !args.isEmpty()) {
			sb.append(" (");
			for (Iterator<?> iterator = args.keySet().iterator(); iterator.hasNext();) {
				Object key = iterator.next();
				sb.append(key).append("=").append(args.get(key));
				if (iterator.hasNext()) {
					sb.append(", ");
				}
			}
			sb.append(")");
		}
		Logger.debug(sb.toString());
	}

}
