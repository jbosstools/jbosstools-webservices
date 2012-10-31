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

package org.jboss.tools.ws.jaxrs.ui.cnf.action;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.navigator.CommonViewer;
import org.jboss.tools.ws.jaxrs.ui.JBossJaxrsUIPlugin;
import org.jboss.tools.ws.jaxrs.ui.cnf.UriPathTemplateCategory;

public class RefreshNavigatorAction extends Action implements ISelectionChangedListener {

	private ISelection selection = null;

	public RefreshNavigatorAction() {
		super("Refresh", JBossJaxrsUIPlugin.getDefault().getImageDescriptor("refresh.gif"));
	}

	@Override
	public void run() {
		ITreeSelection treeSelection = ((ITreeSelection) selection);
		for (Object element : treeSelection.toList()) {
			if (element instanceof UriPathTemplateCategory) {
				((UriPathTemplateCategory) element).refreshContent();
			}
		}
		super.run();
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		Object source = event.getSource();
		if (source instanceof CommonViewer) {
			this.selection = ((CommonViewer) source).getSelection();
		}
	}

	public void setSelection(ISelection selection) {
		this.selection = selection;

	}

}
