/**
 * 
 */
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;

/** Listens to all change events (Java elements and resources) and triggers a new
 * job for each change.<br>
 * Yet, it avoids trigger new Jobs for high level changes (JavaModel,
 * WorkspaceRoot, etc.)
 * 
 * @author xcoulon */
public class JavaElementChangedListener implements IResourceChangeListener, IElementChangedListener {

	/** {@inheritDoc} (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.core.IElementChangedListener#elementChanged(org.eclipse.jdt.core.ElementChangedEvent) */
	@Override
	public void elementChanged(ElementChangedEvent event) {
		if (event.getDelta().getElement().getJavaProject() != null) {
			Job job = new JaxrsMetamodelBuildJob(event);
			job.schedule();
		}
	}

	/** {@inheritDoc} (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent) */
	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		if (event.getDelta().getResource() != null) {
			Job job = new JaxrsMetamodelBuildJob(event);
			job.schedule();
		}
	}

}
