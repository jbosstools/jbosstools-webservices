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
package org.jboss.tools.ws.jaxws.ui.view;

import javax.wsdl.Definition;

import org.jboss.tools.ws.ui.views.CustomTestEntry;

public class JAXWSTestEntry extends CustomTestEntry{
	
	private String action;
	private String body;
	private String operation;
	private String[] nsMessage;
	private Definition wsdlDefintion;
	private String serviceName;
	private String portName;
	private String bindingName;
	private boolean soap12;
	
	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
	public String getOperation() {
		return operation;
	}
	public void setOperation(String operation) {
		this.operation = operation;
	}
	public String[] getNsMessage() {
		return nsMessage;
	}
	public void setNsMessage(String[] nsMessage) {
		this.nsMessage = nsMessage;
	}
	public Definition getWsdlDefintion() {
		return wsdlDefintion;
	}
	public void setWsdlDefintion(Definition wsdlDefintion) {
		this.wsdlDefintion = wsdlDefintion;
	}
	public String getServiceName() {
		return serviceName;
	}
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	public String getPortName() {
		return portName;
	}
	public void setPortName(String portName) {
		this.portName = portName;
	}
	public String getBindingName() {
		return bindingName;
	}
	public void setBindingName(String bindingName) {
		this.bindingName = bindingName;
	}
	public boolean isSoap12() {
		return soap12;
	}
	public void setSoap12(boolean soap12) {
		this.soap12 = soap12;
	}
	@Override
	public JAXWSTestEntry clone() throws CloneNotSupportedException {
		JAXWSTestEntry newEntry = new JAXWSTestEntry();
		newEntry.setAction(this.getAction());
		newEntry.setBindingName(this.getBindingName());
		newEntry.setBody(this.getBody());
		newEntry.setNsMessage(this.getNsMessage());
		newEntry.setOperation(this.getOperation());
		newEntry.setPortName(this.getPortName());
		newEntry.setServiceName(this.getServiceName());
		newEntry.setSoap12(this.isSoap12());
		newEntry.setWsdlDefintion(this.getWsdlDefintion());
		return newEntry;
	}

}
