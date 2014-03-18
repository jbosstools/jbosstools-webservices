/**
 * 
 */
package org.jboss.tools.ws.jaxrs.ui.internal.validation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.AbstractJaxrsJavaTypeElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsApplication;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsJavaElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsMetamodel;

/**
 * Utility class that caches the {@link IJaxrsElement} (indexed by their underlying {@link IResource}) per
 * {@link JaxrsMetamodel}. This class helps tracking changes in the metamodel during the validation process, which
 * occurs *after* the project (re)build. This class is a singleton.
 * 
 * @author xcoulon
 *
 */
public class JaxrsElementsCache {
	/** the singleton instance. */
	private static final JaxrsElementsCache instance = new JaxrsElementsCache();
	/**
	 * {@link IJaxrsElement} are indexed by their associated {@link IJaxrsMetamodel} identifier and their underlying
	 * resource *portable path*. The indexed value is the {@link EnumElementKind} of the element.
	 */
	private Map<String, Map<String, EnumElementKind>> mainCache = new HashMap<String, Map<String, EnumElementKind>>();

	/**
	 * Singleton constructor
	 */
	private JaxrsElementsCache() {
		super();
	}

	public static JaxrsElementsCache getInstance() {
		return instance;
	}

	/**
	 * Indexes all relevant {@link IJaxrsElement}s in the given {@link IJaxrsMetamodel}.
	 * 
	 * @param metamodel
	 *            the metamodel to index.
	 */
	public void index(final IJaxrsMetamodel metamodel) {
		final String metamodelIdentifier = metamodel.getIdentifier();
		final HashMap<String, EnumElementKind> cachedElements = new HashMap<String, EnumElementKind>();
		mainCache.put(metamodelIdentifier, cachedElements);
		final List<IJaxrsElement> allElements = metamodel.getAllElements();
		for (IJaxrsElement element : allElements) {
			if (isRelevantForIndexation(element)) {
				cachedElements.put(element.getResource().getLocation().toPortableString(), element.getElementKind());
			}
		}
	}

	/**
	 * Analyzes if the element should be indexed here: it must have an underlying {@link IResource} and its associated
	 * {@link IJavaElement} (if exists) must be an {@link IType}.
	 * 
	 * @param element
	 *            the {@link IJaxrsElement} to analyze
	 * @return {@code true} if the given element should be indexed, {@code false} otherwise
	 */
	private boolean isRelevantForIndexation(final IJaxrsElement element) {
		if (element == null || element.getResource() == null) {
			return false;
		}
		// include web.xml based Application
		if (element instanceof IJaxrsApplication) {
			return true;
		}
		if (element instanceof IJaxrsJavaElement
				&& ((IJaxrsJavaElement) element).getJavaElement().getElementType() == IJavaElement.TYPE) {
			return true;
		}
		return false;
	}

	/**
	 * Indexes the given {@link IJaxrsElement} in the given {@link IJaxrsMetamodel}.
	 * 
	 * @param element
	 *            the {@link IJaxrsElement} to index.
	 */
	public void index(final IJaxrsElement element) {
		if (element == null || element.getResource() == null || !(element instanceof AbstractJaxrsJavaTypeElement)) {
			return;
		}
		final IJaxrsMetamodel metamodel = element.getMetamodel();
		final String metamodelIdentifier = metamodel.getIdentifier();
		if (!mainCache.containsKey(metamodelIdentifier)) {
			final HashMap<String, EnumElementKind> cachedElements = new HashMap<String, EnumElementKind>();
			mainCache.put(metamodelIdentifier, cachedElements);
		}
		mainCache.get(metamodelIdentifier).put(element.getResource().getLocation().toPortableString(),
				element.getElementKind());
	}

	/**
	 * Looks up the data for the given {@link IResource} associated with the given {@link IJaxrsMetamodel}.
	 * 
	 * @param metamodel
	 *            the contextual metamodel.
	 * @param resource
	 *            the resource whose data should be retrieved.
	 */
	public EnumElementKind lookup(final IJaxrsMetamodel metamodel, final IResource resource) {
		if (resource == null) {
			return null;
		}
		final String metamodelIdentifier = metamodel.getIdentifier();
		final Map<String, EnumElementKind> cachedElements = mainCache.get(metamodelIdentifier);
		if (cachedElements != null) {
			return cachedElements.get(resource.getLocation().toPortableString());
		}
		return null;
	}

	/**
	 * Unindex the data for the given {@link IResource} associated with the given {@link IJaxrsMetamodel}.
	 * 
	 * @param metamodel
	 *            the contextual metamodel.
	 * @param resource
	 *            the resource whose data should be unindexed.
	 */
	public void unindex(final IJaxrsMetamodel metamodel, final IResource resource) {
		if (resource == null) {
			return;
		}
		final String metamodelIdentifier = metamodel.getIdentifier();
		final Map<String, EnumElementKind> cachedElements = mainCache.get(metamodelIdentifier);
		if (cachedElements != null) {
			cachedElements.remove(resource.getLocation().toPortableString());
		}
	}
}
