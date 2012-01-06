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
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder;

import java.util.EventObject;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.jboss.tools.ws.jaxrs.core.internal.utils.ConstantUtils;

/**
 * Event raised when a Resource of interest for the metamodel is changed.
 * 
 * @author xcoulon
 * 
 */
public class ResourceDelta {

	public static final int NO_FLAG = 0;

	/** The resource that changed. */
	private final IResource resource;

	/** The kind of change. */
	private final int deltaKind;

	/** Some flags to describe more precisely the kind of change. */
	private final int flags;

	/**
	 * Full constructor.
	 * 
	 * @param element
	 *            The java element that changed.
	 * @param compilationUnitAST
	 *            The associated compilation unit AST (or null if it does not
	 *            apply to the given element)
	 * @param elementType
	 *            The (detailed) Java Element kind.
	 * @param deltaKind
	 *            The kind of change.
	 * @param compilationUnitAST
	 *            the associated compilation unit AST
	 * @param flags
	 *            the detailed kind of change.
	 * @see IJavaElementDelta for element change kind values.
	 */
	public ResourceDelta(final IResource resource, final int deltaKind, final int flags) {
		this.resource = resource;
		this.deltaKind = deltaKind;
		this.flags = flags;
	}

	/**
	 * @return the element
	 */
	public IResource getResource() {
		return resource;
	}

	/**
	 * @return the deltaKind
	 */
	public int getDeltaKind() {
		return deltaKind;
	}

	/**
	 * @return the flags
	 */
	public int getFlags() {
		return flags;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder("ResourceChangedEvent [").append(ConstantUtils.getStaticFieldName(
				IResource.class, resource.getType()));
		result.append(" '").append(resource.getFullPath()).append("' ");
		result.append(ConstantUtils.getStaticFieldName(IResourceDelta.class, deltaKind).toLowerCase());
		if (flags > 0) {
			int[] f = ConstantUtils.splitConstants(IResourceDelta.class, flags, ""); //$NON-NLS-1$
			result.append(":{");
			for (int i = 0; i < f.length; i++) {
				result.append(ConstantUtils.getStaticFieldName(IResourceDelta.class, f[i], ""));
				if (i < f.length - 1) {
					result.append("+");
				}
			}
			result.append("}");
		}
		result.append("]");
		return result.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + deltaKind;
		result = prime * result + ((resource == null) ? 0 : resource.hashCode());
		result = prime * result + flags;
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final ResourceDelta other = (ResourceDelta) obj;
		if (deltaKind != other.deltaKind) {
			return false;
		}
		if (resource == null && other.resource != null) {
			return false;
		} else if (resource != null && other.resource == null) {
			return false;
		} else if (resource != null && other.resource != null && resource.getType() != other.resource.getType()) {
			return false;
		} else if (resource != null && other.resource != null && !resource.getName().equals(other.resource.getName())) {
			return false;
		}
		/*
		 * if (compilationUnitAST == null) if (other.compilationUnitAST != null)
		 * return false;
		 */
		if (flags != other.flags) {
			return false;
		}
		return true;
	}
}
