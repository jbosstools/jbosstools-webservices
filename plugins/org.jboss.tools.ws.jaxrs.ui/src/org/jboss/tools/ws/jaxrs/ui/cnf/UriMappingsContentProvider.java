/******************************************************************************* 
 * Copyright (c) 2008 - 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.ws.jaxrs.ui.cnf;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.navigator.resources.ProjectExplorer;
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCorePlugin;
import org.jboss.tools.ws.jaxrs.core.configuration.ProjectNatureUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsMetamodelChangedListener;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsEndpointDelta;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsMetamodelDelta;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;

public class UriMappingsContentProvider implements ITreeContentProvider, IJaxrsMetamodelChangedListener {

	private TreeViewer viewer;

	private Map<IProject, UriPathTemplateCategory> uriPathTemplateCategories = new HashMap<IProject, UriPathTemplateCategory>();

	/**
	 * Constructor.
	 * Is called once, when the first 'JAX-RS Web Services' node is created in the {@link ProjectExplorer}. 
	 */
	public UriMappingsContentProvider() {
		Logger.debug("*** Instantiating the UriMappingsContentProvider ***");
		JBossJaxrsCorePlugin.getDefault().addJaxrsMetamodelChangedListener(this);
	}
	
	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	@Override
	public Object[] getChildren(final Object parentElement) {
		if (parentElement instanceof IProject) {
			return getChildren((IProject)parentElement);
		}
		if (parentElement instanceof ITreeContentProvider) {
			Logger.debug("Displaying the children of '{}'", parentElement);
			return ((ITreeContentProvider) parentElement).getChildren(parentElement);
		}
		Logger.debug("*** No children for parent of type '{}' ***", parentElement.getClass().getName());
		return new Object[0];
	}
	
	/**
	 * Return the children elements for the given {@link IProject}. Returned array should contain a single
	 * entry, the {@link UriPathTemplateCategory}.
	 * @param project
	 * @return an array with a single {@link UriPathTemplateCategory} item, or an empty array if an exception occurred
	 */
	private Object[] getChildren(final IProject project) {
		if (!uriPathTemplateCategories.containsKey(project)) {
			final UriPathTemplateCategory uriPathTemplateCategory = new UriPathTemplateCategory(this, project);
			uriPathTemplateCategories.put(project, uriPathTemplateCategory);
		}
		Logger.debug("Displaying the UriPathTemplateCategory for project '{}'", project.getName());
		return new Object[] { uriPathTemplateCategories.get(project) };
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof IProject) {
			Logger.trace("Project '{}' has children: true", ((IProject)element).getName());
			return true;
		} else if (element instanceof ITreeContentProvider) {
			final boolean hasChildren = ((ITreeContentProvider) element).hasChildren(element);
			Logger.trace("Element {} has children: {}", element, hasChildren);
			return hasChildren;
		}
		Logger.debug("Element '{}' has no children", element);
		return false;
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.viewer = (TreeViewer) viewer;
	}

	@Override
	public void dispose() {
		JBossJaxrsCorePlugin.getDefault().removeListener(this);
		uriPathTemplateCategories = null;
	}

	@Override
	public void notifyEndpointChanged(final JaxrsEndpointDelta delta) {
		switch (delta.getKind()) {
		case IJavaElementDelta.ADDED:
		case IJavaElementDelta.REMOVED:
			refreshContent(delta.getEndpoint().getMetamodel());
			break;
		case IJavaElementDelta.CHANGED:
			refreshContent(delta.getEndpoint());
			break;
		}
	}
	
	@Override
	public void notifyEndpointProblemLevelChanged(final IJaxrsEndpoint endpoint) {
		// check if the viewer is already having the appropriate
		// UriPathTemplateCategory for the given project. If not,
		// it is a WaitWhileBuildingElement item, and the project itself must be
		// refresh to replace this temporary
		// element with the expected category.
		final IProject project = endpoint.getProject();
		if (!uriPathTemplateCategories.containsKey(project)) {
			refreshTarget(project);
		} else {
			final UriPathTemplateCategory uriPathTemplateCategory = uriPathTemplateCategories.get(project);
			final UriPathTemplateElement target = uriPathTemplateCategory.getUriPathTemplateElement(endpoint);
			if(target != null) {
				Logger.debug("Refreshing navigator view at level: '{}'", target.getClass().getName());
				// this piece of code must run in an async manner to avoid reentrant
				// call while viewer is busy.
				updateContent(target);
			}
		}
	}

	@Override
	public void notifyMetamodelProblemLevelChanged(final IJaxrsMetamodel metamodel) {
		//FIXME: the UI update should only take place on the UriPathTemplateCategory node, there is no need to 
		// update its children..
		if(metamodel == null) {
			return;
		}
		final IProject project = metamodel.getProject();
		if (uriPathTemplateCategories != null) {
			if (!uriPathTemplateCategories.containsKey(project)) {
				Logger.debug("Adding a UriPathTemplateCategory for project '{}' (case #1)", project.getName());
				UriPathTemplateCategory uriPathTemplateCategory = new UriPathTemplateCategory(this, metamodel.getJavaProject());
				uriPathTemplateCategories.put(project, uriPathTemplateCategory);
				refreshTarget(uriPathTemplateCategories.get(project));
			}
			updateContent(uriPathTemplateCategories.get(project));
		}
	}
	
	@Override
	public void notifyMetamodelChanged(final JaxrsMetamodelDelta delta) {
		final IProject project = delta.getMetamodel().getProject();
		if(delta.getKind() == IJavaElementDelta.REMOVED && uriPathTemplateCategories != null) {
			uriPathTemplateCategories.remove(project);
		}
		refreshTarget(project);
	}

	/**
	 * Refresh the whole JAX-RS Content tree for the given Project
	 * 
	 * @param project
	 */
	public void refreshContent(final IJaxrsMetamodel metamodel) {
		try {
			final IProject project = metamodel.getProject();
			if (uriPathTemplateCategories != null) {
				if(!ProjectNatureUtils.isProjectNatureInstalled(project, ProjectNatureUtils.JAXRS_NATURE_ID)) {
					Logger.debug("*** Project '{}' has no JAX-RS nature installed. ***", project.getName());
					if (uriPathTemplateCategories.containsKey(project)) {
						uriPathTemplateCategories.remove(project);
						refreshTarget(project);
					}	
				}
				else if (!uriPathTemplateCategories.containsKey(project)) {
					Logger.debug("Adding a UriPathTemplateCategory for project '{}' (case #1)", project.getName());
					UriPathTemplateCategory uriPathTemplateCategory = new UriPathTemplateCategory(this, project);
					uriPathTemplateCategories.put(project, uriPathTemplateCategory);
					refreshTarget(project);
				} else {
					Logger.debug("Refreshing UriPathTemplateCategory for project '{}' (case #2)", project.getName());
					refreshTarget(uriPathTemplateCategories.get(project));
				}
			}
		} catch (CoreException e) {
		}
	}

	/**
	 * Refresh the JAX-RS Content tree for the given {@link IJaxrsEndpoint} only
	 * 
	 * @param project
	 */
	private void refreshContent(final IJaxrsEndpoint endpoint) {
		// check if the viewer is already having the appropriate
		// UriPathTemplateCategory for the given project. If not,
		// it is a WaitWhileBuildingElement item, and the project itself must be
		// refresh to replace this temporary
		// element with the expected category.
		final IProject project = endpoint.getProject();
		if (!uriPathTemplateCategories.containsKey(project)) {
			refreshTarget(project);
		} else {
			final UriPathTemplateCategory uriPathTemplateCategory = uriPathTemplateCategories.get(project);
			final UriPathTemplateElement target = uriPathTemplateCategory.getUriPathTemplateElement(endpoint);
			// during initialization, UI may not be available yet.
			if (target != null) {
				Logger.debug("Refreshing navigator view at level: '{}'", target.getClass().getName());
				// this piece of code must run in an async manner to avoid
				// reentrant
				// call while viewer is busy.
				refreshTarget(target);
			}
		}
	}

	/**
	 * Refresh the whole JAX-RS Content tree for the <strong>given target node and all
	 * its subelements</strong>.
	 * 
	 * @param target
	 *            the node to refresh
	 */
	private void refreshTarget(final Object target) {
		// this piece of code must run in an async manner to avoid reentrant
		// call while viewer is busy.
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				if (viewer != null) {
					TreePath[] treePaths = viewer.getExpandedTreePaths();
					Logger.debug("*** Refreshing the viewer (busy: {}) ***", viewer.isBusy());
					viewer.refresh(target, true);
					viewer.setExpandedTreePaths(treePaths);
					Logger.debug("*** Refreshing the viewer... done ***");
				} else {
					Logger.debug("*** Cannot refresh: viewer is null :-( ***");
				}
			}
		});
	}

	/**
	 * Updates only the JAX-RS tree node for the given <strong>target node only, but skips its subelements</strong>.
	 * 
	 * @param target
	 *            the node to refresh
	 */
	protected void updateContent(final Object target) {
		// this piece of code must run in an async manner to avoid reentrant
		// call while viewer is busy.
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				if (viewer != null && target != null) {
					TreePath[] treePaths = viewer.getExpandedTreePaths();
					Logger.debug("*** Updating the viewer at target level: {} (viewer busy: {}) ***", target,
							viewer.isBusy());
					viewer.update(target, null);
					viewer.setExpandedTreePaths(treePaths);
					Logger.debug("*** Refreshing the viewer... done ***");
				} else {
					Logger.debug("*** Cannot refresh: viewer is null :-( ***");
				}
			}
		});
	}
	
	public static class LoadingStub {
		
		private final IJavaProject javaProject;
		
		public LoadingStub(final IJavaProject javaProject) {
			this.javaProject = javaProject;
		}
		
		@Override
		public String toString() {
			return "Loading Stub on '" + javaProject.getElementName() + "'";
		}
	}
	
}
