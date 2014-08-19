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
package org.jboss.tools.ws.jaxrs.core.internal.utils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils.CollectionComparison;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils.MapComparison;
import org.junit.Test;

@SuppressWarnings("serial")
public class CollectionUtilsTestCase {

	@Test
	public void shouldNotFindDifferencesInSameLists() {
		// preconditions
		List<String> control = Arrays.asList("a", "b", "c");
		List<String> test = Arrays.asList("a", "b", "c");
		// operation
		final List<String> difference = CollectionUtils.difference(control, test);
		// verifications
		assertThat(difference.size(), equalTo(0));
	}

	@Test
	public void shouldFindDifferencesInListsOfSameSize() {
		// preconditions
		List<String> control = Arrays.asList("a", "b", "d");
		List<String> test = Arrays.asList("a", "b", "c");
		// operation
		List<String> diffs = (List<String>) CollectionUtils.difference(control, test);
		// verifications
		assertThat(diffs.size(), equalTo(1));
		assertThat(diffs.get(0), equalTo("d"));
	}

	@Test
	public void shouldFindDifferencesInListsOfDifferentSizeWithMissingEntry() {
		// preconditions
		List<String> control = Arrays.asList("a", "b", "c", "d");
		List<String> test = Arrays.asList("a", "b", "c");
		// operation
		List<String> diffs = (List<String>) CollectionUtils.difference(control, test);
		// verifications
		assertThat(diffs.size(), equalTo(1));
		assertThat(diffs.get(0), equalTo("d"));
	}

	@Test
	public void shouldNotFindDifferencesInListsOfDifferentSizeWithMissingEntry() {
		// preconditions
		List<String> control = Arrays.asList("a", "b", "c");
		List<String> test = Arrays.asList("a", "b", "c", "d");
		// operation
		List<String> diffs = (List<String>) CollectionUtils.difference(control, test);
		// verifications
		assertThat(diffs.size(), equalTo(0));
	}

	@Test
	public void shouldFindDifferencesInListsOfDifferentSizeWithDifferentEntries() {
		// preconditions
		List<String> control = Arrays.asList("a", "b", "c", "d");
		List<String> test = Arrays.asList("a", "b", "c", "e", "f");
		// operation
		List<String> diffs = (List<String>) CollectionUtils.difference(control, test);
		// verifications
		assertThat(diffs.size(), equalTo(1));
		assertThat(diffs.get(0), equalTo("d"));
	}

	@Test
	public void shouldReturnNullWhenControlListToDifferentiateIsNull() {
		// preconditions
		List<String> control = null;
		List<String> test = Arrays.asList("a", "b", "d", "c");
		// operation
		List<String> diffs = (List<String>) CollectionUtils.difference(control, test);
		// verifications
		assertNull(diffs);
	}

	@Test
	public void shouldReturnControlWhenTestListToDifferentiateIsNull() {
		// preconditions
		List<String> control = Arrays.asList("a", "b", "c");
		List<String> test = null;
		// operation
		List<String> diffs = (List<String>) CollectionUtils.difference(control, test);
		// verifications
		assertThat(diffs, equalTo(control));
	}

	@Test
	public void shouldFindIntersectionsInSameLists() {
		// preconditions
		List<String> control = Arrays.asList("a", "b", "c");
		List<String> test = Arrays.asList("a", "b", "c");
		// operation
		final Collection<String> intersection = CollectionUtils.intersection(control, test);
		// verifications
		assertThat(intersection.size(), equalTo(3));
	}

	@Test
	public void shouldFindIntersectionsInListsOfSameSize() {
		// preconditions
		List<String> control = Arrays.asList("a", "b", "d");
		List<String> test = Arrays.asList("a", "b", "c");
		// operation
		List<String> diffs = (List<String>) CollectionUtils.intersection(control, test);
		// verifications
		assertThat(diffs.size(), equalTo(2));
		assertThat(diffs, contains("a", "b"));
	}

