package org.jboss.tools.ws.jaxrs.core.internal.utils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class CollectionUtilsTestCase {

	@Test
	public void shouldNotFindDifferencesInSameLists() {
		List<String> test = Arrays.asList("a", "b", "c");
		List<String> control = Arrays.asList("a", "b", "c");
		assertThat(CollectionUtils.compare(test, control).size(), equalTo(0));
	}

	@Test
	public void shouldFindDifferencesInListsOfSameSize() {
		List<String> test = Arrays.asList("a", "b", "c");
		List<String> control = Arrays.asList("a", "b", "d");
		final List<String> diffs = CollectionUtils.compare(test, control);
		assertThat(diffs.size(), equalTo(1));
		assertThat(diffs.get(0), equalTo("c"));
	}

	@Test
	public void shouldFindDifferencesInListsOfDifferentSize() {
		List<String> test = Arrays.asList("a", "b", "c", "d");
		List<String> control = Arrays.asList("a", "b", "c", "dd");
		final List<String> diffs = CollectionUtils.compare(test, control);
		assertThat(diffs.size(), equalTo(1));
		assertThat(diffs.get(0), equalTo("d"));
	}

	@Test
	public void shouldReturnTestWhenControlIsNull() {
		List<String> test = Arrays.asList("a", "b", "d", "c");
		List<String> control = null;
		final List<String> diffs = CollectionUtils.compare(test, control);
		assertThat(diffs, equalTo(test));
	}

	@Test
	public void shouldReturnControlWhenTestIsNull() {
		List<String> test = null;
		List<String> control = Arrays.asList("a", "b", "c");
		final List<String> diffs = CollectionUtils.compare(test, control);
		assertThat(diffs, equalTo(control));
	}
}
