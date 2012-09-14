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

package org.jboss.tools.ws.jaxrs.core.jdt;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.getAnnotation;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsElements.CONSUMES;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsElements.ENCODED;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsElements.GET;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsElements.HTTP_METHOD;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsElements.PATH;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsElements.PATH_PARAM;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsElements.PRODUCES;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsElements.RESPONSE;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.Assert;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.AbstractCommonTestCase;
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCoreTestsPlugin;
import org.jboss.tools.ws.jaxrs.core.WorkbenchUtils;
import org.junit.Test;
import org.osgi.framework.Bundle;

public class JdtUtilsTestCase extends AbstractCommonTestCase {

	private final Bundle bundle = JBossJaxrsCoreTestsPlugin.getDefault().getBundle();

	private final IProgressMonitor progressMonitor = new NullProgressMonitor();

	private IType getType(String typeName) throws CoreException {
		return JdtUtils.resolveType(typeName, javaProject, progressMonitor);
	}

	private IMethod getMethod(IType parentType, String methodName) throws JavaModelException {
		return WorkbenchUtils.getMethod(parentType, methodName);
	}

	@Test
	public void shouldResolveTypeByQNameInSourceCode() throws CoreException {
		Assert.assertNotNull("Type not found", JdtUtils.resolveType(
				"org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper", javaProject,
				new NullProgressMonitor()));
	}

	@Test
	public void shouldResolveTypeByQNameInLibrary() throws CoreException {
		Assert.assertNotNull("Type not found",
				JdtUtils.resolveType("javax.persistence.PersistenceException", javaProject, new NullProgressMonitor()));
	}

	@Test
	public void shouldNotResolveTypeByUnknownQName() throws CoreException {
		Assert.assertNull("No Type expected",
				JdtUtils.resolveType("unknown.class", javaProject, new NullProgressMonitor()));
	}

	@Test
	public void shouldResolveTypeByVeryQName() throws CoreException {
		Assert.assertNotNull("Type not found", JdtUtils.resolveType(
				"org.jboss.tools.ws.jaxrs.sample.extra.TestQualifiedException.TestException", javaProject,
				new NullProgressMonitor()));
	}

	@Test
	public void shouldAssertTypeIsAbstract() throws CoreException {
		IType type = JdtUtils.resolveType("org.jboss.resteasy.plugins.providers.AbstractEntityProvider", javaProject,
				new NullProgressMonitor());
		Assert.assertNotNull("Type not found", type);
		Assert.assertTrue("Type is abstract", JdtUtils.isAbstractType(type));
	}

	@Test
	public void shouldNotAssertTypeIsAbstract() throws CoreException {
		IType type = JdtUtils.resolveType(
				"org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper", javaProject,
				new NullProgressMonitor());
		Assert.assertNotNull("Type not found", type);
		Assert.assertFalse("Type is not abstract", JdtUtils.isAbstractType(type));
	}

	@Test
	public void shouldNotResolveTypeWithNullQName() throws CoreException {
		IType type = JdtUtils.resolveType(null, javaProject, new NullProgressMonitor());
		Assert.assertNull("No type was expected", type);
	}

	@Test
	public void shouldResolveTypeHierarchyFromClass() throws CoreException {
		IType type = JdtUtils.resolveType(
				"org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper", javaProject,
				new NullProgressMonitor());
		Assert.assertNotNull("Type not found", type);
		Assert.assertNotNull("Type hierarchy not found",
				JdtUtils.resolveTypeHierarchy(type, false, new NullProgressMonitor()));
	}

	@Test
	public void shouldResolveTypeHierarchyFromInterface() throws CoreException {
		IType type = JdtUtils.resolveType("javax.ws.rs.ext.MessageBodyReader", javaProject, new NullProgressMonitor());
		Assert.assertNotNull("Type not found", type);
		Assert.assertNotNull("Type hierarchy not found",
				JdtUtils.resolveTypeHierarchy(type, false, new NullProgressMonitor()));
	}

