/**
 * 
 */
package org.jboss.tools.ws.jaxrs.core.metamodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.metamodel.BaseElement.EnumKind;
import org.jboss.tools.ws.jaxrs.core.utils.JdtUtils;

/**
 * @author xcoulon
 * 
 */
public class Routes {

	/** The JAX-RS metamodel. */
	private final Metamodel metamodel;

	/**
	 * The set of trees. A tree has a root node and has some children. Each path
	 * of the tree is a route.
	 */
	// private final Set<ResourceMethodNode> trees = new
	// HashSet<ResourceMethodNode>();

	private final List<Route> routes = new ArrayList<Route>();

	private final Map<ResourceMethod, List<Route>> routesIndex = new HashMap<ResourceMethod, List<Route>>();

	private final Map<ResourceMethod, ResourceMethodNode> nodesIndex = new HashMap<ResourceMethod, ResourceMethodNode>();

	/**
	 * @param m
	 */
	public Routes(Metamodel metamodel) {
		this.metamodel = metamodel;
	}

	public void addFrom(ResourceMethod resourceMethod, IProgressMonitor progressMonitor) throws CoreException {
		switch (resourceMethod.getParentResource().getKind()) {
		case ROOT_RESOURCE:
			computeNodes(null, resourceMethod, progressMonitor);
			break;
		case SUBRESOURCE:
			List<ResourceMethod> subresourceLocators = metamodel.getResources().findSubresourceLocators(
					resourceMethod.getParentResource().getJavaElement(), progressMonitor);
			for (ResourceMethod subresourceLocator : subresourceLocators) {
				ResourceMethodNode subresourceLocatorNode = nodesIndex.get(subresourceLocator);
				if (subresourceLocatorNode != null) {
					computeNodes(subresourceLocatorNode, resourceMethod, progressMonitor);
					// trees.addAll(addedNodes);
				}
			}
			break;
		}
	}

	public void merge(ResourceMethod resourceMethod, IProgressMonitor progressMonitor) throws CoreException {
		removeFrom(resourceMethod, progressMonitor);
		addFrom(resourceMethod, progressMonitor);
	}

	public void removeFrom(Resource removedResource, IProgressMonitor progressMonitor) {
		if (removedResource == null) {
			return;
		}
		for (ResourceMethod removedResourceMethod : removedResource.getAllMethods()) {
			removeFrom(removedResourceMethod, progressMonitor);
		}
	}

	public void removeFrom(ResourceMethod removedResourceMethod, IProgressMonitor progressMonitor) {
		if (removedResourceMethod == null) {
			return;
		}
		if (this.routesIndex.containsKey(removedResourceMethod)) {
			List<Route> routesToRemove = routesIndex.get(removedResourceMethod);
			for (Route route : routesToRemove) {
				this.routes.remove(route);
			}
			this.routesIndex.remove(removedResourceMethod);
		}

		if (this.nodesIndex.containsKey(removedResourceMethod)) {
			ResourceMethodNode resourceMethodNode = this.nodesIndex.get(removedResourceMethod);
			Set<ResourceMethodNode> removedNodes = resourceMethodNode.remove();
			for (ResourceMethodNode removedNode : removedNodes) {
				this.nodesIndex.remove(removedNode.getElement());
			}
		}

	}

	private List<ResourceMethodNode> computeNodes(final ResourceMethodNode parent, final ResourceMethod resourceMethod,
			final IProgressMonitor progressMonitor) throws CoreException {
		List<ResourceMethodNode> childrenNodes = new ArrayList<ResourceMethodNode>();
		ResourceMethodNode node = new ResourceMethodNode(parent, resourceMethod);
		childrenNodes.add(node);
		nodesIndex.put(resourceMethod, node);
		switch (resourceMethod.getKind()) {
		// leaf node: can compute route from this node back to root parent node
		case RESOURCE_METHOD:
		case SUBRESOURCE_METHOD:
			computeRoute(node);
			break;
		case SUBRESOURCE_LOCATOR:
			IType returnType = resourceMethod.getReturnType();
			if (returnType == null) {
				Logger.warn("No return type defined for subresource locator method "
						+ resourceMethod.getJavaElement().getElementName());
				break;
			}
			ITypeHierarchy subresourceTypeHierarchy = JdtUtils.resolveTypeHierarchy(returnType, false, progressMonitor);
			for (IType subresourceType : subresourceTypeHierarchy.getSubtypes(returnType)) {
				Resource subresource = metamodel.getResources().getByType(subresourceType);
				if (subresource != null && !subresource.equals(resourceMethod.getParentResource())
						&& subresource.getKind() == EnumKind.SUBRESOURCE) {
					for (ResourceMethod subresourceMethod : subresource.getAllMethods()) {
						node.getChildren().addAll(computeNodes(node, subresourceMethod, progressMonitor));
					}
				}
			}
			childrenNodes.add(node);
		}
		return childrenNodes;
	}

	/**
	 * Compute the route from the given leaf node.
	 */
	private Route computeRoute(final ResourceMethodNode leafNode) {
		LinkedList<ResourceMethod> resourceMethods = new LinkedList<ResourceMethod>();
		ResourceMethodNode node = leafNode;
		while (node != null) {
			resourceMethods.addFirst(node.getElement());
			node = node.getParent();
		}
		try {
			Route route = new Route(resourceMethods);
			if (!routes.contains(route)) {
				routes.add(route);
			}
			for (ResourceMethod rm : resourceMethods) {
				// avoid duplicates
				if (!this.routesIndex.containsKey(rm)) {
					this.routesIndex.put(rm, new ArrayList<Route>());
				}
				if (!this.routesIndex.get(rm).contains(route)) {
					Logger.debug("Added route " + route.toString());
					this.routesIndex.get(rm).add(route);
				}
			}
			return route;
		} catch (InvalidModelElementException e) {
			Logger.error("Failed to compute route", e);
		}
		return null;
	}

	/**
	 * @return the routesIndex
	 */
	public List<Route> getAll() {
		Collections.sort(routes);
		return routes;
	}

	/**
	 * @return the routes that contain this method or null if none found.
	 */
	public List<Route> getByResourceMethod(ResourceMethod resourceMethod) {
		return routesIndex.get(resourceMethod);
	}

	public final ResourceMethod getByMapping(final HTTPMethod httpMethod, final String uriPathTemplateFragment,
			final String consumes, final String produces) {
		for (Entry<ResourceMethod, List<Route>> entry : routesIndex.entrySet()) {
			for (Route route : entry.getValue()) {
				if (route.getEndpoint().matches(httpMethod, uriPathTemplateFragment, consumes, produces)) {
					return route.getResourceMethods().getLast();
				}
			}
		}
		return null;
	}

	/**
	 * @return the metamodel
	 */
	public Metamodel getMetamodel() {
		return metamodel;
	}

	/**
	 * Clears all data.
	 */
	public void reset() {
		this.routes.clear();
		this.routesIndex.clear();
		this.nodesIndex.clear();
	}

}
