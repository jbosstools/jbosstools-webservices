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

package org.jboss.tools.ws.ui.test.dialogs;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import junit.framework.TestCase;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jboss.tools.ws.ui.dialogs.EnumParamType;
import org.jboss.tools.ws.ui.dialogs.URLTemplateParameter;
import org.jboss.tools.ws.ui.utils.JAXRSPathTemplateParser;
import org.junit.Test;

/**
 * @author xcoulon
 * 
 */
public class JaxrsURLTemplateParserTestCase extends TestCase {

	/**
	 * Generates a custom matcher for the {@link URLTemplateParameter} based on
	 * the given parameters
	 * 
	 * @param name
	 * @param type
	 * @param originalContent
	 * @param mandatory
	 * @return
	 */
	private Matcher<URLTemplateParameter> matches(final String name, final String type, final String originalContent,
			final EnumParamType paramType) {
		return new BaseMatcher<URLTemplateParameter>() {

			@Override
			public boolean matches(Object item) {
				final URLTemplateParameter templateParam = (URLTemplateParameter) item;
				return templateParam.getName().equals(name) 
						&& templateParam.getDatatype().equals(type)
						&& templateParam.getDefaultValue().isEmpty()
						&& templateParam.getOriginalContent().equals(originalContent)
						&& templateParam.getParameterType() == paramType;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText(name + ", " + type + ", " + originalContent + " ("+ paramType + ")");
			}

		};
	}

	/**
	 * Generates a custom matcher for the {@link URLTemplateParameter} based on
	 * the given parameters
	 * 
	 * @param name
	 * @param type
	 * @param originalContent
	 * @param mandatory
	 * @return
	 */
	private Matcher<URLTemplateParameter> matches(final String name, final String type, final String defaultValue, final String originalContent,
			final EnumParamType paramType) {
		return new BaseMatcher<URLTemplateParameter>() {
			
			@Override
			public boolean matches(Object item) {
				final URLTemplateParameter templateParam = (URLTemplateParameter) item;
				return templateParam.getName().equals(name) 
						&& templateParam.getDatatype().equals(type)
						&& templateParam.getDefaultValue().equals(defaultValue)
						&& templateParam.getOriginalContent().equals(originalContent)
						&& templateParam.getParameterType() == paramType;
			}
			
			@Override
			public void describeTo(Description description) {
				description.appendText(name + ", " + type + "=" + defaultValue + ", " + originalContent + " ("+ paramType + ")");
			}
			
		};
	}

	@Test
	public void testParseURLTemplateWith3MandatoryParameters() {
		// given
		final String template = "http://localhost/application/rest/{path:String}/{to:String}/{id:Integer}";
		// when
		final URLTemplateParameter[] templateParameters = JAXRSPathTemplateParser.parse(template);
		// then
		assertThat(templateParameters.length, equalTo(3));
		// param #1
		assertThat(templateParameters[0], matches("path", "String", "{path:String}", EnumParamType.PATH_PARAM));
		// param #2
		assertThat(templateParameters[1], matches("to", "String", "{to:String}", EnumParamType.PATH_PARAM));
		// param #3
		assertThat(templateParameters[2], matches("id", "Integer", "{id:Integer}", EnumParamType.PATH_PARAM));
	}

	@Test
	public void testParseURLTemplateWithMatrixParamWithoutDefaultValue() {
		// given
		final String template = "http://localhost:8080/org.jboss.tools.ws.jaxrs.tests.sampleproject2/app/customers/{id:Integer};country={String}";
		// when
		final URLTemplateParameter[] templateParameters = JAXRSPathTemplateParser.parse(template);
		// then
		assertThat(templateParameters.length, equalTo(2));
		// param #1
		assertThat(templateParameters[0], matches("id", "Integer", "{id:Integer}", EnumParamType.PATH_PARAM));
		// param #2
		assertThat(templateParameters[1], matches("country", "String", ";country={String}", EnumParamType.MATRIX_PARAM));
	}

