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

package org.jboss.tools.ws.jaxrs.ui.cnf.action;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonViewer;
import org.jboss.tools.ws.jaxrs.ui.JBossJaxrsUIPlugin;
import org.jboss.tools.ws.jaxrs.ui.wizards.JaxrsResourceCreationWizard;

/**
 * @author xcoulon
 *
 */
public class CreateResourceAction extends Action implements ISelectionChangedListener {

	private IStructuredSelection selection = null;

	public CreateResourceAction() {
		super("New JAX-RS Resource", JBossJaxrsUIPlugin.getDefault().getImageDescriptor("new_webserv_wiz.gif"));
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run() {
		final ITreeSelection treeSelection = ((ITreeSelection) selection);
		@SuppressWarnings("rawtypes")
		final List selections = treeSelection.toList();
		if (selections.isEmpty()) {
			return;
		}
		open(new JaxrsResourceCreationWizard(), Display.getDefault().getActiveShell());
	}
	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		Object source = event.getSource();
		if (source instanceof CommonViewer) {
			this.selection = (IStructuredSelection) ((CommonViewer) source).getSelection();
		}
	}
	
	public void setSelection(final IStructuredSelection selection) {
		this.selection = selection;
	}
	
	private boolean open(IWorkbenchWizard wizard, Shell shell) {
		WizardDialog dialog = new WizardDialog(shell, wizard);
		wizard.init(PlatformUI.getWorkbench(), selection);
		dialog.create();
		return dialog.open() == Dialog.OK;

	}
}
