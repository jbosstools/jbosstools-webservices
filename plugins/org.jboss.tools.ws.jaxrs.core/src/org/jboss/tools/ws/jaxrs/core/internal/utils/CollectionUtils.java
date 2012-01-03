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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collections utility class.
 * 
 * @author xcoulon
 */
public class CollectionUtils {

	/** Private constructor of the utility class. */
	private CollectionUtils() {
	}

	/**
	 * Compute the difference of elements between the 2 given maps
	 * 
	 * @param control
	 *            the control collection
	 * @param test
	 *            the test collection
	 * @return the elements of the control map that are not part of the test
	 *         map. The process works with keys and does not compare the values.
	 */
	public static <K, V> Map<K, V> difference(Map<K, V> control, Map<K, V> test) {
		if (control == null) {
			return null;
		}
		if (test == null) {
			return control;
		}
		List<K> keys = difference(control.keySet(), test.keySet());
		Map<K, V> result = new HashMap<K, V>();
		for (K key : keys) {
			result.put(key, control.get(key));
		}
		return result;
	}

	/**
	 * Compute the difference of elements between the 2 given collections
	 * 
	 * @param control
	 *            the control collection
	 * @param test
	 *            the test collection
	 * @return the elements of the control collection that are not part of the
	 *         test collection.
	 */
	public static <T> List<T> difference(Collection<T> control, Collection<T> test) {
		if (control == null) {
			return null;
		}
		List<T> result = new ArrayList<T>(control);
		if (test != null) {
			result.removeAll(test);
		}
		return result;
	}

	/**
	 * Compute the intersection of elements between the 2 given maps
	 * 
	 * @param control
	 *            the control collection
	 * @param test
	 *            the test collection
	 * @return the elements of the control map that whose keys are part of the
	 *         test map. The process works with keys and does not compare the
	 *         values.
	 */
	public static <K, V> Map<K, V> intersection(Map<K, V> control, Map<K, V> test) {
		if (control == null) {
			return null;
		}
		if (test == null) {
			return control;
		}
		Collection<K> keys = intersection(control.keySet(), test.keySet());
		Map<K, V> result = new HashMap<K, V>();
		for (K key : keys) {
			result.put(key, control.get(key));
		}
		return result;
	}

	/**
	 * Compute the intersection of elements between the 2 given collections
	 * 
	 * @param control
	 *            the control collection
	 * @param test
	 *            the test collection
	 * @return the elements of the control collection that are not part of the
	 *         test collection.
	 */
	public static <T> Collection<T> intersection(Collection<T> control, Collection<T> test) {
		if (control == null) {
			return null;
		}
		List<T> result = new ArrayList<T>(control);
		if (test != null) {
			result.retainAll(test);
		}
		return result;
	}

}
