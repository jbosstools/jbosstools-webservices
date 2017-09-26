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
package org.jboss.tools.ws.ui.views;

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Status;

public class WSTestStatus extends Status {

	private String resultsText;
	private Map<String, List<String>> headers;
	
	public WSTestStatus(int severity, String pluginId, String message) {
		super(severity, pluginId, message);
	}

	public String getResultsText() {
		return resultsText;
	}

	public void setResultsText(String resultsText) {
		this.resultsText = resultsText;
	}

	/**
	 * @since 2.0
	 */
	public Map<String, List<String>> getHeaders() {
		return headers;
	}

	/**
	 * @since 2.0
	 */
	public void setHeaders(Map<String, List<String>> headers) {
		this.headers = headers;
	}
}
