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

package org.jboss.tools.ws.jaxrs.ui.internal.utils;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.jboss.tools.ws.jaxrs.ui.JBossJaxrsUIPlugin;

/**
 * A logger wrapper utility for classes in the current bundle only.
 * 
 * @author xcoulon
 * 
 */
public final class Logger {

	/** The debug name, matching the .options file. */
	private static final String DEBUG = JBossJaxrsUIPlugin.PLUGIN_ID + "/debug";

	/**
	 * The private constructor of the static class.
	 */
	private Logger() {
	}

	/**
	 * Logs a message with an 'error' severity.
	 * 
	 * @param message
	 *            the message to log
	 * @param t
	 *            the throwable cause
	 */
	public static void error(String message) {
		JBossJaxrsUIPlugin.getDefault().getLog().log(new Status(Status.ERROR, JBossJaxrsUIPlugin.PLUGIN_ID, message));
	}

	/**
	 * Logs a message with an 'error' severity.
	 * 
	 * @param message
	 *            the message to log
	 * @param t
	 *            the throwable cause
	 */
	public static void error(final String message, final Throwable t) {
		JBossJaxrsUIPlugin.getDefault().getLog()
				.log(new Status(Status.ERROR, JBossJaxrsUIPlugin.PLUGIN_ID, message, t));
	}

	/**
	 * Logs a message with a 'warning' severity.
	 * 
	 * @param message
	 *            the message to log
	 */
	public static void warn(final String message) {
		JBossJaxrsUIPlugin.getDefault().getLog().log(new Status(Status.WARNING, JBossJaxrsUIPlugin.PLUGIN_ID, message));
	}

	public static void warn(String message, Throwable cause) {
		JBossJaxrsUIPlugin.getDefault().getLog()
				.log(new Status(Status.WARNING, JBossJaxrsUIPlugin.PLUGIN_ID, message, cause));
	}

	public static void info(String message) {
		JBossJaxrsUIPlugin.getDefault().getLog().log(new Status(Status.INFO, JBossJaxrsUIPlugin.PLUGIN_ID, message));
	}

	/**
	 * Outputs a debug message in the trace file (not the error view of the
	 * runtime workbench). Traces must be activated for this plugin in order to
	 * see the output messages.
	 * 
	 * @param message
	 *            the message to trace.
	 */
	public static void debug(final String message) {
		String debugOption = Platform.getDebugOption(DEBUG);
		if (JBossJaxrsUIPlugin.getDefault().isDebugging() && "true".equalsIgnoreCase(debugOption)) {
			System.out.println("[" + Thread.currentThread().getName() + "] " + message);
		}

	}

}
