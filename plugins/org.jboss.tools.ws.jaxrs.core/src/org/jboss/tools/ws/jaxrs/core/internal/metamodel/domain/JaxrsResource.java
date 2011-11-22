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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.wst.validation.ValidatorMessage;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsApplication;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResource;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResourceMethod;

/** From the spec : A resource class is a Java class that uses JAX-RS annotations
 * to implement a corresponding Web resource. Resource classes are POJOs that
 * have at least one method annotated with @Path or a request method designator.
 * 
 * @author xcoulon */
public class JaxrsResource extends JaxrsElement<IType> implements IJaxrsResource {

	/** Optional Application. */
	private final JaxrsApplication application = null;

	// private Annotation pathAnnotation = null;

	// private Annotation consumesAnnotation = null;

	// private Annotation producesAnnotation = null;

	private final Map<String, JaxrsParamField> paramFields = new HashMap<String, JaxrsParamField>();

	private final Map<String, JaxrsParamBeanProperty> paramBeanProperties = new HashMap<String, JaxrsParamBeanProperty>();

	private final Map<String, IJaxrsResourceMethod> resourceMethods = new HashMap<String, IJaxrsResourceMethod>();

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

		public Builder pathTemplate(Annotation pathAnnotation) {
			this.pathAnnotation = pathAnnotation;
			return this;
		}

		public Builder consumes(Annotation consumesAnnotation) {
			this.consumesAnnotation = consumesAnnotation;
			return this;
		}