	@Test
	public void shouldResolveTypeHierarchyWithLibraries() throws CoreException {
		IType type = JdtUtils.resolveType("javax.ws.rs.ext.MessageBodyReader", javaProject, new NullProgressMonitor());
		Assert.assertNotNull("Type not found", type);
		Assert.assertNotNull("Type hierarchy not found",
				JdtUtils.resolveTypeHierarchy(type, true, new NullProgressMonitor()));
	}

	@Test
	public void shouldNotResolveTypeHierarchyOfRemovedClass() throws CoreException {
		IType type = JdtUtils.resolveType(
				"org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper", javaProject,
				new NullProgressMonitor());
		Assert.assertNotNull("Type not found", type);
		type.delete(true, new NullProgressMonitor());
		Assert.assertNull("No Type hierarchy expected",
				JdtUtils.resolveTypeHierarchy(type, false, new NullProgressMonitor()));
	}

	@Test
	public void shouldResolveConcreteTypeArgumentsOnBinaryTypesWithoutSources() throws CoreException,
			OperationCanceledException, InterruptedException {

		IType parameterizedType = JdtUtils.resolveType("org.jboss.resteasy.plugins.providers.jaxb.CollectionProvider",
				javaProject, progressMonitor);
		Assert.assertNotNull("Parameterized Type not found", parameterizedType);
		IType matchSuperInterfaceType = JdtUtils.resolveType("javax.ws.rs.ext.MessageBodyReader", javaProject,
				progressMonitor);
		Assert.assertNotNull("Interface Type not found", matchSuperInterfaceType);
		ITypeHierarchy parameterizedTypeHierarchy = JdtUtils.resolveTypeHierarchy(parameterizedType, false,
				progressMonitor);
		boolean removedReferencedLibrarySourceAttachment = WorkbenchUtils.removeReferencedLibrarySourceAttachment(
				javaProject, "resteasy-jaxb-provider-2.0.1.GA.jar", progressMonitor);
		Assert.assertTrue("Source attachment was not removed (not found?)", removedReferencedLibrarySourceAttachment);
		CompilationUnit compilationUnit = JdtUtils.parse(parameterizedType, null);
		List<IType> resolvedTypeParameters = JdtUtils.resolveTypeArguments(parameterizedType, compilationUnit,
				matchSuperInterfaceType, parameterizedTypeHierarchy, progressMonitor);
		Assert.assertNull("No type parameters expected", resolvedTypeParameters);
	}

	@Test
	public void shouldNotResolveTypeArgumentsOnBinaryImplementation() throws CoreException {
		IType parameterizedType = JdtUtils.resolveType(
				"org.jboss.resteasy.plugins.providers.jaxb.AbstractJAXBProvider", javaProject,
				new NullProgressMonitor());
		Assert.assertNotNull("Parameterized Type not found", parameterizedType);
		IType matchGenericType = JdtUtils.resolveType("org.jboss.resteasy.plugins.providers.AbstractEntityProvider",
				javaProject, new NullProgressMonitor());
		Assert.assertNotNull("Interface Type not found", matchGenericType);
		ITypeHierarchy parameterizedTypeHierarchy = JdtUtils.resolveTypeHierarchy(parameterizedType, false,
				new NullProgressMonitor());
		CompilationUnit compilationUnit = JdtUtils.parse(parameterizedType, null);
		List<IType> resolvedTypeParameters = JdtUtils.resolveTypeArguments(parameterizedType, compilationUnit,
				matchGenericType, parameterizedTypeHierarchy, new NullProgressMonitor());
		Assert.assertNull("No type parameters expected", resolvedTypeParameters);
	}

	@Test
	public void shouldResolveMultipleConcreteTypeArgumentsOnSourceImplementation() throws CoreException {
		IType parameterizedType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.extra.AnotherDummyProvider",
				javaProject, new NullProgressMonitor());
		Assert.assertNotNull("Parameterized Type not found", parameterizedType);

		// MessageBodyReader
		IType matchGenericType = JdtUtils.resolveType("javax.ws.rs.ext.MessageBodyReader", javaProject,
				new NullProgressMonitor());
		Assert.assertNotNull("Interface Type not found", matchGenericType);
		CompilationUnit compilationUnit = JdtUtils.parse(parameterizedType, null);
		ITypeHierarchy parameterizedTypeHierarchy = JdtUtils.resolveTypeHierarchy(parameterizedType, false,
				new NullProgressMonitor());

