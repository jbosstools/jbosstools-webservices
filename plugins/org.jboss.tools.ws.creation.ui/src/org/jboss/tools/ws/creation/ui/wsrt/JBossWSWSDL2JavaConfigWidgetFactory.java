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
package org.jboss.tools.ws.creation.ui.wsrt;

import org.eclipse.wst.command.internal.env.core.data.DataMappingRegistry;
import org.eclipse.wst.command.internal.env.ui.widgets.INamedWidgetContributor;
import org.eclipse.wst.command.internal.env.ui.widgets.INamedWidgetContributorFactory;
import org.eclipse.wst.command.internal.env.ui.widgets.SimpleWidgetContributor;
import org.eclipse.wst.command.internal.env.ui.widgets.WidgetContributor;
import org.eclipse.wst.command.internal.env.ui.widgets.WidgetContributorFactory;
import org.eclipse.wst.command.internal.env.ui.widgets.WidgetDataContributor;
import org.jboss.tools.ws.creation.core.commands.InitialClientCommand;
import org.jboss.tools.ws.creation.core.commands.InitialCommand;
import org.jboss.tools.ws.creation.core.data.ServiceModel;
import org.jboss.tools.ws.creation.ui.messages.JBossWSCreationUIMessages;
import org.jboss.tools.ws.creation.ui.widgets.WSDL2JavaCodeGenConfigWidget;

@SuppressWarnings("restriction")
public class JBossWSWSDL2JavaConfigWidgetFactory implements
		INamedWidgetContributorFactory {

	private SimpleWidgetContributor servicesXMLSelectWidgetContrib;
	private ServiceModel model;
	private WSDL2JavaCodeGenConfigWidget servicesXMLSelectWidget;

	public JBossWSWSDL2JavaConfigWidgetFactory() {
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
		dataRegistry.addMapping(InitialCommand.class, JBossWSCreationUIMessages.JBossWS_WebServiceDataModel,JBossWSWSDL2JavaConfigWidgetFactory.class);
		dataRegistry.addMapping(InitialClientCommand.class,JBossWSCreationUIMessages.JBossWS_WebServiceDataModel, JBossWSWSDL2JavaConfigWidgetFactory.class);
	}

	public void setWebServiceDataModel(ServiceModel model) {
		this.model = model;
		if (servicesXMLSelectWidget != null) {
			servicesXMLSelectWidget.setModel(model);
		}
	}

	private void init() {
		servicesXMLSelectWidget = new WSDL2JavaCodeGenConfigWidget(model);
		servicesXMLSelectWidgetContrib = createWidgetContributor(
				JBossWSCreationUIMessages.JBossWSWSDL2JavaConfigWidgetFactory_Title,
				JBossWSCreationUIMessages.JBossWSWSDL2JavaConfigWidgetFactory_Description,
				servicesXMLSelectWidget);
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
