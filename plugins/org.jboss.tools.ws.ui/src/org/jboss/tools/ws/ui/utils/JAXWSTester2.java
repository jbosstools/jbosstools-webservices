/******************************************************************************* 
 * Copyright (c) 2010 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.ui.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.Service.Mode;
import javax.xml.ws.handler.MessageContext;

import org.apache.axis.message.SOAPEnvelope;

/**
 * Test a JAX-WS web service using the JAX-WS API
 * @author bfitzpat
 *
 */
public class JAXWSTester2 {

	// the response message to pass back
	private String resultBody;
	
	private SOAPEnvelope resultSOAP;
	
	private SOAPBody resultSOAPBody;
	
	// the result HTTP headers to pass back
	private Map<String, String> resultHeaders;
	
	// utility constant
	private static final String EMPTY_STRING = ""; //$NON-NLS-1$

	/**
	 * Return the response message
	 * @return 
	 */
	public String getResultBody() {
		return this.resultBody;
	}
	
	public SOAPBody getResultSOAPBody() {
		return this.resultSOAPBody;
	}
	
	/**
	 * Return a map of HTTP headers from the response
	 * @return
	 */
	public Map<String, String> getResultHeaders() {
		return this.resultHeaders;
	}
	
	public SOAPEnvelope getResultSOAP(){
		return this.resultSOAP;
	}
	
	/**
	 * Invoke the JAX-WS service
	 * @param endpointurl
	 * @param actionurl
	 * @param body
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public void doTest( String endpointurl, String actionurl, String ns, 
			String serviceName, String messageName, String body ) throws Exception {
		
		this.resultBody = EMPTY_STRING;

		URL serviceURL = new URL (endpointurl); //"http://www.ecubicle.net/gsearch_rss.asmx"
		QName serviceQName = new QName (ns, serviceName); // "http://www.ecubicle.net/webservices", "gsearch_rss"
		Service s = Service.create(serviceURL, serviceQName);
		
		QName messageQName = new QName(ns, messageName); //"http://www.ecubicle.net/webservices", "gsearch_rssSoap"
		Dispatch<SOAPMessage> d = s.createDispatch(messageQName, SOAPMessage.class, Mode.MESSAGE);
		d.getRequestContext().put(BindingProvider.SOAPACTION_URI_PROPERTY, actionurl); //"http://www.ecubicle.net/webservices/GetSearchResults");
		
		MessageFactory mf = MessageFactory.newInstance();
		SOAPMessage m = mf.createMessage( null, new ByteArrayInputStream(body.getBytes()));
		m.saveChanges();
		
		SOAPMessage o = d.invoke(m);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		o.writeTo(baos);
		this.resultBody = baos.toString();
		this.resultSOAPBody = o.getSOAPBody();
		
		if (d.getResponseContext() != null) {
			Object responseHeaders = d.getResponseContext().get(MessageContext.HTTP_RESPONSE_HEADERS);
			if ( responseHeaders != null && responseHeaders instanceof Map) {
				this.resultHeaders = (Map<String, String>) responseHeaders;
			}
		}
	}
}
