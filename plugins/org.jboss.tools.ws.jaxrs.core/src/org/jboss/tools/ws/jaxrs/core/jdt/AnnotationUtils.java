/******************************************************************************* 
 * Copyright (c) 2008 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.ws.jaxrs.core.jdt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames;

/**
 * Utility class to for {@link Annotation}
 * 
 * @author xcoulon
 *
 */
public class AnnotationUtils {

	/**
	 * private constructor of the utility class
	 */
	private AnnotationUtils() {
	}

	/**
	 * Extracts all templates parameters from the given annotation, assuming it
	 * is a {@code @Path} annotation. For example, for an annotation such as:
	 * {@code @Path("/ foo}/{bar: [a-z]}), this method will return a list of 2
	 * strings: {@code "foo"} and {@code "bar"}.
	 * 
	 * @param pathAnnotation
	 *            the annotation to manipulate
	 * @return the list of templates found in the given annotation.
	 */
	public static Map<String, Annotation> extractTemplateParameters(final Annotation pathAnnotation) {
		final Map<String, Annotation> proposals = new HashMap<String, Annotation>();
		if (pathAnnotation != null && pathAnnotation.getFullyQualifiedName().equals(JaxrsClassnames.PATH)) {
			if (pathAnnotation != null && pathAnnotation.getValue() != null) {
				final String value = pathAnnotation.getValue();
				final List<String> params = extractParamsFromUriTemplateFragment(value);
				for (String param : params) {
					proposals.put(param, pathAnnotation);
				}
			}
		}
		return proposals;
	}

	/**
	 * Extracts all the character sequences inside of curly braces ('{' and '}')
	 * and returns them as a list of strings
	 * 
	 * @param value
	 *            the given value
	 * @return the list of character sequences, or an empty list
	 */
	public static List<String> extractParamsFromUriTemplateFragment(String value) {
		List<String> params = new ArrayList<String>();
		int beginIndex = -1;
		while ((beginIndex = value.indexOf("{", beginIndex + 1)) != -1) {
			int semicolonIndex = value.indexOf(":", beginIndex);
			int closingCurlyBraketIndex = value.indexOf("}", beginIndex);
			int endIndex = (semicolonIndex != -1) ? Math.min(semicolonIndex, closingCurlyBraketIndex)
					: closingCurlyBraketIndex;
			params.add(value.substring(beginIndex + 1, endIndex).trim());
		}
		return params;
	}

}
