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

package org.jboss.tools.ws.jaxrs.ui.wizards;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.wizards.NewElementWizard;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.jboss.tools.common.ui.CommonUIImages;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsMetamodelLocator;
import org.jboss.tools.ws.jaxrs.ui.cnf.UriPathTemplateCategory;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;

/**
 * Wizard to create a new JAX-RS Resource with Resource Methods, and optionally
 * a JAX-RS Activator if none already exists.
 * 
 * @author xcoulon
 *
 */
@SuppressWarnings("restriction")
public class JaxrsResourceCreationWizard extends NewElementWizard implements INewWizard {

	/** The JAX-RS Metamodel of the current Java Project. */
	private IJaxrsMetamodel metamodel;

	/** The current selection in the workbench when this wizard was opened. */
	private IStructuredSelection selection;

	/** The JAX-RS Resource settings page. */
	private JaxrsResourceCreationWizardPage resourcePage;

	private JaxrsApplicationCreationWizardPage applicationPage;

	/**
	 * Default constructor.
	 */
	public JaxrsResourceCreationWizard() {
		setDefaultPageImageDescriptor(CommonUIImages.getInstance().getOrCreateImageDescriptor(
				CommonUIImages.WEB_SERVICE_IMAGE));
	}

	@Override
	public void init(final IWorkbench workbench, final IStructuredSelection selection) {
		this.selection = selection;
		final IJavaProject javaProject = getJavaProject(selection);
		if (javaProject != null) {
			try {
				this.metamodel = JaxrsMetamodelLocator.get(javaProject);
			} catch (CoreException e) {
				Logger.error("Failed to retrieve JAX-RS metamodel for Java Project '" + javaProject + "'", e);
			}
		}
	}

	/**
	 * @return the {@link IJavaProject} associated with the given selection, or
	 *         {@code null} if none was found.
	 * @param selection
	 *            the current selection when calling the Wizard.
	 */
	private IJavaProject getJavaProject(final IStructuredSelection selection) {
		final Object selectedElement = selection.getFirstElement();
		if (selectedElement instanceof IJavaElement) {
			return ((IJavaElement) selectedElement).getJavaProject();
		} else if (selectedElement instanceof UriPathTemplateCategory) {
			return ((UriPathTemplateCategory) selectedElement).getJavaProject();
		}
		return null;

	}

	@Override
	public void addPages() {
		resourcePage = new JaxrsResourceCreationWizardPage();
		addPage(resourcePage);
		resourcePage.init(selection);
		applicationPage = new JaxrsApplicationCreationWizardPage(true);
		addPage(applicationPage);
		// skip by default if there's already an application
		if (this.metamodel != null && metamodel.hasApplication()) {
			applicationPage.setApplicationAlreadyExists(true);
		}
		applicationPage.init(selection);
	}

	@Override
	public boolean performFinish() {
		boolean resourceCreated = super.performFinish();
		if (resourceCreated) {
			try {
				JavaUI.openInEditor(resourcePage.getCreatedType());
			} catch (PartInitException e) {
				Logger.error("Failed to open '" + resourcePage.getCreatedType().getFullyQualifiedName() + "'", e);
			} catch (JavaModelException e) {
				Logger.error("Failed to open '" + resourcePage.getCreatedType().getFullyQualifiedName() + "'", e);
			}
		}
		return resourceCreated;
	}

	@Override
	protected void finishPage(IProgressMonitor monitor) throws InterruptedException, CoreException {
		this.resourcePage.createType(monitor);
		if (this.applicationPage != null) {
			this.applicationPage.createType(monitor);
		}
	}

	@Override
	public IJavaElement getCreatedElement() {
		return this.resourcePage.getCreatedType();
	}

}
