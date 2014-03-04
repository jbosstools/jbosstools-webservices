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

package org.jboss.tools.ws.ui.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.tools.ws.ui.dialogs.EnumParamType;
import org.jboss.tools.ws.ui.dialogs.URLTemplateParameter;
import org.jboss.tools.ws.ui.dialogs.URLTemplateParameter.Builder;

/**
 * Utility class to parse an URL Template and extract all the parameters from
 * it.
 * 
 * @author xcoulon
 * 
 */
public class JAXRSPathTemplateParser {

	/** Pattern to match expression such as {@code "{name:int}" }.*/
	private static final Pattern pathParamPattern = Pattern.compile("\\{(\\w+):(.*)\\}"); //$NON-NLS-1$
	
	/** Pattern to match expression such as {@code "name={int}" }.*/
	private static final Pattern optionalParamsPattern = Pattern.compile("(;|\\?|&)(\\w+)=\\{(.*)\\}"); //$NON-NLS-1$
	/**
	 * Parses the given urlTemplate and extracts all
	 * {@link URLTemplateParameter}s from it.
	 * <pathParamPattern>
	 * Supports a mix of syntaxes:
	 * <ul>
	 * <li>{@code /rest/members/id:int}</li>
	 * <li>{@code /rest/members/id:[0-9][0-9]*}</li>
	 * <li>{@code /rest/members;matrix=int}?start={int}&size={int}}</li>
	 * 
	 * and supports also syntax with {@code List} and {@code Set}, such as:
	 * {@code /rest/members/query?from=int&to={int}&orderBy={orderBy:List&lt;String&gt;}}
	 * 
	 * </ul>
	 * 
	 * <p>Finally, it should also work with the following kind of syntax:</p>
	 * {@code rest/members/user/{id:int}/{encoding:(/encoding/[^/]+?)?};matrix={String}?start={int}
	 * 
	 * @param urlTemplate
	 * @return a list of {@link URLTemplateParameter}s, or emty list if none was
	 *         found.
	 * 
	 */
	public static URLTemplateParameter[] parse(final String urlTemplate) {
		// quick fail
		if (urlTemplate == null || urlTemplate.isEmpty()) {
			return new URLTemplateParameter[0];
		}
		final List<URLTemplateParameter> templateParameters = new ArrayList<URLTemplateParameter>();
		// let's scan the given template, looking for content between '{' and
		// '}' characters
		int scanIndex = 0;
		while (scanIndex != -1 || scanIndex >= urlTemplate.length()) {
			final int beginIndex = urlTemplate.indexOf('{', scanIndex);
			final int endIndex = urlTemplate.indexOf('}', beginIndex);
			if (beginIndex == -1 || endIndex == -1) {
				break;
			}
			String expression = urlTemplate.substring(beginIndex, endIndex + 1);
			// expression is of the form: {$name:$type}
			if (expression.contains(":")) { //$NON-NLS-1$
				final Matcher m = pathParamPattern.matcher(expression);
				if(m.matches()) {
					final URLTemplateParameter templateParameter = URLTemplateParameter.Builder.from(expression)
							.withName(m.group(1), true).withDatatype(m.group(2)).withParamType(EnumParamType.PATH_PARAM).build();
					templateParameters.add(templateParameter);
				}
			}
			// expression is of the form: $name={$type}, we should rewind the
			// scanner
			else {
				final String prefix = urlTemplate.substring(scanIndex, beginIndex);
				final int rewindedLocation = CharSearcher.findLastIndexOf(';', '?', '&').in(prefix);
				expression = urlTemplate.substring(scanIndex + rewindedLocation, endIndex + 1);
				final Matcher m = optionalParamsPattern.matcher(expression);
				if(m.matches()) {
					final Builder builder = URLTemplateParameter.Builder.from(expression)
						.withName(m.group(2), false).withDatatype(m.group(3));
					char separator = prefix.charAt(rewindedLocation);
					switch(separator) {
					case ';':
						builder.withParamType(EnumParamType.MATRIX_PARAM);
						break;
					case '?':
					case '&':
						builder.withParamType(EnumParamType.QUERY_PARAM);
						break;
					}
					final URLTemplateParameter templateParameter = builder.build();
					templateParameters.add(templateParameter);
				}
			}
			scanIndex = endIndex+1;
		}

		return templateParameters.toArray(new URLTemplateParameter[templateParameters.size()]);
	}

	/**
	 * Utility class to search for the last position of a token amongst a set of
	 * given tokens in a {@link String}
	 * 
	 * @author xcoulon
	 * 
	 */
	static class CharSearcher {

		private final char[] tokens;

		private CharSearcher(char[] tokens) {
			this.tokens = tokens;
		}

		static CharSearcher findLastIndexOf(final char... tokens) {
			return new CharSearcher(tokens);
		}

		/**
		 * Returns the highest index of the tokens locations in the given
		 * content
		 * 
		 * @param content
		 * @return
		 */
		public int in(final String content) {
			int location = -1;
			for(char token : tokens) {
				location = Math.max(location, content.lastIndexOf(token));
			}
			return location;
		}

	}

}
