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

import java.util.LinkedHashSet;
import java.util.Set;

import org.jboss.tools.ws.ui.views.CustomTestEntry;
import org.jboss.tools.ws.ui.views.WSProperty;

public class JAXRSTestEntry extends CustomTestEntry{
	
	private Set<WSProperty> requestParams;
	private Set<WSProperty> requestHeaders;
	private String body;
	
	public Set<WSProperty> getRequestParams() {
		if(requestParams == null) {
			requestParams = new LinkedHashSet<>();
		}
		return requestParams;
	}
	public void setRequestParams(Set<WSProperty> requestParams) {
		this.requestParams = requestParams;
	}
	public Set<WSProperty> getRequestHeaders() {
		if(requestHeaders == null) {
			requestHeaders = new LinkedHashSet<>();
		}
		return requestHeaders;
	}
	public void setRequestHeaders(Set<WSProperty> requestHeaders) {
		this.requestHeaders = requestHeaders;
	}
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
	
	@Override
	public CustomTestEntry clone() throws CloneNotSupportedException {
		JAXRSTestEntry newEntry = new JAXRSTestEntry();
		newEntry.setRequestHeaders(this.getRequestHeaders());
		newEntry.setRequestParams(this.getRequestParams());
		newEntry.setBody(this.getBody());
		return newEntry;
	}

}
