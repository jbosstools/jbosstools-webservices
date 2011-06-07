/**
 * 
 */
package org.jboss.tools.ws.jaxrs.core.metamodel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author xcoulon
 * 
 */
public class ResourceMethodNode {

	private final ResourceMethod element;

	private final ResourceMethodNode parent;

	private final List<ResourceMethodNode> children = new ArrayList<ResourceMethodNode>();

	public ResourceMethodNode(ResourceMethodNode parent, ResourceMethod element) {
		this.parent = parent;
		this.element = element;
	}

	/**
	 * @return the element
	 */
	public ResourceMethod getElement() {
		return element;
	}

	/**
	 * @return the parent
	 */
	public ResourceMethodNode getParent() {
		return parent;
	}

	/**
	 * @return the children
	 */
	public List<ResourceMethodNode> getChildren() {
		return children;
	}

	/**
	 * @return true if this node is a Leaf (ie, it has no child node).
	 */
	public boolean isLeaf() {
		return children.isEmpty();
	}

	/**
	 * @return True if this node is is the root node (ie, it has no parent
	 *         node).
	 */
	public boolean isRoot() {
		return (parent == null);
	}

	public Set<ResourceMethodNode> remove() {
		Set<ResourceMethodNode> removedNodes = new HashSet<ResourceMethodNode>();
		removedNodes.add(this);
		for (Iterator<ResourceMethodNode> iterator = children.iterator(); iterator.hasNext();) {
			ResourceMethodNode child = iterator.next();
			removedNodes.addAll(child.remove());
			iterator.remove();
		}
		return removedNodes;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder(element.toString());
		if (!children.isEmpty()) {
			buffer.append("\nchildren:\n");
			for (ResourceMethodNode child : children) {
				buffer.append(" ").append(child).append("\n");
			}
		}
		return buffer.toString();
	}
}
