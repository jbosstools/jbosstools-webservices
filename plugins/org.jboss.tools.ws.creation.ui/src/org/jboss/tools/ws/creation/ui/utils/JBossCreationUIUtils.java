/******************************************************************************* 
 * Copyright (c) 2008 - 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.ws.creation.ui.utils;

import java.util.Iterator;
import java.util.List;

import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.ws.creation.core.data.ServiceModel;
import org.jboss.tools.ws.creation.core.messages.JBossWSCreationCoreMessages;

public class JBossCreationUIUtils {

	public static Combo createComboItem(Composite configCom, ServiceModel model, String label, String tooltip) {
		JBossCreationUIUtils.createLabel(configCom,label,tooltip);
		return JBossCreationUIUtils.createCombo(configCom, model,tooltip);
	}
    
	public static void createLabel(Composite configCom, String label, String tooltip) {
        final Label srcDirLabel = new Label(configCom, SWT.NONE);
        srcDirLabel.setText(label);
        srcDirLabel.setToolTipText(tooltip);
	}

	public static Combo createCombo(Composite parent, final ServiceModel model, String tooltip) {
		Combo combo = new Combo(parent, SWT.READ_ONLY);
        combo.setToolTipText(tooltip);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		combo.setLayoutData(gd);
        return combo;
	}
	
    public static boolean populateServiceCombo(Combo serviceCombo, Definition definition) {
    	serviceCombo.removeAll();
		if (definition != null && definition.getServices() != null && !definition.getServices().isEmpty()) {
			Iterator<?> iter = definition.getServices().values().iterator();
			boolean hasServicePort = false;
			while (iter.hasNext()) {
				Service service = (Service) iter.next();
				StringBuffer serviceName = new StringBuffer(service.getQName().getLocalPart()).append("#"); //$NON-NLS-1$
				if (service.getPorts() != null && !service.getPorts().isEmpty()){
					Iterator<?> innerIter = service.getPorts().values().iterator();
					while (innerIter.hasNext()) {
						serviceName.append(((Port)innerIter.next()).getName());
						if (innerIter.hasNext()) {
							serviceName.append(","); //$NON-NLS-1$
						}
					}
					hasServicePort = true;
				}
				serviceCombo.add(serviceName.toString());
				serviceCombo.setData(serviceName.toString(), service);
			}
			if (hasServicePort) {
				serviceCombo.select(0);
				return true;
			}
		} 
		return false;
    }
    
    public static boolean populateSourceFolderCombo(Combo outputDirCombo, List<String> list) {
		outputDirCombo.removeAll();
		if (list != null && list.size() > 0) {
			for (int i=0; i < list.size(); i++) {
				outputDirCombo.add(list.get(i));
			}
			outputDirCombo.select(0);
			return true;
		} else {
			return false;
		}
    }

	public static Text createTextItem(Composite configCom, ServiceModel model,
			String label, String tooltip) {
		JBossCreationUIUtils.createLabel(configCom,label,tooltip);
		return JBossCreationUIUtils.createText(configCom, model,tooltip);
	}

	public static Text createText(Composite parent, ServiceModel model,
			String tooltip) {
		Text text = new Text(parent, SWT.NONE);
		text.setToolTipText(tooltip);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		text.setLayoutData(gd);
        return text;
	}

	public static void populateTargetCombo(Combo targetCombo, ServiceModel model) {
		targetCombo.add(JBossWSCreationCoreMessages.Value_Target_0, 0);
		targetCombo.add(JBossWSCreationCoreMessages.Value_Target_1, 1);
		targetCombo.add(JBossWSCreationCoreMessages.Value_Target_2, 2);
		if (JBossWSCreationCoreMessages.Value_Target_0.equals(model.getTarget())) {
			targetCombo.select(0);
		} else if (JBossWSCreationCoreMessages.Value_Target_1.equals(model.getTarget())) {
			targetCombo.select(1);
		} else {
			targetCombo.select(2);
		}
	}

	public static Text createTextItemWithButton(Composite configCom, ServiceModel model, String label, String tooltip) {
		JBossCreationUIUtils.createLabel(configCom,label,tooltip);
		Text text = JBossCreationUIUtils.createTextWithButton(configCom, tooltip);	
		return text;
	}

	public static Text createTextWithButton(Composite configCom, String tooltip) {
		Text text = new Text(configCom, SWT.BORDER);
		text.setToolTipText(tooltip);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));	
		return text; 
	}

	public static Button createCheckButton(Composite configCom,
			String label) {
		Button button = new Button(configCom, SWT.CHECK);
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		button.setLayoutData(gd);
		button.setText(label);
		return button;
	}

}
