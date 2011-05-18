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

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.navigator.CommonViewer;
import org.jboss.tools.ws.jaxrs.ui.JBossJaxrsUIPlugin;
import org.jboss.tools.ws.jaxrs.ui.cnf.UriPathTemplateElement;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;

public class CopyToClipboardAction extends Action implements ISelectionChangedListener {

	private ISelection selection = null;

	public CopyToClipboardAction() {
		super("Copy URI Path Template", JBossJaxrsUIPlugin.getDefault().createImageDescriptor(
				"copyqualifiedname.gif"));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run() {
		ITreeSelection treeSelection = ((ITreeSelection) selection);
		@SuppressWarnings("rawtypes")
		List selections = treeSelection.toList();
		if (selections.isEmpty()) {
			return;
		}
		Object selectedObject = selections.get(0);
		try {
			if (selectedObject instanceof UriPathTemplateElement) {
				String uriPathTemplate = ((UriPathTemplateElement) selectedObject).getResolvedUriMapping().getFullUriPathTemplate();
				Clipboard clipboard = new Clipboard(Display.getCurrent());
				clipboard.setContents(new Object[]{uriPathTemplate}, new Transfer[]{TextTransfer.getInstance()});
			}
		} catch (Exception e) {
			Logger.error("Failed to open Java editor", e);
		}
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
