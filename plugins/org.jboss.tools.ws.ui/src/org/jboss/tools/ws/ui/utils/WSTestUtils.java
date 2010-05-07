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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.axis.Message;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.message.SOAPEnvelope;

/**
 * Static utility methods for testing JAX-RS and JAX-WS web services
 * @author bfitzpat
 *
 */
public class WSTestUtils {
	
	private static Map<?, ?> requestHeaders = null;
	private static Map<?, ?> resultHeaders = null;
	private static String EMPTY_STRING = ""; //$NON-NLS-1$

	public static Map<?, ?> getResultHeaders() {
		return WSTestUtils.resultHeaders;
	}
	
	public static Map<?, ?> getRequestHeaders() {
		return WSTestUtils.requestHeaders;
	}
	
	/*
	 * Start building the web query. Append parameters to URL
	 */
	private static String buildWebQuery(Map<String, String> parameters) throws Exception {
		if (!parameters.isEmpty()) {
	        StringBuilder sb = new StringBuilder();
	        for (Map.Entry<String, String> entry : parameters.entrySet()) {
	            String key = URLEncoder.encode(entry.getKey(), "UTF-8"); //$NON-NLS-1$
	            String value = URLEncoder.encode(entry.getValue(), "UTF-8"); //$NON-NLS-1$
	            sb.append(key).append("=").append(value).append("&"); //$NON-NLS-1$ //$NON-NLS-2$
	        }
	        return sb.toString().substring(0, sb.length() - 1);
		}
		return EMPTY_STRING;
    }

	// static simple call, pass in url, parms, and headers
    public static String callRestfulWebService(String address, Map<String, String> parameters, Map<String, String> headers) throws Exception {
        return callRestfulWebService(address, parameters, headers, "GET", null, null, 0); //$NON-NLS-1$
    }

    // static call - provide url, parms, headers, the method type, and request body
    public static String callRestfulWebService(String address, Map<String, String> parameters, Map<String, String> headers, String methodType, String requestBody) throws Exception {
        return callRestfulWebService(address, parameters, headers, methodType, requestBody, null, 0);
    }

    // static call - provide url, parms, headers, the method type, request body, proxy string, and port # as string
    public static String callRestfulWebService(String address, Map<String, String> parameters, Map<String, String> headers, String methodType, String requestBody, String proxy, String port) throws Exception {
        return callRestfulWebService(address, parameters, headers, methodType, requestBody, proxy, Integer.parseInt(port));
    }

    // static call - provide url, parms, headers, the method type, request body, proxy string, port #
    public static String callRestfulWebService(String address, Map<String, String> parameters, Map<String, String> headers, String methodType, String requestBody, String proxy, int port) throws Exception {

    	// handle the proxy
        Proxy proxyObject = null;
        if (proxy != null && proxy.length() > 0 && port > 0) {
            InetSocketAddress proxyAddress = new InetSocketAddress(proxy, port);
            proxyObject = new Proxy(Proxy.Type.HTTP, proxyAddress);
        }

        // clear the returned results
        String response = EMPTY_STRING;
        
        // get the parms string
        String query = buildWebQuery(parameters);

        // build the complete URL
        URL url = null;
        if (query != null) {
        	// add the ? if there are parameters
            if (!address.endsWith("?") && methodType.equalsIgnoreCase("GET") ) {  //$NON-NLS-1$//$NON-NLS-2$
            	address = address + "?"; //$NON-NLS-1$
            }
        	// add parms to the url if we have some
        	url = new URL(address + query);
        } else {
        	url = new URL(address);
        }

        // make connection
        HttpURLConnection httpurlc = null;
        if (proxyObject == null) {
            httpurlc = (HttpURLConnection) url.openConnection();
        } else {
        	// if have proxy, pass it along
            httpurlc = (HttpURLConnection) url.openConnection(proxyObject);
        }
        
        // since we are expecting output back, set to true
       	httpurlc.setDoOutput(true);
        
        // not sure what this does - may be used for authentication?
        httpurlc.setAllowUserInteraction(false);
        
        // set whether this is a GET or POST
        httpurlc.setRequestMethod(methodType);
        
        // if we have headers to add
        if (headers != null && !headers.isEmpty()) {
        	Iterator<?> iter = headers.entrySet().iterator();
        	while (iter.hasNext()) {
        		Entry<?, ?> entry = (Entry<?, ?>)iter.next();
        		if (entry.getKey() != null && entry.getKey() instanceof String)
        			httpurlc.addRequestProperty((String) entry.getKey(), (String) entry.getValue());
        	}
        }
        
        requestHeaders = httpurlc.getRequestProperties();
        
        // CONNECT!
        httpurlc.connect();

        // If we are doing a POST and we have some request body to pass along, do it
        if (requestBody != null && ( methodType.equalsIgnoreCase("POST")  //$NON-NLS-1$
        		|| methodType.equalsIgnoreCase("PUT"))) { //$NON-NLS-1$
        	requestBody = stripNLsFromXML(requestBody);
        	OutputStreamWriter out = new OutputStreamWriter(httpurlc.getOutputStream());
        	out.write(requestBody);
        	out.close();
        }

        // retrieve result and put string results into the response
        InputStream is = (InputStream) httpurlc.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));//$NON-NLS-1$
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
            sb.append("\n");//$NON-NLS-1$
        }
        br.close();
        response = sb.toString();
        
        resultHeaders = httpurlc.getHeaderFields();
        
        // disconnect explicitly (may not be necessary)
        httpurlc.disconnect();

        return response;
    }	

	/*
	 * Invokes the WS and returns a result
	 */
	public static String invokeWS( String endpointurl, String actionurl, String body )  throws Exception {
		
		/* 
		 * the endpoint & action urls + the soap in are what we
		 * need to invoke the WS
		 */
		String endpoint = endpointurl;
		String action = actionurl;
		String soapIn = body;	

    	/* Use AXIS to call the WS */
		String document = stripNLsFromXML(soapIn);
		Service service = new Service();
		Call call= (Call) service.createCall();
		call.setTargetEndpointAddress( new java.net.URL(endpoint) );
		call.setOperationStyle( org.apache.axis.constants.Style.MESSAGE );
		if ( action != null ) {
		    call.setProperty(Call.SOAPACTION_USE_PROPERTY,Boolean.TRUE);
		    call.setProperty(Call.SOAPACTION_URI_PROPERTY,action);
		}
		Message message = new Message(document);
		SOAPEnvelope envelope = call.invoke( message );
		System.out.println(envelope.getHeaders().toString());
		
		String cleanedUp = stripNLsFromXML(envelope.getBody().toString());
		return cleanedUp;
	}
	
	public static String addNLsToXML( String incoming ) {
		String outgoing = null;
		if (incoming != null) {
			outgoing = incoming.replaceAll("><",">\n<");//$NON-NLS-1$ //$NON-NLS-2$
		}
		return outgoing;
	}
	
	public static String stripNLsFromXML ( String incoming ) {
		String outgoing = null;
		if (incoming != null) {
			outgoing = incoming.replaceAll(">\n<","><");//$NON-NLS-1$ //$NON-NLS-2$
			if (outgoing.contains("\n"))//$NON-NLS-1$ 
				outgoing.replaceAll("\n"," ");//$NON-NLS-1$ //$NON-NLS-2$
			if (outgoing.contains("\r"))//$NON-NLS-1$ 
				outgoing.replaceAll("\r"," ");//$NON-NLS-1$ //$NON-NLS-2$
		}
		return outgoing;
	}
}
