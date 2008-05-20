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
import org.jboss.tools.ws.creation.ui.widgets.CodeGenConfigWidget;

public class JBossWSConfigWidgetFactory implements INamedWidgetContributorFactory {

	private SimpleWidgetContributor  servicesXMLSelectWidgetContrib;
	private ServiceModel model;

	public JBossWSConfigWidgetFactory(){	  
	}

	public INamedWidgetContributor getFirstNamedWidget(){
		if( servicesXMLSelectWidgetContrib == null ) init();
		return servicesXMLSelectWidgetContrib;
	}

	public INamedWidgetContributor getNextNamedWidget( INamedWidgetContributor widgetContributor){
		if( servicesXMLSelectWidgetContrib == null ) init();
		INamedWidgetContributor nextWidgetContrib = null;
		return nextWidgetContrib;
	}

	public void registerDataMappings(DataMappingRegistry dataRegistry) 
	{
		// Map the data model from the defaulting command to this widget factory.
		// The framework will actually to the call to getWebServiceDataModel in
		// the ExampleDefaultingCommand class and then call the setWebServiceDataModel
		// method in this class.
		dataRegistry.addMapping( InitialCommand.class, 
		                         "WebServiceDataModel",  //$NON-NLS-1$
								             JBossWSConfigWidgetFactory.class );
		dataRegistry.addMapping(InitialClientCommand.class, "WebServiceDataModel", JBossWSConfigWidgetFactory.class);
	}

	public void setWebServiceDataModel( ServiceModel model ){
		this.model = model;
	}

	private void init(){
		//Pages of Axis2 Web Services Java Bean Scenario
		CodeGenConfigWidget servicesXMLSelectWidget = 
								new CodeGenConfigWidget(model);
		servicesXMLSelectWidgetContrib  = createWidgetContributor( 
				"JBoss Web Service Code Generation Configuration",
				"Please input the appropriate option for the code generation",
				servicesXMLSelectWidget );
	}

	private SimpleWidgetContributor createWidgetContributor(String title, 
															String description, 
															final WidgetDataContributor contributor){
		SimpleWidgetContributor widgetContrib  = new SimpleWidgetContributor();
		widgetContrib.setTitle(title);
		widgetContrib.setDescription(description);
		widgetContrib.setFactory( new WidgetContributorFactory(){
			public WidgetContributor create(){
				return contributor;
			}
		});
		return widgetContrib;
	}   

}
