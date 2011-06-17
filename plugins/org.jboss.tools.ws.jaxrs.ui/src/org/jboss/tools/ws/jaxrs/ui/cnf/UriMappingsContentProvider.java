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
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.internal.ui.util.CoreUtility;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.jboss.tools.ws.jaxrs.core.metamodel.Metamodel;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;

@SuppressWarnings("restriction")
public class UriMappingsContentProvider implements ITreeContentProvider, IResourceChangeListener {

	private TreeViewer viewer;

	private Map<IProject, Object> uriPathTemplateCategories = new HashMap<IProject, Object>();

	public UriMappingsContentProvider() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		workspace.addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
	}

	@Override
	public Object[] getChildren(Object parentElement) {

		if (parentElement instanceof IProject) {
			long startTime = new Date().getTime();
			IProject project = (IProject) parentElement;
			try {
				if (!uriPathTemplateCategories.containsKey(project)) {
					Metamodel metamodel = Metamodel.get(project);
					if (metamodel == null) {
						// trigger background build and immediately return a
						// temporary element to the UI
						Job[] jobs = Job.getJobManager().find(null);
						if (jobs != null) {
							for (Job job : jobs) {
								if (job.belongsTo(ResourcesPlugin.FAMILY_AUTO_BUILD)
										|| job.belongsTo(ResourcesPlugin.FAMILY_AUTO_REFRESH)
										|| job.belongsTo(ResourcesPlugin.FAMILY_MANUAL_BUILD)
										|| job.belongsTo(ResourcesPlugin.FAMILY_MANUAL_REFRESH)
										&& job.getState() == Job.RUNNING) {
									// joining running job
									Logger.debug("Joining Running job: " + job.getName() + "(blocking="
											+ job.isBlocking() + "/state=" + job.getState() + ")");
									job.join();
									Logger.debug("Job finished: " + job.getName());
								}
							}
						}
						// after running job is done, check if the metamodel
						// was
						// built, otherwise, force it.
						metamodel = Metamodel.get(project);
						if (metamodel == null) {
							Logger.debug("Metamodel is (still) null for project '" + project.getName() + "'");
							CoreUtility.startBuildInBackground(project);
							return new Object[] { new WaitWhileBuildingElement() };
						}
					}
					UriPathTemplateCategory uriPathTemplateCategory = new UriPathTemplateCategory(this, metamodel,
							project);
					uriPathTemplateCategories.put(project, uriPathTemplateCategory);
				}
				return new Object[] { uriPathTemplateCategories.get(project) };
			} catch (CoreException e) {
				Logger.error("Failed to retrieve JAX-RS Metamodel in project '" + project.getName() + "'", e);
			} catch (InterruptedException e) {
				Logger.error(
						"Failed to join currently running job while building or retrieving metamodel for project '"
								+ project.getName() + "'", e);
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
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		uriPathTemplateCategories = null;
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		refreshContent();
	}

	protected void refreshContent() {
		Logger.debug("Refreshing navigator view");
		if (Display.getCurrent() != null) {
			if (viewer != null) {
				TreePath[] treePaths = viewer.getExpandedTreePaths();
				viewer.refresh();
				viewer.setExpandedTreePaths(treePaths);
			}
		} else {
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (viewer != null) {
						TreePath[] treePaths = viewer.getExpandedTreePaths();
						viewer.refresh();
						viewer.setExpandedTreePaths(treePaths);
					}
				}
			});
		}
	}

}
