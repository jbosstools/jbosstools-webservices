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

package org.jboss.tools.ws.ui.test.dialogs;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

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
public class JaxrsURLTemplateParserTestCase {

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
				return templateParam.getName().equals(name) && templateParam.getDatatype().equals(type)
						&& templateParam.getOriginalContent().equals(originalContent)
						&& templateParam.getParameterType() == paramType;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText(name + ", " + type + ", " + originalContent + " ("+ paramType + ")");
			}

		};
	}

	@Test
	public void shouldParseURLTemplateWith3MandatoryParameters() {
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
	public void shouldParseURLTemplateWithACrazyMixOfParameters() {
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
	public void shouldValidateString() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("{id:String}").withDatatype("String")
				.withName("id", true).withParamType(EnumParamType.PATH_PARAM).build();
		// operation
		parameter.setValue("foo!");
		// verification
		assertThat(parameter.validate(), nullValue());
	}
	
	@Test
	public void shouldValidateInteger() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("{id:Integer}").withDatatype("Integer")
				.withName("id", true).withParamType(EnumParamType.PATH_PARAM).build();
		// operation
		parameter.setValue("1");
		// verification
		assertThat(parameter.validate(), nullValue());
	}

	@Test
	public void shouldReplacePathParamWithInteger() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("{id:int}").withDatatype("int")
				.withName("id", true).withParamType(EnumParamType.PATH_PARAM).build();
		// operation
		parameter.setValue("1");
		// verification
		assertThat(parameter.getReplacementContent(), equalTo("1"));
	}

	@Test
	public void shouldReplaceMatrixParamWithInteger() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("id={Integer}").withDatatype("Integer")
				.withName("id", true).withParamType(EnumParamType.MATRIX_PARAM).build();
		// operation
		parameter.setValue("1");
		// verification
		assertThat(parameter.getReplacementContent(), equalTo("id=1"));
	}
	
	@Test
	public void shouldNotValidateInteger() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("id={int}").withDatatype("int")
				.withName("id", true).withParamType(EnumParamType.MATRIX_PARAM).build();
		// operation
		parameter.setValue("a");
		// verification
		assertThat(parameter.validate(), notNullValue());
	}

	@Test
	public void shouldValidateLong() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("{id:Long}").withDatatype("Long")
				.withName("id", true).withParamType(EnumParamType.PATH_PARAM).build();
		// operation
		parameter.setValue("1");
		// verification
		assertThat(parameter.validate(), nullValue());
	}

	@Test
	public void shouldReplacePathParamWithLong() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("{id:Long}").withDatatype("Long")
				.withName("id", true).withParamType(EnumParamType.PATH_PARAM).build();
		// operation
		parameter.setValue("1");
		// verification
		assertThat(parameter.getReplacementContent(), equalTo("1"));
	}
	
	@Test
	public void shouldNotValidateLong() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("{id:long}").withDatatype("long")
				.withName("id", true).withParamType(EnumParamType.PATH_PARAM).build();
		// operation
		parameter.setValue("a");
		// verification
		assertThat(parameter.validate(), notNullValue());
	}

	@Test
	public void shouldValidateDouble() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("{id:Double}").withDatatype("Double")
				.withName("id", true).withParamType(EnumParamType.PATH_PARAM).build();
		// operation
		parameter.setValue("1");
		// verification
		assertThat(parameter.validate(), nullValue());
	}

	@Test
	public void shouldReplacePathParamWithDouble() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("{id:Double}").withDatatype("Double")
				.withName("id", true).withParamType(EnumParamType.PATH_PARAM).build();
		// operation
		parameter.setValue("1.1");
		// verification
		assertThat(parameter.getReplacementContent(), equalTo("1.1"));
	}
	
	@Test
	public void shouldValidateDoubleWithLongValue() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("{id:Double}").withDatatype("Double")
				.withName("id", true).withParamType(EnumParamType.PATH_PARAM).build();
		// operation
		parameter.setValue("1");
		// verification
		assertThat(parameter.validate(), nullValue());
	}
	
	@Test
	public void shouldValidateDoubleWithIntValue() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("{id:Double}").withDatatype("Double")
				.withName("id", true).withParamType(EnumParamType.PATH_PARAM).build();
		// operation
		parameter.setValue("1");
		// verification
		assertThat(parameter.validate(), nullValue());
	}
	
	@Test
	public void shouldNotValidateDouble() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("{id:Double}").withDatatype("Double")
				.withName("id", true).withParamType(EnumParamType.PATH_PARAM).build();
		// operation
		parameter.setValue("a");
		// verification
		assertThat(parameter.validate(), notNullValue());
	}

	@Test
	public void shouldValidateFloat() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("{id:Float}").withDatatype("Float")
				.withName("id", true).withParamType(EnumParamType.PATH_PARAM).build();
		// operation
		parameter.setValue("1.1");
		// verification
		assertThat(parameter.validate(), nullValue());
	}

	@Test
	public void shouldReplacePathParamWithFloat() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("{id:Float}").withDatatype("Float")
				.withName("id", true).withParamType(EnumParamType.PATH_PARAM).build();
		// operation
		parameter.setValue("1.1");
		// verification
		assertThat(parameter.getReplacementContent(), equalTo("1.1"));
	}
	
	@Test
	public void shouldValidateFloatWithInteger() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("{id:Float}").withDatatype("Float")
				.withName("id", true).withParamType(EnumParamType.PATH_PARAM).build();
		// operation
		parameter.setValue("1");
		// verification
		assertThat(parameter.validate(), nullValue());
	}
	
	@Test
	public void shouldNotValidateFloat() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("{id:Float}").withDatatype("Float")
				.withName("id", true).withParamType(EnumParamType.PATH_PARAM).build();
		// operation
		parameter.setValue("a");
		// verification
		assertThat(parameter.validate(), notNullValue());
	}
	
	@Test
	public void shouldValidateListOfIntegersWithSingleValue() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("matrix={List<Integer>}").withDatatype("Integer")
				.withName("id", true).withParamType(EnumParamType.MATRIX_PARAM).build();
		// operation
		parameter.setValue("1");
		// verification
		assertThat(parameter.validate(), nullValue());
	}

	@Test
	public void shouldValidateListOfIntegersWithMultipleValues() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("matrix={List<Integer>}").withDatatype("List<Integer>")
				.withName("id", true).withParamType(EnumParamType.MATRIX_PARAM).build();
		// operation
		parameter.setValue("1,2,3");
		// verification
		assertThat(parameter.validate(), nullValue());
	}
	
	@Test
	public void shouldReplaceListOfIntegersWithSingleValueInMatrixParams() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("matrix={List<Integer>}").withDatatype("List<Integer>")
				.withName("matrix", true).withParamType(EnumParamType.MATRIX_PARAM).build();
		// operation
		parameter.setValue("1");
		// verification
		assertThat(parameter.getReplacementContent(), equalTo(";matrix=1"));
	}
	
	@Test
	public void shouldReplaceListOfIntegersWithMultipleValuesInMatrixParams() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("matrix={List<Integer>}").withDatatype("List<Integer>")
				.withName("matrix", true).withParamType(EnumParamType.MATRIX_PARAM).build();
		// operation
		parameter.setValue("1,2,3");
		// verification
		assertThat(parameter.getReplacementContent(), equalTo(";matrix=1;matrix=2;matrix=3"));
	}
	
	@Test
	public void shouldReplaceListOfIntegersWithSingleValueInQueryParams() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("?query={List<Integer>}").withDatatype("List<Integer>")
				.withName("query", true).withParamType(EnumParamType.QUERY_PARAM).build();
		// operation
		parameter.setValue("1");
		// verification
		assertThat(parameter.getReplacementContent(), equalTo("?query=1"));
	}
	
	@Test
	public void shouldReplaceListOfIntegersWithMultipleValuesInQueryParams() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("?query={List<Integer>}").withDatatype("List<Integer>")
				.withName("query", true).withParamType(EnumParamType.QUERY_PARAM).build();
		// operation
		parameter.setValue("1, 2");
		// verification
		assertThat(parameter.getReplacementContent(), equalTo("?query=1&query=2"));
	}
	
	@Test
	public void shouldReplaceListOfIntegersWithSingleValueInSecondQueryParams() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("&query={List<Integer>}").withDatatype("List<Integer>")
				.withName("query", true).withParamType(EnumParamType.QUERY_PARAM).build();
		// operation
		parameter.setValue("1");
		// verification
		assertThat(parameter.getReplacementContent(), equalTo("&query=1"));
	}

	@Test
	public void shouldReplaceListOfIntegersWithMultipleValuesInSecondQueryParams() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("?query={List<Integer>}").withDatatype("List<Integer>")
				.withName("query", true).withParamType(EnumParamType.QUERY_PARAM).build();
		// operation
		parameter.setValue("1, 2");
		// verification
		assertThat(parameter.getReplacementContent(), equalTo("?query=1&query=2"));
	}
	
	@Test
	public void shouldNotValidateListOfInteger() {
		// pre-condition
		URLTemplateParameter parameter = URLTemplateParameter.Builder.from("{id:int}").withDatatype("Integer")
				.withName("id", true).withParamType(EnumParamType.PATH_PARAM).build();
		// operation
		parameter.setValue("1,2");
		// verification
		assertThat(parameter.validate(), notNullValue());
	}
}