		public Builder produces(Annotation producesAnnotation) {
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

	@Override
	public final boolean isRootResource() {
		return getKind() == EnumKind.ROOT_RESOURCE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResource#isSubresource()
	 */
	@Override
	public boolean isSubresource() {
		return getKind() == EnumKind.SUBRESOURCE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jboss.tools.ws.jaxrs.core.internal.metamodel.IResource#getKind()
	 */
	@Override
	public final EnumKind getKind() {
		final Annotation pathAnnotation = getAnnotation(Path.class.getName());
		if (pathAnnotation != null) {
			return EnumKind.ROOT_RESOURCE;
		} else if (resourceMethods.size() > 0 || paramFields.size() > 0 || paramBeanProperties.size() > 0) {
			return EnumKind.SUBRESOURCE;
		}
		return EnumKind.UNDEFINED;
	}

	@Override
	public final void hasErrors(final boolean hasErrors) {
		super.hasErrors(hasErrors);
		if (!hasErrors) {
			for (IJaxrsResourceMethod resourceMethod : resourceMethods.values()) {
				resourceMethod.hasErrors(hasErrors);
			}
		}
	}

	private static String computeKey(final IMethod method) throws JavaModelException {
		StringBuffer key = new StringBuffer(method.getElementName()).append('(');
		for (String parameterType : method.getParameterTypes()) {
			key.append(parameterType);
		}
		return key.append(')').toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jboss.tools.ws.jaxrs.core.internal.metamodel.IResource#getName()
	 */
	@Override
	public final String getName() {
		return getJavaElement().getElementName();
	}

	@Override
	public String getPathTemplate() {
		final Annotation pathAnnotation = getAnnotation(Path.class.getName());
		if (pathAnnotation == null) {
			return null;
		}
		return pathAnnotation.getValue("value");
	}

	@Override
	public Annotation getPathAnnotation() {
		final Annotation pathAnnotation = getAnnotation(Path.class.getName());
		return pathAnnotation;
	}

	@Override
	public List<String> getConsumedMediaTypes() {
		final Annotation consumesAnnotation = getAnnotation(Consumes.class.getName());
		if (consumesAnnotation != null) {
			return consumesAnnotation.getValues("value");
		}
		return null;
	}

	@Override
	public Annotation getConsumesAnnotation() {
		final Annotation consumesAnnotation = getAnnotation(Consumes.class.getName());
		return consumesAnnotation;
	}

	@Override
	public List<String> getProducedMediaTypes() {
		final Annotation producesAnnotation = getAnnotation(Produces.class.getName());
		if (producesAnnotation != null) {
			return producesAnnotation.getValues("value");
		}
		return null;
	}

	@Override
	public Annotation getProducesAnnotation() {
		final Annotation producesAnnotation = getAnnotation(Produces.class.getName());
		return producesAnnotation;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jboss.tools.ws.jaxrs.core.internal.metamodel.IResource#getByJavaMethod
	 * (org.eclipse.jdt.core.IMethod)
	 */
	@Override
	public final IJaxrsResourceMethod getByJavaMethod(final IMethod javaMethod) throws JavaModelException {
		return resourceMethods.get(computeKey(javaMethod));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jboss.tools.ws.jaxrs.core.internal.metamodel.IResource#getApplication
	 * ()
	 */
	@Override
	public final IJaxrsApplication getApplication() {
		return application;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jboss.tools.ws.jaxrs.core.internal.metamodel.IResource#getResourceMethods
	 * ()
	 * 
	 * @Override public final List<IJaxrsResourceMethod> getResourceMethods() {
	 * return filterElementsByKind(EnumKind.RESOURCE_METHOD); }
	 */

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jboss.tools.ws.jaxrs.core.internal.metamodel.IResource#
	 * getSubresourceMethods()
	 */
	@Override
	public final List<IJaxrsResourceMethod> getSubresourceMethods() {
		return filterElementsByKind(EnumKind.SUBRESOURCE_METHOD);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jboss.tools.ws.jaxrs.core.internal.metamodel.IResource#
	 * getSubresourceLocators()
	 */
	@Override
	public final List<IJaxrsResourceMethod> getSubresourceLocators() {
		return filterElementsByKind(EnumKind.SUBRESOURCE_LOCATOR);
	}

	private final List<IJaxrsResourceMethod> filterElementsByKind(EnumKind kind) {
		List<IJaxrsResourceMethod> matches = new ArrayList<IJaxrsResourceMethod>();
		for (Entry<String, IJaxrsResourceMethod> entry : this.resourceMethods.entrySet()) {
			IJaxrsResourceMethod resource = entry.getValue();
			if (resource.getKind() == kind) {
				matches.add(resource);
			}
		}
		return matches;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jboss.tools.ws.jaxrs.core.internal.metamodel.IResource#getAllMethods
	 * ()
	 */
	@Override
	public final List<IJaxrsResourceMethod> getAllMethods() {
		return Collections.unmodifiableList(new ArrayList<IJaxrsResourceMethod>(resourceMethods.values()));
	}

	@Override
	public String toString() {
		return new StringBuffer().append(getName()).append(" (root:").append(isRootResource()).append(") ").toString();
	}

	public void addField(JaxrsParamField field) {
		this.paramFields.put(field.getJavaElement().getHandleIdentifier(), field);
	}

	public boolean removeField(JaxrsParamField field) {
		return (this.paramFields.remove(field.getJavaElement().getHandleIdentifier()) != null);
	}

	public void addMethod(IJaxrsResourceMethod method) {
		this.resourceMethods.put(method.getJavaElement().getHandleIdentifier(), method);
	}

	public boolean removeMethod(IJaxrsResourceMethod method) {
		return (this.resourceMethods.remove(method.getJavaElement().getHandleIdentifier()) != null);
	}

	public Collection<JaxrsParamField> getFields() {
		return paramFields.values();
	}

	@Override
	public List<IJaxrsResourceMethod> getResourceMethods() {
		return new ArrayList<IJaxrsResourceMethod>(resourceMethods.values());
	}

	@Override
	public List<ValidatorMessage> validate() {
		List<ValidatorMessage> messages = new ArrayList<ValidatorMessage>();
		// delegating the validation to the undelying resource methods 
		for(Entry<String, IJaxrsResourceMethod> entry : resourceMethods.entrySet()) {
			final IJaxrsResourceMethod resourceMethod = entry.getValue();
			messages.addAll(resourceMethod.validate());
		}
		return messages;
	}

}
