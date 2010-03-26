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

package org.jboss.tools.ws.creation.ui.wsrt;

import org.eclipse.wst.command.internal.env.core.data.DataMappingRegistry;
import org.eclipse.wst.command.internal.env.ui.widgets.INamedWidgetContributor;
import org.eclipse.wst.command.internal.env.ui.widgets.INamedWidgetContributorFactory;
import org.eclipse.wst.command.internal.env.ui.widgets.SimpleWidgetContributor;
import org.eclipse.wst.command.internal.env.ui.widgets.WidgetContributor;
import org.eclipse.wst.command.internal.env.ui.widgets.WidgetContributorFactory;
import org.eclipse.wst.command.internal.env.ui.widgets.WidgetDataContributor;
import org.jboss.tools.ws.creation.core.commands.InitialCommand;
import org.jboss.tools.ws.creation.core.data.ServiceModel;
import org.jboss.tools.ws.creation.ui.messages.JBossWSCreationUIMessages;
import org.jboss.tools.ws.creation.ui.widgets.Java2WSDLCodeGenConfigWidget;

/**
 * @author Grid Qian
 */
@SuppressWarnings("restriction")
public class JBossWSJava2WSDLConfigWidgetFactory implements
		INamedWidgetContributorFactory {

	private SimpleWidgetContributor servicesXMLSelectWidgetContrib;
	private ServiceModel model;

	public JBossWSJava2WSDLConfigWidgetFactory() {
	}

	public INamedWidgetContributor getFirstNamedWidget() {
		if (servicesXMLSelectWidgetContrib == null)
			init();
		return servicesXMLSelectWidgetContrib;
	}

	public INamedWidgetContributor getNextNamedWidget(
			INamedWidgetContributor widgetContributor) {
		if (servicesXMLSelectWidgetContrib == null)
			init();
		INamedWidgetContributor nextWidgetContrib = null;
		return nextWidgetContrib;
	}

	public void registerDataMappings(DataMappingRegistry dataRegistry) {
		// Map the data model from the defaulting command to this widget
		// factory.
		// The framework will actually to the call to getWebServiceDataModel in
		// the ExampleDefaultingCommand class and then call the
		// setWebServiceDataModel
		// method in this class.
		dataRegistry.addMapping(InitialCommand.class, JBossWSCreationUIMessages.JBossWS_WebServiceDataModel,
				JBossWSJava2WSDLConfigWidgetFactory.class);
	}

	public void setWebServiceDataModel(ServiceModel model) {
		this.model = model;
	}

	private void init() {
		// Pages of JBossWS Java2WSDL
		Java2WSDLCodeGenConfigWidget java2WSDLWidget = new Java2WSDLCodeGenConfigWidget(
				model);
		servicesXMLSelectWidgetContrib = createWidgetContributor(
				JBossWSCreationUIMessages.JBossWSJava2WSDLConfigWidgetFactory_Title,
				JBossWSCreationUIMessages.JBossWSJava2WSDLConfigWidgetFactory_Description,
				java2WSDLWidget);
	}

	private SimpleWidgetContributor createWidgetContributor(String title,
			String description, final WidgetDataContributor contributor) {
		SimpleWidgetContributor widgetContrib = new SimpleWidgetContributor();
		widgetContrib.setTitle(title);
		widgetContrib.setDescription(description);
		widgetContrib.setFactory(new WidgetContributorFactory() {
			public WidgetContributor create() {
				return contributor;
			}
		});
		return widgetContrib;
	}

}
