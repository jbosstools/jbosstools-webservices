/******************************************************************************* 
 * Copyright (c) 2010 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.ws.ui.wizards;

import java.io.File;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jst.j2ee.project.JavaEEProjectUtilities;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.jboss.tools.ws.creation.core.commands.MergeWebXMLCommand;
import org.jboss.tools.ws.creation.core.commands.ServiceSampleCreationCommand;
import org.jboss.tools.ws.creation.core.data.ServiceModel;
import org.jboss.tools.ws.ui.messages.JBossWSUIMessages;

public class JBossWSGenerateWizard extends Wizard implements INewWizard {

	private JBossWSGenerateWebXmlWizardPage firstPage;
	private JBossWSGenerateSampleClassWizardPage secondPage;

	String NAMEDEFAULT = "HelloWorld"; //$NON-NLS-1$
	String PACKAGEDEFAULT = "org.jboss.samples.webservices"; //$NON-NLS-1$
	String CLASSDEFAULT = "HelloWorld"; //$NON-NLS-1$

	private String serviceName = NAMEDEFAULT;
	private String packageName = PACKAGEDEFAULT;
	private String className = CLASSDEFAULT;
	private boolean useDefaultServiceName = true;
	private boolean useDefaultClassName = true;
	
	private IStructuredSelection selection;
	private IProject project;
	private static String WEB = "web.xml"; //$NON-NLS-1$
	private File webFile;
	private boolean hasInited = false;

	public JBossWSGenerateWizard() {
		super();
		super.setWindowTitle(JBossWSUIMessages.JBossWS_GenerateWizard_Title);
		super.setHelpAvailable(false);
	}

	public void addPages() {
		super.addPages();
		firstPage = new JBossWSGenerateWebXmlWizardPage("first"); //$NON-NLS-1$
		secondPage = new JBossWSGenerateSampleClassWizardPage("second"); //$NON-NLS-1$
		addPage(firstPage);
		addPage(secondPage);
	}

	@Override
	public boolean performFinish() {
		if (canFinish()) {
			ServiceModel model = new ServiceModel();
			model.setWebProjectName(project.getName());
			model.addServiceClasses(new StringBuffer().append(
					getPackageName())
					.append(".").append(getClassName()).toString()); //$NON-NLS-1$
			model.setServiceName(getServiceName());
			model.setUpdateWebxml(true);
			model.setCustomPackage(getPackageName());
			try {
				new MergeWebXMLCommand(model).execute(null, null);
				new ServiceSampleCreationCommand(model).execute(null, null);
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		return true;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public boolean isUseDefaultServiceName() {
		return useDefaultServiceName;
	}

	public void setUseDefaultServiceName(boolean useDefaultServiceName) {
		this.useDefaultServiceName = useDefaultServiceName;
	}

	public boolean isUseDefaultClassName() {
		return useDefaultClassName;
	}

	public void setUseDefaultClassName(boolean useDefaultClassName) {
		this.useDefaultClassName = useDefaultClassName;
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;

		if (this.selection.getFirstElement() instanceof JavaProject) {
			project = ((JavaProject)this.selection.getFirstElement()).getProject();
		}
		else if (this.selection.getFirstElement() instanceof IProject) {
			project = (IProject) this.selection.getFirstElement();
		}
		if (project != null
				&& JavaEEProjectUtilities.isDynamicWebProject(project)) {
			webFile = findFileByPath(project.getLocation().toOSString());
			if (webFile != null) {
				// TODO: if this is valid, we can finish
			}
		}
		hasInited = true;
	}

	@Override
	public boolean canFinish() {
		if (hasInited && webFile == null ) {
			if (firstPage != null && !firstPage.getControl().isDisposed()) {
				firstPage.setErrorMessage(JBossWSUIMessages.JBossWS_GenerateWizard_NotDynamicWebProject_Error);
			}
			return false;
		}
		return super.canFinish();
	}

	private File findFileByPath(String path) {
		File ret = null;
		File folder = new File(path);
		if (folder.isDirectory()) {
			File[] files = folder.listFiles();
			for (File file : files) {
				ret = findFileByPath(file.getAbsolutePath());
				if (ret != null) {
					break;
				}
			}
		} else {
			if (WEB.equals(folder.getName())) {
				ret = folder;
			}
		}
		return ret;
	}

}
