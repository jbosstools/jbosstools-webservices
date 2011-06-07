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

import java.util.List;

import org.eclipse.jdt.core.IJavaElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.MediaTypeCapabilities;

public class UriPathTemplateMediaTypeMappingElement {

	public enum EnumCapabilityType {
		CONSUMES, PRODUCES;
	}

	private final EnumCapabilityType type;

	private final MediaTypeCapabilities mediaTypeCapabilities;

	public UriPathTemplateMediaTypeMappingElement(final MediaTypeCapabilities mediaTypes, final EnumCapabilityType type) {
		super();
		this.mediaTypeCapabilities = mediaTypes;
		this.type = type;
	}

	public EnumCapabilityType getType() {
		return type;
	}

	public IJavaElement getElement() {
		return mediaTypeCapabilities.getElement();
	}

	public List<String> getMediaTypes() {
		return mediaTypeCapabilities.getMediatypes();
	}

}
