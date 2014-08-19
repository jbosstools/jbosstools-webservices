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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * @author xcoulon
 *
 */
public class CollectionsUtilTestCase {
	
	@Test
	public void shouldMatchCollections() {
		// given
		final List<String> test = Arrays.asList("a", "c", "b", "d");
		final List<String> control = Arrays.asList("a", "b", "d", "c");
		// when
		final boolean result = CollectionUtils.containsInAnyOrder(test, control);
		// then
		assertThat(result, equalTo(true));
	}

	@Test
	public void shouldNotMatchCollectionsWithDifferentItems() {
		// given
		final List<String> test = Arrays.asList("a", "c", "b", "e");
		final List<String> control = Arrays.asList("a", "b", "d", "c");
		// when
		final boolean result = CollectionUtils.containsInAnyOrder(test, control);
		// then
		assertThat(result, equalTo(false));
	}

	@Test
	public void shouldNotMatchCollectionsWithDifferentSizes() {
		// given
		final List<String> test = Arrays.asList("a", "c", "b");
		final List<String> control = Arrays.asList("a", "b", "d", "c");
		// when
		final boolean result = CollectionUtils.containsInAnyOrder(test, control);
		// then
		assertThat(result, equalTo(false));
	}
	
	@Test
	public void shouldNotMatchCollectionsWhenTestNull() {
		// given
		final List<String> test = null;
		final List<String> control = Arrays.asList("a", "b", "d", "c");
		// when
		final boolean result = CollectionUtils.containsInAnyOrder(test, control);
		// then
		assertThat(result, equalTo(false));
	}

	@Test
	public void shouldNotMatchCollectionsWhenControlNull() {
		// given
		final List<String> test = Arrays.asList("a", "b", "d", "c");
		final List<String> control = null;
		// when
		final boolean result = CollectionUtils.containsInAnyOrder(test, control);
		// then
		assertThat(result, equalTo(false));
	}
	
}