	@Test
	public void testParseURLTemplateWithTwoMatrixParamsWithDefaultValue() {
		// given
		final String template = "http://localhost:8080/org.jboss.tools.ws.jaxrs.tests.sampleproject2/app/customers/{id:Integer};country={String:\"EN\"};long={long:\"123456\"}";
		// when
		final URLTemplateParameter[] templateParameters = JAXRSPathTemplateParser.parse(template);
		// then
		assertThat(templateParameters.length, equalTo(3));
		// param #1
		assertThat(templateParameters[0], matches("id", "Integer", "{id:Integer}", EnumParamType.PATH_PARAM));
		// param #2
		assertThat(templateParameters[1], matches("country", "String", "EN", ";country={String:\"EN\"}", EnumParamType.MATRIX_PARAM));
		// param #3
		assertThat(templateParameters[2], matches("long", "long", "123456", ";long={long:\"123456\"}", EnumParamType.MATRIX_PARAM));
	}

	@Test
	public void testParseURLTemplateWithQueryParamWithoutDefaultValue() {
		// given
		final String template = "http://localhost:8080/org.jboss.tools.ws.jaxrs.tests.sampleproject2/app/customers/{id:Integer}?country={String}";
		// when
		final URLTemplateParameter[] templateParameters = JAXRSPathTemplateParser.parse(template);
		// then
		assertThat(templateParameters.length, equalTo(2));
		// param #1
		assertThat(templateParameters[0], matches("id", "Integer", "{id:Integer}", EnumParamType.PATH_PARAM));
		// param #2
		assertThat(templateParameters[1], matches("country", "String", "?country={String}", EnumParamType.QUERY_PARAM));
	}

	@Test
	public void testParseURLTemplateWithTwoQueryParamWithoutDefaultValue() {
		// given
		final String template = "http://localhost:8080/org.jboss.tools.ws.jaxrs.tests.sampleproject2/app/customers/{id:Integer}?country={String}&lang={String}";
		// when
		final URLTemplateParameter[] templateParameters = JAXRSPathTemplateParser.parse(template);
		// then
		assertThat(templateParameters.length, equalTo(3));
		// param #1
		assertThat(templateParameters[0], matches("id", "Integer", "{id:Integer}", EnumParamType.PATH_PARAM));
		// param #2
		assertThat(templateParameters[1], matches("country", "String", "?country={String}", EnumParamType.QUERY_PARAM));
		// param #3
		assertThat(templateParameters[2], matches("lang", "String", "&lang={String}", EnumParamType.QUERY_PARAM));
	}
	
	@Test
	public void testParseURLTemplateWithQueryParamWithDefaultValue() {
		// given
		final String template = "http://localhost:8080/org.jboss.tools.ws.jaxrs.tests.sampleproject2/app/customers/{id:Integer}?country={String:\"EN\"}";
		// when
		final URLTemplateParameter[] templateParameters = JAXRSPathTemplateParser.parse(template);
		// then
		assertThat(templateParameters.length, equalTo(2));
		// param #1
		assertThat(templateParameters[0], matches("id", "Integer", "{id:Integer}", EnumParamType.PATH_PARAM));
		// param #2
		assertThat(templateParameters[1], matches("country", "String", "EN", "?country={String:\"EN\"}", EnumParamType.QUERY_PARAM));
	}
	
	@Test
	public void testParseURLTemplateWithTwoQueryParamsWithDefaultValue() {
		// given
		final String template = "http://localhost:8080/org.jboss.tools.ws.jaxrs.tests.sampleproject2/app/customers/{id:Integer}?country={String:\"EN\"}&long={float:\"1.23\"}";
		// when
		final URLTemplateParameter[] templateParameters = JAXRSPathTemplateParser.parse(template);
		// then
		assertThat(templateParameters.length, equalTo(3));
		// param #1
		assertThat(templateParameters[0], matches("id", "Integer", "{id:Integer}", EnumParamType.PATH_PARAM));
		// param #2
		assertThat(templateParameters[1], matches("country", "String", "EN", "?country={String:\"EN\"}", EnumParamType.QUERY_PARAM));
		// param #3
		assertThat(templateParameters[2], matches("long", "float", "1.23", "&long={float:\"1.23\"}", EnumParamType.QUERY_PARAM));
	}
	
