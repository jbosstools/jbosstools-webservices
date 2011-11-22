/**
 * 
 */
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain;

import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IField;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsParamField;

/** @author xcoulon */
public class JaxrsParamField extends JaxrsElement<IField> implements IJaxrsParamField {

	private final JaxrsResource parentResource;

	public JaxrsParamField(IField javaField, Annotation annotation, JaxrsResource parentResource,
			JaxrsMetamodel metamodel) {
		super(javaField, annotation, metamodel);
		this.parentResource = parentResource;
	}

	public JaxrsParamField(IField javaField, List<Annotation> annotations, JaxrsResource parentResource,
			JaxrsMetamodel metamodel) {
		super(javaField, annotations, metamodel);
		this.parentResource = parentResource;
		this.parentResource.addField(this);
	}

	@Override
	public EnumElementKind getElementKind() {
		return EnumElementKind.RESOURCE_FIELD;
	}

	public Annotation getPathParamAnnotation() {
		return getAnnotation(PathParam.class.getName());
	}

	public Annotation getQueryParamAnnotation() {
		return getAnnotation(QueryParam.class.getName());
	}

	public Annotation getMatrixParamAnnotation() {
		return getAnnotation(MatrixParam.class.getName());
	}

	public Annotation getDefaultValueAnnotation() {
		return getAnnotation(DefaultValue.class.getName());
	}

	@Override
	public void validate(IProgressMonitor progressMonitor) throws CoreException {

	}

	@Override
	public EnumKind getKind() {
		if (getPathParamAnnotation() != null) {
			return EnumKind.PATH_PARAM_FIELD;
		}
		if (getQueryParamAnnotation() != null) {
			return EnumKind.QUERY_PARAM_FIELD;
		}
		if (getMatrixParamAnnotation() != null) {
			return EnumKind.MATRIX_PARAM_FIELD;
		}
		return EnumKind.UNDEFINED;
	}

	public JaxrsResource getParentResource() {
		return parentResource;
	}

}
