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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	 *         'test', removed from 'control' or are in common in the two maps.
	 *         In case of the 'itemsInCommon', the returned items are those of
	 *         the 'control' map.
	 */
	public static <K, V> MapComparison<K, V> compare(Map<K, V> control, Map<K, V> test) {
		final Map<K, V> addedItems = CollectionUtils.difference(test, control);
		final Map<K, V> removedItems = CollectionUtils.difference(control, test);
		final Map<K, V> itemsInCommon = CollectionUtils.intersection(control, test);
		return new MapComparison<K, V>(addedItems, itemsInCommon, removedItems);
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
	 *         collections. In case of the 'itemsInCommon', the returned items
	 *         are those of the 'control' collection.
	 */
	public static <T> CollectionComparison<T> compare(Collection<T> control, Collection<T> test) {
		final Collection<T> addedItems = CollectionUtils.difference(test, control);
		final Collection<T> removedItems = CollectionUtils.difference(control, test);
		final Collection<T> itemsInCommon = CollectionUtils.intersection(control, test);
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
	 * Compute the intersection of elements between the 2 given collections
	 * 
	 * @param control
	 *            the control collection
	 * @param test
	 *            the test collection
	 * @return the elements of the control collection that are also part of the
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

	/**
	 * Compares the content of the 2 given collections
	 * 
	 * @param collection
	 * @param otherCollection
	 * @return true if the intersection of both collections is not empty (ie,
	 *         collections have values in common), false otherwise.
	 */
	public static boolean hasIntersection(final Collection<String> collection, final Collection<String> otherCollection) {
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
	public static <T> boolean contains(T[] source, T element) {
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

		private final Map<K, V> itemsInCommon;

		private final Map<K, V> removedItems;

		MapComparison(final Map<K, V> addedItems, Map<K, V> itemsInCommon, Map<K, V> removedItems) {
			this.addedItems = addedItems;
			this.itemsInCommon = itemsInCommon;
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
		public Map<K, V> getItemsInCommon() {
			return itemsInCommon;
		}

		/**
		 * @return the items that were in the 'control' map, but not in the
		 *         'test' map.
		 */
		public Map<K, V> getRemovedItems() {
			return removedItems;
		}
	}

	public static class CollectionComparison<T> {

		private final Collection<T> addedItems;

		private final Collection<T> itemsInCommon;

		private final Collection<T> removedItems;

		CollectionComparison(final Collection<T> addedItems, Collection<T> itemsInCommon, Collection<T> removedItems) {
			this.addedItems = addedItems;
			this.itemsInCommon = itemsInCommon;
			this.removedItems = removedItems;
		}

		/**
		 * @return the items found in the 'test' collection, but which were not
		 *         in the 'control' one.
		 */
		public Collection<T> getAddedItems() {
			return addedItems;
		}

		/**
		 * @return the items in common, retrieved from the 'control' collection.
		 */
		public Collection<T> getItemsInCommon() {
			return itemsInCommon;
		}

		/**
		 * @return the items that were in the 'control' collection, but not in
		 *         the 'test' collection.
		 */
		public Collection<T> getRemovedItems() {
			return removedItems;
		}

	}

	/**
	 * Converts the given elements into a set
	 * 
	 * @param elements
	 * @return the set containing the given elements
	 */
	public static <T> Set<T> toSet(T... elements) {
		final Set<T> result = new HashSet<T>();
		for (T element : elements) {
			result.add(element);
		}
		return result;
	}

	/**
	 * <p>
	 * Filters the given elements using the given predicate and cast the result
	 * into the expected subtype. <strong>This method may throw
	 * {@link ClassCastException} if one of the filtered elements are not a
	 * subtype of B !</strong>
	 * </p>
	 * 
	 * @param elements
	 * @param predicate
	 * @return the subtype list of matching elements.
	@SuppressWarnings("unchecked")
	public static <A, B extends A> List<B> filter(final Collection<A> elements, Predicate<A> predicate) {
		final Collection<A> filteredElements = Collections2.filter(elements, predicate);
		List<B> typedElements = new ArrayList<B>(filteredElements.size());
		for (A filteredElement : filteredElements) {
			typedElements.add((B) filteredElement);
		}
		return typedElements;
	}
	 */

	/**
	 * <p>
	 * Find the given element using the given predicate and cast the result
	 * into the expected subtype. <strong>This method may throw
	 * {@link ClassCastException} if the found element is not a
	 * subtype of B !</strong>
	 * </p>
	 * 
	 * @param elements
	 * @param predicate
	 * @return the subtype list of matching elements.
	@SuppressWarnings("unchecked")
	public static <A, B extends A> B find(final Collection<A> elements, Predicate<A> predicate) {
		return (B)(Iterables.find(elements, predicate, null));
	}
	 */
	
	/**
	 * Returns <code>true</code> if the given collection is not null and not empty, <code>false</code> otherwise.
	 * @param elements the collection to check
	 * @return true or false...
	 */
	public static boolean notNullNorEmpty(final Collection<?> elements) {
		return elements != null && !elements.isEmpty();
	}

}
