/******************************************************************************* 
 * Copyright (c) 2008 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.ws.ui.utils;

import java.io.File;

/**
 * @author Grid Qian
 */
public class JBossWSUIUtils {

	public static String addAnotherNodeToPath(String currentPath, String newNode) {
		return currentPath + File.separator + newNode;
	}

	public static String addNodesToPath(String currentPath, String[] newNode) {
		String returnPath = currentPath;
		for (int i = 0; i < newNode.length; i++) {
			returnPath = returnPath + File.separator + newNode[i];
		}
		return returnPath;
	}

}