package org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain;

import static org.eclipse.jdt.core.IJavaElement.FIELD;
import static org.eclipse.jdt.core.IJavaElement.METHOD;
import static org.eclipse.jdt.core.IJavaElement.TYPE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod.Builder;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.JavaMethodParameter;
import org.jboss.tools.ws.jaxrs.core.jdt.JavaMethodSignature;
import org.jboss.tools.ws.jaxrs.core.jdt.JaxrsAnnotationsScanner;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResourceMethod;

public class JaxrsElementFactory {

	/** Attempts to create a new JAX-RS element from the given Java annotation.
	 * 
	 * @param element
	 * @param ast
	 * @param metamodel
	 * @return
	 * @throws CoreException */
	public List<IJaxrsElement<?>> createElement(IAnnotation javaAnnotation, CompilationUnit ast,
			JaxrsMetamodel metamodel) throws CoreException {
		final List<IJaxrsElement<?>> elements = new ArrayList<IJaxrsElement<?>>();
		Annotation annotation = JdtUtils.resolveAnnotation(javaAnnotation, ast);
		final String annotationName = annotation.getName();
		if (annotationName.equals(HttpMethod.class.getName())) {
			elements.add(createHttpMethod(annotation, ast, metamodel));
		} else {
			switch (javaAnnotation.getParent().getElementType()) {
			case TYPE:
				if (annotationName.equals(Path.class.getName())) {
					elements.add(createResource(annotation, ast, metamodel));
				}
				break;
			case METHOD:
				final IJaxrsHttpMethod httpMethod = metamodel.getHttpMethod(annotationName);
				if (annotationName.equals(Path.class.getName())) {
					elements.add(createResourceMethod(annotation, ast, metamodel));
				} else if (httpMethod != null) {
					elements.add(createResourceMethod(annotation, ast, metamodel));
				}
				break;
			case FIELD:
				if (annotationName.equals(PathParam.class.getName())
						|| annotationName.equals(QueryParam.class.getName())
						|| annotationName.equals(MatrixParam.class.getName())) {
					elements.add(createField(annotation, ast, metamodel));
				}
				break;
			}
		}
		return elements;
	}

	/** Creates a JAX-RS Root Resource from the given path annotation and its
	 * AST, and adds it to the given JAX-RS Metamodel.
	 * 
	 * @param pathAnnotation
	 *            the @Path annotation found on the Java Type
	 * @param ast
	 *            the AST associated to the annotated java type
	 * @param metamodel
	 *            the current metamodel, in which the Root Resource should be
	 *            added
	 * @return the created Root Resource
	 * @throws CoreException */
	public JaxrsResource createResource(final Annotation pathAnnotation, final CompilationUnit ast,
			final JaxrsMetamodel metamodel) throws CoreException {
		// create the resource:
		return createResource((IType) pathAnnotation.getJavaParent(), ast, metamodel);
	}

	/** Creates a JAX-RS Resource (and its methods) from the given type and its
	 * AST, and adds it
	 * to the given JAX-RS Metamodel.
	 * 
	 * @param javaType
	 *            the Java Type
	 * @param ast
	 *            the AST associated to the java type
	 * @param metamodel
	 *            the current metamodel, in which the JAX-RS Resource should be
	 *            added
	 * @return the created resource
	 * @throws CoreException */
	public JaxrsResource createResource(IType javaType, CompilationUnit ast, JaxrsMetamodel metamodel)
			throws CoreException {
		// create the resource:
		final JaxrsResource resource = internalCreateResource(javaType, ast, metamodel);
		// find the resource methods, subresource methods and subresource
		// locators of this resource:
		final List<IMethod> javaMethods = JaxrsAnnotationsScanner.findResourceMethods(javaType,
				metamodel.getAllHttpMethods(), new NullProgressMonitor());
		for (IMethod javaMethod : javaMethods) {
			final IJaxrsResourceMethod resourceMethod = internalCreateResourceMethod(javaMethod, ast, metamodel,
					resource);
			if (resourceMethod != null) {
				metamodel.add(resourceMethod);
			}
		}
		// find the available type fields
		for (IField javaField : javaType.getFields()) {
			internalCreateField(javaField, ast, metamodel, resource);
		}
		// well, sorry.. this is not a valid JAX-RS resource..
		if (resource.getKind() == EnumKind.UNDEFINED) {
			return null;
		}
		metamodel.add(resource);
		return resource;
	}

