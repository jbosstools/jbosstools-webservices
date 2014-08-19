/******************************************************************************* 
 * Copyright (c) 2011 - 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/

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
