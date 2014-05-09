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

package org.jboss.tools.ws.jaxrs.ui.wizards;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.wizards.NewElementWizard;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.jboss.tools.common.ui.CommonUIImages;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;

/**
 * Wizard to create a new JAX-RS Resource with Resource Methods, and optionally
 * a JAX-RS Activator if none already exists.
 * 
 * @author xcoulon
 *
 */
@SuppressWarnings("restriction")
public class JaxrsApplicationCreationWizard extends NewElementWizard implements INewWizard {

	/** The current selection in the workbench when this wizard was opened. */
	private IStructuredSelection selection;

	/** The JAX-RS Application settings page. */
	private JaxrsApplicationCreationWizardPage applicationPage;

	/**
	 * Default constructor.
	 */
	public JaxrsApplicationCreationWizard() {
		setDefaultPageImageDescriptor(CommonUIImages.getInstance().getOrCreateImageDescriptor(
				CommonUIImages.WEB_SERVICE_IMAGE));
	}

	@Override
	public void init(final IWorkbench workbench, final IStructuredSelection selection) {
		this.selection = selection;
	}

	@Override
	public void addPages() {
		this.applicationPage = new JaxrsApplicationCreationWizardPage(false);
		addPage(applicationPage);
		applicationPage.init(selection);
	}

	@Override
	public boolean performFinish() {
		boolean applicationCreated = super.performFinish();
		if (applicationCreated) {
			if (this.applicationPage.getApplicationMode() == JaxrsApplicationCreationWizardPage.APPLICATION_JAVA) {
				try {
					JavaUI.openInEditor(applicationPage.getCreatedType());
				} catch (PartInitException e) {
					Logger.error("Failed to open '" + this.applicationPage.getCreatedType().getFullyQualifiedName()
							+ "'", e);
				} catch (JavaModelException e) {
					Logger.error("Failed to open '" + this.applicationPage.getCreatedType().getFullyQualifiedName()
							+ "'", e);
				}
			} else {
				final IFile webxmlResource = applicationPage.getWebxmlResource();
				if (webxmlResource != null && webxmlResource.exists()) {
					IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

					final IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry()
							.getDefaultEditor(webxmlResource.getName());
					try {
						page.openEditor(new FileEditorInput(webxmlResource), desc.getId());
					} catch (PartInitException e) {
						Logger.error("Failed to open '" + webxmlResource.getLocation().toString() + "'", e);
					}
				}

			}
		}
		return applicationCreated;
	}

	@Override
	protected void finishPage(IProgressMonitor monitor) throws InterruptedException, CoreException {
		this.applicationPage.createType(monitor);
	}

	@Override
	public IJavaElement getCreatedElement() {
		if (this.applicationPage.getApplicationMode() == JaxrsApplicationCreationWizardPage.APPLICATION_JAVA) {
			return this.applicationPage.getCreatedType();
		}
		return null;
	}

}