	@Test
	public void shouldFindIntersectionsInListsOfDifferentSizeWithMissingEntryInTest() {
		// preconditions
		List<String> control = Arrays.asList("a", "b", "c", "d");
		List<String> test = Arrays.asList("a", "b", "c");
		// operation
		List<String> diffs = (List<String>) CollectionUtils.intersection(control, test);
		// verifications
		assertThat(diffs.size(), equalTo(3));
		assertThat(diffs, contains("a", "b", "c"));
	}

	@Test
	public void shouldFindIntersectionsInListsOfDifferentSizeWithMissingEntryInControl() {
		// preconditions
		List<String> control = Arrays.asList("a", "b", "c");
		List<String> test = Arrays.asList("a", "b", "c", "d");
		// operation
		List<String> diffs = (List<String>) CollectionUtils.intersection(control, test);
		// verifications
		assertThat(diffs.size(), equalTo(3));
		assertThat(diffs, contains("a", "b", "c"));
	}

	@Test
	public void shouldFindIntersectionsInListsOfDifferentSizeWithDifferentEntries() {
		// preconditions
		List<String> control = Arrays.asList("a", "b", "c", "d");
		List<String> test = Arrays.asList("a", "b", "c", "e", "f");
		// operation
		List<String> diffs = (List<String>) CollectionUtils.intersection(control, test);
		// verifications
		assertThat(diffs.size(), equalTo(3));
		assertThat(diffs, contains("a", "b", "c"));
	}

	@Test
	public void shouldReturnNullWhenControlListToIntersectIsNull() {
		// preconditions
		List<String> control = null;
		List<String> test = Arrays.asList("a", "b", "d", "c");
		// operation
		List<String> diffs = (List<String>) CollectionUtils.intersection(control, test);
		// verifications
		assertNull(diffs);
	}

	@Test
	public void shouldReturnControlWhenTestListToIntersectIsNull() {
		// preconditions
		List<String> control = Arrays.asList("a", "b", "c");
		List<String> test = null;
		// operation
		List<String> diffs = (List<String>) CollectionUtils.intersection(control, test);
		// verifications
		assertThat(diffs, equalTo(control));
	}

	@Test
	public void shouldNotFindDifferencesInSameMaps() {
		// preconditions
		Map<String, String> control = new HashMap<String, String>() {
			{
				put("a", "one");
				put("b", "two");
				put("c", "three");
			}
		};
		Map<String, String> test = new HashMap<String, String>() {
			{
				put("a", "one");
				put("b", "two");
				put("c", "three");
			}
		};
		// operation
		final Map<String, String> difference = CollectionUtils.difference(control, test);
		// verifications
		assertThat(difference.size(), equalTo(0));
	}

	@Test
	public void shouldFindDifferencesInMapsOfSameSizeWithDifferentValues() {
		// preconditions
		Map<String, String> control = new HashMap<String, String>() {
			{
				put("a", "one");
				put("b", "two");
				put("c", "three");
			}
		};
		Map<String, String> test = new HashMap<String, String>() {
			{
				put("a", "one+");
				put("b", "two");
				put("c", "three");
			}
		};
		// operation
		Map<String, String> diffs = CollectionUtils.difference(control, test);
		// verifications
		assertThat(diffs.size(), equalTo(1));
		assertThat(diffs.get("a"), equalTo("one+"));
	}

	@Test
	public void shouldFindDifferencesInMapsOfSameSizeWithDifferentKeys() {
		// preconditions
		Map<String, String> control = new HashMap<String, String>() {
			{
				put("a", "one");
				put("b", "two");
				put("d", "four");
			}
		};
		Map<String, String> test = new HashMap<String, String>() {
			{
				put("a", "one");
				put("b", "two");
				put("c", "three");
			}
		};
		// operation
		Map<String, String> diffs = CollectionUtils.difference(control, test);
		// verifications
		assertThat(diffs.size(), equalTo(1));
		assertThat(diffs.get("d"), equalTo("four"));
	}

