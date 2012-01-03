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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

@SuppressWarnings("serial")
public class CollectionUtilsTestCase {

	@Test
	public void shouldNotFindDifferencesInSameLists() {
		List<String> control = Arrays.asList("a", "b", "c");
		List<String> test = Arrays.asList("a", "b", "c");
		assertThat(CollectionUtils.difference(control, test).size(), equalTo(0));
	}

	@Test
	public void shouldFindDifferencesInListsOfSameSize() {
		List<String> control = Arrays.asList("a", "b", "d");
		List<String> test = Arrays.asList("a", "b", "c");
		List<String> diffs = (List<String>) CollectionUtils.difference(control, test);
		assertThat(diffs.size(), equalTo(1));
		assertThat(diffs.get(0), equalTo("d"));
	}

	@Test
	public void shouldFindDifferencesInListsOfDifferentSizeWithMissingEntry() {
		List<String> control = Arrays.asList("a", "b", "c", "d");
		List<String> test = Arrays.asList("a", "b", "c");
		List<String> diffs = (List<String>) CollectionUtils.difference(control, test);
		assertThat(diffs.size(), equalTo(1));
		assertThat(diffs.get(0), equalTo("d"));
	}

	@Test
	public void shouldNotFindDifferencesInListsOfDifferentSizeWithMissingEntry() {
		List<String> control = Arrays.asList("a", "b", "c");
		List<String> test = Arrays.asList("a", "b", "c", "d");
		List<String> diffs = (List<String>) CollectionUtils.difference(control, test);
		assertThat(diffs.size(), equalTo(0));
	}

	@Test
	public void shouldFindDifferencesInListsOfDifferentSizeWithDifferentEntries() {
		List<String> control = Arrays.asList("a", "b", "c", "d");
		List<String> test = Arrays.asList("a", "b", "c", "e", "f");
		List<String> diffs = (List<String>) CollectionUtils.difference(control, test);
		assertThat(diffs.size(), equalTo(1));
		assertThat(diffs.get(0), equalTo("d"));
	}

	@Test
	public void shouldReturnNullWhenControlListToDifferentiateIsNull() {
		List<String> control = null;
		List<String> test = Arrays.asList("a", "b", "d", "c");
		List<String> diffs = (List<String>) CollectionUtils.difference(control, test);
		assertNull(diffs);
	}

	@Test
	public void shouldReturnControlWhenTestListToDifferentiateIsNull() {
		List<String> control = Arrays.asList("a", "b", "c");
		List<String> test = null;
		List<String> diffs = (List<String>) CollectionUtils.difference(control, test);
		assertThat(diffs, equalTo(control));
	}

	@Test
	public void shouldFindIntersectionsInSameLists() {
		List<String> control = Arrays.asList("a", "b", "c");
		List<String> test = Arrays.asList("a", "b", "c");
		assertThat(CollectionUtils.intersection(control, test).size(), equalTo(3));
	}

	@Test
	public void shouldFindIntersectionsInListsOfSameSize() {
		List<String> control = Arrays.asList("a", "b", "d");
		List<String> test = Arrays.asList("a", "b", "c");
		List<String> diffs = (List<String>) CollectionUtils.intersection(control, test);
		assertThat(diffs.size(), equalTo(2));
		assertThat(diffs, contains("a", "b"));
	}

	@Test
	public void shouldFindIntersectionsInListsOfDifferentSizeWithMissingEntryInTest() {
		List<String> control = Arrays.asList("a", "b", "c", "d");
		List<String> test = Arrays.asList("a", "b", "c");
		List<String> diffs = (List<String>) CollectionUtils.intersection(control, test);
		assertThat(diffs.size(), equalTo(3));
		assertThat(diffs, contains("a", "b", "c"));
	}

	@Test
	public void shouldFindIntersectionsInListsOfDifferentSizeWithMissingEntryInControl() {
		List<String> control = Arrays.asList("a", "b", "c");
		List<String> test = Arrays.asList("a", "b", "c", "d");
		List<String> diffs = (List<String>) CollectionUtils.intersection(control, test);
		assertThat(diffs.size(), equalTo(3));
		assertThat(diffs, contains("a", "b", "c"));
	}

	@Test
	public void shouldFindIntersectionsInListsOfDifferentSizeWithDifferentEntries() {
		List<String> control = Arrays.asList("a", "b", "c", "d");
		List<String> test = Arrays.asList("a", "b", "c", "e", "f");
		List<String> diffs = (List<String>) CollectionUtils.intersection(control, test);
		assertThat(diffs.size(), equalTo(3));
		assertThat(diffs, contains("a", "b", "c"));
	}

	@Test
	public void shouldReturnNullWhenControlListToIntersectIsNull() {
		List<String> control = null;
		List<String> test = Arrays.asList("a", "b", "d", "c");
		List<String> diffs = (List<String>) CollectionUtils.intersection(control, test);
		assertNull(diffs);
	}

