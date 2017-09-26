/******************************************************************************* 
 * Copyright (c) 2011 - 2014 Red Hat, Inc. and others.  
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.jaxws.ui.view;

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
import org.jboss.tools.ws.ui.views.WebServicesTestView;

/**
 * Action to fire up the WS Tester from a WSDL in a navigator view
 * 
 * @author bfitzpat
 *
 */
public class RunWSTesterAction implements IObjectActionDelegate {

	private ISelection selection = null;

	@Override
	public void run(final IAction action) {
		if (this.selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) this.selection;
			if (ssel.getFirstElement() instanceof IFile) {
				IFile wsdlFile = (IFile) ssel.getFirstElement();
				IWorkbench wb = PlatformUI.getWorkbench();
				IWorkbenchWindow win = wb.getActiveWorkbenchWindow();
				IWorkbenchPage page = win.getActivePage();
				String id = WebServicesTestView.ID;
				try {
					IViewPart part = page.showView(id);
					if (part instanceof WebServicesTestView) {
						WebServicesTestView testerView = (WebServicesTestView) part;
						String url = wsdlFile.getRawLocationURI().toURL().toExternalForm();
						testerView.setURL(url);
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
