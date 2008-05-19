/**
 * JBoss, a Division of Red Hat
 * Copyright 2006, Red Hat Middleware, LLC, and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
* This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.tools.ws.creation.core.utils;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;





public class WSDLPropertyReader {

	private Definition definition = null;

	public void readWSDL(String filepath) throws WSDLException {

		WSDLFactory wsdlFactory;

		wsdlFactory = WSDLFactory.newInstance();
		WSDLReader reader = wsdlFactory.newWSDLReader();
		definition = reader.readWSDL(filepath);
	}


	/**
	 * get the default package derived by the targetNamespace
	 */
	public String packageFromTargetNamespace(){
		
		String packageName = definition.getTargetNamespace(); 
		String returnPkg = getPackageNameFromNamespce(packageName);
		
		return returnPkg;
		
		
	}

    private static String getPackageNameFromNamespce(String namespace) {

        String hostname = null;
        String path = "";

        try {
            java.net.URL url = new java.net.URL(namespace);

            hostname = url.getHost();
            path = url.getPath();
        } catch (MalformedURLException e) {
            if (namespace.indexOf(":") > -1) {
                hostname = namespace.substring(namespace.indexOf(":") + 1);

                while (hostname.startsWith("/")) {
                    hostname = hostname.substring(1);
                }

                if (hostname.indexOf("/") > -1) {
                    hostname = hostname.substring(0, hostname.indexOf("/"));
                }
            } else {
                hostname = namespace.replace('/','.');
            }
        }

        if (hostname == null || hostname.length() == 0) {
            return null;
        }

        hostname = hostname.replace('-', '_');
        path = path.replace('-', '_');

        path = path.replace(':', '_');

     
        if ((path.length() > 0) && (path.charAt(path.length() - 1) == '/')) {
            path = path.substring(0, path.length() - 1);
        }

   
        StringTokenizer st = new StringTokenizer(hostname, ".:");
        String[] nodes = new String[st.countTokens()];

        for (int i = 0; i < nodes.length; ++i) {
            nodes[i] = st.nextToken();
        }

        StringBuffer sb = new StringBuffer(namespace.length());

        for (int i = nodes.length - 1; i >= 0; --i) {
            appendToPackage(sb, nodes[i], (i == nodes.length - 1));
        }

        StringTokenizer st2 = new StringTokenizer(path, "/");

        while (st2.hasMoreTokens()) {
            appendToPackage(sb, st2.nextToken(), false);
        }
        
        return sb.toString().toLowerCase();
    }

    private static void appendToPackage(StringBuffer sb, String nodeName,
			boolean firstNode) {

		if (JBossWSCreationUtils.isJavaKeyword(nodeName)) {
			nodeName = "_" + nodeName;
		}

		if (!firstNode) {
			sb.append('.');
		}

		if (Character.isDigit(nodeName.charAt(0))) {
			sb.append('_');
		}

		if (nodeName.indexOf('.') != -1) {
			char[] buf = nodeName.toCharArray();

			for (int i = 0; i < nodeName.length(); i++) {
				if (buf[i] == '.') {
					buf[i] = '_';
				}
			}

			nodeName = new String(buf);
		}

		sb.append(nodeName);
	}
    
    
	/**
	 * Returns a list of service names the names are local parts
	 * 
	 * @return
	 */
	public List<String> getServiceList() { 
		
		List<String> returnList = new ArrayList<String>();

		Service service;
		Map serviceMap = definition.getServices();

		if (serviceMap != null && !serviceMap.isEmpty()) {
			Iterator<Service> serviceIterator = serviceMap.values().iterator();
			while (serviceIterator.hasNext()) {

				service = (Service) serviceIterator.next();
				returnList.add(service.getQName().getLocalPart());
			}
		}
		return returnList;
	}
	
	/**
	 * Returns a list of service names the names are local parts
	 * 
	 * @return
	 */
	public List<String> getPortTypeList() { 
		
		List<String> returnList = new ArrayList<String>();

		PortType portType;
		Map portTypeMap = definition.getPortTypes();

		if (portTypeMap != null && !portTypeMap.isEmpty()) {
			Iterator<Service> portTypeIterator = portTypeMap.values().iterator();
			while (portTypeIterator.hasNext()) {

				portType = (PortType) portTypeIterator.next();
				returnList.add(portType.getQName().getLocalPart());
			}
		}
		return returnList;
	}

	/**
	 * Returns a list of ports for a particular service the names are QNames
	 * 
	 * @return
	 */
	public List<String> getPortNameList(QName serviceName) {

		List<String> returnList = new ArrayList<String>();
		Service service = definition.getService(serviceName);
		Port port = null;
		if (service != null) {
			Map portMap = service.getPorts();
			if (portMap != null && !portMap.isEmpty()) {
				Iterator<Port> portIterator = portMap.values().iterator();
				while (portIterator.hasNext()) {
					port = (Port) portIterator.next();
					returnList.add(port.getName());
				}
			}

		}
		return returnList;
	}
	
    

}

