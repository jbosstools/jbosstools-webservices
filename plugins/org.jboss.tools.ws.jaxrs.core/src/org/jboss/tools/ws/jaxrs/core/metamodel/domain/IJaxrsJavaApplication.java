/**
 * 
 */
package org.jboss.tools.ws.jaxrs.core.metamodel.domain;

import java.util.Map;

import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsJavaApplication;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;

/**
 * Public Interface for Java-based JAX-RS Applications.
 * @author xcoulon
 *
 */
public interface IJaxrsJavaApplication extends IJaxrsApplication {
	
	/**
	 * @return the list of Name Binding annotations indexed by their fully qualified name on the element (relevant for
	 *         {@link JaxrsResource}, {@link JaxrsResourceMethod} and
	 *         {@link JaxrsJavaApplication}).
	 */
	public Map<String, Annotation> getNameBindingAnnotations();
}
