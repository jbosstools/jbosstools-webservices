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

package org.jboss.tools.ws.jaxrs.core.metamodel;

import java.util.ArrayList;
import java.util.List;

/**
 * Application classes can declare the supported request and response media
 * types using the @Consumes and @Produces annotations respectively. These
 * annotations MAY be applied to a resource method, a resource class, or to an
 * entity provider. Use of these annotations on a resource method overrides any
 * on the resource class or on an entity provider for a method argument or
 * return type. In the absence of either of these annotations, support for any
 * media type is assumed.
 * 
 * @author xcoulon
 * 
 */
public class MediaTypeCapabilities implements Comparable<MediaTypeCapabilities> {

	private final List<String> consumedMimeTypes = new ArrayList<String>();

	private final List<String> producedMimeTypes = new ArrayList<String>();

	/**
	 * Constructor with default values.
	 * 
	 * @param consumedMimeTypes
	 * @param producedMimeTypes
	 */
	public MediaTypeCapabilities() {
	}

	/**
	 * Constructor with specific values.
	 * 
	 * @param consumedMimeTypes
	 * @param producedMimeTypes
	 */
	public MediaTypeCapabilities(final List<String> consumedMimeTypes, final List<String> producedMimeTypes) {
		if (consumedMimeTypes != null) {
			this.consumedMimeTypes.addAll(consumedMimeTypes);
		}
		if (producedMimeTypes != null) {
			this.producedMimeTypes.addAll(producedMimeTypes);
		}
	}

	/**
	 * @return the consumedMimeTypes
	 */
	public final List<String> getConsumedMimeTypes() {
		return consumedMimeTypes;
	}

	/**
	 * @return the producedMimeTypes. Never null, size can be 0
	 */
	public final List<String> getProducedMimeTypes() {
		return producedMimeTypes;
	}

	
	public void merge(MediaTypeCapabilities mediaTypeCapabilities) {
		setConsumedMimeTypes(mediaTypeCapabilities.getConsumedMimeTypes());
		setProducedMimeTypes(mediaTypeCapabilities.getProducedMimeTypes());
	}


	/**
	 * Clears the previous consumed types and replaces with the one given in parametets.
	 * @param consumes the list of new consumed mediatypes
	 */
	public final void setConsumedMimeTypes(final List<String> consumes) {
		this.consumedMimeTypes.clear();
		if (consumes != null) {
			this.consumedMimeTypes.addAll(consumes);
		}
	}

	public final void setProducedMimeTypes(final List<String> produces) {
		this.producedMimeTypes.clear();
		if (produces != null) {
			this.producedMimeTypes.addAll(produces);
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((consumedMimeTypes == null) ? 0 : consumedMimeTypes.hashCode());
		result = prime * result + ((producedMimeTypes == null) ? 0 : producedMimeTypes.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public final boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		MediaTypeCapabilities other = (MediaTypeCapabilities) obj;
		if (consumedMimeTypes == null) {
			if (other.consumedMimeTypes != null) {
				return false;
			}
		} else if (!consumedMimeTypes.equals(other.consumedMimeTypes)) {
			return false;
		}
		if (producedMimeTypes == null) {
			if (other.producedMimeTypes != null) {
				return false;
			}
		} else if (!producedMimeTypes.equals(other.producedMimeTypes)) {
			return false;
		}
		return true;
	}

	@Override
	public final int compareTo(final MediaTypeCapabilities other) {
		int comp = compare(this.getConsumedMimeTypes(), other.getConsumedMimeTypes());
		if(comp != 0) {
			return comp;
		}
		return compare(this.getProducedMimeTypes(), other.getProducedMimeTypes());
	}
	
	private static int compare(final List<String> oneList, final List<String> otherList) {
		for(int i = 0; i < oneList.size(); i++) {
			if(i >= otherList.size()) {
				return 1; // 'this' is greater than 'other'
			}
			int comp = oneList.get(i).compareTo(otherList.get(i));
			if(comp != 0) {
				return comp;
			}
		}
		return 0;
	}


}
