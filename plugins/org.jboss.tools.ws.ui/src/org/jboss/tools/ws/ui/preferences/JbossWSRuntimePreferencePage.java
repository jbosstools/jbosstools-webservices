/******************************************************************************* 
 * Copyright (c) 2008 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 

package org.jboss.tools.ws.ui.preferences;

import java.io.File;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jboss.tools.ws.core.JbossWSCorePlugin;
import org.jboss.tools.ws.ui.JbossWSUIMessages;
import org.jboss.tools.ws.ui.JbossWSUIPlugin;
import org.jboss.tools.ws.ui.UIUtils;

public class JbossWSRuntimePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

		private Text jbosswsPath; 
		private Text statusLabel;
		private Combo aarExtensionCombo; 
		private Combo serviceDatabindingCombo;
		private Combo clientDatabindingCombo;
		private Button generateServerSideInterfaceCheckBoxButton;
		private Button generateAllCheckBoxButton;
		private Button syncAndAsyncRadioButton;
		private Button syncOnlyRadioButton;
		private Button asyncOnlyRadioButton;
		private Button clientTestCaseCheckBoxButton;
		private Button clientGenerateAllCheckBoxButton;

		protected Control createContents(Composite superparent) {
			
			IPreferenceStore ps = JbossWSCorePlugin.getDefault().getPreferenceStore();
			this.setPreferenceStore(ps);
			
			UIUtils uiUtils = new UIUtils(JbossWSUIPlugin.PLUGIN_ID);
			final Composite  mainComp = uiUtils.createComposite(superparent, 1);
			
			TabFolder jbosswsPreferenceTab = new TabFolder(mainComp, SWT.WRAP);
			jbosswsPreferenceTab.setLayoutData( new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_BOTH) );

			
			//-----------------------------Axis2 Runtime Location Group------------------------------//
			TabItem runtimeInstalLocationItem = new TabItem(jbosswsPreferenceTab, SWT.WRAP);
			runtimeInstalLocationItem.setText(JbossWSUIMessages.JBOSSWS_RUNTIME);
			runtimeInstalLocationItem.setToolTipText(JbossWSUIMessages.JBOSSWS_RUNTIME_TOOLTIP);
			
			Composite runtimeTab = uiUtils.createComposite(jbosswsPreferenceTab, 1);
			runtimeTab.setLayoutData( new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_BOTH ) );
			Composite runtimeGroup = uiUtils.createComposite(runtimeTab, 3);

			runtimeInstalLocationItem.setControl(runtimeTab);
			runtimeTab.setToolTipText(JbossWSUIMessages.JBOSSWS_RUNTIME_TOOLTIP);

			jbosswsPath = uiUtils.createText(runtimeGroup, JbossWSUIMessages.JBOSSWS_RUNTIME_LOCATION , null, null , SWT.BORDER);
			
			Button browseButton = uiUtils.createPushButton(runtimeGroup, JbossWSUIMessages.LABEL_BROUSE, null, null);
			browseButton.addSelectionListener( new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					handleBrowse(mainComp.getShell());
				}     
			}); 

			jbosswsPath.addModifyListener( new ModifyListener(){
				public void modifyText(ModifyEvent e){
					statusUpdate(runtimeExist(jbosswsPath.getText()));
					// runtimeTab.layout();
				}
			});
			new org.eclipse.swt.widgets.Label(runtimeTab, SWT.HORIZONTAL);  // Leave some vertical space.
			statusLabel = new Text(runtimeTab, SWT.BACKGROUND | SWT.READ_ONLY | SWT.CENTER | SWT.WRAP | SWT.H_SCROLL);
			statusLabel.setLayoutData( new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.FILL_BOTH) );

			//--------------------------------jbossws Runtime Preferences------------------------------//

			TabItem codegenPreferencesItem = new TabItem(jbosswsPreferenceTab, SWT.WRAP);
			codegenPreferencesItem.setText(JbossWSUIMessages.JBOSSWS_PREFERENCES);
			codegenPreferencesItem.setToolTipText(JbossWSUIMessages.JBOSSWS_PREFERENCES_TOOLTIP);

			Composite codegenGroup = uiUtils.createComposite(jbosswsPreferenceTab, 1);
			codegenPreferencesItem.setControl(codegenGroup);
			codegenGroup.setToolTipText(JbossWSUIMessages.JBOSSWS_PREFERENCES_TOOLTIP);

			///////////////////////////////////////////////////////////////////////////////////////////

			//Service Codegen Options
			Composite serviceCodegenGroup = uiUtils.createComposite(codegenGroup, 1);

			Text serviceCodegenGroupLabel= new Text(serviceCodegenGroup, SWT.READ_ONLY |SWT.WRAP);
			serviceCodegenGroupLabel.setText(JbossWSUIMessages.LABEL_WEB_SERVICE_CODEGEN);

			Composite dataBindComp = uiUtils.createComposite(serviceCodegenGroup, 2);
			//Data binding
			serviceDatabindingCombo = uiUtils.createCombo(dataBindComp, JbossWSUIMessages.LABEL_DATABINDING, null, null, SWT.READ_ONLY);

			//the server side interface option
			generateServerSideInterfaceCheckBoxButton = uiUtils.createCheckbox(serviceCodegenGroup, JbossWSUIMessages.LABEL_GENERATE_SERVERSIDE_INTERFACE, null, null);

			// generate all
			generateAllCheckBoxButton = uiUtils.createCheckbox(serviceCodegenGroup, JbossWSUIMessages.LABEL_GENERATE_ALL, null,null);

			uiUtils.createHorizontalSeparator(codegenGroup,2);
			///////////////////////////////////////////////////////////////////////////////////////////

			///Client Codegen Options
			Composite clientCodegenGroup = uiUtils.createComposite(codegenGroup, 1);
			Text clientCodegenGroupLabel= new Text(clientCodegenGroup, SWT.READ_ONLY);
			clientCodegenGroupLabel.setText(JbossWSUIMessages.LABEL_WEB_SERVICE_CLIENT_CODEGEN);

			Group clientModeRadioComp = uiUtils.createGroup(clientCodegenGroup, JbossWSUIMessages.LABEL_CLIENT_SIDE, null, null);

			//client side buttons
			syncAndAsyncRadioButton = uiUtils.createRadioButton(clientModeRadioComp, JbossWSUIMessages.LABEL_SYNC_AND_ASYNC, null, null);
			syncOnlyRadioButton 	= uiUtils.createRadioButton(clientModeRadioComp,JbossWSUIMessages.LABEL_SYNC, null, null);
			asyncOnlyRadioButton 	= uiUtils.createRadioButton(clientModeRadioComp, JbossWSUIMessages.LABEL_ASYNC, null, null);

			Composite dataBind = uiUtils.createComposite(clientCodegenGroup, 2);
			clientDatabindingCombo = uiUtils.createCombo(dataBind, JbossWSUIMessages.LABEL_DATABINDING, null, null, SWT.READ_ONLY);

			// generate test case option
			clientTestCaseCheckBoxButton = uiUtils.createCheckbox(clientCodegenGroup, JbossWSUIMessages.LABEL_GENERATE_TESTCASE_CAPTION, null, null);

			// generate all
			clientGenerateAllCheckBoxButton = uiUtils.createCheckbox(clientCodegenGroup, JbossWSUIMessages.LABEL_GENERATE_ALL, null, null);


			uiUtils.createHorizontalSeparator(codegenGroup,2);

			///////////////////////////////////////////////////////////////////////////////////////////

			///AAR Options
			Composite aarGroup = uiUtils.createComposite(codegenGroup,1);

			Text arrGroupLabel= new Text(aarGroup, SWT.READ_ONLY);
			arrGroupLabel.setText(JbossWSUIMessages.LABEL_WEB_SERVICE_AAR);

			Composite aarExtGroup = uiUtils.createComposite(aarGroup,2);

			//aar extention 
			aarExtensionCombo = uiUtils.createCombo(aarExtGroup, JbossWSUIMessages.LABEL_AAR_EXTENTION, null, null, SWT.READ_ONLY );

			initializeValues();
			jbosswsPreferenceTab.setEnabled(true);
			jbosswsPreferenceTab.setVisible(true);
			return mainComp;
		}

		public void init(IWorkbench workbench) {
		}


		/**
		 * Pops up the file browse dialog box
		 */
		private void handleBrowse(Shell parent) {
			DirectoryDialog fileDialog = new DirectoryDialog(parent);
			String fileName = fileDialog.open();
			if (fileName != null) {
				jbosswsPath.setText(fileName);
			}
		}

		private void statusUpdate(boolean status){
			if(statusLabel != null){
				if(!jbosswsPath.getText().equals("")){
					if (status) {
						statusLabel.setText(JbossWSUIMessages.LABEL_JBOSSWS_RUNTIME_LOAD);
					} else {
						statusLabel.setText(JbossWSUIMessages.LABEL_JBOSSWS_RUNTIME_LOAD_ERROR);
					}
				}else{
					statusLabel.setText(JbossWSUIMessages.LABEL_JBOSSWS_RUNTIME_NOT_EXIT);
				}
			}
		}

		private boolean runtimeExist(String path){

			File jbosswsHomeDir = new File(path);
			if (!jbosswsHomeDir.isDirectory()) 
				return false;

//			String axis2LibPath = Axis2CoreUtils.addAnotherNodeToPath(axis2HomeDir.getAbsolutePath(), "lib");
//			String axis2WebappPath = Axis2CoreUtils.addAnotherNodeToPath(axis2HomeDir.getAbsolutePath(), "webapp");
//			if (new File(axis2LibPath).isDirectory() && new File(axis2WebappPath).isDirectory()) 
//				return true;
//			else {
//				String axis2WarPath = Axis2CoreUtils.addAnotherNodeToPath( path, "axis2.war");
//				if (new File(axis2WarPath).isFile()) {
//					RuntimePropertyUtils.writeWarStausToPropertiesFile(true);
//					return true;
//				} else 				
//					return false;
//			}
			return true;
		}

		private void storeValues(){
			IPreferenceStore store =  this.getPreferenceStore();
			System.out.println(jbosswsPath.getText());
			store.setValue("jbosswsruntimelocation", jbosswsPath.getText());
			
			
//			// set values in the persistent context 
//			Axis2EmitterContext context = WebServiceAxis2CorePlugin.getDefault().getAxisEmitterContext();
//			context.setAxis2RuntimeLocation( axis2Path.getText());
//			RuntimePropertyUtils.writeServerPathToPropertiesFile(axis2Path.getText());
//			context.setServiceDatabinding(serviceDatabindingCombo.getItem(serviceDatabindingCombo.getSelectionIndex()));
//			context.setServiceInterfaceSkeleton( generateServerSideInterfaceCheckBoxButton.getSelection());
//			context.setServiceGenerateAll(generateAllCheckBoxButton.getSelection());
//			context.setAsync(asyncOnlyRadioButton.getSelection());
//			context.setSync(syncOnlyRadioButton.getSelection());
//			context.setClientDatabinding(clientDatabindingCombo.getItem(clientDatabindingCombo.getSelectionIndex()));
//			context.setClientTestCase(clientTestCaseCheckBoxButton.getSelection());
//			context.setClientGenerateAll(clientGenerateAllCheckBoxButton.getSelection());
//			context.setAarExtention(aarExtensionCombo.getText());
		}

		/**
		 * Initializes states of the controls using default values
		 * in the preference store.
		 */
		private void initializeDefaults() {
			IPreferenceStore preferenceStore = getPreferenceStore();
			jbosswsPath.setText(preferenceStore.getDefaultString("jbosswsruntimelocation"));
//			aarExtensionCombo.select(0);
//			serviceDatabindingCombo.select(0);
//			clientDatabindingCombo.select(0);
//			generateServerSideInterfaceCheckBoxButton.setSelection(Axis2EmitterDefaults.isServiceInterfaceSkeleton());
//			generateAllCheckBoxButton.setSelection(Axis2EmitterDefaults.isServiceGenerateAll());
//			syncAndAsyncRadioButton.setSelection(((Axis2EmitterDefaults.isClientSync() || Axis2EmitterDefaults.isClientAsync())==false)?true:
//				(Axis2EmitterDefaults.isClientSync()) && Axis2EmitterDefaults.isClientAsync());
//			syncOnlyRadioButton.setSelection( Axis2EmitterDefaults.isClientSync() && !Axis2EmitterDefaults.isClientAsync());
//			asyncOnlyRadioButton.setSelection(
//					Axis2EmitterDefaults.isClientAsync() && !Axis2EmitterDefaults.isClientSync());
//
//			clientTestCaseCheckBoxButton.setSelection(Axis2EmitterDefaults.isClientTestCase());
//			clientGenerateAllCheckBoxButton.setSelection(Axis2EmitterDefaults.isClientGenerateAll());
//			axis2Path.setText(Axis2EmitterDefaults.getAxis2RuntimeLocation());
		}

		private void initializeValues()
		{
			IPreferenceStore preferenceStore = getPreferenceStore();
			jbosswsPath.setText(preferenceStore.getString("jbosswsruntimelocation"));
//			Axis2EmitterContext context = WebServiceAxis2CorePlugin.getDefault().getAxisEmitterContext();
//
//			String[] databindingItems = {context.getServiceDatabinding().toUpperCase()};
//			serviceDatabindingCombo.setItems(databindingItems);
//			serviceDatabindingCombo.select(0);
//			clientDatabindingCombo.setItems(databindingItems);
//			clientDatabindingCombo.select(0);
//
//			generateServerSideInterfaceCheckBoxButton.setSelection( context.isServiceInterfaceSkeleton());
//			generateAllCheckBoxButton.setSelection(context.isServiceGenerateAll());
//
//			syncAndAsyncRadioButton.setSelection(((context.isSync() || context.isAsync())==false) ?true
//					:(context.isSync()) && context.isAsync());
//			syncOnlyRadioButton.setSelection(context.isSync() && !context.isAsync() );
//			asyncOnlyRadioButton.setSelection(context.isAsync() && !context.isSync());
//
//			clientTestCaseCheckBoxButton.setSelection(context.isClientTestCase());
//			clientGenerateAllCheckBoxButton.setSelection(context.isClientGenerateAll());
//
//			String[] aarExtentionItems = { Axis2Constants.AAR };
//			aarExtensionCombo.setItems(aarExtentionItems);
//			aarExtensionCombo.select(0);
//			
//			String serverPath = context.getAxis2RuntimeLocation();
//			if ( serverPath != null){
//				axis2Path.setText(serverPath);
//				statusUpdate(runtimeExist(serverPath));
//				RuntimePropertyUtils.writeWarStausToPropertiesFile(false);
//			}
//			else
//				statusUpdate(false);
		}

		/**
		 * Default button has been pressed.
		 */
		protected void performDefaults() {
			super.performDefaults();
			initializeDefaults();
		}

		/**
		 * Apply button has been pressed.
		 */
		protected void performApply() {
			performOk();
		}

		/**
		 * OK button has been pressed.
		 */	
		public boolean performOk() {
			storeValues();
			return true;
		}

	}
