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

import static org.eclipse.jdt.core.IJavaElementDelta.CHANGED;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCorePlugin;
import org.jboss.tools.ws.jaxrs.core.configuration.ProjectNatureUtils;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.utils.ConstantUtils;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.metamodel.JaxrsMetamodelDelta;
import org.jboss.tools.ws.jaxrs.core.metamodel.JaxrsMetamodelLocator;

/**
 * The JAX-RS Metamodel builder. Invoked when a full build or an incremental build is triggered on a project on which
 * the JAX-RS nature is installed.
 * 
 * This builder is responsible of the creation and update of the JAX-RS Metamodel which is kept in the project's session
 * properties.
 * 
 * @author xcoulon
 */
public class JaxrsMetamodelBuilder extends IncrementalProjectBuilder {

	public static final int SCALE = 10;

	/** The standard 'Java type' marker type. */
	public static final String JAVA_PROBLEM = "org.eclipse.jdt.core.problem";

	/** The custom 'JAX-RS Problem' marker type. */
	public static final String JAXRS_PROBLEM = "org.jboss.tools.ws.jaxrs.metamodelMarker";

	/** The Java element change listener name. */
	public static final QualifiedName JAXRS_ELEMENT_CHANGE_LISTENER_NAME = new QualifiedName(
			JBossJaxrsCorePlugin.PLUGIN_ID, "jaxrsPostReconcileListener");

	private final JaxrsElementChangedPublisher metamodelChangedPublisher = new JaxrsElementChangedPublisher();

	private final JaxrsMetamodelChangedProcessor metamodelChangedProcessor = new JaxrsMetamodelChangedProcessor();

	private final ResourceChangedProcessor resourceChangedProcessor = new ResourceChangedProcessor();

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
		case FULL_BUILD: // also called after a CLEAN_BUILD
		case AUTO_BUILD:
		case INCREMENTAL_BUILD:
			build(kind, project, monitor);
			break;
		}
		return new IProject[] { project };
	}

	/**
	 * Checks if the running operation was cancelled by the user, as reported by the progress monitor.
	 * 
	 * @param monitor
	 *            the progress monitor.
	 */
	// @see http://www.eclipse.org/articles/Article-Builders/builders.html
	protected final void checkCancel(final IProgressMonitor monitor) {
		if (monitor.isCanceled()) {
			forgetLastBuiltState(); // not always necessary
			throw new OperationCanceledException();
		}
	}

	/**
	 * Performs a full build of the project's JAX-RS Metamodel. This method has a public visibility so that it can be
	 * called from other components
	 * 
	 * @param project
	 *            the project
	 * @param progressMonitor
	 *            the progress monitor
	 */
	private void build(final int buildKind, final IProject project, final IProgressMonitor progressMonitor) {
		long startTime = new Date().getTime();
		try {
			progressMonitor.beginTask("Building JAX-RS Metamodel", 4 * SCALE);
			Logger.debug("Building JAX-RS Metamodel for project {}...", project.getName());
			// extract the relevant delta bound to this built (some resources or entire project)
			final List<ResourceDelta> affectedResources = extractAffectedResources(project, progressMonitor);
			progressMonitor.worked(SCALE);
			// compute changes on the JAX-RS Application(s), HttpMethods, Resources, etc.
			final boolean withReset = (buildKind == FULL_BUILD || buildKind == CLEAN_BUILD);
			final JaxrsMetamodelDelta metamodelDelta = resourceChangedProcessor
					.processAffectedResources(project, withReset, affectedResources, new SubProgressMonitor(progressMonitor, SCALE));
			progressMonitor.worked(SCALE);
			metamodelChangedProcessor.processAffectedMetamodel(metamodelDelta, new SubProgressMonitor(progressMonitor,
					SCALE));
			progressMonitor.worked(SCALE);
			metamodelChangedPublisher.publish(metamodelDelta, new SubProgressMonitor(progressMonitor, SCALE));
			progressMonitor.worked(SCALE);
		} catch (CoreException e) {
			Logger.error("Error while building the JAX-RS Metamodel for project " + project.getName(), e);
		} finally {
			long endTime = new Date().getTime();
			Logger.debug("JAX-RS Metamodel for project '{}' fully built in {} ms.", project.getName(),
					(endTime - startTime));
			try {
				final JaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(project);
				Logger.debug(
						"JAX-RS Metamodel for project '{}' now has {} HttpMethods, {} Resources and {} Endpoints.",
						project.getName(), metamodel.getAllHttpMethods().size(), metamodel.getAllResources().size(),
						metamodel.getAllEndpoints().size());
			} catch (Throwable e) {
				// debug level since the purpose was to display a debug message
				Logger.debug("Error occurred: {}", e);
			}
			progressMonitor.done();
		}
	}

	private List<ResourceDelta> extractAffectedResources(final IProject project, final IProgressMonitor progressMonitor)
			throws CoreException {
		final IResourceDelta delta = getDelta(project);
		final List<ResourceDelta> events = new ArrayList<ResourceDelta>();
		if (delta != null) {
			events.addAll(new ResourceDeltaScanner().scanAndFilterEvent(delta, new SubProgressMonitor(
					progressMonitor, SCALE)));
		} else {
			events.add(new ResourceDelta(project, CHANGED, 0));
		}
		return events;
	}

	/**
	 * Trace the kind of build in the log.
	 * 
	 * @param kind
	 *            the build kind
	 * @param args
	 * @param project
	 *            the project being built
	 */
	private void logBuild(final int kind, @SuppressWarnings("rawtypes") final Map args, final IProject project) {
		StringBuilder sb = new StringBuilder("JAX-RS Builder called after '");
		sb.append(ConstantUtils.getStaticFieldName(IncrementalProjectBuilder.class, kind));
		sb.append("' on project ").append(project.getName());
		Logger.debug(sb.toString());
	}

}
