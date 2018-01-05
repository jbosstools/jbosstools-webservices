/******************************************************************************* 
 * Copyright (c) 2017 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.ws.jaxrs.ui.view;

import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.jboss.tools.ws.jaxrs.ui.JBossJAXRSUIMessages;
import org.jboss.tools.ws.ui.views.WSProperty;
import org.jboss.tools.ws.ui.views.FormExpansionAdapter;
import org.jboss.tools.ws.ui.views.WebServicesTestView;

/**
 * @since 2.0
 */
public class RequestHeadersAndParamsComposite {
	
	private PropertiesList headersList;
	private PropertiesList parmsList;

	public void createControl(final WebServicesTestView view, Composite parent) {
		ExpandableComposite headersComposite = view.getToolkit().createExpandableComposite(parent,
				ExpandableComposite.TREE_NODE | ExpandableComposite.TITLE_BAR | ExpandableComposite.CLIENT_INDENT);
		headersComposite.setText(JBossJAXRSUIMessages.JAXRSWSTestView2_Headers_Section);

		headersList = new PropertiesList(headersComposite, SWT.NONE);
		headersList.addPropertiesListener(new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				JAXRSTestEntry entry = JAXRSType.getRSTestEntry(view.getCurrentEntry());
				entry.setRequestHeaders((Set<WSProperty>)arg0.data);
				
			}
		});
		headersComposite.setClient(headersList);
		view.getToolkit().adapt(headersList);
		GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, false);
		headersComposite.setLayoutData(gd2);
		headersComposite.addExpansionListener(new FormExpansionAdapter(view));

		ExpandableComposite parametersComposite = view.getToolkit().createExpandableComposite(parent,
				ExpandableComposite.TREE_NODE | ExpandableComposite.TITLE_BAR | ExpandableComposite.CLIENT_INDENT);
		parametersComposite.setText(JBossJAXRSUIMessages.JAXRSWSTestView2_Parameters_Section);

		parmsList = new PropertiesList(parametersComposite, SWT.NONE);
		parmsList.addPropertiesListener(new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				JAXRSTestEntry entry = JAXRSType.getRSTestEntry(view.getCurrentEntry());
				entry.setRequestParams((Set<WSProperty>)arg0.data);
				
			}
		});
		parametersComposite.setClient(parmsList);
		view.getToolkit().adapt(parmsList);
		GridData gd4 = new GridData(SWT.FILL, SWT.FILL, true, false);
		parametersComposite.setLayoutData(gd4);
		parametersComposite.addExpansionListener(new FormExpansionAdapter(view));
	}
	
	public void setHeadersAndParamsValues(JAXRSTestEntry entry) {
		headersList.setProperties(entry.getRequestHeaders());
		parmsList.setProperties(entry.getRequestParams());
	}
}
