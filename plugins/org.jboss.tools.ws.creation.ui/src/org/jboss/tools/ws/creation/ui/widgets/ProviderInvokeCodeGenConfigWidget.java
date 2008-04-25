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

package org.jboss.tools.ws.creation.ui.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.command.internal.env.ui.widgets.SimpleWidgetDataContributor;
import org.eclipse.wst.command.internal.env.ui.widgets.WidgetDataEvents;
import org.jboos.tools.ws.creation.core.data.ServiceModel;

public class ProviderInvokeCodeGenConfigWidget extends SimpleWidgetDataContributor {
	
	private ServiceModel model;

	public ProviderInvokeCodeGenConfigWidget(ServiceModel model){
		this.model = model;
		model.setGenWSDL(true);
	}
	
	public WidgetDataEvents addControls( Composite parent, Listener statusListener){
		
		Composite configCom = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);		
		configCom.setLayout(layout);
		configCom.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		final Button wsdlGen = new Button(configCom, SWT.CHECK|SWT.NONE);
		GridData wsdlGenData = new GridData();
		wsdlGenData.horizontalSpan = 2;
		wsdlGen.setLayoutData(wsdlGenData);
		wsdlGen.setText("Generete WSDL file");
		wsdlGen.setSelection(true);		
		wsdlGen.addSelectionListener(new SelectionListener(){

			public void widgetDefaultSelected(SelectionEvent e) {				
				
			}

			public void widgetSelected(SelectionEvent e) {
				model.setGenWSDL(wsdlGen.getSelection());
				
			}
			
		});
		return this;
	}
}
