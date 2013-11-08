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

	/** Private constructor of this utility class. */
	private CollectionUtils() {
	}

	/**
	 * Handy method to initializes a map from the given key/value pair.
	 * 
	 * @param key
	 *            the key in the returned map
	 * @param value
	 *            the value put in the returned map
	 * @return a map containing a single entry identified by 'key' hodling the
	 *         given value.
	 */
	public static <K, V> Map<K, V> toMap(final K key, final V value) {
		Map<K, V> map = new HashMap<K, V>();
		map.put(key, value);
		return map;
	}

	/**
	 * Compares the content of the two given maps (using difference and
	 * intersection approaches, based on
	 * <code>equals<code>  comparison of each elements in both maps).
	 * 
	 * @param control
	 *            the 'control' (or 'original') map
	 * @param test
	 *            the 'test' (or 'modified') map
	 * @return a structure that indicates the items that were added in the
	 *         'test' (ie: found in 'test' but not in 'control') , removed from 'control' (ie: found in 'control' but not in 'test') or changed in the 'control' map (ie: elements have same key but different values).
	 */
	public static <K, V> MapComparison<K, V> compare(final Map<K, V> control, final Map<K, V> test) {
		// added items
		final Map<K, V> addedItems = new HashMap<K, V>();
		final Collection<K> addedItemKeys = CollectionUtils.difference(test.keySet(), control.keySet());
		for(K addedItemKey : addedItemKeys) {
			addedItems.put(addedItemKey, test.get(addedItemKey));
		}
		// removed items
		final Collection<K> removedItemKeys = CollectionUtils.difference(control.keySet(), test.keySet());
		final Map<K, V> removedItems = new HashMap<K, V>();
		for(K removedItemKey : removedItemKeys) {
			removedItems.put(removedItemKey, control.get(removedItemKey));
		}
		// changed items
		final Collection<K> changedItemKeys = CollectionUtils.intersection(control.keySet(), test.keySet());
		final Map<K, V> changedItems = new HashMap<K, V>();
		for(K changedItemKey : changedItemKeys) {
			if(!control.get(changedItemKey).equals(test.get(changedItemKey))) {
				changedItems.put(changedItemKey, test.get(changedItemKey));
			}
		}
		return new MapComparison<K, V>(addedItems, changedItems, removedItems);
	}

	/**
	 * Compute the difference of indexed elements between the 2 given maps
	 * 
	 * @param control
	 *            the control collection
	 * @param test
	 *            the test collection
	 * @return the elements of the control map that are not part of the test
	 *         map. The process returns &lt;key,value&gt; pairs whose key is missing in the 'test' map, and also the elements that have the same 'key' but different values.
	 */
	public static <K, V> Map<K, V> difference(final Map<K, V> control, final Map<K, V> test) {
		if (control == null) {
			return null;
		}
		if (test == null) {
			return control;
		}
		final Map<K, V> result = new HashMap<K, V>();
		// different keys (ie, those not found in 'test')
		final Collection<K> differentKeys = difference(control.keySet(), test.keySet());
		for (K key : differentKeys) {
			result.put(key, control.get(key));
		}
		// different values
		final Collection<K> sameKeys = intersection(control.keySet(), test.keySet());
		for(K key : sameKeys) {
			final V controlValue = control.get(key);
			final V testValue = test.get(key);
			if(!controlValue.equals(testValue)) {
				result.put(key, testValue);
			}
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
	public static <K, V> Map<K, V> intersection(final Map<K, V> control, final Map<K, V> test) {
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
	 * Compares the content of the two given collections (using difference and
	 * intersection approaches, based on
	 * <code>equals<code>  comparison of each elements in both maps).
	 * 
	 * @param control
	 *            the 'control' (or 'original') collection
	 * @param test
	 *            the 'test' (or 'modified') collection
	 * @return a structure that indicates the items that were added in the
	 *         'test', removed from 'control' or are in common in the two
	 *         collections. In case of the 'changedItems', the returned items
	 *         are those of the 'control' collection.
	 */
	public static <T> CollectionComparison<T> compare(final List<T> control, final List<T> test) {
		final List<T> addedItems = CollectionUtils.difference(test, control);
		final List<T> removedItems = CollectionUtils.difference(control, test);
		final List<T> itemsInCommon = CollectionUtils.intersection(control, test);
		return new CollectionComparison<T>(addedItems, itemsInCommon, removedItems);
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
	public static <T> List<T> difference(final Collection<T> control, final Collection<T> test) {
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
	 * Compute the intersection of elements between the 2 given collections
	 * 
	 * @param control
	 *            the control collection
	 * @param test
	 *            the test collection
	 * @return the elements of the control collection that are also part of the
	 *         test collection.
	 */
	public static <T> List<T> intersection(final Collection<T> control, final Collection<T> test) {
		if (control == null) {
			return null;
		}
		List<T> result = new ArrayList<T>(control);
		if (test != null) {
			result.retainAll(test);
		}
		return result;
	}

	/**
	 * Compares the content of the 2 given collections
	 * 
	 * @param collection
	 * @param otherCollection
	 * @return true if the intersection of both collections is not empty (ie,
	 *         collections have values in common), false otherwise.
	 */
	public static boolean hasIntersection(final List<String> collection, final List<String> otherCollection) {
		return !intersection(collection, otherCollection).isEmpty();
	}

	public static <T> T[] append(T[] sourceArray, T extraElement, T[] targetArray) {
		System.arraycopy(sourceArray, 0, targetArray, 0, sourceArray.length);
		targetArray[targetArray.length - 1] = extraElement;
		return targetArray;
	}

	/**
	 * Returns true if the given source contains the given element, false
	 * otherwise
	 * 
	 * @param source
	 *            the array of elements
	 * @param element
	 *            the element to find in the array
	 * @return true if found, false otherwise
	 */
	public static <T> boolean contains(final T[] source, final T element) {
		if (element == null || source == null) {
			return false;
		}
		for (T item : source) {
			if (item.equals(element)) {
				return true;
			}
		}
		return false;
	}

	public static class MapComparison<K, V> {

		private final Map<K, V> addedItems;

		private final Map<K, V> changedItems;

		private final Map<K, V> removedItems;

		MapComparison(final Map<K, V> addedItems, final Map<K, V> itemsInCommon, final Map<K, V> removedItems) {
			this.addedItems = addedItems;
			this.changedItems = itemsInCommon;
			this.removedItems = removedItems;
		}

		/**
		 * @return the items found in the 'test' map, but which were not in the
		 *         'control' one.
		 */
		public Map<K, V> getAddedItems() {
			return addedItems;
		}

		/**
		 * @return the items in common, retrieved from the 'control' map.
		 */
		public Map<K, V> getChangedItems() {
			return changedItems;
		}

		/**
		 * @return the items that were in the 'control' map, but not in the
		 *         'test' map.
		 */
		public Map<K, V> getRemovedItems() {
			return removedItems;
		}

		/**
		 * Returns true if at least one item was added, changed or removed. Returns false otherwise.
		 * @return true if at least one item was added, changed or removed. Returns false otherwise.
		 */
		public boolean hasDifferences() {
			if(addedItems.isEmpty() && removedItems.isEmpty() && changedItems.isEmpty()) {
				return false;
			}
			return true;
		}
	}

	public static class CollectionComparison<T> {

		private final List<T> addedItems;

		private final List<T> itemsInCommon;

		private final List<T> removedItems;

		CollectionComparison(final List<T> addedItems, final List<T> itemsInCommon, final List<T> removedItems) {
			this.addedItems = addedItems;
			this.itemsInCommon = itemsInCommon;
			this.removedItems = removedItems;
		}

		/**
		 * @return the items found in the 'test' collection, but which were not
		 *         in the 'control' one.
		 */
		public List<T> getAddedItems() {
			return addedItems;
		}

		/**
		 * @return the items in common, retrieved from the 'control' collection.
		 */
		public List<T> getItemsInCommon() {
			return itemsInCommon;
		}

		/**
		 * @return the items that were in the 'control' collection, but not in
		 *         the 'test' collection.
		 */
		public List<T> getRemovedItems() {
			return removedItems;
		}

	}

	/**
	 * Returns <code>true</code> if the given collection is not null and not empty, <code>false</code> otherwise.
	 * @param elements the collection to check
	 * @return true or false...
	 */
	public static boolean notNullNorEmpty(final List<?> elements) {
		return elements != null && !elements.isEmpty();
	}

}
