/******************************************************************************* 
 * Copyright (c) 2011 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.ui.actions;

import java.net.MalformedURLException;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.jboss.tools.ws.ui.JBossWSUIPlugin;
import org.jboss.tools.ws.ui.views.JAXRSWSTestView2;

/**
 * Action to fire up the WS Tester from a WSDL in a navigator view
 * @author bfitzpat
 *
 */
public class RunWSTesterAction implements IObjectActionDelegate {

	private ISelection selection = null;

	public RunWSTesterAction() {
		// empty
	}

	@Override
	public void run(final IAction action) {
		if (this.selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) this.selection;
			if (ssel.getFirstElement() instanceof IFile) {
				IFile wsdlFile = (IFile) ssel.getFirstElement();
				IWorkbench wb = PlatformUI.getWorkbench();
				IWorkbenchWindow win = wb.getActiveWorkbenchWindow();
				IWorkbenchPage page = win.getActivePage();				
				String id = JAXRSWSTestView2.ID;
				try {
					IViewPart part = page.showView(id);
					if (part instanceof JAXRSWSTestView2) {
						JAXRSWSTestView2 testerView = (JAXRSWSTestView2) part;
						String url = wsdlFile.getRawLocationURI().toURL().toExternalForm();
						testerView.setWSDLURL(url);
					}
				} catch (PartInitException e) {
					e.printStackTrace();
					JBossWSUIPlugin.log(e);
				} catch (MalformedURLException e) {
					e.printStackTrace();
					JBossWSUIPlugin.log(e);
				}
			}
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		// empty
	}

}
