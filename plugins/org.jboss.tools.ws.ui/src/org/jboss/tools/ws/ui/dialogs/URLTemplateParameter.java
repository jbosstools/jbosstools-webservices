/******************************************************************************* 
 * Copyright (c) 01 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.ui.dialogs;

import org.eclipse.osgi.util.NLS;
import org.jboss.tools.ws.ui.messages.JBossWSUIMessages;

/**
 * Class that holds information on a parameter in an URL Template
 * 
 * @author xcoulon
 * 
 */
public class URLTemplateParameter {

	public static final String BYTE = "byte"; //$NON-NLS-1$
	public static final String SHORT = "short"; //$NON-NLS-1$
	public static final String INT = "int"; //$NON-NLS-1$
	public static final String LONG = "Long"; //$NON-NLS-1$
	public static final String FLOAT = "float"; //$NON-NLS-1$
	public static final String DOUBLE = "double"; //$NON-NLS-1$
	public static final String CHAR = "char"; //$NON-NLS-1$
	public static final String BOOLEAN = "boolean"; //$NON-NLS-1$

	public static class Builder {

		private String name = null;
		private String datatype = null;
		private final String originalContent;
		private EnumParamType paramType;

		/**
		 * Private constructor, users should start from the
		 * {@link Builder#from()} method
		 * 
		 * @param originalContent
		 *            the original content in the URL Template
		 */
		private Builder(final String originalContent) {
			this.originalContent = originalContent;
		}

		public static Builder from(final String originalContent) {
			return new Builder(originalContent);
		}

		public Builder withName(final String name, final boolean mandatory) {
			this.name = name;
			return this;
		}

		public Builder withDatatype(final String datatype) {
			this.datatype = datatype;
			return this;
		}

		public Builder withParamType(final EnumParamType paramType) {
			this.paramType = paramType;
			return this;
		}

		public URLTemplateParameter build() {
			return new URLTemplateParameter(originalContent, name, datatype, paramType);
		}

	}

	/** The original content in the URL Template. */
	private final String originalContent;

	/** The parameter name. */
	private final String name;
	
	/** The datatype associated with this param. */
	private final String datatype;
	
	/** The type of parameter. */
	private final EnumParamType paramType;

	/** The regex associated with the datatype. */
	private final String regex;

	/** The value given by the user. */
	private String value = ""; //$NON-NLS-1$
	
	/** Marker to know if the given parameter can have multiple values.*/
	private boolean isMultiple = false;

	/**
	 * Constructor
	 * 
	 * @param originalContent
	 * @param name
	 * @param mandatory
	 * @param datatype
	 */
	private URLTemplateParameter(final String originalContent, final String name, final String datatype,
			final EnumParamType paramType) {
		this.originalContent = originalContent;
		this.name = name;
		this.datatype = datatype;
		this.regex = getRegex(datatype);
		this.paramType = paramType;

	}