	private JaxrsResource internalCreateResource(IType type, CompilationUnit ast, JaxrsMetamodel metamodel)
			throws JavaModelException {
		final Map<String, Annotation> annotations = JdtUtils.resolveAnnotations(type, ast, Path.class.getName(),
				Consumes.class.getName(), Produces.class.getName());
		final Annotation pathAnnotation = annotations.get(Path.class.getName());
		final Annotation consumesAnnotation = annotations.get(Consumes.class.getName());
		final Annotation producesAnnotation = annotations.get(Produces.class.getName());
		final JaxrsResource resource = new JaxrsResource.Builder(type, metamodel).pathTemplate(pathAnnotation)
				.consumes(consumesAnnotation).produces(producesAnnotation).build();
		return resource;
	}

	/** Creates a JAX-RS resource method from the given annotation (@Path or an
	 * HttpMethod) and its AST, and finally adds it to the given JAX-RS
	 * Metamodel.
	 * 
	 * @param annotation
	 * @param ast
	 * @param metamodel
	 * @return
	 * @throws CoreException */
	public IJaxrsResourceMethod createResourceMethod(Annotation annotation, CompilationUnit ast,
			JaxrsMetamodel metamodel) throws CoreException {
		final IMethod method = (IMethod) annotation.getJavaParent();
		return createResourceMethod(method, ast, metamodel);
	}

	/** Creates a JAX-RS resource method from the given annotation (@Path or an
	 * HttpMethod) and its AST, and finally adds it to the given JAX-RS
	 * Metamodel.
	 * 
	 * @param annotation
	 * @param ast
	 * @param metamodel
	 * @return
	 * @throws CoreException */
	public IJaxrsResourceMethod createResourceMethod(IMethod method, CompilationUnit ast, JaxrsMetamodel metamodel)
			throws CoreException {

		final IType parentType = (IType) method.getParent();
		JaxrsResource parentResource = (JaxrsResource) metamodel.getElement(parentType);
		boolean parentResourceCreated = false;
		if (parentResource == null) {
			// create parentResource on-the-fly
			parentResource = internalCreateResource(parentType, ast, metamodel);
			parentResourceCreated = true;
		}
		final IJaxrsResourceMethod resourceMethod = internalCreateResourceMethod(method, ast, metamodel, parentResource);
		if (resourceMethod != null) {
			metamodel.add(resourceMethod);
			// now, the parent resource can be surely added to the metamodel
			if (parentResourceCreated) {
				metamodel.add(parentResource);
			}
			return resourceMethod;
		}
		return null;
	}

	private IJaxrsResourceMethod internalCreateResourceMethod(IMethod javaMethod, CompilationUnit ast,
			JaxrsMetamodel metamodel, JaxrsResource parentResource) throws JavaModelException {
		final List<String> httpMethodAnnotationNames = new ArrayList<String>();
		for (IJaxrsHttpMethod httpMethod : metamodel.getAllHttpMethods()) {
			httpMethodAnnotationNames.add(httpMethod.getJavaElement().getFullyQualifiedName());
		}
		final List<String> annotationNames = new ArrayList<String>();
		annotationNames.addAll(Arrays.asList(Path.class.getName(), Produces.class.getName(), Consumes.class.getName()));
		annotationNames.addAll(httpMethodAnnotationNames);
		final Map<String, Annotation> annotations = JdtUtils.resolveAnnotations(javaMethod, ast, annotationNames);
		Annotation httpMethod = null;
		final Annotation pathAnnotation = annotations.get(Path.class.getName());
		for (String httpMethodAnnotationName : httpMethodAnnotationNames) {
			if (annotations.containsKey(httpMethodAnnotationName)) {
				httpMethod = annotations.get(httpMethodAnnotationName);
				break;
			}
		}
		if (httpMethod == null && pathAnnotation == null) {
			Logger.debug("Cannot create ResourceMethod: no Path annotation nor HttpMethod found on method {}.{}()", javaMethod.getParent().getElementName(), javaMethod.getElementName());
		} else {
			final Annotation producesAnnotation = annotations.get(Produces.class.getName());
			final Annotation consumesAnnotation = annotations.get(Consumes.class.getName());
			final JavaMethodSignature methodSignature = JdtUtils.resolveMethodSignature(javaMethod, ast);
			final Builder builder = new JaxrsResourceMethod.Builder(javaMethod, parentResource, metamodel)
					.pathTemplate(pathAnnotation).consumes(consumesAnnotation).produces(producesAnnotation)
					.httpMethod(httpMethod).returnType(methodSignature.getReturnedType());
			for (JavaMethodParameter methodParam : methodSignature.getMethodParameters()) {
				builder.methodParameter(methodParam);
			}
			final IJaxrsResourceMethod resourceMethod = builder.build();

			return resourceMethod;
		}
		return null;

	}

