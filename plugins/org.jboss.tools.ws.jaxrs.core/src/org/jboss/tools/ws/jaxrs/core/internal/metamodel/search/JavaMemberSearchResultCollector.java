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

package org.jboss.tools.ws.jaxrs.core.internal.metamodel.search;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchRequestor;

/**
 * Java Search results collector. Filters the results to only keep the java
 * element kind given to the constructor.
 * 
 * @author xcoulon
 * 
 */
public class JavaMemberSearchResultCollector extends SearchRequestor {

	/** The filtered search results. */
	private final ArrayList<IMember> resultMembers = new ArrayList<IMember>();

	/** The expected kind of java elements to filter. */
	private final int kind;

	/** The search scope. */
	private final IJavaSearchScope searchScope;

	/**
	 * Full constructor.
	 * 
	 * @param k
	 *            the java element kind
	 * @param scope
	 *            the scope of the search
	 */
	public JavaMemberSearchResultCollector(final int k, final IJavaSearchScope scope) {
		this.kind = k;
		this.searchScope = scope;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void acceptSearchMatch(final SearchMatch match) throws CoreException {
		IMember element = (IMember) match.getElement();
		if (!searchScope.encloses(element)) {
			return;
		}
		// check that the element matches the expected kind, and avoid duplicate
		// results
		IMember member = (IMember) element.getAncestor(kind);
		if (member != null && !resultMembers.contains(member)) {
			resultMembers.add(member);
		}
	}

	/**
	 * @param clazz
	 *            the expected class type of the returned elements (used for
	 *            strong result typing)
	 * @param <T>
	 *            the type of the return elements
	 * @return the resultMembers
	 */
	@SuppressWarnings("unchecked")
	public final <T> Set<T> getResult() {
		final Set<T> results = new HashSet<T>();
		for (IMember member : resultMembers) {
			results.add((T) member);
		}
		return results;
	}

	/**
	 * @return {@code false} if no element was found during the search, {@code true} otherwise. 
	 */
	public boolean hasResult() {
		return !resultMembers.isEmpty();
	}

}
