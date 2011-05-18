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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.IMember;
import org.jboss.tools.ws.jaxrs.core.metamodel.BaseElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.BaseElement.EnumType;

/**
 * Collection filter utility class.
 * @author xcoulon
 *
 */
public final class CollectionFilterUtil {

	/**
	 * Private constructor of the utility class.
	 */
	private CollectionFilterUtil() {
		
	}
	
	/**
	 * Filter elements in the given collection, given their JAX-RS kind.
	 * @param <T> the java type of the elements in the collection 
	 * @param elements > the collection of elements to filter
	 * @param kind the JAX-RS kind to match in the filter
	 * @return the matching elements
	 */
	public static <T extends BaseElement<? extends IMember>> List<T> filterElementsByKind(final Collection<T> elements,
			final BaseElement.EnumType... kind) {
		List<T> matches = new ArrayList<T>();
		List<EnumType> kinds = Arrays.asList(kind);
		for (T element : elements) {
			if (kinds.contains(element.getKind())) {
				matches.add(element);
			}
		}
		return Collections.unmodifiableList(matches);
	}

}