		List<IType> resolvedTypeParameters = JdtUtils.resolveTypeArguments(parameterizedType, compilationUnit,
				matchGenericType, parameterizedTypeHierarchy, new NullProgressMonitor());
		Assert.assertNotNull("No type parameters found", resolvedTypeParameters);
		Assert.assertEquals("Wrong number of type parameters found", 1, resolvedTypeParameters.size());
		Assert.assertEquals("Wrong type parameter found", "java.lang.Double", resolvedTypeParameters.get(0)
				.getFullyQualifiedName());
		// MessageBodyWriter
		matchGenericType = JdtUtils.resolveType("javax.ws.rs.ext.MessageBodyWriter", javaProject,
				new NullProgressMonitor());
		Assert.assertNotNull("Interface Type not found", matchGenericType);
		resolvedTypeParameters = JdtUtils.resolveTypeArguments(parameterizedType, compilationUnit, matchGenericType,
				parameterizedTypeHierarchy, new NullProgressMonitor());
		Assert.assertNotNull("No type parameters found", resolvedTypeParameters);
		Assert.assertEquals("Wrong number of type parameters found", 1, resolvedTypeParameters.size());
		Assert.assertEquals("Wrong type parameter found", "java.math.BigDecimal", resolvedTypeParameters.get(0)
				.getFullyQualifiedName());
		// ExceptionMapper
		matchGenericType = JdtUtils.resolveType("javax.ws.rs.ext.ExceptionMapper", javaProject,
				new NullProgressMonitor());
		Assert.assertNotNull("Interface Type not found", matchGenericType);
		resolvedTypeParameters = JdtUtils.resolveTypeArguments(parameterizedType, compilationUnit, matchGenericType,
				parameterizedTypeHierarchy, new NullProgressMonitor());
		Assert.assertNotNull("No type parameters found", resolvedTypeParameters);
		Assert.assertEquals("Wrong number of type parameters found", 1, resolvedTypeParameters.size());
		Assert.assertEquals("Wrong type parameter found", "java.lang.Exception", resolvedTypeParameters.get(0)
				.getFullyQualifiedName());
	}

	@Test
	public void shouldNotResolveTypeArgumentsOnWrongHierarchy() throws CoreException {
		IType parameterizedType = JdtUtils.resolveType("org.jboss.resteasy.plugins.providers.jaxb.CollectionProvider",
				javaProject, new NullProgressMonitor());
		IType unrelatedType = JdtUtils.resolveType("javax.ws.rs.ext.ExceptionMapper", javaProject,
				new NullProgressMonitor());

		Assert.assertNotNull("Parameterized Type not found", parameterizedType);
		IType matchSuperInterfaceType = JdtUtils.resolveType("javax.ws.rs.ext.MessageBodyReader", javaProject,
				new NullProgressMonitor());
		Assert.assertNotNull("Interface Type not found", matchSuperInterfaceType);
		ITypeHierarchy unrelatedTypeHierarchy = JdtUtils.resolveTypeHierarchy(unrelatedType, false,
				new NullProgressMonitor());
		CompilationUnit compilationUnit = JdtUtils.parse(parameterizedType, null);
		Assert.assertNull(JdtUtils.resolveTypeArguments(parameterizedType, compilationUnit, matchSuperInterfaceType,
				unrelatedTypeHierarchy, new NullProgressMonitor()));
	}

	@Test
	public void shouldResolveTopLevelTypeFromSourceType() throws JavaModelException, CoreException {

		IType resourceType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource",
				javaProject, progressMonitor);
		Assert.assertNotNull("ResourceType not found", resourceType);
		Assert.assertNotNull("Type not found", JdtUtils.resolveTopLevelType(resourceType.getCompilationUnit()));
	}

	@Test
	public void shouldNotResolveTopLevelTypeOnBinaryType() throws JavaModelException, CoreException {

		IType resourceType = JdtUtils.resolveType("org.jboss.resteasy.plugins.providers.jaxb.CollectionProvider",
				javaProject, progressMonitor);
		Assert.assertNotNull("ResourceType not found", resourceType);
		Assert.assertNull("Type not found", JdtUtils.resolveTopLevelType(resourceType.getCompilationUnit()));
	}

	@Test
	public void shouldGetTopLevelTypeOKNoneInSourceType() throws JavaModelException, CoreException {
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject, "Empty.txt",
				"org.jboss.tools.ws.jaxrs.sample", "PersistenceExceptionMapper.java", bundle);
		Assert.assertNotNull("Resource not found", compilationUnit);
		Assert.assertNull("Type not expected", JdtUtils.resolveTopLevelType(compilationUnit));
	}

	@Test
	public void shouldResolveTopLevelTypeOnSourceWithMultipleTypes() throws JavaModelException, CoreException {
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject, "Multi.txt",
				"org.jboss.tools.ws.jaxrs.sample", "PersistenceExceptionMapper.java", bundle);
		Assert.assertNotNull("Resource not found", compilationUnit);
		Assert.assertNotNull("Type not found", JdtUtils.resolveTopLevelType(compilationUnit));
	}

	@Test
	public void shouldReturnTrueOnTopLevelTypeDetection() throws JavaModelException, CoreException {

		IType resourceType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource",
				javaProject, progressMonitor);
		Assert.assertNotNull("ResourceType not found", resourceType);
		Assert.assertTrue("Wrong result", JdtUtils.isTopLevelType(resourceType));
	}

	@Test
	public void shouldGetCompiltationUnitFromType() throws CoreException {
		IResource resource = project
				.findMember("src/main/java/org/jboss/tools/ws/jaxrs/sample/services/BookResource.java");
		Assert.assertNotNull("Resource not found", resource);
		Assert.assertNotNull("CompilationUnit not found", JdtUtils.getCompilationUnit(resource));
	}

	@Test
	public void shouldGetCompiltationUnitFromProject() {
		IResource resource = project.findMember("src/main/resources/log4j.xml");
		Assert.assertNotNull("Resource not found", resource);
		Assert.assertNull("CompilationUnit not expected", JdtUtils.getCompilationUnit(resource));
	}

	@Test
	public void shoudNotParseNullMember() throws CoreException {
		Assert.assertNull(JdtUtils.parse((IMember) null, progressMonitor));
	}

	@Test
	public void shoudResolveSourceTypeAnnotationFromName() throws CoreException {
		// pre-conditions
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				progressMonitor);
		Assert.assertNotNull("Type not found", type);
		// operation
		Annotation javaAnnotation = JdtUtils.resolveAnnotation(type, JdtUtils.parse(type, progressMonitor), "Path");
		// verifications
		assertThat(javaAnnotation.getJavaAnnotation(), notNullValue());
		assertThat(javaAnnotation.getName(), equalTo(PATH.qualifiedName));
		assertThat(javaAnnotation.getJavaAnnotationElements().size(), equalTo(1));
		assertThat(javaAnnotation.getJavaAnnotationElements().get("value").get(0), equalTo("/customers"));
		assertThat(javaAnnotation.getRegion(), notNullValue());
	}

	@Test
	public void shoudResolveSourceTypeAnnotationsFromNames() throws CoreException {
		// pre-conditions
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				progressMonitor);
		Assert.assertNotNull("Type not found", type);
		// operation
		Map<String, Annotation> javaAnnotations = JdtUtils.resolveAnnotations(type,
				JdtUtils.parse(type, progressMonitor), PATH.qualifiedName, CONSUMES.qualifiedName,
				PRODUCES.qualifiedName, ENCODED.qualifiedName);
		// verifications
		assertThat(javaAnnotations.size(), equalTo(4));
		for (Entry<String, Annotation> entry : javaAnnotations.entrySet()) {
			assertThat(entry.getKey(), equalTo(entry.getValue().getName()));
			assertThat(entry.getValue().getJavaAnnotation(), notNullValue());
			assertThat(entry.getValue().getRegion(), notNullValue());
		}
	}

	@Test
	public void shoudResolveSourceTypeAnnotationFromElement() throws CoreException {
		// pre-conditions
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				progressMonitor);
		Assert.assertNotNull("Type not found", type);
		IAnnotation annotation = type.getAnnotation("Path");
		Assert.assertNotNull("Annotation not found", annotation);
		// operation
		Annotation javaAnnotation = JdtUtils.resolveAnnotation(annotation, JdtUtils.parse(type, progressMonitor));
		// verifications
		assertThat(javaAnnotation.getJavaAnnotation(), equalTo(annotation));
		assertThat(javaAnnotation.getName(), equalTo(PATH.qualifiedName));
		assertThat(javaAnnotation.getJavaAnnotationElements().size(), equalTo(1));
		assertThat(javaAnnotation.getJavaAnnotationElements().get("value").get(0), equalTo("/customers"));
	}

	@Test
	public void shoudNotResolveUnknownSourceTypeAnnotationFromClassName() throws CoreException {
		// pre-conditions
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				progressMonitor);
		Assert.assertNotNull("Type not found", type);
		// operation
		Annotation javaAnnotation = getAnnotation(type, HTTP_METHOD.qualifiedName);
		// verifications
		assertThat(javaAnnotation, nullValue());
	}

	@Test
	public void shoudResolveBinaryTypeAnnotationFromClassName() throws CoreException {
		// pre-conditions
		IType type = JdtUtils.resolveType(GET.qualifiedName, javaProject, progressMonitor);
		Assert.assertNotNull("Type not found", type);
		// operation
		Annotation javaAnnotation = JdtUtils.resolveAnnotation(type, JdtUtils.parse(type, progressMonitor),
				HTTP_METHOD.qualifiedName);
		// verifications
		assertThat(javaAnnotation.getJavaAnnotation(), notNullValue());
		assertThat(javaAnnotation.getName(), equalTo(HTTP_METHOD.qualifiedName));
		assertThat(javaAnnotation.getJavaAnnotationElements().size(), equalTo(1));
		assertThat(javaAnnotation.getJavaAnnotationElements().get("value").get(0), equalTo("GET"));
	}

	@Test
	public void shoudResolveBinaryTypeAnnotationsFromClassNames() throws CoreException {
		// pre-conditions
		IType type = JdtUtils.resolveType(GET.qualifiedName, javaProject, progressMonitor);
		Assert.assertNotNull("Type not found", type);
		// operation
		Map<String, Annotation> javaAnnotations = JdtUtils.resolveAnnotations(type,
				JdtUtils.parse(type, progressMonitor), HTTP_METHOD.qualifiedName);
		// verifications
		assertThat(javaAnnotations.size(), equalTo(1));
		Annotation javaAnnotation = javaAnnotations.get(HTTP_METHOD.qualifiedName);
		assertThat(javaAnnotation, notNullValue());
		assertThat(javaAnnotation.getJavaAnnotation(), notNullValue());
		assertThat(javaAnnotation.getName(), equalTo(HTTP_METHOD.qualifiedName));
		assertThat(javaAnnotation.getJavaAnnotationElements().size(), equalTo(1));
		assertThat(javaAnnotation.getJavaAnnotationElements().get("value").get(0), equalTo("GET"));
	}

	@Test
	public void shoudResolveBinaryTypeAnnotationFromElement() throws CoreException {
		// pre-conditions
		IType type = JdtUtils.resolveType(GET.qualifiedName, javaProject, progressMonitor);
		Assert.assertNotNull("Type not found", type);
		IAnnotation javaAnnotation = type.getAnnotation(HTTP_METHOD.qualifiedName);
		Assert.assertTrue("Annotation not found", javaAnnotation.exists());
		// operation
		Annotation annotation = JdtUtils.resolveAnnotation(javaAnnotation, JdtUtils.parse(type, progressMonitor));
		// verifications
		assertThat(annotation.getJavaAnnotation(), equalTo(javaAnnotation));
		assertThat(annotation.getName(), equalTo(HTTP_METHOD.qualifiedName));
		assertThat(annotation.getJavaAnnotationElements().size(), equalTo(1));
		assertThat(annotation.getJavaAnnotationElements().get("value").get(0), equalTo("GET"));
	}

	@Test
	public void shoudNotResolveBinaryTypeUnknownAnnotationFromElement() throws CoreException {
		// pre-conditions
		IType type = JdtUtils.resolveType(GET.qualifiedName, javaProject, progressMonitor);
		Assert.assertNotNull("Type not found", type);
		IAnnotation javaAnnotation = type.getAnnotation(PATH.qualifiedName);
		Assert.assertFalse("Annotation not expected", javaAnnotation.exists());
		// operation
		Annotation annotation = getAnnotation(type, PATH.qualifiedName);
		// verifications
		assertThat(annotation, nullValue());
	}

	@Test
	public void shouldResolveJavaMethodSignatures() throws CoreException {
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// operation
		final List<JavaMethodSignature> methodSignatures = JdtUtils.resolveMethodSignatures(type,
				JdtUtils.parse(type, progressMonitor));
		// verification
		Assert.assertEquals(7, methodSignatures.size());
	}

	@Test
	public void shouldResolveJavaMethodSignaturesWithNullAnnotationValue() throws CoreException {
		// pre-condition
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IMethod method = WorkbenchUtils.getMethod(type, "getCustomer");
		method = WorkbenchUtils.replaceFirstOccurrenceOfCode(method, "@PathParam(\"id\") Integer id", "@PathParam() Integer id",
				false);
		// operation
		final JavaMethodSignature methodSignature = JdtUtils.resolveMethodSignature(method,
				JdtUtils.parse(type, progressMonitor));
		// verification
		Assert.assertNotNull(methodSignature);
		Assert.assertEquals(2, methodSignature.getMethodParameters().size());
		Assert.assertNull(methodSignature.getMethodParameters().get(0).getAnnotation(PATH_PARAM.qualifiedName).getValue("value"));
	}
	
	@Test
	public void shouldResolveJavaMethodSignaturesForParameterizedType() throws CoreException {
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.ParameterizedResource");
		// operation
		final List<JavaMethodSignature> methodSignatures = JdtUtils.resolveMethodSignatures(type,
				JdtUtils.parse(type, progressMonitor));
		// verification
		Assert.assertEquals(1, methodSignatures.size());
	}
	
	@Test
	public void shouldResolveJavaMethodSignature() throws CoreException {
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod method = getMethod(type, "getCustomers");
		// operation
		final JavaMethodSignature methodSignature = JdtUtils.resolveMethodSignature(method,
				JdtUtils.parse(type, progressMonitor));
		// verification
		assertThat(methodSignature, notNullValue());
		assertThat(methodSignature.getJavaMethod(), notNullValue());
		assertThat(methodSignature.getMethodParameters().size(), equalTo(3));
		final ISourceRange sourceRange = method.getSourceRange();
		for (JavaMethodParameter parameter : methodSignature.getMethodParameters()) {
			assertThat(parameter.getAnnotations().size(), isOneOf(1, 2));
			for (Annotation annotation : parameter.getAnnotations()) {
				assertThat(annotation.getRegion().getOffset(), greaterThan(sourceRange.getOffset()));
				assertThat(annotation.getRegion().getOffset(),
						lessThan(sourceRange.getOffset() + sourceRange.getLength()));
			}
		}
		assertThat(methodSignature.getReturnedType().getFullyQualifiedName(), endsWith(".List"));
	}

	@Test
	public void shouldConfirmSuperType() throws CoreException {
		final IType bookType = getType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final IType objectType = getType(Object.class.getName());
		assertThat(JdtUtils.isTypeOrSuperType(objectType, bookType), is(true));
	}

	@Test
	public void shouldConfirmSuperTypeWhenSameType() throws CoreException {
		final IType subType = getType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final IType superType = getType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		assertThat(JdtUtils.isTypeOrSuperType(superType, subType), is(true));
	}

	@Test
	public void shouldNotConfirmSuperType() throws CoreException {
		final IType bookType = getType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final IType objectType = getType(RESPONSE.qualifiedName);
		assertThat(JdtUtils.isTypeOrSuperType(objectType, bookType), is(false));
	}

}
