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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.jboss.tools.ws.jaxrs.core.metamodel.Metamodel;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;

public class UriMappingsContentProvider implements ITreeContentProvider, IResourceChangeListener {

	private TreeViewer viewer;

	private Map<IProject, Metamodel> metamodels = new HashMap<IProject, Metamodel>();

	private Map<IProject, UriPathTemplateCategory> uriPathTemplateCategories = new HashMap<IProject, UriPathTemplateCategory>();

	public UriMappingsContentProvider() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		workspace.addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
	}

	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IProject) {
			IProject project = (IProject) parentElement;
			try {
				if (!uriPathTemplateCategories.containsKey(project)) {
					Metamodel metamodel = Metamodel.get(project);
					if (metamodel == null) {
						return null;
					}
					UriPathTemplateCategory uriPathTemplateCategory = new UriPathTemplateCategory(this, metamodel,
							project);
					metamodels.put(project, metamodel);
					uriPathTemplateCategories.put(project, uriPathTemplateCategory);
				}
				return new Object[] { uriPathTemplateCategories.get(project) };
			} catch (CoreException e) {
				Logger.error("Failed to retrieve JAX-RS Metamodel in project '" + project.getName() + "'", e);
			}
		} else if (parentElement instanceof ITreeContentProvider) {
			return ((ITreeContentProvider) parentElement).getChildren(parentElement);
		}
		return null;
	}

	public Object getParent(Object element) {
		return null;
	}

	public boolean hasChildren(Object element) {
		if (element instanceof ITreeContentProvider) {
			return ((ITreeContentProvider) element).hasChildren(element);
		}
		return false;
	}

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
