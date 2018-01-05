/******************************************************************************* 
 * Copyright (c) 2008 - 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.ws.jaxws.core.test.util;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.AbstractConsole;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleListener;
import org.eclipse.ui.console.IConsoleManager;

public class JBossJAXWSCoreTestUtils {

	public static IJavaProject getJavaProjectByName(String projectName)
			throws JavaModelException {

		IJavaModel model = JavaCore.create(ResourcesPlugin.getWorkspace()
				.getRoot());
		model.open(null);

		IJavaProject[] projects = model.getJavaProjects();

		for (IJavaProject proj : projects) {
			if (proj.getProject().getName().equals(projectName)) {
				return proj;
			}
		}

		return null;
	}

	public static IConsoleManager getConsoleManager() {
		IConsoleManager consolemanager = ConsolePlugin.getDefault()
				.getConsoleManager();
		consolemanager.addConsoleListener(new IConsoleListener() {
			public void consolesAdded(IConsole[] consoles) {
				for (int i = 0; i < consoles.length; i++) {
					((AbstractConsole) consoles[i]).activate();
				}

			}
			public void consolesRemoved(IConsole[] consoles) {
				for (int i = 0; i < consoles.length; i++) {
					((AbstractConsole) consoles[i]).destroy();
				}

			}
		});
		return consolemanager;
	}
	
	public static void delay(long durationInMilliseconds) {
		Display display = Display.getCurrent();
		if (display != null) {
			long t2 = System.currentTimeMillis() + durationInMilliseconds;
			while (System.currentTimeMillis() < t2) {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}
			display.update();
		} else {
			try {
				Thread.sleep(durationInMilliseconds);
			} catch (InterruptedException e) {
			}
		}
	}
}
