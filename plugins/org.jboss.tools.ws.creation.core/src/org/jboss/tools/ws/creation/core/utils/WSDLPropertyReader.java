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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.command.internal.env.core.common.StatusUtils;





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
		packageName = packageName.substring(packageName.lastIndexOf("/") + 1);
		String returnPkg = "";
		StringTokenizer st = new StringTokenizer(packageName, ".");
		while(st.hasMoreTokens()){
			if("".equals(returnPkg)){
				returnPkg = st.nextToken();
			}else{
				returnPkg = st.nextToken() + "." + returnPkg;
			}
		}
		
		return returnPkg;
		
		
	}

	/**
	 * Returns a list of service names the names are QNames
	 * 
	 * @return
	 */
	public List<QName> getServiceList() { 
		
		List<QName> returnList = new ArrayList<QName>();

		Service service;
		Map serviceMap = definition.getServices();

		if (serviceMap != null && !serviceMap.isEmpty()) {
			Iterator<Service> serviceIterator = serviceMap.values().iterator();
			while (serviceIterator.hasNext()) {

				service = (Service) serviceIterator.next();
				returnList.add(service.getQName());
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

