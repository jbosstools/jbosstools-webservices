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

import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsElements.CONSUMES;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsElements.PATH;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsElements.PRODUCES;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.wst.validation.ValidatorMessage;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResource;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResourceField;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResourceMethod;

/**
 * From the spec : A resource class is a Java class that uses JAX-RS annotations
 * to implement a corresponding Web resource. Resource classes are POJOs that
 * have at least one method annotated with @Path or a request method designator.
 * 
 * @author xcoulon
 */
public class JaxrsResource extends JaxrsJavaElement<IType> implements IJaxrsResource {

	private final Map<String, JaxrsResourceField> resourceFields = new HashMap<String, JaxrsResourceField>();

	private final Map<String, JaxrsParamBeanProperty> paramBeanProperties = new HashMap<String, JaxrsParamBeanProperty>();

	private final Map<String, JaxrsResourceMethod> resourceMethods = new HashMap<String, JaxrsResourceMethod>();

	public static class Builder {
		final IType javaType;
		final JaxrsMetamodel metamodel;
		private Annotation consumesAnnotation;
		private Annotation producesAnnotation;
		private Annotation pathAnnotation;

		public Builder(final IType javaType, final JaxrsMetamodel metamodel) {
			this.javaType = javaType;
			this.metamodel = metamodel;
		}

		public Builder pathTemplate(final Annotation pathAnnotation) {
			this.pathAnnotation = pathAnnotation;
			return this;
		}

		public Builder consumes(final Annotation consumesAnnotation) {
			this.consumesAnnotation = consumesAnnotation;
			return this;
		}

		public Builder produces(final Annotation producesAnnotation) {
			this.producesAnnotation = producesAnnotation;
			return this;
		}

		public JaxrsResource build() {
			List<Annotation> annotations = new ArrayList<Annotation>();
			if (pathAnnotation != null) {
				annotations.add(pathAnnotation);
			}
			if (consumesAnnotation != null) {
				annotations.add(consumesAnnotation);
			}
			if (producesAnnotation != null) {
				annotations.add(producesAnnotation);
			}
			JaxrsResource resource = new JaxrsResource(javaType, annotations, metamodel);
			return resource;
		}
	}

	private JaxrsResource(final IType javaType, final List<Annotation> annotations, final JaxrsMetamodel metamodel) {
		super(javaType, annotations, metamodel);
	}

	@Override
	public EnumElementKind getElementKind() {
		return EnumElementKind.RESOURCE;
	}

	public final boolean isRootResource() {
		return getKind() == EnumKind.ROOT_RESOURCE;
	}

	public boolean isSubresource() {
		return getKind() == EnumKind.SUBRESOURCE;
	}

	@Override
	public final EnumKind getKind() {
		final Annotation pathAnnotation = getAnnotation(PATH.qualifiedName);
		if (pathAnnotation != null) {
			return EnumKind.ROOT_RESOURCE;
		} else if (resourceMethods.size() > 0 || resourceFields.size() > 0 || paramBeanProperties.size() > 0) {
			return EnumKind.SUBRESOURCE;
		}
		return EnumKind.UNDEFINED;
	}

	public final String getName() {
		return getJavaElement().getElementName();
	}

	@Override
	public String getPathTemplate() {
		final Annotation pathAnnotation = getAnnotation(PATH.qualifiedName);
		if (pathAnnotation == null) {
			return null;
		}
		return pathAnnotation.getValue("value");
	}
	
	@Override
	public boolean hasPathTemplate() {
		final Annotation pathAnnotation = getPathAnnotation();
		return pathAnnotation != null && pathAnnotation.getValue("value") != null;
	}

	public Annotation getPathAnnotation() {
		final Annotation pathAnnotation = getAnnotation(PATH.qualifiedName);
		return pathAnnotation;
	}

	@Override
	public List<String> getConsumedMediaTypes() {
		final Annotation consumesAnnotation = getAnnotation(CONSUMES.qualifiedName);
		if (consumesAnnotation != null) {
			return consumesAnnotation.getValues("value");
		}
		return null;
	}

	@Override
	public List<String> getProducedMediaTypes() {
		final Annotation producesAnnotation = getAnnotation(PRODUCES.qualifiedName);
		if (producesAnnotation != null) {
			return producesAnnotation.getValues("value");
		}
		return null;
	}

	public Annotation getProducesAnnotation() {
		final Annotation producesAnnotation = getAnnotation(PRODUCES.qualifiedName);
		return producesAnnotation;
	}

	@Override
	public final List<IJaxrsResourceMethod> getAllMethods() {
		return Collections.unmodifiableList(new ArrayList<IJaxrsResourceMethod>(resourceMethods.values()));
	}

	public final List<IJaxrsResourceField> getAllFields() {
		return Collections.unmodifiableList(new ArrayList<IJaxrsResourceField>(resourceFields.values()));
	}

	@Override
	public String toString() {
		return new StringBuffer().append("Resource '").append(getName()).append("' (root=").append(isRootResource()).append(") ").toString();
	}

	public void addElement(JaxrsResourceElement<?> element) {
		switch (element.getElementKind()) {
		case RESOURCE_FIELD:
			this.resourceFields.put(element.getJavaElement().getHandleIdentifier(), (JaxrsResourceField) element);
			break;
		case RESOURCE_METHOD:
			this.resourceMethods.put(element.getJavaElement().getHandleIdentifier(), (JaxrsResourceMethod) element);
			break;
		default:
			break;
		}
	}

	public boolean removeField(IJaxrsResourceField resourceField) {
		return (this.resourceFields.remove(resourceField.getJavaElement().getHandleIdentifier()) != null);
	}

	public void addMethod(JaxrsResourceMethod method) {
		this.resourceMethods.put(method.getJavaElement().getHandleIdentifier(), method);
	}

	public boolean removeMethod(IJaxrsResourceMethod method) {
		return (this.resourceMethods.remove(method.getJavaElement().getHandleIdentifier()) != null);
	}

	public Map<String, JaxrsResourceField> getFields() {
		return resourceFields;
	}

	public Map<String, JaxrsResourceMethod> getMethods() {
		return resourceMethods;
	}

	@Override
	public List<ValidatorMessage> validate() throws JavaModelException {
		List<ValidatorMessage> messages = new ArrayList<ValidatorMessage>();
		// delegating the validation to the undelying resource methods
		for (Entry<String, JaxrsResourceMethod> entry : resourceMethods.entrySet()) {
			final JaxrsResourceMethod resourceMethod = entry.getValue();
			messages.addAll(resourceMethod.validate());
		}
		return messages;
	}

}
