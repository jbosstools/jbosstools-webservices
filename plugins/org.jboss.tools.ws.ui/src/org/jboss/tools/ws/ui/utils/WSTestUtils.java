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

/**
 * Static utility methods for testing JAX-RS and JAX-WS web services
 * @author bfitzpat
 *
 */
public class WSTestUtils {
	
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