	@Test
	public void shouldFindDifferencesInMapsOfDifferentSizeWithDifferentValues() {
		// preconditions
		Map<String, String> control = new HashMap<String, String>() {
			{
				put("a", "one");
				put("b", "two");
				put("c", "three");

			}
		};
		Map<String, String> test = new HashMap<String, String>() {
			{
				put("a", "one+");
				put("b", "two");
				put("c", "three");
				put("d", "four");
			}
		};
		// operation
		Map<String, String> diffs = CollectionUtils.difference(control, test);
		// verifications
		assertThat(diffs.size(), equalTo(1));
		assertThat(diffs.get("a"), equalTo("one+"));
	}

	@Test
	public void shouldNotFindDifferencesInMapsOfDifferentSizeWithSameValues() {
		// preconditions
		Map<String, String> control = new HashMap<String, String>() {
			{
				put("a", "one");
				put("b", "two");
				put("c", "three");

			}
		};
		Map<String, String> test = new HashMap<String, String>() {
			{
				put("a", "one");
				put("b", "two");
				put("c", "three");
				put("d", "four");
			}
		};
		// operation
		Map<String, String> diffs = CollectionUtils.difference(control, test);
		// verifications
		assertThat(diffs.size(), equalTo(0));
	}

	@Test
	public void shouldReturnNullWhenControlMapToDifferentiateIsNull() {
		// preconditions
		Map<String, String> control = null;
		Map<String, String> test = new HashMap<String, String>() {
			{
				put("a", "one");
				put("b", "two");
				put("c", "three");
				put("d", "four");
			}
		};
		// operation
		Map<String, String> diffs = CollectionUtils.difference(control, test);
		// verifications
		assertNull(diffs);
	}

	@Test
	public void shouldReturnControlWhenTestMapToDifferentiateIsNull() {
		// preconditions
		Map<String, String> control = new HashMap<String, String>() {
			{
				put("a", "one");
				put("b", "two");
				put("c", "three");

			}
		};
		Map<String, String> test = null;
		// operation
		Map<String, String> diffs = CollectionUtils.difference(control, test);
		// verifications
		assertThat(diffs, equalTo(control));
	}
	
	@Test
	public void shouldNotFindIntersectionsInSameMaps() {
		// preconditions
		Map<String, String> control = new HashMap<String, String>() {
			{
				put("a", "one");
				put("b", "two");
				put("c", "three");
			}
		};
		Map<String, String> test = new HashMap<String, String>() {
			{
				put("a", "one");
				put("b", "two");
				put("c", "three");
			}
		};
		// operation
		final Map<String, String> result = CollectionUtils.intersection(control, test);
		// verifications
		assertThat(result.size(), equalTo(3));
	}

	@Test
	public void shouldNotFindIntersectionsInMapsOfSameSizeWithDifferentValues() {
		// preconditions
		Map<String, String> control = new HashMap<String, String>() {
			{
				put("a", "one");
				put("b", "two");
				put("c", "three");
			}
		};
		Map<String, String> test = new HashMap<String, String>() {
			{
				put("a", "one+");
				put("b", "two");
				put("c", "three");
			}
		};
		// operation
		Map<String, String> diffs = CollectionUtils.intersection(control, test);
		// verifications
		assertThat(diffs.size(), equalTo(3));
		assertThat(diffs.get("a"), equalTo("one"));
	}

	@Test
	public void shouldFindIntersectionsInMapsOfSameSizeWithDifferentKeys() {
		// preconditions
		Map<String, String> control = new HashMap<String, String>() {
			{
				put("a", "one");
				put("b", "two");
				put("d", "four");
			}
		};
		Map<String, String> test = new HashMap<String, String>() {
			{
				put("a", "one");
				put("b", "two");
				put("c", "three");
			}
		};
		// operation
		Map<String, String> diffs = CollectionUtils.intersection(control, test);
		// verifications
		assertThat(diffs.size(), equalTo(2));
		assertThat(diffs.keySet(), containsInAnyOrder("a", "b"));
	}