	/** Creates a JAX-RS HTTP Method from the given pathAnnotation and its AST,
	 * and adds it to the given JAX-RS Metamodel.
	 * 
	 * @param ast
	 * @param metamodel
	 * @param annotation
	 * @return
	 * @throws CoreException */
	public JaxrsHttpMethod createHttpMethod(final IType javaType, final CompilationUnit ast,
			final JaxrsMetamodel metamodel) throws CoreException {
		Annotation httpMethodAnnotation = JdtUtils.resolveAnnotation(javaType, ast, HttpMethod.class);
		if (httpMethodAnnotation == null) {
			return null;
		}
		final JaxrsHttpMethod httpMethod = new JaxrsHttpMethod(javaType, httpMethodAnnotation, metamodel);
		metamodel.add(httpMethod);
		return httpMethod;
	}

	/** Creates a JAX-RS HTTP Method from the given pathAnnotation and its AST,
	 * and adds it to the given JAX-RS Metamodel.
	 * 
	 * @param ast
	 * @param metamodel
	 * @param annotation
	 * @return
	 * @throws CoreException */
	public JaxrsHttpMethod createHttpMethod(final Annotation annotation, final CompilationUnit ast,
			final JaxrsMetamodel metamodel) throws CoreException {
		final JaxrsHttpMethod httpMethod = new JaxrsHttpMethod((IType) annotation.getJavaParent(), annotation,
				metamodel);
		metamodel.add(httpMethod);
		return httpMethod;
	}

	/** Create a JAX-RS Resource field from the given annotation.
	 * 
	 * @param pathParamannotation
	 * @param ast
	 * @param metamodel
	 * @return
	 * @throws JavaModelException
	 * @throws CoreException */
	public JaxrsParamField createField(Annotation annotation, CompilationUnit ast, JaxrsMetamodel metamodel)
			throws JavaModelException {
		final IField javaField = (IField) annotation.getJavaParent();
		return createField(javaField, ast, metamodel);
	}

	public JaxrsParamField createField(IField javaField, CompilationUnit ast, JaxrsMetamodel metamodel)
			throws JavaModelException {
		final IType parentType = (IType) javaField.getParent();
		IJaxrsElement<?> parentResource = metamodel.getElement(parentType);
		boolean parentResourceCreated = false;
		if (parentResource == null) {
			parentResourceCreated = true;
			// creating the parent resource but not adding it to the metamodel
			// yet..
			parentResource = internalCreateResource(parentType, ast, metamodel);
		}
		if (parentResource != null && parentResource.getElementKind() == EnumElementKind.RESOURCE) {
			final JaxrsParamField field = internalCreateField(javaField, ast, metamodel, (JaxrsResource) parentResource);
			if (field != null) {
				metamodel.add(field);
				// now, the parent resource can be surely added to the metamodel
				if (parentResourceCreated) {
					metamodel.add(parentResource);
				}
			}
			return field;

		}
		return null;
	}

	private JaxrsParamField internalCreateField(IField javaField, CompilationUnit ast, JaxrsMetamodel metamodel,
			final JaxrsResource parentResource) throws JavaModelException {
		final List<String> supportedFieldAnnotations = Arrays.asList(MatrixParam.class.getName(),
				QueryParam.class.getName(), PathParam.class.getName(), CookieParam.class.getName(),
				HeaderParam.class.getName(), DefaultValue.class.getName());
		final Map<String, Annotation> annotations = JdtUtils.resolveAnnotations(javaField, ast,
				supportedFieldAnnotations);
		if ((annotations.size() == 1 && !annotations.containsKey(DefaultValue.class.getName()))
				|| (annotations.size() == 2 && annotations.containsKey(DefaultValue.class.getName()))) {
			final JaxrsParamField field = new JaxrsParamField(javaField,
					new ArrayList<Annotation>(annotations.values()), parentResource, metamodel);
			return field;
		}
		return null;
	}

}
