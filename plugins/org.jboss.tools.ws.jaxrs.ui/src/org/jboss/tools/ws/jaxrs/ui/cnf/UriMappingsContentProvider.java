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

package org.jboss.tools.ws.jaxrs.ui.cnf;

import java.util.Date;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.internal.ui.util.CoreUtility;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsEndpointChangedEvent;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.pubsub.EventService;
import org.jboss.tools.ws.jaxrs.core.pubsub.Subscriber;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;

@SuppressWarnings("restriction")
public class UriMappingsContentProvider implements ITreeContentProvider, Subscriber { // ,
	// IResourceChangeListener
	// {

	private TreeViewer viewer;

	private Map<IProject, Object> uriPathTemplateCategories = new HashMap<IProject, Object>();

	public UriMappingsContentProvider() {
		// IWorkspace workspace = ResourcesPlugin.getWorkspace();
		// workspace.addResourceChangeListener(this,
		// IResourceChangeEvent.POST_CHANGE);
		EventService.getInstance().subscribe(this);
	}

	@Override
	public Object[] getChildren(final Object parentElement) {

		if (parentElement instanceof IProject) {
			long startTime = new Date().getTime();
			IProject project = (IProject) parentElement;
			try {
				if (!uriPathTemplateCategories.containsKey(project)) {
					IJaxrsMetamodel jaxrsMetamodel = JaxrsMetamodel.get(project);
					if (jaxrsMetamodel == null) {
						Logger.debug("Metamodel needs to be built for project '" + project.getName() + "'");
						Job buildJob = CoreUtility.getBuildJob(project);
						buildJob.setRule(ResourcesPlugin.getWorkspace().getRoot());
						// be notified when job is done to refresh navigator
						// content.
						buildJob.addJobChangeListener(new JobChangeAdapter() {
							@Override
							public void done(IJobChangeEvent event) {
								refreshContent((IProject) parentElement);
							}
						});
						buildJob.schedule();
						return new Object[] { new WaitWhileBuildingElement() };
					}
					UriPathTemplateCategory uriPathTemplateCategory = new UriPathTemplateCategory(this, jaxrsMetamodel,
							project);
					uriPathTemplateCategories.put(project, uriPathTemplateCategory);
				}
				return new Object[] { uriPathTemplateCategories.get(project) };
			} catch (CoreException e) {
				Logger.error("Failed to retrieve JAX-RS Metamodel in project '" + project.getName() + "'", e);
			} finally {
				long endTime = new Date().getTime();
				Logger.debug("JAX-RS Metamodel UI for project '" + project.getName() + "' refreshed in "
						+ (endTime - startTime) + "ms.");
			}
		} else if (parentElement instanceof ITreeContentProvider) {
			return ((ITreeContentProvider) parentElement).getChildren(parentElement);
		}
		return null;
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof ITreeContentProvider) {
			return ((ITreeContentProvider) element).hasChildren(element);
		}
		return false;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.viewer = (TreeViewer) viewer;
	}

	@Override
	public void dispose() {
		// ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		uriPathTemplateCategories = null;
	}

	/*
	 * @Override
	 * public void resourceChanged(IResourceChangeEvent event) {
	 * refreshChangedProjects(event.getDelta());
	 * }
	 */

	@Override
	public void inform(EventObject event) {
		if (event instanceof IJaxrsEndpointChangedEvent) {
			final IJaxrsEndpointChangedEvent change = (IJaxrsEndpointChangedEvent) event;
			final IJaxrsEndpoint endpoint = change.getEndpoint();
			refreshContent(endpoint.getJavaProject().getProject());
		}
	}

	// TODO : register custom listener/event to metamodel to avoid refreshes for
	// any kind of changes
	private void refreshChangedProjects(IResourceDelta delta) {
		if (delta.getResource() instanceof IProject) {
			Logger.debug("Refreshing after project change");
			refreshContent((IProject) delta.getResource());
			return;
		}
		for (IResourceDelta childDelta : delta.getAffectedChildren()) {
			refreshChangedProjects(childDelta);
		}
	}

	protected void refreshContent(final IProject project) {
		Logger.debug("Refreshing navigator view");
		final Object target = uriPathTemplateCategories.containsKey(project) ? uriPathTemplateCategories.get(project)
				: project;
		if (Display.getCurrent() != null) {
			if (viewer != null) {
				TreePath[] treePaths = viewer.getExpandedTreePaths();
				viewer.refresh(target);
				viewer.setExpandedTreePaths(treePaths);
			}
		} else {
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (viewer != null) {
						TreePath[] treePaths = viewer.getExpandedTreePaths();
						viewer.refresh(target);
						viewer.setExpandedTreePaths(treePaths);
					}
				}
			});
		}
	}

	@Override
	/**
	 * Subscriber ID
	 */
	public String getId() {
		return "UI";
	}

}
