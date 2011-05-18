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

import java.util.Arrays;
import java.util.List;

public class UriPathTemplateMediaTypeMappingElement {

	public enum EnumMediaType {
		CONSUMES, PROVIDES;
	}

	private final List<String> mediaTypes;

	private final EnumMediaType mediaType;

	public UriPathTemplateMediaTypeMappingElement(List<String> mediaTypes, EnumMediaType mediaType) {
		super();
		this.mediaType = mediaType;
		if (mediaTypes != null && !mediaTypes.isEmpty()) {
			this.mediaTypes = mediaTypes;
		} else {
			this.mediaTypes = Arrays.asList("*/*");
		}
	}

	/**
	 * @return the mediaTypes
	 */
	public List<String> getMediaTypes() {
		return mediaTypes;
	}

	/**
	 * @return the mediaType
	 */
	public EnumMediaType getMediaType() {
		return mediaType;
	}

}
