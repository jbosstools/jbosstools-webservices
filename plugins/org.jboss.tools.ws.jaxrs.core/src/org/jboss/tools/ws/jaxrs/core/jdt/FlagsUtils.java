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

package org.jboss.tools.ws.jaxrs.core.jdt;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsWebxmlApplication;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IAnnotatedElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta;
import org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames;

/**
 * @author xcoulon
 *
 */
public class FlagsUtils {

	/**
	 * Private constructor for the utility class. 
	 */
	private FlagsUtils() {
	}

	/**
	 * @return the {@link Flags} that describe the kind of annotations that the given {@code element} carries.
	 * @param element the element to analyze
	 */
	public static Flags computeElementFlags(final IJaxrsElement element) {
		final Flags flags = new Flags();
		if(element == null) {
			return flags;
		}
		final JaxrsMetamodel metamodel = (JaxrsMetamodel) element.getMetamodel();
		final Set<String> httpMethodNames = new HashSet<String>();
		if(metamodel != null) {
			httpMethodNames.addAll(metamodel.findAllHttpMethodNames());
		}
		if(element instanceof IAnnotatedElement) {
			final Map<String, Annotation> annotations = ((IAnnotatedElement)element).getAnnotations();
			for(Entry<String, Annotation> entry : annotations.entrySet()) {
				final String annotationName = entry.getValue().getFullyQualifiedName();
				if(annotationName.equals(JaxrsClassnames.APPLICATION_PATH)) {
					flags.addFlags(JaxrsElementDelta.F_APPLICATION_PATH_ANNOTATION);
				} else if(annotationName.equals(JaxrsClassnames.BEAN_PARAM)) {
						flags.addFlags(JaxrsElementDelta.F_BEAN_PARAM_ANNOTATION);
				} else if(annotationName.equals(JaxrsClassnames.CONSUMES)) {
					flags.addFlags(JaxrsElementDelta.F_CONSUMES_ANNOTATION);
				} else if(annotationName.equals(JaxrsClassnames.DEFAULT_VALUE)) {
					flags.addFlags(JaxrsElementDelta.F_DEFAULT_VALUE_ANNOTATION);
				} else if(annotationName.equals(JaxrsClassnames.FORM_PARAM)) {
					flags.addFlags(JaxrsElementDelta.F_FORM_PARAM_ANNOTATION);
				} else if(annotationName.equals(JaxrsClassnames.MATRIX_PARAM)) {
					flags.addFlags(JaxrsElementDelta.F_MATRIX_PARAM_ANNOTATION);
				} else if(annotationName.equals(JaxrsClassnames.NAME_BINDING)) {
					flags.addFlags(JaxrsElementDelta.F_NAME_BINDING_ANNOTATION);
				} else if(annotationName.equals(JaxrsClassnames.PATH)) {
					flags.addFlags(JaxrsElementDelta.F_PATH_ANNOTATION);
				} else if(annotationName.equals(JaxrsClassnames.PATH_PARAM)) {
					flags.addFlags(JaxrsElementDelta.F_PATH_PARAM_ANNOTATION);
				} else if(annotationName.equals(JaxrsClassnames.PROVIDER)) {
					flags.addFlags(JaxrsElementDelta.F_PROVIDER_ANNOTATION);
				} else if(annotationName.equals(JaxrsClassnames.PRODUCES)) {
					flags.addFlags(JaxrsElementDelta.F_PRODUCES_ANNOTATION);
				} else if(annotationName.equals(JaxrsClassnames.QUERY_PARAM)) {
					flags.addFlags(JaxrsElementDelta.F_QUERY_PARAM_ANNOTATION);
				} else if(annotationName.equals(JaxrsClassnames.RETENTION)) {
					flags.addFlags(JaxrsElementDelta.F_RETENTION_ANNOTATION);
				} else if(annotationName.equals(JaxrsClassnames.TARGET)) {
					flags.addFlags(JaxrsElementDelta.F_TARGET_ANNOTATION);
				} else if(httpMethodNames.contains(annotationName)) {
					flags.addFlags(JaxrsElementDelta.F_HTTP_METHOD_ANNOTATION);
				}
			}
		} else if(element instanceof JaxrsWebxmlApplication) {
			final JaxrsWebxmlApplication webxmlApplication = (JaxrsWebxmlApplication) element;
			if(webxmlApplication.isOverride()) {
				flags.addFlags(JaxrsElementDelta.F_APPLICATION_PATH_VALUE_OVERRIDE);
			} 
			flags.addFlags(JaxrsElementDelta.F_APPLICATION_CLASS_NAME);
			flags.addFlags(JaxrsElementDelta.F_APPLICATION_PATH_ANNOTATION);
		}
		
		return flags;
	}

}
