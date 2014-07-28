/**
 * 
 */
package org.jboss.tools.ws.jaxrs.core.metamodel.domain;

import org.eclipse.jdt.core.IJavaElement;

/**
 * @author xcoulon
 *
 */
public interface IJaxrsJavaElement extends IJaxrsElement, IAnnotatedElement {

	/**
	 * @return the underlying {@link IJaxrsElement} for this JAX-RS Element.
	 */
	IJavaElement getJavaElement();
}
