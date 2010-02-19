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

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.ws.creation.core.data.ServiceModel;
import org.jboss.tools.ws.ui.messages.JBossWSUIMessages;

public class JBossWSGenerateWebXmlWizardPage extends WizardPage {

	private JBossWSGenerateWizard wizard;
	private Text name;
	private Combo projects;
	private Button checkDefault;
	private boolean bHasChanged = false;

	protected JBossWSGenerateWebXmlWizardPage(String pageName) {
		super(pageName);
		this
				.setTitle(JBossWSUIMessages.JBossWS_GenerateWizard_GenerateWebXmlPage_Title);
		this
				.setDescription(JBossWSUIMessages.JBossWS_GenerateWizard_GenerateWebXmlPage_Description);
	}

	public void createControl(Composite parent) {
		Composite composite = createDialogArea(parent);
		this.wizard = (JBossWSGenerateWizard) this.getWizard();
		
		Group group = new Group (composite, SWT.NONE);
		group.setText(JBossWSUIMessages.JBossWSGenerateWebXmlWizardPage_Project_Group);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(gd);
		
		projects = new Combo(group, SWT.BORDER | SWT.DROP_DOWN);
		projects.setToolTipText(JBossWSUIMessages.JBossWSGenerateWebXmlWizardPage_Project_Group_Tooltip);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		projects.setLayoutData(gd);
		refreshProjectList(wizard.getServiceModel().getWebProjectName());
		
		projects.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				wizard.setProject(projects.getText());
				name.setText(updateDefaultName());
				wizard.getSecondPage().refresh();
				bHasChanged = true;
				setPageComplete(isPageComplete());
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
		
		Group group2 = new Group (composite, SWT.NONE);
		group2.setText(JBossWSUIMessages.JBossWSGenerateWebXmlWizardPage_Web_Service_Group);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		group2.setLayout(new GridLayout(2, false));
		group2.setLayoutData(gd);

		new Label(group2, SWT.NONE)
				.setText(JBossWSUIMessages.JBossWS_GenerateWizard_GenerateWebXmlPage_ServiceName_Label);
		name = new Text(group2, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		name.setLayoutData(gd);
		name.setText(updateDefaultName());
		name.setEnabled(!wizard.isUseDefaultServiceName());
		name.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				wizard.setServiceName(name.getText());
				bHasChanged = true;
				setPageComplete(isPageComplete());
			}

		});

		checkDefault = new Button(group2, SWT.CHECK);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		checkDefault.setLayoutData(gd);
		checkDefault.setSelection(wizard.isUseDefaultServiceName());
		checkDefault
				.setText(JBossWSUIMessages.JBossWS_GenerateWizard_WizardPage_CheckButton_Label);
		checkDefault.addSelectionListener(new SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent e) {
			}

			public void widgetSelected(SelectionEvent e) {
				if (checkDefault.getSelection()) {
					checkDefault.setSelection(true);
					name.setText(wizard.NAMEDEFAULT);
				} else {
					checkDefault.setSelection(false);
				}
				name.setEnabled(!checkDefault.getSelection());
				wizard.setUseDefaultServiceName(!checkDefault.getSelection());
			}

		});
		
		setControl(composite);
	}

	private void refreshProjectList ( String projectName ) {
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
		wizard.setServiceName(name.getText());
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
	
	private String updateDefaultName () {
		ServiceModel model = wizard.getServiceModel();
		JBossWSGenerateWizardValidator.setServiceModel(model);
		String currentName = wizard.getServiceName();
		IStatus status = JBossWSGenerateWizardValidator.isWSNameValid();
		try {
			if (status.getSeverity() == IStatus.ERROR && !JavaEEProjectUtilities.isDynamicWebProject(wizard.getProject())) {
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
	
	private boolean validate() {
		ServiceModel model = wizard.getServiceModel();
		JBossWSGenerateWizardValidator.setServiceModel(model);
		if (!projects.isDisposed() && projects.getText().length() > 0) {
			model.setWebProjectName(projects.getText());
		}
		IStatus status = JBossWSGenerateWizardValidator.isWSNameValid();
		if (status != null) {
			setErrorMessage(status.getMessage());
			return false;
		}
		else {
			setErrorMessage(null);
			return true;
		}
	}
	
	private String[] getProjects() {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		ArrayList<String> dynamicProjects = new ArrayList<String>();
		for (int i = 0; i < projects.length; i++) {
			boolean isDynamicWebProject = JavaEEProjectUtilities.isDynamicWebProject(projects[i]);
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
