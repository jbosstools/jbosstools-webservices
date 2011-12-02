/**
 * 
 */
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.JavaElementDelta;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.pubsub.EventService;

/** @author xcoulon */
@SuppressWarnings("restriction")
public class JaxrsMetamodelBuildJob extends Job {

	private final JavaElementChangedProcessor javaElementChangedProcessor = new JavaElementChangedProcessor();

	private final JaxrsElementChangedProcessor jaxrsElementChangedProcessor = new JaxrsElementChangedProcessor();

	private static final int SCALE = 10;

	private final Object event;

	public JaxrsMetamodelBuildJob(final IProject project, final boolean requiresReset) throws CoreException {
		super("JAX-RS Metamodel build for " + project.getName() + "...");
		IJavaProject javaProject = JavaCore.create(project);
		JavaElementDelta delta = new JavaElementDelta(javaProject);
		delta.added();
		this.event = new ElementChangedEvent(delta, ElementChangedEvent.POST_RECONCILE);
		final JaxrsMetamodel metamodel = JaxrsMetamodel.get(javaProject);
		if (metamodel == null) {
			JaxrsMetamodel.create(javaProject);
		} else if (requiresReset) {
			metamodel.reset();
		}
	}

	public JaxrsMetamodelBuildJob(final IResourceChangeEvent event) {
		super("Incremental JAX-RS Metamodel build..."); //$NON-NLS-1$
		this.event = event;
	}

	public JaxrsMetamodelBuildJob(final ElementChangedEvent event) {
		super("Incremental JAX-RS Metamodel build..."); //$NON-NLS-1$
		this.event = event;
	}

	@Override
	protected IStatus run(IProgressMonitor progressMonitor) {
		try {
			progressMonitor.beginTask("Build JAX-RS Metamodel", 8 * SCALE);
			progressMonitor.worked(SCALE);
			// create fake event at the JavaProject level:
			// scan and filter delta, retrieve a list of java changes
			final List<JavaElementChangedEvent> events = new ElementChangedEventScanner().scanAndFilterEvent(event,
					new SubProgressMonitor(progressMonitor, SCALE));
			// process events against HTTP Methods, retrieve jaxrs changes
			// TODO: use the delegate (with renaming ?) to process the
			// changes.
			// FIXME : will need to add support for JavaProject changes !!!
			final List<JaxrsElementChangedEvent> jaxrsElementChanges = javaElementChangedProcessor.processEvents(
					events, progressMonitor);
			final List<JaxrsEndpointChangedEvent> jaxrsEndpointChanges = jaxrsElementChangedProcessor.processEvents(
					jaxrsElementChanges, progressMonitor);
			if(jaxrsEndpointChanges == null || jaxrsEndpointChanges.isEmpty()) {
				Logger.debug("No JAX-RS change to publish to the UI");
			} else {
			for (JaxrsEndpointChangedEvent change : jaxrsEndpointChanges) {
				Logger.debug(change.toString());
				EventService.getInstance().publish(change);
			}
			}
		} catch (Throwable e) {
			Logger.error("Failed to build or refresh the JAX-RS metamodel", e);
		} finally {
			progressMonitor.done();
		}
		return Status.OK_STATUS;
	}
}
