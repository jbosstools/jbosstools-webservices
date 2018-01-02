/******************************************************************************* 
 * Copyright (c) 2010 - 2014 Red Hat, Inc. and others.  
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.jaxws.ui.wizards;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jst.j2ee.project.JavaEEProjectUtilities;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.ws.jaxws.core.data.ServiceModel;
import org.jboss.tools.ws.jaxws.ui.JBossJAXWSUIMessages;
import org.jboss.tools.ws.jaxws.ui.utils.JBossWSCreationUtils;

public class JBossWSGenerateWizardPage extends WizardPage {

	private JBossWSGenerateWizard wizard;
	private Text name;
	private Combo projects;
	private boolean bHasChanged = false;
	private Text packageName;
	private Text className;

	protected JBossWSGenerateWizardPage(String pageName) {
		super(pageName);
		this
				.setTitle(JBossJAXWSUIMessages.JBossWS_GenerateWizard_GenerateWizardPage_Title);
		this
				.setDescription(JBossJAXWSUIMessages.JBossWS_GenerateWizard_GenerateWizardPage_Description);
	}

	public void createControl(Composite parent) {
		Composite composite = createDialogArea(parent);
		this.wizard = (JBossWSGenerateWizard) this.getWizard();

		Group group = new Group(composite, SWT.NONE);
		group
				.setText(JBossJAXWSUIMessages.JBossWS_GenerateWizard_GenerateWizardPage_Project_Group);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(gd);

		projects = new Combo(group, SWT.BORDER | SWT.DROP_DOWN);
		projects
				.setToolTipText(JBossJAXWSUIMessages.JBossWS_GenerateWizard_GenerateWizardPage_Project_Group_Tooltip);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		projects.setLayoutData(gd);
		refreshProjectList(wizard.getServiceModel().getWebProjectName());

		projects.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				wizard.setProject(projects.getText());
				name.setText(updateDefaultName());
				className.setText(updateDefaultClassName());
				bHasChanged = true;
				setPageComplete(isPageComplete());
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});

		Group group2 = new Group(composite, SWT.NONE);
		group2
				.setText(JBossJAXWSUIMessages.JBossWS_GenerateWizard_GenerateWizardPage_Web_Service_Group);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		group2.setLayout(new GridLayout(2, false));
		group2.setLayoutData(gd);

		new Label(group2, SWT.NONE)
				.setText(JBossJAXWSUIMessages.JBossWS_GenerateWizard_GenerateWizardPage_ServiceName_Label);
		name = new Text(group2, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		name.setLayoutData(gd);
		name.setText(updateDefaultName());
		name.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				wizard.setServiceName(name.getText());
				bHasChanged = true;
				setPageComplete(isPageComplete());
			}

		});

		Group group3 = new Group(composite, SWT.NONE);
		group3.setText(JBossJAXWSUIMessages.JBossWS_GenerateWizard_GenerateWizardPage_Class_Group);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		group3.setLayout(new GridLayout(2, false));
		group3.setLayoutData(gd);
		
		new Label(group3, SWT.NONE)
				.setText(JBossJAXWSUIMessages.JBossWS_GenerateWizard_GenerateWizardPage_Package_Label);
		packageName = new Text(group3, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		packageName.setLayoutData(gd);
		packageName.setText(wizard.getPackageName());
		packageName.addModifyListener(new ModifyListener() {
		
			public void modifyText(ModifyEvent e) {
				wizard.setPackageName(packageName.getText());
				setPageComplete(isPageComplete());
			}
		
		});
		
		new Label(group3, SWT.NONE)
				.setText(JBossJAXWSUIMessages.JBossWS_GenerateWizard_GenerateWizardPage_ClassName_Label);
		className = new Text(group3, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		className.setLayoutData(gd);
		className.setText(updateDefaultClassName());
		className.addModifyListener(new ModifyListener() {
		
			public void modifyText(ModifyEvent e) {
				wizard.setClassName(className.getText());
				setPageComplete(isPageComplete());
			}
		
		});

		setControl(composite);
	}

	private void refreshProjectList(String projectName) {
		String[] projectNames = getProjects();
		boolean foundInitialProject = false;
		projects.removeAll();
		for (int i = 0; i < projectNames.length; i++) {
			projects.add(projectNames[i]);
			if (projectNames[i].equals(projectName)) {
				foundInitialProject = true;
			}
		}
		if (foundInitialProject)
			projects.setText(projectName);
	}

	public IWizardPage getNextPage() {
		return super.getNextPage();
	}

	private Composite createDialogArea(Composite parent) {
		// create a composite with standard margins and spacing
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 7;
		layout.marginWidth = 7;
		layout.verticalSpacing = 4;
		layout.horizontalSpacing = 4;
		layout.numColumns = 2;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		return composite;
	}

	@Override
	public boolean isPageComplete() {
		return validate();
	}

	private String updateDefaultName() {
		ServiceModel model = wizard.getServiceModel();
		JBossWSGenerateWizardValidator.setServiceModel(model);
		String currentName = wizard.getServiceName();
		IStatus status = JBossWSGenerateWizardValidator.isWSNameValid();
		try {
			if (status.getSeverity() == IStatus.ERROR
					&& !JavaEEProjectUtilities.isDynamicWebProject(wizard
							.getProject())) {
				return currentName;
			}
		} catch (NullPointerException npe) {
			return currentName;
		}
		String testName = currentName;
		int i = 1;
		while (status != null) {
			testName = currentName + i;
			wizard.setServiceName(testName);
			model = wizard.getServiceModel();
			JBossWSGenerateWizardValidator.setServiceModel(model);
			status = JBossWSGenerateWizardValidator.isWSNameValid();
			i++;
		}
		return testName;
	}
	
	private String updateDefaultClassName() {
		ServiceModel model = wizard.getServiceModel();
		JBossWSGenerateWizardValidator.setServiceModel(model);
		String currentName = wizard.getClassName();
		if (wizard.getProject() == null) {
			return currentName;
		} else {
			boolean isDynamicWebProject = false;
			try {
				if (wizard.getProject().getNature(
						"org.eclipse.wst.common.project.facet.core.nature") != null) { //$NON-NLS-1$
					isDynamicWebProject = true;
				}
			} catch (CoreException e) {
				// ignore
			}
			if (!isDynamicWebProject) {
				return currentName;
			}
		}
		String testName = currentName;
		IStatus status = JBossWSGenerateWizardValidator.isWSClassValid(
				testName, wizard.getProject());
		int i = 1;
		while (status != null && status.getSeverity() == IStatus.ERROR) {
			testName = currentName + i;
			wizard.setClassName(testName);
			model = wizard.getServiceModel();
			JBossWSGenerateWizardValidator.setServiceModel(model);
			status = JBossWSGenerateWizardValidator.isWSClassValid(testName,
					wizard.getProject());
			i++;
		}
		return testName;
	}


	private boolean validate() {
		ServiceModel model = wizard.getServiceModel();
		JBossWSGenerateWizardValidator.setServiceModel(model);
		if (!projects.isDisposed() && projects.getText().length() > 0) {
			model.setWebProjectName(projects.getText());
		}

		if (((JBossWSGenerateWizard) this.getWizard()).getProject() == null) {
			setErrorMessage(JBossJAXWSUIMessages.Error_JBossWS_GenerateWizard_NoProjectSelected);
			return false;
		}

		IFile web = ((JBossWSGenerateWizard) this.getWizard()).getWebFile();
		if (web == null || !web.exists()) {
			setErrorMessage(JBossJAXWSUIMessages.Error_JBossWS_GenerateWizard_NotDynamicWebProject);
			return false;
		}

		try {
			if (""	.equals(JBossWSCreationUtils.getJavaProjectSrcLocation(((JBossWSGenerateWizard) this.getWizard()).getProject()))) { //$NON-NLS-1$
				setErrorMessage(JBossJAXWSUIMessages.Error_JBossWS_GenerateWizard_NoSrcInProject);
				return false;
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}

		IStatus status = JBossWSGenerateWizardValidator.isWSNameValid();
		if (status != null) {
			setErrorMessage(status.getMessage());
			return false;
		} 
		
		IStatus classNameStatus = JBossWSGenerateWizardValidator.isWSClassValid(model
				.getCustomClassName(), wizard.getProject());
		if (classNameStatus != null) {
			if (classNameStatus.getSeverity() == IStatus.ERROR) {
				setMessage(classNameStatus.getMessage(), DialogPage.ERROR);
				return false;
			} else if (classNameStatus.getSeverity() == IStatus.WARNING) {
				setMessage(classNameStatus.getMessage(), DialogPage.WARNING);
				return true;
			}
		} 
		
		setMessage(JBossJAXWSUIMessages.JBossWS_GenerateWizard_GenerateWizardPage_Description);
		setErrorMessage(null);
		return true;
	}

	private String[] getProjects() {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		ArrayList<String> dynamicProjects = new ArrayList<String>();
		for (int i = 0; i < projects.length; i++) {
			boolean isDynamicWebProject = JavaEEProjectUtilities
					.isDynamicWebProject(projects[i]);
			if (isDynamicWebProject) {
				dynamicProjects.add(projects[i].getName());
			}
		}
		return dynamicProjects.toArray(new String[dynamicProjects.size()]);
	}

	protected boolean hasChanged() {
		return bHasChanged;
	}
}
