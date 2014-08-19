/******************************************************************************* 
 * Copyright (c) 2008 - 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain;

import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.BEAN_PARAM;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.COOKIE_PARAM;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.FORM_PARAM;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.HEADER_PARAM;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.MATRIX_PARAM;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PATH_PARAM;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.QUERY_PARAM;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils;

/**
 * All JAX-RS xxxxParam annotations that are recognized and relevant for the tooling.
 * @author xcoulon
 *
 */
public class JaxrsParamAnnotations {

	public static final List<String> PARAM_ANNOTATIONS = Arrays.asList(PATH_PARAM, QUERY_PARAM, MATRIX_PARAM, COOKIE_PARAM, HEADER_PARAM, FORM_PARAM, BEAN_PARAM);
	
	/**
	 * Checks if at least one of the given annotation fully qualified names exists in the list of relevant JAX-RS xxxxParam annotation names.
	 * @param annotationNames the fully qualified names of the annotations to match against the {@link JaxrsParamAnnotations#PARAM_ANNOTATIONS} list.
	 * @return {@code true} if at least one of the given annotation names matches, {@code false} otherwise.
	 */
	public static boolean matchesAtLeastOne(final Collection<String> annotationNames) {
		return CollectionUtils.hasIntersection(annotationNames, PARAM_ANNOTATIONS);
	}

}
