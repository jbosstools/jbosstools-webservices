package org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain;

import java.util.List;

import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsApplication;

public abstract class JaxrsApplication extends JaxrsElement<IType> implements IJaxrsApplication {
	
	
	public JaxrsApplication(IType element, Annotation annotation, JaxrsMetamodel metamodel) {
		super(element, annotation, metamodel);
	}

	public JaxrsApplication(IType element, List<Annotation> annotations, JaxrsMetamodel metamodel) {
		super(element, annotations, metamodel);
	}
	
	/**
	 * @return the value of the {@link javax.ws.rs.ApplicationPath} annotation or null if it does not exist in te
	 * associated java type.
	 */
	public abstract String getApplicationPath();
	
	public abstract int update(JaxrsApplication eventApplication);
}