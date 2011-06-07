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
import org.jboss.tools.ws.jaxrs.ui.cnf.UriPathTemplateMediaTypeMappingElement.EnumCapabilityType;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;

public class UriPathTemplateElementsSorter extends ViewerSorter {

	public UriPathTemplateElementsSorter() {
	}

	public UriPathTemplateElementsSorter(Collator collator) {
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
		if (e1 instanceof UriPathTemplateMediaTypeMappingElement
				&& e2 instanceof UriPathTemplateMediaTypeMappingElement) {
			UriPathTemplateMediaTypeMappingElement element1 = (UriPathTemplateMediaTypeMappingElement) e1;
			if (element1.getType() == EnumCapabilityType.CONSUMES) {
				return -1;
			}
			return 1;
		} else if (e1 instanceof UriPathTemplateMethodMappingElement
				&& e2 instanceof UriPathTemplateMediaTypeMappingElement) {
			return 1;
		} else if (e1 instanceof UriPathTemplateMediaTypeMappingElement
				&& e2 instanceof UriPathTemplateMethodMappingElement) {
			return -1;
		}
		Logger.warn("Unexpected comparison: " + e1.getClass().getSimpleName() + " vs " + e2.getClass().getSimpleName());
		return 0;
	}
}
