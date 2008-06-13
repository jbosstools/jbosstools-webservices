package org.jboss.tools.ws.creation.ui.startup;

import org.eclipse.ui.IStartup;

public class SetDefaultWebServiceServerRuntime implements IStartup {

	public void earlyStartup() {
		org.eclipse.jst.ws.internal.consumption.ui.plugin.WebServiceConsumptionUIPlugin.getInstance().getPluginPreferences().setDefault("PREFERENCE_SERVER", "org.eclipse.jst.server.generic.jboss42");
		org.eclipse.jst.ws.internal.consumption.ui.plugin.WebServiceConsumptionUIPlugin.getInstance().getPluginPreferences().setDefault("PREFERENCE_RUNTIME", "org.jboss.tools.ws.creation.jbossWebServiceRT");
		
	}

}
