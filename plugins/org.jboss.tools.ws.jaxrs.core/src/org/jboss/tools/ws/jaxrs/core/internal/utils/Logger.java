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

package org.jboss.tools.ws.jaxrs.core.internal.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCorePlugin;

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

/**
 * A logger wrapper utility for classes in the current bundle only.
 */
public final class Logger {

	/** The debug name, matching the .options file. */
	private static final String DEBUG = JBossJaxrsCorePlugin.PLUGIN_ID + "/debug";

	private static final ThreadLocal<DateFormat> dateFormatter = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("HH:mm:ss.SSS");
		}
	};

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
	public static void error(final String message, final Throwable t) {
		JBossJaxrsCorePlugin.getDefault().getLog()
				.log(new Status(Status.ERROR, JBossJaxrsCorePlugin.PLUGIN_ID, message, t));
	}

	/**
	 * Logs a message with an 'error' severity.
	 * 
	 * @param message
	 *            the message to log
	 */
	public static void error(final String message) {
		JBossJaxrsCorePlugin.getDefault().getLog()
				.log(new Status(Status.ERROR, JBossJaxrsCorePlugin.PLUGIN_ID, message));
	}

	/**
	 * Logs a message with an 'warning' severity.
	 * 
	 * @param message
	 *            the message to log
	 * @param t
	 *            the throwable cause
	 */
	public static void warn(final String message, final Throwable t) {
		JBossJaxrsCorePlugin.getDefault().getLog()
				.log(new Status(Status.WARNING, JBossJaxrsCorePlugin.PLUGIN_ID, message, t));
	}

	/**
	 * Logs a message with a 'warning' severity.
	 * 
	 * @param message
	 *            the message to log
	 */
	public static void warn(final String message) {
		JBossJaxrsCorePlugin.getDefault().getLog()
				.log(new Status(Status.WARNING, JBossJaxrsCorePlugin.PLUGIN_ID, message));
	}

	/**
	 * Logs a message with an 'info' severity.
	 * 
	 * @param message
	 *            the message to log
	 */
	public static void info(String message) {
		JBossJaxrsCorePlugin.getDefault().getLog()
				.log(new Status(Status.INFO, JBossJaxrsCorePlugin.PLUGIN_ID, message));
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
		if (JBossJaxrsCorePlugin.getDefault().isDebugging() && "true".equalsIgnoreCase(debugOption)) {
			System.out.println("[" + Thread.currentThread().getName() + "] " + message);
		}

	}

	/**
	 * Outputs a debug message in the trace file (not the error view of the
	 * runtime workbench). Traces must be activated for this plugin in order to
	 * see the output messages.
	 * 
	 * @param message
	 *            the message to trace.
	 */
	public static void debug(final String message, Object... items) {
		String debugOption = Platform.getDebugOption(DEBUG);
		String valuedMessage = message;
		if (JBossJaxrsCorePlugin.getDefault() != null && JBossJaxrsCorePlugin.getDefault().isDebugging()
				&& "true".equalsIgnoreCase(debugOption)) {
			for (Object item : items) {
				valuedMessage = valuedMessage.replaceFirst("\\{\\}", (item != null ? item.toString() : "null"));
			}

			System.out.println(dateFormatter.get().format(new Date()) + " [" + Thread.currentThread().getName() + "] "
					+ valuedMessage);
		}

	}
}
