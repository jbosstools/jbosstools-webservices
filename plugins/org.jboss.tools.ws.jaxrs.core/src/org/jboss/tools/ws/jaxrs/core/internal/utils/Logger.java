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

	/** The 'info' level name, matching the .options file. */
	private static final String INFO = JBossJaxrsCorePlugin.PLUGIN_ID + "/info";

	/** The 'debug' level name, matching the .options file. */
	private static final String DEBUG = JBossJaxrsCorePlugin.PLUGIN_ID + "/debug";

	/** The 'trace' level name, matching the .options file. */
	private static final String TRACE = JBossJaxrsCorePlugin.PLUGIN_ID + "/trace";

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
		if (JBossJaxrsCorePlugin.getDefault() != null) {
			JBossJaxrsCorePlugin.getDefault().getLog()
					.log(new Status(Status.ERROR, JBossJaxrsCorePlugin.PLUGIN_ID, message, t));
		} else {
			// at least write in the .log file
			t.printStackTrace();
		}
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
	 * Logs a message with an 'info' severity, if the 'INFO' tracing option is enabled, to avoid unwanted extra
	 * messages in the error log.
	 * 
	 * @param message
	 *            the message to log
	 */
	public static void info(String message) {
		if (isOptionEnabled(INFO)) {
			JBossJaxrsCorePlugin.getDefault().getLog()
					.log(new Status(Status.INFO, JBossJaxrsCorePlugin.PLUGIN_ID, message));
		}
	}

	/**
	 * Outputs a debug message in the trace file (not the error view of the runtime workbench). Traces must be activated
	 * for this plugin in order to see the output messages.
	 * 
	 * @param message
	 *            the message to trace.
	 */
	public static void debug(final String message) {
		debug(message, (Object[]) null);

	}

	/**
	 * Outputs a 'debug' level message in the .log file (not the error view of the runtime workbench). Traces must be
	 * activated for this plugin in order to see the output messages.
	 * 
	 * @param message
	 *            the message to trace.
	 */
	public static void debug(final String message, Object... items) {
		log(DEBUG, message, items);
	}

	/**
	 * Outputs a 'trace' level message in the .log file (not the error view of the runtime workbench). Traces must be
	 * activated for this plugin in order to see the output messages.
	 * 
	 * @param message
	 *            the message to trace.
	 */
	public static void trace(final String message, final Object... items) {
		log(TRACE, message, items);
	}

	private static void log(final String level, final String message, final Object... items) {
		try {
			if (isOptionEnabled(level)) {
				String valuedMessage = message;
				if (items != null) {
					for (Object item : items) {
						valuedMessage = valuedMessage.replaceFirst("\\{\\}", (item != null ? item.toString()
								.replaceAll("\\$", ".") : "null"));
					}
				}
				System.out.println(dateFormatter.get().format(new Date()) + " [" + Thread.currentThread().getName()
						+ "] " + valuedMessage);
			}
		} catch (RuntimeException e) {
			System.err.println("Failed to write proper debug message with template:\n " + message + "\n and items:");
			for (Object item : items) {
				System.err.println(" " + item);
			}
		}
	}

	private static boolean isOptionEnabled(String level) {
		final String debugOption = Platform.getDebugOption(level);
		return JBossJaxrsCorePlugin.getDefault() != null && JBossJaxrsCorePlugin.getDefault().isDebugging()
				&& "true".equalsIgnoreCase(debugOption);
	}

	public static boolean isDebugEnabled() {
		return isOptionEnabled(DEBUG);
	}
}
