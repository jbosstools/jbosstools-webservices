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

import org.eclipse.jface.wizard.IWizardPage;
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
import org.jboss.tools.ws.ui.messages.JBossWSUIMessages;

public class JBossWSGenerateWebXmlWizardPage extends WizardPage {
	private JBossWSGenerateWizard wizard;
    private Text name;
    private Button checkDefault;
    
    
	protected JBossWSGenerateWebXmlWizardPage(String pageName) {
		super(pageName);
		this.setTitle(JBossWSUIMessages.JBossWS_GenerateWizard_GenerateWebXmlPage_Title);
		this.setDescription(JBossWSUIMessages.JBossWS_GenerateWizard_GenerateWebXmlPage_Description);
	}

	public void createControl(Composite parent) {
		Composite composite = createDialogArea(parent);
		this.wizard = (JBossWSGenerateWizard)this.getWizard();
		new Label (composite, SWT.NONE).setText(JBossWSUIMessages.JBossWS_GenerateWizard_GenerateWebXmlPage_ServiceName_Label);
		name = new Text(composite, SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		name.setLayoutData(gd);
		name.setText(wizard.getServiceName());
		name.setEnabled(!wizard.isUseDefaultServiceName());
		name.addModifyListener(new ModifyListener(){

			public void modifyText(ModifyEvent e) {
				if (!"".equals(name.getText())){ //$NON-NLS-1$
					setPageComplete(true);
				} else {
					setPageComplete(false);
				}
			}
			
		});
		
		
		checkDefault = new Button(composite, SWT.CHECK);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		checkDefault.setLayoutData(gd);
		checkDefault.setSelection(wizard.isUseDefaultServiceName());
		checkDefault.setText(JBossWSUIMessages.JBossWS_GenerateWizard_WizardPage_CheckButton_Label);
		checkDefault.addSelectionListener(new SelectionListener(){

			public void widgetDefaultSelected(SelectionEvent e) {
			}

			public void widgetSelected(SelectionEvent e) {
				if (checkDefault.getSelection()){
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
	
	public IWizardPage getNextPage(){
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
}
