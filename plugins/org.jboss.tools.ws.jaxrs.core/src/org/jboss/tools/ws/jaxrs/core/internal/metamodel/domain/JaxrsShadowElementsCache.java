/******************************************************************************* 
 * Copyright (c) 2011 - 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsApplication;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsJavaElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsMetamodel;

/**
 * Utility class that caches the {@link IJaxrsElement} (indexed by their
 * underlying {@link IResource}) per {@link JaxrsMetamodel}. This class helps
 * tracking changes in the metamodel during the validation process, which occurs
 * *after* the project (re)build. This class is a singleton.
 * 
 * @author xcoulon
 *
 */
public class JaxrsShadowElementsCache {
	/**
	 * {@link IJaxrsElement} are indexed by their underlying resource *portable
	 * path*. The indexed value is the {@link EnumElementKind} of the element.
	 */
	private Map<String, EnumElementKind> elements = new HashMap<String, EnumElementKind>();

	/**
	 * Constructor
	 */
	protected JaxrsShadowElementsCache() {
		super();
	}

	/**
	 * Indexes all relevant {@link IJaxrsElement}s in the given
	 * {@link IJaxrsMetamodel}.
	 * 
	 * @param metamodel
	 *            the metamodel to index.
	 */
	public void index(final IJaxrsMetamodel metamodel) {
		final List<IJaxrsElement> allElements = metamodel.getAllElements();
		for (IJaxrsElement element : allElements) {
			if (isRelevantForIndexation(element)) {
				elements.put(element.getResource().getLocation().toPortableString(), element.getElementKind());
			}
		}
	}

	/**
	 * Analyzes if the element should be indexed here: it must have an
	 * underlying {@link IResource} and its associated {@link IJavaElement} (if
	 * exists) must be an {@link IType}.
	 * 
	 * @param element
	 *            the {@link IJaxrsElement} to analyze
	 * @return {@code true} if the given element should be indexed,
	 *         {@code false} otherwise
	 */
	private boolean isRelevantForIndexation(final IJaxrsElement element) {
		if (element == null || element.getResource() == null) {
			return false;
		}
		// include web.xml based Application
		if (element instanceof IJaxrsApplication) {
			return true;
		}
		if (element instanceof IJaxrsJavaElement
				&& ((IJaxrsJavaElement) element).getJavaElement().getElementType() == IJavaElement.TYPE) {
			return true;
		}
		return false;
	}

	/**
	 * Indexes the given {@link IJaxrsElement} in the given
	 * {@link IJaxrsMetamodel}.
	 * 
	 * @param element
	 *            the {@link IJaxrsElement} to index.
	 */
	public void index(final IJaxrsElement element) {
		if (element == null || element.getResource() == null) {
			return;
		}
		elements.put(element.getResource().getLocation().toPortableString(), element.getElementKind());
	}

	/**
	 * Looks up the data for the given {@link IResource} associated with the
	 * given {@link IJaxrsMetamodel}.
	 * 
	 * @param resource
	 *            the resource whose data should be retrieved.
	 */
	public EnumElementKind lookup(final IResource resource) {
		if (resource == null) {
			return null;
		}
		return elements.get(resource.getLocation().toPortableString());
	}

	/**
	 * Unindex the data for the given {@link IResource} associated with the
	 * given {@link IJaxrsMetamodel}.
	 * 
	 * @param resource
	 *            the resource whose data should be unindexed.
	 */
	public void unindex(final IResource resource) {
		if (resource == null) {
			return;
		}
		elements.remove(resource.getLocation().toPortableString());
	}
}
