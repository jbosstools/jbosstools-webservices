/*******************************************************************************
 * Copyright (c) 2007 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.ws.creation.ui.project.facet;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.wst.common.frameworks.datamodel.DataModelEvent;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.frameworks.datamodel.IDataModelListener;
import org.eclipse.wst.common.project.facet.ui.AbstractFacetWizardPage;
import org.eclipse.wst.common.project.facet.ui.IFacetWizardPage;
import org.jboss.tools.ws.core.classpath.JbossWSRuntime;
import org.jboss.tools.ws.core.facet.delegate.IJBossWSFacetDataModelProperties;
import org.jboss.tools.ws.core.utils.JbossWSCoreUtils;
import org.jboss.tools.ws.ui.preferences.JbossRuntimeListFieldEditor;

/**
 * @author Dennyxu
 * 
 */
public class JBossWSFacetInstallPage extends AbstractFacetWizardPage implements
		IFacetWizardPage, IDataModelListener {

	private Button btnServerSupplied;
	private Button btnUserSupplied;
	private Combo cmbRuntimes;
	private Button btnDeploy;
	private Button btnNew;
	
	private IDataModel model;	
	
	public JBossWSFacetInstallPage() {
		super("JBossWS Facet");
		setTitle("JBossWS Facet");
		setDescription("Select JBossWS Web Service runtime");
	}

	public void setConfig(Object config) {
		this.model = (IDataModel)config;
		
	}

	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		Composite composite = new Composite(parent, SWT.NONE);
		

		GridLayout gridLayout = new GridLayout(4, false);
		composite.setLayout(gridLayout);
		
		btnServerSupplied = new Button(composite, SWT.RADIO);
		btnServerSupplied.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				setServerSuppliedSelection(e);
			}			
		});
		GridData gd = new GridData();

		gd.horizontalSpan = 1;
		btnServerSupplied.setLayoutData(gd);
		
		Label lblServerSupplied = new Label(composite, SWT.NONE);
		lblServerSupplied.addMouseListener(new MouseAdapter(){
			public void mouseDown(MouseEvent e) {
				btnServerSupplied.setSelection(true);
				setServerSuppliedSelection(e);
			}
		});
		lblServerSupplied.setText("Server Supplied JBossWS Runtime");
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		lblServerSupplied.setLayoutData(gd);
		
		btnUserSupplied = new Button(composite, SWT.RADIO);
		btnUserSupplied.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				setUserSuppliedSelection(e);
			}			
		});
		
		
		cmbRuntimes = new Combo(composite, SWT.READ_ONLY);
		initializeRuntimesCombo(cmbRuntimes);
		cmbRuntimes.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		cmbRuntimes.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				String runtimeName = cmbRuntimes.getText();				
				JbossWSRuntime jr = (JbossWSRuntime)cmbRuntimes.getData(runtimeName);
				model.setStringProperty(IJBossWSFacetDataModelProperties.JBOSS_WS_RUNTIME_HOME, jr.getHomeDir());
				model.setStringProperty(IJBossWSFacetDataModelProperties.JBOSS_WS_RUNTIME_ID, runtimeName);
			}
		});
		
		btnDeploy = new Button(composite, SWT.CHECK);
		btnDeploy.setText("Deploy");
		btnDeploy.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				model.setBooleanProperty(IJBossWSFacetDataModelProperties.JBOSS_WS_DEPLOY, btnDeploy.getSelection());
			}
		});
		
		btnNew = new Button(composite, SWT.NONE);
		btnNew.setText("New...");
		btnNew.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				newJBossWSRuntime();
			}
		});
		
		setControl(composite);
		
				
		
		
	}

	protected void setServerSuppliedSelection(EventObject e) {
		btnUserSupplied.setSelection(false);
		model.setBooleanProperty(IJBossWSFacetDataModelProperties.JBOSS_WS_RUNTIME_IS_SERVER_SUPPLIED, true);
		enableUserSupplied(false);
		
	}
	
	protected void setUserSuppliedSelection(EventObject e) {
		btnServerSupplied.setSelection(false);
		model.setBooleanProperty(IJBossWSFacetDataModelProperties.JBOSS_WS_RUNTIME_IS_SERVER_SUPPLIED, false);
		String runtimeId = cmbRuntimes.getText();
		model.setStringProperty(IJBossWSFacetDataModelProperties.JBOSS_WS_RUNTIME_ID, runtimeId);
		enableUserSupplied(true);
		
	}
	
	protected void enableUserSupplied(boolean enabled){
		cmbRuntimes.setEnabled(enabled);
		btnDeploy.setEnabled(enabled);
		btnNew.setEnabled(enabled);
		
	}

	protected void initializeRuntimesCombo(Combo cmRuntime){
		Map<String, JbossWSRuntime> mpRuntimes = JbossWSCoreUtils.getJbossWSRutntimeMap();
		int i = -1; 
		for(String key: mpRuntimes.keySet()){
			i++;
			JbossWSRuntime jr = mpRuntimes.get(key);
			cmRuntime.add(jr.getName());
			cmRuntime.setData(jr.getName(), jr.getHomeDir());
			
			//get default jbossws runtime
			if(jr.isDefault()){
				cmRuntime.select(i);
			}
		}
	}
	
	/*
	 * create a new jbossws runtime and set  user supplied runtime to the new one
	 */
	protected void newJBossWSRuntime(){
		List<JbossWSRuntime> exists = new ArrayList<JbossWSRuntime>();
		List<JbossWSRuntime> added = new ArrayList<JbossWSRuntime>();
		exists.addAll(JbossWSCoreUtils.getJbossWSRutntimeMap().values());
		JbossRuntimeListFieldEditor.JbossWSRuntimeNewWizard newRtwizard = new JbossRuntimeListFieldEditor.JbossWSRuntimeNewWizard(exists, added);
		WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), newRtwizard);
		if(dialog.open() == dialog.OK){
			initializeRuntimesCombo(cmbRuntimes);
			cmbRuntimes.select(cmbRuntimes.getItemCount() - 1);
		}
	}
	
	public void propertyChanged(DataModelEvent event) {
		
	}

}