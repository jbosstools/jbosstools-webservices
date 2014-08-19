/******************************************************************************* 
 * Copyright (c) 2008 - 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.ws.jaxrs.ui.internal.utils;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author xcoulon
 *
 */
public class CollectionUtils {

	/**
	 * Private constructor for the utility class
	 */
	private CollectionUtils() {

	}

	/**
	 * Compares the content of the two list, no matter how they are ordered
	 * @param test the list containing items to check
	 * @param control the list containing the expected values
	 * @return {@code true}
	 */
	public static <T extends Comparable<? super T>> boolean containsInAnyOrder(final List<T> test, final List<T> control) {
		if (test == null || control == null) {
			return false;
		}
		if(test.size() != control.size()) {
			return false;
		}
		final List<T> orderedControl = new LinkedList<T>(control);
		Collections.sort(orderedControl);
		final List<T> orderedTest = new LinkedList<T>(test);
		Collections.sort(orderedTest);
		final Iterator<T> testIterator = orderedTest.iterator();
		for(Iterator<T> controlIterator = orderedControl.iterator(); controlIterator.hasNext();) {
			T controlItem = controlIterator.next();
			T testItem = testIterator.next();
			if(testItem == null || !testItem.equals(controlItem)) {
				return false;
			}
		}
		return true;
	}

}
