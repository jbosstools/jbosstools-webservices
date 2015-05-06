/******************************************************************************* 
 * Copyright (c) 2015 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.websockets.ui.internal.ca;

import org.eclipse.jface.viewers.StyledString;

/**
 * 
 * @author Viacheslav Kabanovich
 *
 */
public class WebsocketMethodInfo {
	String methodName;
	String[] paramTypes;
	String[] paramNames;
	String annotation;
	StyledString displayName;
	String info;

	public WebsocketMethodInfo(
			String methodName, 
			String[] paramTypes, 
			String[] paramNames,
			String annotation, 
			StyledString displayName, 
			String info) {
		this.methodName = methodName;
		this.paramTypes = paramTypes;
		this.paramNames = paramNames;
		this.annotation = annotation;
		this.displayName = displayName;
		this.info = info;
	}
}
