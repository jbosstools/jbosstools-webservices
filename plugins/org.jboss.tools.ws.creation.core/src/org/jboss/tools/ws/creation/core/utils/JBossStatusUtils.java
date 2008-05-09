package org.jboss.tools.ws.creation.core.utils;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;

public class JBossStatusUtils {

	public static IStatus errorStatus(String message, Throwable exc) {
		return new Status(IStatus.ERROR, "id", 0, message, exc);
	}
	
	public static IStatus errorStatus(String message) {
		return new Status(IStatus.ERROR, "id", message);
	}
	
	public static MultiStatus errorMultiStatus(String message, IStatus[] status) {
		return new MultiStatus("id", 0, status, message,  null);
	}
	
	public static MultiStatus errorMultiStatus(String message) {
		return new MultiStatus("id", 0, message,  null);
	}
}
