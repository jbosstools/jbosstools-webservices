/******************************************************************************* 
Le * Copyright (c) 2008 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * real racin
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain;

import static org.eclipse.jdt.core.IJavaElement.FIELD;
import static org.eclipse.jdt.core.IJavaElement.METHOD;
import static org.eclipse.jdt.core.IJavaElement.TYPE;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.APPLICATION_PATH;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.CONSUMES;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.COOKIE_PARAM;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.DEFAULT_VALUE;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.HEADER_PARAM;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.HTTP_METHOD;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.MATRIX_PARAM;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PATH;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PATH_PARAM;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PRODUCES;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PROVIDER;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.QUERY_PARAM;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.RETENTION;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.TARGET;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod.Builder;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Pair;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname;
import org.jboss.tools.ws.jaxrs.core.jdt.JavaMethodParameter;
import org.jboss.tools.ws.jaxrs.core.jdt.JavaMethodSignature;
import org.jboss.tools.ws.jaxrs.core.jdt.JaxrsAnnotationsScanner;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsHttpMethod;

/**
 * Factory for JAX-RS elements that should be created from Java elements.
 * @author Xavier Coulon
 *
 */
public class JaxrsElementFactory {

	/**
	 * Attempts to create a new JAX-RS element from the given Java annotation, <b>without adding it to the given JAX-RS
	 * Metamodel</b>
	 * 
	 * @param element
	 * @param ast
	 * @param metamodel
	 * @return the created JAX-RS element or null if the given Java annotation is not a valid one.
	 * @throws CoreException
	 */
	public JaxrsJavaElement<?> createElement(IAnnotation javaAnnotation, CompilationUnit ast, JaxrsMetamodel metamodel)
			throws CoreException {
		Annotation annotation = JdtUtils.resolveAnnotation(javaAnnotation, ast);
		if(annotation == null) { // annotation on package declaration are ignored
			return null;
		}
		final String annotationName = annotation.getFullyQualifiedName();
		if (annotationName.equals(HTTP_METHOD.qualifiedName)) {
			final JaxrsHttpMethod httpMethod = createHttpMethod(annotation, ast, metamodel);
			return httpMethod;
		} else if (annotationName.equals(APPLICATION_PATH.qualifiedName)) {
			final JaxrsJavaApplication application = createApplication(annotation, ast, metamodel);
			return application;
		} else {
			switch (javaAnnotation.getParent().getElementType()) {
			case TYPE:
				if (annotationName.equals(PATH.qualifiedName)) {
					return createResource(annotation, ast, metamodel);
				}
				break;
			case METHOD:
				final JaxrsHttpMethod httpMethod = (JaxrsHttpMethod) metamodel.getHttpMethod(annotationName);
				if (annotationName.equals(PATH.qualifiedName)) {
					return createResourceMethod(annotation, ast, metamodel);
				} else if (httpMethod != null) {
					return createResourceMethod(annotation, ast, metamodel);
				}
				break;
			case FIELD:
				if (annotationName.equals(PATH_PARAM.qualifiedName)
						|| annotationName.equals(QUERY_PARAM.qualifiedName)
						|| annotationName.equals(MATRIX_PARAM.qualifiedName)) {
					return createField(annotation, ast, metamodel);
				}
				break;
			}
		}
		return null;
	}

	/**
	 * Creates a JAX-RS Root Resource (including its methods and its fields) from the given path annotation and its AST,
	 * <b>without adding it to the given JAX-RS Metamodel</b>
	 * 
	 * @param pathAnnotation
	 *            the @Path annotation found on the Java Type
	 * @param ast
	 *            the AST associated to the annotated java type
	 * @param metamodel
	 *            the current metamodel, in which the Root Resource should be added
	 * @return the created Root Resource
	 * @throws CoreException
	 */
	public JaxrsResource createResource(final Annotation pathAnnotation, final CompilationUnit ast,
			final JaxrsMetamodel metamodel) throws CoreException {
		// create the resource:
		return createResource((IType) pathAnnotation.getJavaParent(), ast, metamodel);
	}

