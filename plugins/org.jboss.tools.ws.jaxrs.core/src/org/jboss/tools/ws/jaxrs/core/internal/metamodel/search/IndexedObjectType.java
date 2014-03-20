/******************************************************************************* 
 * Copyright (c) 2008 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.ws.jaxrs.core.internal.metamodel.search;

/**
 * Enumeration to define the types of JAX-RS objects that can be indexed and
 * which prefix to use in the identifier, to avoid collision of ids.
 * 
 * @author xcoulon
 * 
 */
public enum IndexedObjectType {
	
	JAX_RS_ELEMENT("element:"), JAX_RS_ENDPOINT("endpoint:"), PROBLEM_MARKER("marker:");
	
	private final String prefix;
	
	private IndexedObjectType(final String prefix) {
		this.prefix = prefix;
	}

	/**
	 * @return the prefix
	 */
	public String getPrefix() {
		return prefix;
	}

}