	@Test
	public void testParseURLTemplateWithMatrixAndQueryParamsWithDefaultValue() {
		// given
		final String template = "http://localhost:8080/org.jboss.tools.ws.jaxrs.tests.sampleproject2/app/customers/{id:Integer};country={String:\"EN\"}?shape={String:\"shape!\"}";
		// when
		final URLTemplateParameter[] templateParameters = JAXRSPathTemplateParser.parse(template);
		// then
		assertThat(templateParameters.length, equalTo(3));
		// param #1
		assertThat(templateParameters[0], matches("id", "Integer", "{id:Integer}", EnumParamType.PATH_PARAM));
		// param #2
		assertThat(templateParameters[1], matches("country", "String", "EN", ";country={String:\"EN\"}", EnumParamType.MATRIX_PARAM));
		// param #3
		assertThat(templateParameters[2], matches("shape", "String", "shape!", "?shape={String:\"shape!\"}", EnumParamType.QUERY_PARAM));
	}

	@Test
	public void testParseURLTemplateWithACrazyMixOfParameters() {
		// given
		final String template = "http://localhost/application/rest/{path:.*};matrix={List<Integer>}/{format:(/format/[^/]+?)?}/{encoding:(/encoding/[^/]+?)?}/{to:\\d+}/{id:Integer}?start={Integer}&size={Integer}&foo={List<Integer>}";
		// when
		final URLTemplateParameter[] templateParameters = JAXRSPathTemplateParser.parse(template);
		// then
		assertThat(templateParameters.length, equalTo(9));
		// param #1
		assertThat(templateParameters[0], matches("path", ".*", "{path:.*}", EnumParamType.PATH_PARAM));
		// param #2
		assertThat(templateParameters[1], matches("matrix", "List<Integer>", ";matrix={List<Integer>}", EnumParamType.MATRIX_PARAM));
		// param #3
		assertThat(templateParameters[2], matches("format", "(/format/[^/]+?)?", "{format:(/format/[^/]+?)?}", EnumParamType.PATH_PARAM));
		// param #4
		assertThat(templateParameters[3], matches("encoding", "(/encoding/[^/]+?)?", "{encoding:(/encoding/[^/]+?)?}", EnumParamType.PATH_PARAM));
		// param #5
		assertThat(templateParameters[4], matches("to", "\\d+", "{to:\\d+}", EnumParamType.PATH_PARAM));
		// param #6
		assertThat(templateParameters[5], matches("id", "Integer", "{id:Integer}", EnumParamType.PATH_PARAM));
		// param #7
		assertThat(templateParameters[6], matches("start", "Integer", "?start={Integer}", EnumParamType.QUERY_PARAM));
		// param #8
		assertThat(templateParameters[7], matches("size", "Integer", "&size={Integer}", EnumParamType.QUERY_PARAM));
		// param #9
		assertThat(templateParameters[8], matches("foo", "List<Integer>", "&foo={List<Integer>}", EnumParamType.QUERY_PARAM));
	}
	