	@Test
	public void shouldReturnControlWhenTestListToIntersectIsNull() {
		List<String> control = Arrays.asList("a", "b", "c");
		List<String> test = null;
		List<String> diffs = (List<String>) CollectionUtils.intersection(control, test);
		assertThat(diffs, equalTo(control));
	}

	@Test
	public void shouldNotFindDifferencesInSameMaps() {
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
		assertThat(CollectionUtils.difference(control, test).size(), equalTo(0));
	}

	@Test
	public void shouldNotFindDifferencesInMapsOfSameSizeWithDifferentValues() {
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
		Map<String, String> diffs = CollectionUtils.difference(control, test);
		assertThat(diffs.size(), equalTo(0));
	}

	@Test
	public void shouldFindDifferencesInMapsOfSameSizeWithDifferentKeys() {
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
		Map<String, String> diffs = CollectionUtils.difference(control, test);
		assertThat(diffs.size(), equalTo(1));
		assertThat(diffs.get("d"), equalTo("four"));
	}

	@Test
	public void shouldNotFindDifferencesInMapsOfDifferentSizeWithDifferentValues() {
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
		Map<String, String> diffs = CollectionUtils.difference(control, test);
		assertThat(diffs.size(), equalTo(0));
	}

	@Test
	public void shouldNotFindDifferencesInMapsOfDifferentSizeWithSameValues() {
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
		Map<String, String> diffs = CollectionUtils.difference(control, test);
		assertThat(diffs.size(), equalTo(0));
	}

	@Test
	public void shouldReturnNullWhenControlMapToDifferentiateIsNull() {
		Map<String, String> control = null;
		Map<String, String> test = new HashMap<String, String>() {
			{
				put("a", "one");
				put("b", "two");
				put("c", "three");
				put("d", "four");
			}
		};
		Map<String, String> diffs = CollectionUtils.difference(control, test);
		assertNull(diffs);
	}

	@Test
	public void shouldReturnControlWhenTestMapToDifferentiateIsNull() {
		Map<String, String> control = new HashMap<String, String>() {
			{
				put("a", "one");
				put("b", "two");
				put("c", "three");

			}
		};
		Map<String, String> test = null;
		Map<String, String> diffs = CollectionUtils.difference(control, test);
		assertThat(diffs, equalTo(control));
	}
	
	@Test
	public void shouldNotFindIntersectionsInSameMaps() {
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
		final Map<String, String> result = CollectionUtils.intersection(control, test);
		assertThat(result.size(), equalTo(3));
	}

	@Test
	public void shouldNotFindIntersectionsInMapsOfSameSizeWithDifferentValues() {
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
		Map<String, String> diffs = CollectionUtils.intersection(control, test);
		assertThat(diffs.size(), equalTo(3));
		assertThat(diffs.get("a"), equalTo("one"));
	}

	@Test
	public void shouldFindIntersectionsInMapsOfSameSizeWithDifferentKeys() {
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
		Map<String, String> diffs = CollectionUtils.intersection(control, test);
		assertThat(diffs.size(), equalTo(2));
		assertThat(diffs.keySet(), containsInAnyOrder("a", "b"));
	}

	@Test
	public void shouldFindIntersectionsInMapsOfDifferentSizeWithDifferentValues() {
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
		Map<String, String> diffs = CollectionUtils.intersection(control, test);
		assertThat(diffs.size(), equalTo(3));
		assertThat(diffs.keySet(), containsInAnyOrder("a", "b", "c"));
		assertThat(diffs.get("a"), equalTo("one"));
	}

	@Test
	public void shouldFindIntersectionsInMapsOfDifferentSizeWithSameValues() {
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
		Map<String, String> diffs = CollectionUtils.intersection(control, test);
		assertThat(diffs.size(), equalTo(3));
		assertThat(diffs.keySet(), containsInAnyOrder("a", "b", "c"));
		assertThat(diffs.get("a"), equalTo("one"));
	}

	@Test
	public void shouldReturnNullWhenControlMapToIntersectIsNull() {
		Map<String, String> control = null;
		Map<String, String> test = new HashMap<String, String>() {
			{
				put("a", "one");
				put("b", "two");
				put("c", "three");
				put("d", "four");
			}
		};
		Map<String, String> diffs = CollectionUtils.intersection(control, test);
		assertNull(diffs);
	}

	@Test
	public void shouldReturnControlWhenTestMapToIntersectIsNull() {
		Map<String, String> control = new HashMap<String, String>() {
			{
				put("a", "one");
				put("b", "two");
				put("c", "three");

			}
		};
		Map<String, String> test = null;
		Map<String, String> diffs = CollectionUtils.intersection(control, test);
		assertThat(diffs, equalTo(control));
	}
}