	/**
	 * Creates a JAX-RS Resource (including its methods and its fields) from the given type and its AST, <b>without
	 * adding it to the given JAX-RS Metamodel</b>
	 * 
	 * @param javaType
	 *            the Java Type
	 * @param ast
	 *            the AST associated to the java type
	 * @param metamodel
	 *            the current metamodel, in which the JAX-RS Resource should be added
	 * @return the created resource, or null if the java type did not exist.
	 * @throws CoreException
	 */
	public JaxrsResource createResource(IType javaType, CompilationUnit ast, JaxrsMetamodel metamodel)
			throws CoreException {
		if(!javaType.exists()) {
			return null;
		}
		// create the resource:
		final JaxrsResource resource = internalCreateResource(javaType, ast, metamodel);
		// find the resource methods, subresource methods and subresource
		// locators of this resource:
		final List<IMethod> javaMethods = JaxrsAnnotationsScanner.findResourceMethods(javaType,
				metamodel.getAllHttpMethods(), new NullProgressMonitor());
		for (IMethod javaMethod : javaMethods) {
			internalCreateResourceMethod(javaMethod, ast, metamodel, resource);
		}
		// find the available type fields
		for (IField javaField : javaType.getFields()) {
			internalCreateField(javaField, ast, metamodel, resource);
		}
		// well, sorry.. this is not a valid JAX-RS resource..
		if (resource.getElementKind() == EnumElementKind.UNDEFINED) {
			return null;
		}
		return resource;
	}