	@Test
	public void testValidateString() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("{id:String}").withDatatype("String")
				.withName("id", true).withParamType(EnumParamType.PATH_PARAM).build();
		// operation
		parameter.setValue("foo!");
		// verification
		assertThat(parameter.validate(), nullValue());
	}
	
	@Test
	public void testValidateInteger() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("{id:Integer}").withDatatype("Integer")
				.withName("id", true).withParamType(EnumParamType.PATH_PARAM).build();
		// operation
		parameter.setValue("1");
		// verification
		assertThat(parameter.validate(), nullValue());
	}

	@Test
	public void testReplacePathParamWithInteger() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("{id:int}").withDatatype("int")
				.withName("id", true).withParamType(EnumParamType.PATH_PARAM).build();
		// operation
		parameter.setValue("1");
		// verification
		assertThat(parameter.getReplacementContent(), equalTo("1"));
	}

	@Test
	public void testReplaceMatrixParamWithInteger() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("id={Integer}").withDatatype("Integer")
				.withName("id", true).withParamType(EnumParamType.MATRIX_PARAM).build();
		// operation
		parameter.setValue("1");
		// verification
		assertThat(parameter.getReplacementContent(), equalTo(";id=1"));
	}
	
	@Test
	public void testNotValidateInteger() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("id={int}").withDatatype("int")
				.withName("id", true).withParamType(EnumParamType.MATRIX_PARAM).build();
		// operation
		parameter.setValue("a");
		// verification
		assertThat(parameter.validate(), notNullValue());
	}

	@Test
	public void testValidateLong() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("{id:Long}").withDatatype("Long")
				.withName("id", true).withParamType(EnumParamType.PATH_PARAM).build();
		// operation
		parameter.setValue("1");
		// verification
		assertThat(parameter.validate(), nullValue());
	}

	@Test
	public void testReplacePathParamWithLong() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("{id:Long}").withDatatype("Long")
				.withName("id", true).withParamType(EnumParamType.PATH_PARAM).build();
		// operation
		parameter.setValue("1");
		// verification
		assertThat(parameter.getReplacementContent(), equalTo("1"));
	}
	
	@Test
	public void testNotValidateLong() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("{id:long}").withDatatype("long")
				.withName("id", true).withParamType(EnumParamType.PATH_PARAM).build();
		// operation
		parameter.setValue("a");
		// verification
		assertThat(parameter.validate(), notNullValue());
	}

	@Test
	public void testValidateDouble() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("{id:Double}").withDatatype("Double")
				.withName("id", true).withParamType(EnumParamType.PATH_PARAM).build();
		// operation
		parameter.setValue("1");
		// verification
		assertThat(parameter.validate(), nullValue());
	}

	@Test
	public void testReplacePathParamWithDouble() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("{id:Double}").withDatatype("Double")
				.withName("id", true).withParamType(EnumParamType.PATH_PARAM).build();
		// operation
		parameter.setValue("1.1");
		// verification
		assertThat(parameter.getReplacementContent(), equalTo("1.1"));
	}
	
	@Test
	public void testValidateDoubleWithLongValue() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("{id:Double}").withDatatype("Double")
				.withName("id", true).withParamType(EnumParamType.PATH_PARAM).build();
		// operation
		parameter.setValue("1");
		// verification
		assertThat(parameter.validate(), nullValue());
	}
	
	@Test
	public void testValidateDoubleWithIntValue() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("{id:Double}").withDatatype("Double")
				.withName("id", true).withParamType(EnumParamType.PATH_PARAM).build();
		// operation
		parameter.setValue("1");
		// verification
		assertThat(parameter.validate(), nullValue());
	}
	
	@Test
	public void testNotValidateDouble() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("{id:Double}").withDatatype("Double")
				.withName("id", true).withParamType(EnumParamType.PATH_PARAM).build();
		// operation
		parameter.setValue("a");
		// verification
		assertThat(parameter.validate(), notNullValue());
	}

	@Test
	public void testValidateFloat() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("{id:Float}").withDatatype("Float")
				.withName("id", true).withParamType(EnumParamType.PATH_PARAM).build();
		// operation
		parameter.setValue("1.1");
		// verification
		assertThat(parameter.validate(), nullValue());
	}

	@Test
	public void testReplacePathParamWithFloat() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("{id:Float}").withDatatype("Float")
				.withName("id", true).withParamType(EnumParamType.PATH_PARAM).build();
		// operation
		parameter.setValue("1.1");
		// verification
		assertThat(parameter.getReplacementContent(), equalTo("1.1"));
	}
	
	@Test
	public void testValidateFloatWithInteger() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("{id:Float}").withDatatype("Float")
				.withName("id", true).withParamType(EnumParamType.PATH_PARAM).build();
		// operation
		parameter.setValue("1");
		// verification
		assertThat(parameter.validate(), nullValue());
	}
	
	@Test
	public void testNotValidateFloat() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("{id:Float}").withDatatype("Float")
				.withName("id", true).withParamType(EnumParamType.PATH_PARAM).build();
		// operation
		parameter.setValue("a");
		// verification
		assertThat(parameter.validate(), notNullValue());
	}
	
	@Test
	public void testValidateListOfIntegersWithSingleValue() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("matrix={List<Integer>}").withDatatype("Integer")
				.withName("id", true).withParamType(EnumParamType.MATRIX_PARAM).build();
		// operation
		parameter.setValue("1");
		// verification
		assertThat(parameter.validate(), nullValue());
	}

	@Test
	public void testValidateListOfIntegersWithMultipleValues() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("matrix={List<Integer>}").withDatatype("List<Integer>")
				.withName("id", true).withParamType(EnumParamType.MATRIX_PARAM).build();
		// operation
		parameter.setValue("1,2,3");
		// verification
		assertThat(parameter.validate(), nullValue());
	}
	
	@Test
	public void testReplaceListOfIntegersWithSingleValueInMatrixParams() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("matrix={List<Integer>}").withDatatype("List<Integer>")
				.withName("matrix", true).withParamType(EnumParamType.MATRIX_PARAM).build();
		// operation
		parameter.setValue("1");
		// verification
		assertThat(parameter.getReplacementContent(), equalTo(";matrix=1"));
	}
	
	@Test
	public void testReplaceListOfIntegersWithMultipleValuesInMatrixParams() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("matrix={List<Integer>}").withDatatype("List<Integer>")
				.withName("matrix", true).withParamType(EnumParamType.MATRIX_PARAM).build();
		// operation
		parameter.setValue("1,2,3");
		// verification
		assertThat(parameter.getReplacementContent(), equalTo(";matrix=1;matrix=2;matrix=3"));
	}
	
	@Test
	public void testReplaceListOfIntegersWithSingleValueInQueryParams() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("?query={List<Integer>}").withDatatype("List<Integer>")
				.withName("query", true).withParamType(EnumParamType.QUERY_PARAM).build();
		// operation
		parameter.setValue("1");
		// verification
		assertThat(parameter.getReplacementContent(), equalTo("?query=1"));
	}
	
	@Test
	public void testReplaceListOfIntegersWithMultipleValuesInQueryParams() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("?query={List<Integer>}").withDatatype("List<Integer>")
				.withName("query", true).withParamType(EnumParamType.QUERY_PARAM).build();
		// operation
		parameter.setValue("1, 2");
		// verification
		assertThat(parameter.getReplacementContent(), equalTo("?query=1&query=2"));
	}
	
	@Test
	public void testReplaceListOfIntegersWithSingleValueInSecondQueryParams() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("&query={List<Integer>}").withDatatype("List<Integer>")
				.withName("query", true).withParamType(EnumParamType.QUERY_PARAM).build();
		// operation
		parameter.setValue("1");
		// verification
		assertThat(parameter.getReplacementContent(), equalTo("&query=1"));
	}

	@Test
	public void testReplaceListOfIntegersWithMultipleValuesInSecondQueryParams() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("?query={List<Integer>}").withDatatype("List<Integer>")
				.withName("query", true).withParamType(EnumParamType.QUERY_PARAM).build();
		// operation
		parameter.setValue("1, 2");
		// verification
		assertThat(parameter.getReplacementContent(), equalTo("?query=1&query=2"));
	}
	
	@Test
	public void testNotValidateListOfInteger() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("{id:int}").withDatatype("Integer")
				.withName("id", true).withParamType(EnumParamType.PATH_PARAM).build();
		// operation
		parameter.setValue("1,2");
		// verification
		assertThat(parameter.validate(), notNullValue());
	}
}