	/**
	 * @param datatype
	 */
	public String getRegex(final String datatype) {
		if(datatype.startsWith("List<") || datatype.startsWith("Set<") || datatype.startsWith("SortedSet<")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			this.isMultiple = true;
			final String internalPattern = getRegex(datatype.substring(datatype.indexOf("<") + 1, datatype.length() - 1)); //$NON-NLS-1$
			return internalPattern + "(," + internalPattern + ")*"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		else if (datatype.equals(BOOLEAN) || datatype.equals(Boolean.class.getSimpleName())) {
			return "(\"true\"|\"false\"|\"yes\"|\"no\"|\"y\"|\"n\"|\"0\"|\"1\")"; //$NON-NLS-1$
		} else if (datatype.equals(SHORT) || datatype.equals(Short.class.getSimpleName())) {
			return "(\\d)+"; //$NON-NLS-1$
		} else if (datatype.equals(INT) || datatype.equals(Integer.class.getSimpleName())) {
			return "(\\d)+"; //$NON-NLS-1$
		} else if (datatype.equals(LONG) || datatype.equals(Long.class.getSimpleName())) {
			return "-?\\d{1,19}"; //$NON-NLS-1$
		} else if (datatype.equals(FLOAT) || datatype.equals(Float.class.getSimpleName())) {
			return "[-+]?[0-9]*\\.?[0-9]+"; //$NON-NLS-1$
		} else if (datatype.equals(DOUBLE) || datatype.equals(Double.class.getSimpleName())) {
			return "[-+]?[0-9]*\\.?[0-9]+"; //$NON-NLS-1$
		} else if (datatype.equals(CHAR) || datatype.equals(Character.class.getSimpleName())) {
			return "."; //$NON-NLS-1$
		} else if (datatype.equals(String.class.getSimpleName())) {
			return ".+"; //$NON-NLS-1$
		} else {
			return datatype;
		}
	}

	/**
	 * Validates the value of this {@link URLTemplateParameter} against its
	 * type.
	 * 
	 * @return an error message or {@code null} if no error was found.
	 */
	public String validate() {
		// fail fast
		if (this.regex == null) {
			return null;
		} else if(value.isEmpty() && paramType != EnumParamType.PATH_PARAM) {
			return null;
		}
		// mandatory non-regex, but missing
		else if (this.paramType == EnumParamType.PATH_PARAM && value.isEmpty()) {
			final String errorMessage = JBossWSUIMessages.WSTesterURLInputsDialog_Validation_Error_Missing_Value;
			return NLS.bind(errorMessage, new String[] { this.name });
		} else if (!this.value.matches(regex)) {
			final String errorMessage = JBossWSUIMessages.WSTesterURLInputsDialog_Validation_Error_Invalid;
			return NLS.bind(errorMessage, new String[] { this.name, this.datatype, this.value });
		}

		//
		/*
		 * if (this.mandatory && this.regex != null) { boolean valid =
		 * this.value.matches(this.regex); if (!valid) { final String
		 * errorMessage =
		 * JBossWSUIMessages.WSTesterURLInputsDialog_Validation_Error_String;
		 * return NLS.bind(errorMessage, new String[] { this.name, this.value,
		 * this.regex }); } } else if (this.mandatory && this.datatype != null)
		 * { if (this.datatype.equals("int")) { try {
		 * Integer.parseInt(this.value); } catch (NumberFormatException nfe) {
		 * final String errorMessage =
		 * JBossWSUIMessages.WSTesterURLInputsDialog_Int_Validation_Error_String
		 * ; return NLS.bind(errorMessage, new String[] { this.name }); } } }
		 */
		return null;
	}

	public static boolean isInt(final String value) {
		try {
			Integer.parseInt(value);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	public static boolean isLong(final String value) {
		try {
			Long.parseLong(value);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	public static boolean isDouble(final String value) {
		try {
			Double.parseDouble(value);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	public static boolean isFloat(final String value) {
		try {
			Float.parseFloat(value);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/**
	 * @return the replacement content that should be substituted from the
	 *         original content to convert the URL Template into a real URL.
	 */
	@SuppressWarnings("incomplete-switch")
	public String getReplacementContent() {
		if (value == null || value.isEmpty()) {
			return ""; //$NON-NLS-1$
		} 
		// user values are separated with commas but the param name should be repeated
		// in the generated URL and each name/value should be separated with a comma (';') 
		// or a question mark ('?'), depending on the type of parameter
		else if(isMultiple) {
			final StringBuilder replacementContentBuilder = new StringBuilder();
			boolean first=true;
			for (String v : this.value.split(",")) { //$NON-NLS-1$
				switch (paramType) {
				case MATRIX_PARAM:
					replacementContentBuilder.append(';');
					break;
				case QUERY_PARAM:
					if(first) {
						replacementContentBuilder.append(originalContent.charAt(0));
					} else {
						replacementContentBuilder.append('&');
					}
					break;
				}
				first=false;
				replacementContentBuilder.append(this.name).append('=').append(v.trim());
			}
			return replacementContentBuilder.toString();
		} else {
			switch(paramType) {
			case MATRIX_PARAM:
			case QUERY_PARAM:
				return name + '=' + value;
			default:
				return value.trim();
			}
		}
	}

	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @param value
	 *            the value to set
	 */
	public void setValue(final String value) {
		this.value = value;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the datatype
	 */
	public String getDatatype() {
		return datatype;
	}
	
	public EnumParamType getParameterType() {
		return paramType;
	}

	
	public boolean isMandatory() {
		return this.paramType == EnumParamType.PATH_PARAM;
	}

	/**
	 * @return the originalContent
	 */
	public String getOriginalContent() {
		return originalContent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return (name + ", " + datatype + ", " + originalContent + " (" + paramType + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
	}


}