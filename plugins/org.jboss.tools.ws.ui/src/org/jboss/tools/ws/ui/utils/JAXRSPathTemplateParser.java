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

package org.jboss.tools.ws.ui.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.tools.ws.ui.dialogs.EnumParamType;
import org.jboss.tools.ws.ui.dialogs.URLTemplateParameter;

/**
 * Utility class to parse an URL Template and extract all the parameters from
 * it.
 * 
 * @author xcoulon
 * 
 */
public class JAXRSPathTemplateParser {

	/** Pattern to match expression such as {@code "{name:int}" }.*/
	private static final Pattern PATH_PARAM_TEMPLATE_PATTERN = Pattern.compile("\\{(\\w+):([^:]+)\\}"); //$NON-NLS-1$
	
	/** Pattern to match expression such as {@code "name={int}" }.*/
	private static final Pattern OPTIONAL_PARAM_TEMPLATE_PATTERN = Pattern.compile("(;|\\?|&)(\\w+)=\\{([^:]+)\\}"); //$NON-NLS-1$

	/** Pattern to match expression such as <pre>name={String:"FOO"}</pre> or <pre>name={int:"123.456"}</pre>. */
	private static final Pattern OPTIONAL_PARAM_WITH_DEFAULT_VALUE_TEMPLATE_PATTERN = Pattern.compile("(;|\\?|&)(\\w+)=\\{([^:]+):\"(.+)\"\\}"); //$NON-NLS-1$
	
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
			final int bracketBeginIndex = urlTemplate.indexOf('{', scanIndex);
			final int bracketEndIndex = urlTemplate.indexOf('}', bracketBeginIndex);
			final int commaBeginIndex = urlTemplate.indexOf(';', scanIndex);
			final int questionMarkBeginIndex = urlTemplate.indexOf('?', scanIndex);
			final int ampersandMarkBeginIndex = urlTemplate.indexOf('&', scanIndex);
			if (bracketBeginIndex == -1 || bracketEndIndex == -1) {
				break;
			}
			// now, let see which character comes first: bracket, comma, question mark or ampersand
			int nextCharacterIndex = nextCharacterIndex(bracketBeginIndex, commaBeginIndex, questionMarkBeginIndex, ampersandMarkBeginIndex);
			if(nextCharacterIndex == -1) {
				break;
			}
			final char nextCharacter = urlTemplate.charAt(nextCharacterIndex);
			if(nextCharacter == '{') {
				final String expression = urlTemplate.substring(nextCharacterIndex, bracketEndIndex + 1);
				final Matcher pathParamTemplateMatcher = PATH_PARAM_TEMPLATE_PATTERN.matcher(expression);
				if(pathParamTemplateMatcher.matches()) {
					final URLTemplateParameter templateParameter = URLTemplateParameter.Builder.from(expression)
							.withName(pathParamTemplateMatcher.group(1), true)
							.withDatatype(pathParamTemplateMatcher.group(2)).withParamType(EnumParamType.PATH_PARAM)
							.build();
					templateParameters.add(templateParameter);
				}
				scanIndex = nextCharacterIndex+expression.length();
			} else if(nextCharacter == ';' || nextCharacter == '?' || nextCharacter == '&') {
				final String expression = urlTemplate.substring(nextCharacterIndex, bracketEndIndex + 1);
				final Matcher optionalParamTemplateMatcher = OPTIONAL_PARAM_TEMPLATE_PATTERN.matcher(expression);
				final Matcher optionalParamWithDefaultStringValueTemplateMatcher = OPTIONAL_PARAM_WITH_DEFAULT_VALUE_TEMPLATE_PATTERN.matcher(expression);
				if(optionalParamTemplateMatcher.matches()) {
					final URLTemplateParameter templateParameter = URLTemplateParameter.Builder.from(expression)
							.withName(optionalParamTemplateMatcher.group(2), true)
							.withDatatype(optionalParamTemplateMatcher.group(3))
							.withParamType(getParamType(nextCharacter)).build();
					templateParameters.add(templateParameter);
				} else if(optionalParamWithDefaultStringValueTemplateMatcher.matches()) {
					final URLTemplateParameter templateParameter = URLTemplateParameter.Builder.from(expression)
							.withName(optionalParamWithDefaultStringValueTemplateMatcher.group(2), true)
							.withDatatype(optionalParamWithDefaultStringValueTemplateMatcher.group(3))
							.withDefaultValue(optionalParamWithDefaultStringValueTemplateMatcher.group(4))
							.withParamType(getParamType(nextCharacter)).build();
					templateParameters.add(templateParameter);
				}
				scanIndex = nextCharacterIndex+expression.length();
			}
			
		}

		return templateParameters.toArray(new URLTemplateParameter[templateParameters.size()]);
	}

	/**
	 * @param nextCharacter
	 * @return the {@link EnumParamType} given the character:
	 * <ul>
	 * <li>{@code ;} -> {@link EnumParamType#MATRIX_PARAM}</li>
	 * <li>{@code ?} -> {@link EnumParamType#QUERY_PARAM}</li>
	 * <li>{@code &} -> {@link EnumParamType#QUERY_PARAM}</li>
	 *</ul> 
	 */
	private static EnumParamType getParamType(final char nextCharacter) {
		switch(nextCharacter) {
		case '?':
		case '&':
			return EnumParamType.QUERY_PARAM;
		case ';':
			return EnumParamType.MATRIX_PARAM;
		}
		return null;
	}

	/**
	 * Returns the lowest character index value in the given values, excluding the {@code -1} value.
	 * @param characterIndexes
	 * @return the lowest characterIndex value
	 */
	private static int nextCharacterIndex(final int... characterIndexes) {
		int minValue = Integer.MAX_VALUE;
		for(int characterIndex : characterIndexes) {
			if(characterIndex != -1) {
				minValue = Math.min(minValue, characterIndex);
			}
		}
		if(minValue ==Integer.MAX_VALUE) {
			return -1;
		}
		return minValue;
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
