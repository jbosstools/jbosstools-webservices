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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.ws.creation.core.data.ServiceModel;
import org.jboss.tools.ws.ui.messages.JBossWSUIMessages;

public class JBossWSGenerateSampleClassWizardPage extends WizardPage {
	private JBossWSGenerateWizard wizard;
	private Text packageName;
	private Text className;
	private Button checkDefault;

	protected JBossWSGenerateSampleClassWizardPage(String pageName) {
		super(pageName);
		this
				.setTitle(JBossWSUIMessages.JBossWS_GenerateWizard_GenerateSampleClassPage_Title);
		this
				.setDescription(JBossWSUIMessages.JBossWS_GenerateWizard_GenerateSampleClassPage_Description);
	}

	public void createControl(Composite parent) {
		Composite composite = createDialogArea(parent);
		this.wizard = (JBossWSGenerateWizard) this.getWizard();
		new Label(composite, SWT.NONE)
				.setText(JBossWSUIMessages.JBossWS_GenerateWizard_GenerateSampleClassPage_Package_Label);
		packageName = new Text(composite, SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		packageName.setLayoutData(gd);
		packageName.setText(wizard.getPackageName());
		packageName.setEnabled(!wizard.isUseDefaultClassName());
		packageName.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				wizard.setClassName(packageName.getText());
				setPageComplete(isPageComplete());
			}

		});

		new Label(composite, SWT.NONE)
				.setText(JBossWSUIMessages.JBossWS_GenerateWizard_GenerateSampleClassPage_ClassName_Label);
		className = new Text(composite, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		className.setLayoutData(gd);
		className.setText(updateDefaultName());
		className.setEnabled(!wizard.isUseDefaultClassName());
		className.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				wizard.setClassName(className.getText());
				setPageComplete(isPageComplete());
			}

		});

		checkDefault = new Button(composite, SWT.CHECK);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		checkDefault.setLayoutData(gd);
		checkDefault.setSelection(wizard.isUseDefaultClassName());
		checkDefault
				.setText(JBossWSUIMessages.JBossWS_GenerateWizard_WizardPage_CheckButton_Label);
		checkDefault.addSelectionListener(new SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent e) {
			}

			public void widgetSelected(SelectionEvent e) {
				if (checkDefault.getSelection()) {
					checkDefault.setSelection(true);
					packageName.setText(wizard.PACKAGEDEFAULT);
					className.setText(wizard.CLASSDEFAULT);
				} else {
					checkDefault.setSelection(false);
				}
				packageName.setEnabled(!checkDefault.getSelection());
				className.setEnabled(!checkDefault.getSelection());
				wizard.setUseDefaultClassName(!checkDefault.getSelection());
			}

		});
		setControl(composite);

	}

	public boolean isPageComplete() {
		return validate();
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

	private String updateDefaultName () {
		ServiceModel model = wizard.getServiceModel();
		JBossWSGenerateWizardValidator.setServiceModel(model);
		String currentName = wizard.getClassName();
		if (wizard.getProject() == null) {
			return currentName;
		}
		else {
			boolean isDynamicWebProject = false;
			try {
				if (wizard.getProject().getNature("org.eclipse.wst.common.project.facet.core.nature") != null) { //$NON-NLS-1$
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
		IStatus status = JBossWSGenerateWizardValidator.isWSClassValid(testName, wizard.getProject());
		int i = 1;
		while (status != null) {
			testName = currentName + i;
			wizard.setClassName(testName);
			model = wizard.getServiceModel();
			JBossWSGenerateWizardValidator.setServiceModel(model);
			status = JBossWSGenerateWizardValidator.isWSClassValid(testName, wizard.getProject());
			i++;
		}
		return testName;
	}
	
	protected boolean validate() {
		ServiceModel model = wizard.getServiceModel();
		JBossWSGenerateWizardValidator.setServiceModel(model);
		IStatus status = JBossWSGenerateWizardValidator.isWSClassValid(model.getCustomClassName(), wizard.getProject());
		if (status != null) {
			setMessage(status.getMessage(), DialogPage.ERROR);
			return false;
		}
		else {
			setMessage(null);
			return true;
		}
	}
	
	protected void refresh() {
		className.setText(updateDefaultName());
	}
}
