package org.jboss.tools.ws.jaxrs.core.internal.utils;

public class ObjectUtils {

	private ObjectUtils() {

	}

	/**
	 * @param nextHTTPMethod
	 * @return
	 */
	public static boolean compare(Object o1, Object o2) {
		if ((o1 != null && !o1.equals(o2)) || (o2 != null && !o2.equals(o1))) {
			Logger.debug(" Value changed: " + o1 + " -> " + o2);
			return true;
		}
		return false;
	}
}
