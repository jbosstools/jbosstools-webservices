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
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain;

import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.DEFAULT_VALUE;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.MATRIX_PARAM;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PATH_PARAM;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.QUERY_PARAM;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.IField;
import org.eclipse.wst.validation.ValidatorMessage;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResourceField;

/** @author xcoulon */
public class JaxrsResourceField extends JaxrsResourceElement<IField> implements IJaxrsResourceField {

	public JaxrsResourceField(IField javaField, Annotation annotation, JaxrsResource parentResource,
			JaxrsMetamodel metamodel) {
		super(javaField, annotation, parentResource, metamodel);
	}

	public JaxrsResourceField(IField javaField, List<Annotation> annotations, JaxrsResource parentResource,
			JaxrsMetamodel metamodel) {
		super(javaField, annotations, parentResource, metamodel);
	}

	@Override
	public EnumElementCategory getElementCategory() {
		return EnumElementCategory.RESOURCE_FIELD;
	}

	public Annotation getPathParamAnnotation() {
		return getAnnotation(PATH_PARAM.qualifiedName);
	}

	public Annotation getQueryParamAnnotation() {
		return getAnnotation(QUERY_PARAM.qualifiedName);
	}

	public Annotation getMatrixParamAnnotation() {
		return getAnnotation(MATRIX_PARAM.qualifiedName);
	}

	public Annotation getDefaultValueAnnotation() {
		return getAnnotation(DEFAULT_VALUE.qualifiedName);
	}

	@Override
	public List<ValidatorMessage> validate() {
		List<ValidatorMessage> messages = new ArrayList<ValidatorMessage>();
		return messages;
	}

	@Override
	public EnumElementKind getElementKind() {
		if (getPathParamAnnotation() != null) {
			return EnumElementKind.PATH_PARAM_FIELD;
		}
		if (getQueryParamAnnotation() != null) {
			return EnumElementKind.QUERY_PARAM_FIELD;
		}
		if (getMatrixParamAnnotation() != null) {
			return EnumElementKind.MATRIX_PARAM_FIELD;
		}
		return EnumElementKind.UNDEFINED;
	}

	@Override
	public String toString() {
		return "ResourceField '" + getJavaElement().getParent().getElementName() + "." + getJavaElement().getElementName() + "' | annotations=" + getAnnotations();
	}

}
