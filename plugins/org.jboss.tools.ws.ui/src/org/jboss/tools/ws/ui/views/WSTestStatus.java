package org.jboss.tools.ws.ui.views;

import org.eclipse.core.runtime.Status;

public class WSTestStatus extends Status {

	private String resultsText;
	private String headersList;
	
	public WSTestStatus(int severity, String pluginId, String message) {
		super(severity, pluginId, message);
	}

	public String getResultsText() {
		return resultsText;
	}

	public void setResultsText(String resultsText) {
		this.resultsText = resultsText;
	}

	public String getHeadersList() {
		return headersList;
	}

	public void setHeadersList(String headersList) {
		this.headersList = headersList;
	}
}
