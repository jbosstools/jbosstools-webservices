/******************************************************************************* 
 * Copyright (c) 2012 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.ws.ui.utils;

import org.eclipse.core.runtime.IAdaptable;

/**
 * Simple container node
 * @author bfitzpat
 *
 */
public class TreeNode implements IAdaptable {
	
	private String name;
	private TreeParent parent;
	private String ref;
	private boolean isLockedFlag = false;
	private Object data;

	/**
	 * Constructor
	 * @param name
	 */
	public TreeNode(String name) {
		this.name = name;
	}
	/**
	 * Get the name (basic string)
	 * @return
	 */
	public String getName() {
		return name;
	}
	/**
	 * Set the name (basic string)
	 * @param newName
	 */
	public void setName(String newName) {
		this.name = newName;
	}
	/**
	 * Add the parent node so we can claw back up the chain
	 * @param parent
	 */
	public void setParent(TreeParent parent) {
		this.parent = parent;
	}
	/**
	 * Get the parent node
	 * @return
	 */
	public TreeParent getParent() {
		return parent;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getName();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	@SuppressWarnings("rawtypes")
	public Object getAdapter(Class key) {
		return null;
	}
	/**
	 * Get the referenced node
	 * @return
	 */
	public String getRef() {
		return ref;
	}
	/**
	 * Set the referenced node
	 * @param ref
	 */
	public void setRef(String ref) {
		this.ref = ref;
	}
	/**
	 * Is this node locked for movement?
	 * @return
	 */
	protected boolean isMovementLocked() {
		return isLockedFlag;
	}
	/**
	 * Set the "isLocked" flag
	 * @param isLocked
	 */
	protected void setIsMovementLocked(boolean isLocked) {
		this.isLockedFlag = isLocked;
	}
	/**
	 * Return the stashed Java object
	 * @return
	 */
	public Object getData() {
		return data;
	}
	/**
	 * Stash a java object
	 * @param data
	 */
	protected void setData(Object data) {
		this.data = data;
	}
}