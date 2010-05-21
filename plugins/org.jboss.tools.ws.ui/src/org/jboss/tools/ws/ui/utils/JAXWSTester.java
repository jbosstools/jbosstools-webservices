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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.soap.MimeHeader;
import javax.xml.soap.MimeHeaders;

import org.apache.axis.AxisFault;
import org.apache.axis.Message;
import org.apache.axis.MessageContext;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.message.SOAPEnvelope;

/**
 * Tester class for JAX-WS services
 * @author bfitzpat
 *
 */
public class JAXWSTester {
	
	// the response message to pass back
	private String resultBody;
	
	// the result HTTP headers to pass back
	private HashMap<String, String> resultHeaders;
	
	// utility constant
	private static final String EMPTY_STRING = ""; //$NON-NLS-1$
	
	/**
	 * Constructor 
	 */
	public JAXWSTester() {
		// empty
	}
	
	/**
	 * Return the response message
	 * @return 
	 */
	public String getResultBody() {
		return this.resultBody;
	}
	
	/**
	 * Return a map of HTTP headers from the response
	 * @return
	 */
	public Map<String, String> getResultHeaders() {
		return this.resultHeaders;
	}
	
	/**
	 * Invoke the JAX-WS service
	 * @param endpointurl
	 * @param actionurl
	 * @param body
	 * @throws Exception
	 */
	public void doTest( String endpointurl, String actionurl, String body ) throws Exception {
		/* 
		 * the endpoint & action urls + the soap in are what we
		 * need to invoke the WS
		 */
		String endpoint = endpointurl;
		String action = actionurl;
		String soapIn = body;	

    	/* Use AXIS to call the WS */
		String document = WSTestUtils.stripNLsFromXML(soapIn);
		Service service = new Service();
		Call call= (Call) service.createCall();
		call.setTargetEndpointAddress( new java.net.URL(endpoint) );
		call.setOperationStyle( org.apache.axis.constants.Style.MESSAGE );
		if ( action != null ) {
		    call.setProperty(Call.SOAPACTION_USE_PROPERTY,Boolean.TRUE);
		    call.setProperty(Call.SOAPACTION_URI_PROPERTY,action);
		}
		Message message = new Message(document);
		
		SOAPEnvelope envelope = null;
		
		this.resultBody = EMPTY_STRING;

		try {
			envelope = call.invoke( message );

			// Get back the response message
			if (envelope != null && envelope.getBody() != null) {		
				this.resultBody = envelope.getBody().toString();
			}
			
			// Get back the response HTTP headers and pass back as a Map
			if (call != null && call.getMessageContext() != null) {
				MessageContext mc = call.getMessageContext();
				if (mc.getMessage() != null && mc.getMessage().getMimeHeaders() != null) {
					MimeHeaders mh = mc.getMessage().getMimeHeaders();
					Iterator<?> iter = mh.getAllHeaders();
					resultHeaders = new HashMap<String, String>();
					while (iter.hasNext()) {
						MimeHeader next = (MimeHeader)iter.next();
						resultHeaders.put(next.getName(), next.getValue());
					}
				}
			}
		} catch (AxisFault fault){

			// Get back the response message
			if (fault.getFaultString() != null) {		
				this.resultBody = fault.getFaultString();
			}

			// Get back the response HTTP headers and pass back as a Map
			if (fault.getHeaders() != null && !fault.getHeaders().isEmpty()) {
				Iterator<?> iter = fault.getHeaders().iterator();
				resultHeaders = new HashMap<String, String>();
				while (iter.hasNext()) {
					Object next = iter.next();
					resultHeaders.put(next.toString(), ""); //$NON-NLS-1$
				}
			} else 	if (call != null && call.getMessageContext() != null) {
				MessageContext mc = call.getMessageContext();
				if (mc.getMessage() != null && mc.getMessage().getMimeHeaders() != null) {
					MimeHeaders mh = mc.getMessage().getMimeHeaders();
					Iterator<?> iter = mh.getAllHeaders();
					resultHeaders = new HashMap<String, String>();
					while (iter.hasNext()) {
						MimeHeader next = (MimeHeader)iter.next();
						resultHeaders.put(next.getName(), next.getValue());
					}
				}
			}

		}
		
	}
}