	@Test
	public void shouldFindIntersectionsInMapsOfDifferentSizeWithDifferentValues() {
		// preconditions
		Map<String, String> control = new HashMap<String, String>() {
			{
				put("a", "one");
				put("b", "two");
				put("c", "three");

			}
		};
		Map<String, String> test = new HashMap<String, String>() {
			{
				put("a", "one+");
				put("b", "two");
				put("c", "three");
				put("d", "four");
			}
		};
		// operation
		Map<String, String> diffs = CollectionUtils.intersection(control, test);
		// verifications
		assertThat(diffs.size(), equalTo(3));
		assertThat(diffs.keySet(), containsInAnyOrder("a", "b", "c"));
		assertThat(diffs.get("a"), equalTo("one"));
	}

	@Test
	public void shouldFindIntersectionsInMapsOfDifferentSizeWithSameValues() {
		// preconditions
		Map<String, String> control = new HashMap<String, String>() {
			{
				put("a", "one");
				put("b", "two");
				put("c", "three");

			}
		};
		Map<String, String> test = new HashMap<String, String>() {
			{
				put("a", "one");
				put("b", "two");
				put("c", "three");
				put("d", "four");
			}
		};
		// operation
		Map<String, String> diffs = CollectionUtils.intersection(control, test);
		// verifications
		assertThat(diffs.size(), equalTo(3));
		assertThat(diffs.keySet(), containsInAnyOrder("a", "b", "c"));
		assertThat(diffs.get("a"), equalTo("one"));
	}

	@Test
	public void shouldReturnNullWhenControlMapToIntersectIsNull() {
		// preconditions
		Map<String, String> control = null;
		Map<String, String> test = new HashMap<String, String>() {
			{
				put("a", "one");
				put("b", "two");
				put("c", "three");
				put("d", "four");
			}
		};
		// operation
		Map<String, String> diffs = CollectionUtils.intersection(control, test);
		// verifications
		assertNull(diffs);
	}

	@Test
	public void shouldReturnControlWhenTestMapToIntersectIsNull() {
		// preconditions
		Map<String, String> control = new HashMap<String, String>() {
			{
				put("a", "one");
				put("b", "two");
				put("c", "three");

			}
		};
		Map<String, String> test = null;
		// operation
		Map<String, String> diffs = CollectionUtils.intersection(control, test);
		// verifications
		assertThat(diffs, equalTo(control));
	}
	
	@Test
	public void shouldCompareMaps() {
		// preconditions
		Map<String, String> control = new HashMap<String, String>() {
			{
				put("a", "one");
				put("b", "2");
				put("c", "three");

			}
		};
		Map<String, String> test = new HashMap<String, String>() {
			{
				put("a", "one");
				put("b", "two");
				put("d", "four");
			}
		};
		// operation
		MapComparison<String, String> diffs = CollectionUtils.compare(control, test);
		// verifications
		assertThat(diffs.getAddedItems().keySet(), containsInAnyOrder("d"));
		assertThat(diffs.getChangedItems().keySet(), containsInAnyOrder("b"));
		assertThat(diffs.getChangedItems().get("b"), equalTo("two"));
		assertThat(diffs.getRemovedItems().keySet(), containsInAnyOrder("c"));
	}
	
	@Test
	public void shouldCompareLists() {
		// preconditions
		List<String> control = Arrays.asList("a", "b", "c", "d");
		List<String> test = Arrays.asList("a", "b", "c", "e", "f");
		// operation
		CollectionComparison<String> diffs = CollectionUtils.compare(control, test);
		// verifications
		assertThat(diffs.getAddedItems(), containsInAnyOrder("e", "f"));
		assertThat(diffs.getItemsInCommon(), containsInAnyOrder("a", "b", "c"));
		assertThat(diffs.getRemovedItems(), containsInAnyOrder("d"));
	}
}
