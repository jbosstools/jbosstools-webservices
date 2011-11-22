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

public class UriPathTemplateMediaTypeMappingElement {

	public enum EnumCapabilityType {
		CONSUMES, PRODUCES;
	}

	private final EnumCapabilityType type;

	private final List<String> mediaTypeCapabilities;

	private final IJavaElement element;

	public UriPathTemplateMediaTypeMappingElement(final List<String> mediaTypeCapabilities,
			final EnumCapabilityType type, final IJavaElement element) {
		super();
		this.mediaTypeCapabilities = mediaTypeCapabilities;
		this.type = type;
		this.element = element;
	}

	public EnumCapabilityType getType() {
		return type;
	}

	public List<String> getMediaTypes() {
		return mediaTypeCapabilities;
	}

	public IJavaElement getElement() {
		return element;
	}

}