	private JaxrsResource internalCreateResource(IType type, CompilationUnit ast, JaxrsMetamodel metamodel)
			throws JavaModelException {
		final Map<String, Annotation> annotations = JdtUtils.resolveAnnotations(type, ast, PATH.qualifiedName,
				CONSUMES.qualifiedName, PRODUCES.qualifiedName);
		final Annotation pathAnnotation = annotations.get(PATH.qualifiedName);
		final Annotation consumesAnnotation = annotations.get(CONSUMES.qualifiedName);
		final Annotation producesAnnotation = annotations.get(PRODUCES.qualifiedName);
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation)
				.consumes(consumesAnnotation).produces(producesAnnotation).build();
		return resource;
	}

	/**
	 * Creates a JAX-RS resource method from the given annotation (@Path or an HttpMethod) and its AST, <b>without
	 * adding it to the given JAX-RS Metamodel</b>. If the parent resource did not exist before, its is also created.
	 * 
	 * @param annotation
	 * @param ast
	 * @param metamodel
	 * @return
	 * @throws CoreException
	 */
	public JaxrsResourceMethod createResourceMethod(Annotation annotation, CompilationUnit ast, JaxrsMetamodel metamodel)
			throws CoreException {
		final IMethod method = (IMethod) annotation.getJavaParent();
		return createResourceMethod(method, ast, metamodel);
	}

	/**
	 * Creates a JAX-RS resource method from the given annotation (@Path or an HttpMethod) and its AST, <b>without
	 * adding it to the given JAX-RS Metamodel</b>
	 * 
	 * @param annotation
	 * @param ast
	 * @param metamodel
	 * @return
	 * @throws CoreException
	 */
	public JaxrsResourceMethod createResourceMethod(IMethod method, CompilationUnit ast, JaxrsMetamodel metamodel)
			throws CoreException {
		if(!method.exists()) {
			return null;
		}
		final IType parentType = (IType) method.getParent();
		JaxrsResource parentResource = (JaxrsResource) metamodel.getElement(parentType);
		if (parentResource == null) {
			// create parentResource on-the-fly
			parentResource = internalCreateResource(parentType, ast, metamodel);
		}
		final JaxrsResourceMethod resourceMethod = internalCreateResourceMethod(method, ast, metamodel, parentResource);
		return resourceMethod;
	}

	private JaxrsResourceMethod internalCreateResourceMethod(IMethod javaMethod, CompilationUnit ast,
			JaxrsMetamodel metamodel, JaxrsResource parentResource) throws JavaModelException {
		final List<String> httpMethodAnnotationNames = new ArrayList<String>();
		for (IJaxrsHttpMethod httpMethod : metamodel.getAllHttpMethods()) {
			httpMethodAnnotationNames.add(httpMethod.getJavaClassName());
		}
		final List<String> annotationNames = new ArrayList<String>();
		annotationNames.addAll(Arrays.asList(PATH.qualifiedName, PRODUCES.qualifiedName, CONSUMES.qualifiedName));
		annotationNames.addAll(httpMethodAnnotationNames);
		final Map<String, Annotation> annotations = JdtUtils.resolveAnnotations(javaMethod, ast, annotationNames);
		Annotation httpMethod = null;
		final Annotation pathAnnotation = annotations.get(PATH.qualifiedName);
		for (String httpMethodAnnotationName : httpMethodAnnotationNames) {
			if (annotations.containsKey(httpMethodAnnotationName)) {
				httpMethod = annotations.get(httpMethodAnnotationName);
				break;
			}
		}
		if (httpMethod == null && pathAnnotation == null) {
			Logger.debug("Cannot create ResourceMethod: no Path annotation nor HttpMethod found on method {}.{}()",
					javaMethod.getParent().getElementName(), javaMethod.getElementName());
		} else {
			final Annotation producesAnnotation = annotations.get(PRODUCES.qualifiedName);
			final Annotation consumesAnnotation = annotations.get(CONSUMES.qualifiedName);
			final JavaMethodSignature methodSignature = JdtUtils.resolveMethodSignature(javaMethod, ast);
			// avoid creating Resource Method when the Java Method cannot be parsed (ie, syntax/compilation error)P
			if (methodSignature != null) {
				final Builder builder = new JaxrsResourceMethod.Builder(javaMethod, parentResource, metamodel)
						.pathTemplate(pathAnnotation).consumes(consumesAnnotation).produces(producesAnnotation)
						.httpMethod(httpMethod).returnType(methodSignature.getReturnedType());
				for (Entry<String, JavaMethodParameter> methodParamEntry : methodSignature.getMethodParameters().entrySet()) {
					builder.methodParameter(methodParamEntry.getValue());
				}
				final JaxrsResourceMethod resourceMethod = builder.build();

				return resourceMethod;
			}
		}
		return null;

	}

	/**
	 * Creates a JAX-RS HTTP Method from the given {@link javax.ws.rs.HttpMethod} annotation and its AST, <b>without
	 * adding it to the given JAX-RS Metamodel</b>
	 * 
	 * @param ast
	 * @param metamodel
	 * @param annotation
	 * @return
	 * @throws CoreException
	 */
	public JaxrsHttpMethod createHttpMethod(final IType javaType, final CompilationUnit ast,
			final JaxrsMetamodel metamodel) throws CoreException {
		if(!javaType.exists()) {
			return null;
		}
		Map<String, Annotation> annotations = JdtUtils.resolveAnnotations(javaType, ast, HTTP_METHOD.qualifiedName, TARGET.qualifiedName, RETENTION.qualifiedName);
		if (annotations == null || annotations.isEmpty()) {
			return null;
		}
		final JaxrsHttpMethod httpMethod = new JaxrsHttpMethod.Builder(javaType, metamodel).annotations(annotations.values()).build();
		return httpMethod;
	}

	/**
	 * Creates a JAX-RS Application from the given pathAnnotation and its AST, <b>without adding it to the given JAX-RS
	 * Metamodel</b>.
	 * 
	 * @param ast
	 * @param metamodel
	 * @param annotation
	 * @return
	 * @throws CoreException
	 */
	public JaxrsHttpMethod createHttpMethod(final Annotation annotation, final CompilationUnit ast,
			final JaxrsMetamodel metamodel) throws CoreException {
		if (annotation.getJavaParent() != null && annotation.getJavaParent().getElementType() == IJavaElement.TYPE
				&& annotation.getFullyQualifiedName().equals(HTTP_METHOD.qualifiedName)) {
			//return new JaxrsHttpMethod.Builder((IType) annotation.getJavaParent(), metamodel).httpMethod(annotation).build();
			return createHttpMethod((IType) annotation.getJavaParent(), ast, metamodel);
		}
		return null;
	}

	/**
	 * Creates a JAX-RS Application from the given type and its AST, <b>without adding it to the given JAX-RS
	 * Metamodel</b>
	 * 
	 * @param ast
	 * @param metamodel
	 * @param annotation
	 * @return
	 * @throws CoreException
	 */
	public JaxrsJavaApplication createApplication(final IType javaType, final CompilationUnit ast,
			final JaxrsMetamodel metamodel) throws CoreException {
		if(!javaType.exists()) {
			return null;
		}
		Annotation applicationPathAnnotation = JdtUtils.resolveAnnotation(javaType, ast, APPLICATION_PATH.qualifiedName);
		return createApplication(javaType, applicationPathAnnotation, metamodel);
	}

	/**
	 * Creates a JAX-RS Application from the given {@link javax.ws.rs.ApplicationPath } annotation and its AST,
	 * <b>without adding it to the given JAX-RS Metamodel</b>.
	 * 
	 * @param ast
	 * @param metamodel
	 * @param annotation
	 * @return
	 * @throws CoreException
	 */
	public JaxrsJavaApplication createApplication(final Annotation annotation, final CompilationUnit ast,
			final JaxrsMetamodel metamodel) throws CoreException {
		final IJavaElement javaParent = annotation.getJavaParent();
		if (javaParent != null && javaParent.getElementType() == IJavaElement.TYPE
				&& annotation.getFullyQualifiedName().equals(APPLICATION_PATH.qualifiedName)) {
			final IType javaType = (IType) javaParent;
			return createApplication(javaType, annotation, metamodel);
		}
		return null;
	}
	
	/**
	 * Creates a JAX-RS Application from the given type and its AST, <b>without adding it to the given JAX-RS
	 * Metamodel</b>
	 * 
	 * @param ast
	 * @param metamodel
	 * @param annotation
	 * @return
	 * @throws CoreException
	 */
	private JaxrsJavaApplication createApplication(final IType applicationType, final Annotation appPathAnnotation, 
			final JaxrsMetamodel metamodel) throws CoreException {
		if(!applicationType.exists()) {
			return null;
		}
		final IType applicationSupertype = JdtUtils.resolveType(EnumJaxrsClassname.APPLICATION.qualifiedName, applicationType.getJavaProject(), new NullProgressMonitor());
		final boolean isApplicationSubclass = JdtUtils.isTypeOrSuperType(applicationSupertype, applicationType);
		if(isApplicationSubclass || appPathAnnotation != null) {
			return new JaxrsJavaApplication(applicationType, appPathAnnotation, isApplicationSubclass, metamodel);
		}
		return null;
	}


	/**
	 * Create a JAX-RS Resource field from the given annotation, <b>without adding it to the given JAX-RS Metamodel</b>
	 * 
	 * @param pathParamannotation
	 * @param ast
	 * @param metamodel
	 * @return
	 * @throws JavaModelException
	 * @throws CoreException
	 */
	public JaxrsResourceField createField(Annotation annotation, CompilationUnit ast, JaxrsMetamodel metamodel)
			throws JavaModelException {
		final IField javaField = (IField) annotation.getJavaParent();
		return createField(javaField, ast, metamodel);
	}

	public JaxrsResourceField createField(IField javaField, CompilationUnit ast, JaxrsMetamodel metamodel)
			throws JavaModelException {
		if(!javaField.exists()) {
			return null;
		}
		final IType parentType = (IType) javaField.getParent();
		IJaxrsElement parentResource = metamodel.getElement(parentType);
		if (parentResource == null) {
			// creating the parent resource but not adding it to the metamodel
			// yet..
			parentResource = internalCreateResource(parentType, ast, metamodel);
		}
		if (parentResource != null && parentResource.getElementCategory() == EnumElementCategory.RESOURCE) {
			final JaxrsResourceField field = internalCreateField(javaField, ast, metamodel,
					(JaxrsResource) parentResource);
			return field;
		}
		return null;
	}

	private JaxrsResourceField internalCreateField(IField javaField, CompilationUnit ast, JaxrsMetamodel metamodel,
			final JaxrsResource parentResource) throws JavaModelException {
		final List<String> supportedFieldAnnotations = Arrays.asList(MATRIX_PARAM.qualifiedName,
				QUERY_PARAM.qualifiedName, PATH_PARAM.qualifiedName, COOKIE_PARAM.qualifiedName,
				HEADER_PARAM.qualifiedName, DEFAULT_VALUE.qualifiedName);
		final Map<String, Annotation> annotations = JdtUtils.resolveAnnotations(javaField, ast,
				supportedFieldAnnotations);
		if ((annotations.size() == 1 && !annotations.containsKey(DEFAULT_VALUE.qualifiedName))
				|| (annotations.size() == 2 && annotations.containsKey(DEFAULT_VALUE.qualifiedName))) {
			final JaxrsResourceField field = new JaxrsResourceField(javaField, new ArrayList<Annotation>(
					annotations.values()), parentResource, metamodel);
			return field;
		}
		return null;
	}

	public JaxrsWebxmlApplication createApplication(final String javaClassName, final String applicationPath, final IResource resource, final JaxrsMetamodel metamodel) {
		return new JaxrsWebxmlApplication(javaClassName, applicationPath, resource, metamodel);
	}

	/**
	 * Creates a JAX-RS Provider from the given Type. A valid Provider must be annotated with
	 * <code>javax.ws.rs.ext.MessageBodyReader</code>, <code>javax.ws.rs.ext.MessageBodyWriter</code> or
	 * <code>javax.ws.rs.ext.ExceptionMapper</code>. If the given type is not annotated with
	 * <code>javax.ws.rs.ext.Provider</code>, a should be reported to the user.
	 * 
	 * @param javaType
	 * @param metamodel
	 * @throws CoreException in case of underlying exception
	 * @return a representation of the given provider or null in case of invalid type (ie, not a valid JAX-RS Provider)
	 */
	public JaxrsProvider createProvider(final IType javaType, final CompilationUnit ast, final JaxrsMetamodel metamodel, final IProgressMonitor progressMonitor ) throws CoreException {
		if(!javaType.exists()) {
			return null;
		}
		final Map<String, Annotation> annotations = JdtUtils.resolveAnnotations(javaType, ast, PROVIDER.qualifiedName,
				CONSUMES.qualifiedName, PRODUCES.qualifiedName);
		// assert that given java type is not abstract 
		if(JdtUtils.isAbstractType(javaType)) {
			return null;
		}
		ITypeHierarchy providerTypeHierarchy = JdtUtils.resolveTypeHierarchy(javaType, javaType.getJavaProject(), false, progressMonitor);
		IType[] subtypes = providerTypeHierarchy.getSubtypes(javaType);
		// assert that given java type has no sub-type, or continue;
		if (subtypes != null && subtypes.length > 0) {
			return null;
		}
		Map<EnumElementKind, IType> providedKinds = getProvidedKinds(javaType, ast, providerTypeHierarchy, progressMonitor);

		//TODO: annotations are splitted here, but they are aggregated later again in the JaxrsProvider :-/
		final Annotation providerAnnotation = annotations.get(PROVIDER.qualifiedName);
		final Annotation consumesAnnotation = annotations.get(CONSUMES.qualifiedName);
		final Annotation producesAnnotation = annotations.get(PRODUCES.qualifiedName);
		
		final JaxrsProvider provider = new JaxrsProvider.Builder(javaType, metamodel).providing(providerAnnotation, providedKinds)
				.consumes(consumesAnnotation).produces(producesAnnotation).build();
		if(provider.getElementKind() == null) {
			return null;
		}
		return provider;
	}
	
	/**
	 * @param metamodel
	 * @param providerType
	 * @param providerTypeHierarchy
	 * @param providerInterfaces
	 * @param progressMonitor
	 * @param providerTypeHierarchy
	 * @return
	 * @throws CoreException
	 * @throws JavaModelException
	 */
	private static Map<EnumElementKind, IType> getProvidedKinds(final IType providerType,
			final CompilationUnit compilationUnit, final ITypeHierarchy providerTypeHierarchy,
			final IProgressMonitor progressMonitor)
			throws CoreException, JavaModelException {
		final Map<EnumElementKind, IType> providerKinds = new HashMap<EnumElementKind, IType>();
		List<Pair<EnumJaxrsClassname, EnumElementKind>> pairs = new ArrayList<Pair<EnumJaxrsClassname, EnumElementKind>>();
		pairs.add(Pair.makePair(EnumJaxrsClassname.MESSAGE_BODY_READER, EnumElementKind.MESSAGE_BODY_READER));
		pairs.add(Pair.makePair(EnumJaxrsClassname.MESSAGE_BODY_WRITER, EnumElementKind.MESSAGE_BODY_WRITER));
		pairs.add(Pair.makePair(EnumJaxrsClassname.EXCEPTION_MAPPER, EnumElementKind.EXCEPTION_MAPPER));
		
		for (Pair<EnumJaxrsClassname, EnumElementKind> pair : pairs) {
			final IType matchingGenericType = JdtUtils.resolveType(pair.a.qualifiedName, providerType.getJavaProject(), progressMonitor);
			List<IType> argumentTypes = JdtUtils.resolveTypeArguments(providerType, compilationUnit,
					matchingGenericType, providerTypeHierarchy, progressMonitor);
			if (argumentTypes == null || argumentTypes.size() == 0) {
				continue;
			}
			providerKinds.put(pair.b, argumentTypes.get(0));
		}
		return providerKinds;
	}
}
