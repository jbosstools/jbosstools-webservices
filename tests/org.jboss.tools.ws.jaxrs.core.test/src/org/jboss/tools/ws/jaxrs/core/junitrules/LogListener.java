package org.jboss.tools.ws.jaxrs.core.junitrules;

import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;

public class LogListener implements ILogListener {
	
	private boolean errorOccurred = false; 
	
	private final String pluginId;
	
	public LogListener(final String pluginId) {
		this.pluginId = pluginId;
	}
	public boolean isErrorOccurred() {
		return errorOccurred;
	}
	@Override
	public void logging(IStatus status, String plugin) {
		if(status.getSeverity() == IStatus.ERROR && plugin.equals(pluginId)) {
			errorOccurred = true;
		}
	}
}