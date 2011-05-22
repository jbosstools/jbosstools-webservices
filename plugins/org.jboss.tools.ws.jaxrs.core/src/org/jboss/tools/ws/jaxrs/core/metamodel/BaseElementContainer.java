package org.jboss.tools.ws.jaxrs.core.metamodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;

public abstract class BaseElementContainer<T extends BaseElement<?>> implements Iterable<Entry<String, T>> {

	/** the elements of the container. */
	final Map<String, T> elements = new HashMap<String, T>();

	/** the enclosing metamodel. */
	final Metamodel metamodel;

	/**
	 * Full constructor.
	 * 
	 * @param m
	 *            the enclosing metamodel
	 */
	public BaseElementContainer(final Metamodel m) {
		this.metamodel = m;
	}

	/**
	 * Adding elements from the given scope.
	 * 
	 * @throws InvalidModelElementException
	 * 
	 */
	public abstract void addFrom(final IJavaElement scope, final IProgressMonitor progressMonitor)
			throws CoreException, InvalidModelElementException;

	/**
	 * Remove an element from the container given its underlying (eclipse)
	 * resource
	 * 
	 * @param removedResource
	 * @param progressMonitor
	 */
	public final void removeElement(final IResource removedResource, final IProgressMonitor progressMonitor) {
		for (Iterator<T> iterator = elements.values().iterator(); iterator.hasNext();) {
			T element = iterator.next();
			if (removedResource.equals(element.getJavaElement().getResource())) {
				iterator.remove();
				return;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jboss.tools.ws.jaxrs.core.metamodel.IElementContainer#contains(org
	 * .eclipse.jdt.core.IType)
	 */
	public final boolean contains(final IType type) {
		return elements.containsKey(type.getFullyQualifiedName());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jboss.tools.ws.jaxrs.core.metamodel.IElementContainer#getByType(org
	 * .eclipse.jdt.core.IType)
	 */
	public final T getByType(final IType type) {
		return elements.get(type.getFullyQualifiedName());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jboss.tools.ws.jaxrs.core.metamodel.IElementContainer#getByTypeName
	 * (java.lang.String)
	 */
	public final T getByTypeName(final String typeName) {
		return elements.get(typeName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jboss.tools.ws.jaxrs.core.metamodel.IElementContainer#getAll()
	 */
	public final List<T> getAll() {
		return Collections.unmodifiableList(new ArrayList<T>(elements.values()));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jboss.tools.ws.jaxrs.core.metamodel.IElementContainer#getTypeNames()
	 */
	public final Set<String> getTypeNames() {
		return elements.keySet();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jboss.tools.ws.jaxrs.core.metamodel.IElementContainer#size()
	 */
	public final int size() {
		return elements.size();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jboss.tools.ws.jaxrs.core.metamodel.IElementContainer#iterator()
	 */
	@Override
	public final Iterator<Entry<String, T>> iterator() {
		return elements.entrySet().iterator();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jboss.tools.ws.jaxrs.core.metamodel.IElementContainer#reset()
	 */
	public void reset() {
		this.elements.clear();
	}

	@Override
	public String toString() {
		return elements.toString();
	}
}
