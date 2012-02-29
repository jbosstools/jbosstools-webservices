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

import java.util.ArrayList;

/**
 * Extend the base node so it can track a list of children
 * @author bfitzpat
 */
public class TreeParent extends TreeNode {

	private ArrayList<TreeNode> children;
	/**
	 * Constructor
	 * @param name
	 */
	public TreeParent(String name) {
		super(name);
		children = new ArrayList<TreeNode>();
	}
	/**
	 * Add a child to the child list
	 * @param child
	 */
	public void addChild(TreeNode child) {
		children.add(child);
		child.setParent(this);
	}
	/**
	 * Remove a child from the child list
	 * @param child
	 */
	public void removeChild(TreeNode child) {
		children.remove(child);
		child.setParent(null);
	}
	/**
	 * Get the list of children
	 * @return
	 */
	public TreeNode [] getChildren() {
		return (TreeNode [])children.toArray(new TreeNode[children.size()]);
	}
	/**
	 * Does the node have children?
	 * @return
	 */
	public boolean hasChildren() {
		return children.size()>0;
	}
}
