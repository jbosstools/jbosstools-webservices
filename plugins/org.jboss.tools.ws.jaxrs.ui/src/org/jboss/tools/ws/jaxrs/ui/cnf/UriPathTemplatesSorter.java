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

package org.jboss.tools.ws.jaxrs.ui.cnf;

import java.text.Collator;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

public class UriPathTemplatesSorter extends ViewerSorter {

	public UriPathTemplatesSorter() {
	}

	public UriPathTemplatesSorter(Collator collator) {
		super(collator);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ViewerComparator#compare(org.eclipse.jface.
	 * viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		UriPathTemplateElement element1 = (UriPathTemplateElement) e1;
		UriPathTemplateElement element2 = (UriPathTemplateElement) e2;
		return element1.getEndpoint().compareTo(element2.getEndpoint());
	}

}
