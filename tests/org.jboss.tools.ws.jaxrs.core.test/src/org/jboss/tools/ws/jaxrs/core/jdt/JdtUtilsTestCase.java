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
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.resolveAnnotation;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.CONSUMES;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.ENCODED;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.GET;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.HTTP_METHOD;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PATH;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PATH_PARAM;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PRODUCES;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.QUERY_PARAM;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.RESPONSE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
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

	private IField getField(IType parentType, String fieldName) throws CoreException {
		return WorkbenchUtils.getField(parentType, fieldName);
	}

	private IMethod getMethod(IType parentType, String methodName) throws JavaModelException {
		return WorkbenchUtils.getMethod(parentType, methodName);
	}

	private ILocalVariable getLocalVariable(IMethod method, String variableName) throws JavaModelException {
		return WorkbenchUtils.getLocalVariable(method, variableName);
	}

	@Test
	public void shouldResolveTypeByQNameInSourceCode() throws CoreException {
		// preconditions
		// operation
		final IType type = JdtUtils.resolveType(
				"org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper", javaProject,
				new NullProgressMonitor());
		// verification
		Assert.assertNotNull("Type not found", type);
	}

	@Test
	public void shouldResolveTypeByQNameInLibrary() throws CoreException {
		// preconditions
		// operation
		final IType type = JdtUtils.resolveType("javax.persistence.PersistenceException", javaProject,
				new NullProgressMonitor());
		// verification
		Assert.assertNotNull("Type not found", type);
	}

	@Test
	public void shouldNotResolveTypeByUnknownQName() throws CoreException {
		// preconditions
		// operation
		final IType type = JdtUtils.resolveType("unknown.class", javaProject, new NullProgressMonitor());
		// verification
		Assert.assertNull("No Type expected", type);
	}

	@Test
	public void shouldResolveTypeByVeryQName() throws CoreException {
		// preconditions
		// operation
		final IType type = JdtUtils.resolveType(
				"org.jboss.tools.ws.jaxrs.sample.extra.TestQualifiedException.TestException", javaProject,
				new NullProgressMonitor());
		// verification
		Assert.assertNotNull("Type not found", type);
	}

	@Test
	public void shouldAssertTypeIsAbstract() throws CoreException {
		// preconditions
		// operation
		IType type = JdtUtils.resolveType("org.jboss.resteasy.plugins.providers.AbstractEntityProvider", javaProject,
				new NullProgressMonitor());
		// verification
		Assert.assertNotNull("Type not found", type);
		Assert.assertTrue("Type is abstract", JdtUtils.isAbstractType(type));
	}

	@Test
	public void shouldNotAssertTypeIsAbstract() throws CoreException {
		// preconditions
		// operation
		IType type = JdtUtils.resolveType(
				"org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper", javaProject,
				new NullProgressMonitor());
		// verification
		Assert.assertNotNull("Type not found", type);
		Assert.assertFalse("Type is not abstract", JdtUtils.isAbstractType(type));
	}

	@Test
	public void shouldNotResolveTypeWithNullQName() throws CoreException {
		// preconditions
		// operation
		IType type = JdtUtils.resolveType(null, javaProject, new NullProgressMonitor());
		// verification
		Assert.assertNull("No type was expected", type);
	}

	@Test
	public void shouldResolveTypeHierarchyOnClass() throws CoreException {
		// preconditions
		// operation
		IType type = JdtUtils.resolveType(
				"org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper", javaProject,
				new NullProgressMonitor());
		// verification
		Assert.assertNotNull("Type not found", type);
		Assert.assertNotNull("Type hierarchy not found",
				JdtUtils.resolveTypeHierarchy(type, type.getJavaProject(), false, new NullProgressMonitor()));
	}

	@Test
	public void shouldResolveTypeHierarchyOnInterface() throws CoreException {
		// preconditions
		// operation
		IType type = JdtUtils.resolveType("javax.ws.rs.ext.MessageBodyReader", javaProject, new NullProgressMonitor());
		// verification
		Assert.assertNotNull("Type not found", type);
		Assert.assertNotNull("Type hierarchy not found",
				JdtUtils.resolveTypeHierarchy(type, type.getJavaProject(), false, new NullProgressMonitor()));
	}

	@Test
	public void shouldResolveTypeHierarchyOnLibrariesWithSubclasses() throws CoreException {
		// preconditions
		IType type = JdtUtils.resolveType("javax.ws.rs.core.Application", javaProject, new NullProgressMonitor());
		Assert.assertNotNull("Type not found", type);
		// operation
		final ITypeHierarchy hierarchy = JdtUtils.resolveTypeHierarchy(type, type.getJavaProject(), true,
				new NullProgressMonitor());
		// verifications
		Assert.assertNotNull("Type hierarchy not found", hierarchy);
		Assert.assertEquals("Type hierarchy incomplete", 1, hierarchy.getSubtypes(type).length);
	}

	@Test
	public void shouldResolveTypeHierarchyOnLibrariesWithNoSubclass() throws CoreException {
		// preconditions
		IType type = JdtUtils.resolveType("javax.ws.rs.core.Application", javaProject, new NullProgressMonitor());
		Assert.assertNotNull("Type not found", type);
		final IPackageFragmentRoot lib = WorkbenchUtils.getPackageFragmentRoot(javaProject,
				"lib/jaxrs-api-2.0.1.GA.jar", new NullProgressMonitor());
		Assert.assertNotNull("Lib not found", lib);
		// operation
		final ITypeHierarchy hierarchy = JdtUtils.resolveTypeHierarchy(type, lib, true, new NullProgressMonitor());
		// verifications
		Assert.assertNotNull("Type hierarchy not found", hierarchy);
		Assert.assertEquals("Type hierarchy incomplete", 0, hierarchy.getSubtypes(type).length);
	}

	@Test
	public void shouldNotResolveTypeHierarchyOnRemovedClass() throws CoreException {
		// preconditions
		IType type = JdtUtils.resolveType(
				"org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper", javaProject,
				new NullProgressMonitor());
		Assert.assertNotNull("Type not found", type);
		// operation
		type.delete(true, new NullProgressMonitor());
		final ITypeHierarchy hierarchy = JdtUtils.resolveTypeHierarchy(type, type.getJavaProject(), false,
				new NullProgressMonitor());
		// verification
		Assert.assertNull("No Type hierarchy expected", hierarchy);
	}

	@Test
	public void shouldResolveConcreteTypeArgumentsOnBinaryTypesWithoutSources() throws CoreException,
			OperationCanceledException, InterruptedException {
		// preconditions
		IType parameterizedType = JdtUtils.resolveType("org.jboss.resteasy.plugins.providers.jaxb.CollectionProvider",
				javaProject, progressMonitor);
		Assert.assertNotNull("Parameterized Type not found", parameterizedType);
		IType matchSuperInterfaceType = JdtUtils.resolveType("javax.ws.rs.ext.MessageBodyReader", javaProject,
				progressMonitor);
		Assert.assertNotNull("Interface Type not found", matchSuperInterfaceType);
		ITypeHierarchy parameterizedTypeHierarchy = JdtUtils.resolveTypeHierarchy(parameterizedType,
				parameterizedType.getJavaProject(), false, progressMonitor);
		boolean removedReferencedLibrarySourceAttachment = WorkbenchUtils.removeReferencedLibrarySourceAttachment(
				javaProject, "resteasy-jaxb-provider-2.0.1.GA.jar", progressMonitor);
		Assert.assertTrue("Source attachment was not removed (not found?)", removedReferencedLibrarySourceAttachment);
		// operation
		CompilationUnit compilationUnit = JdtUtils.parse(parameterizedType, null);
		List<IType> resolvedTypeParameters = JdtUtils.resolveTypeArguments(parameterizedType, compilationUnit,
				matchSuperInterfaceType, parameterizedTypeHierarchy, progressMonitor);
		// verification
		Assert.assertNull("No type parameters expected", resolvedTypeParameters);
	}

	@Test
	public void shouldNotResolveTypeArgumentsOnBinaryImplementation() throws CoreException {
		// preconditions
		IType parameterizedType = JdtUtils.resolveType(
				"org.jboss.resteasy.plugins.providers.jaxb.AbstractJAXBProvider", javaProject,
				new NullProgressMonitor());
		Assert.assertNotNull("Parameterized Type not found", parameterizedType);
		IType matchGenericType = JdtUtils.resolveType("org.jboss.resteasy.plugins.providers.AbstractEntityProvider",
				javaProject, new NullProgressMonitor());
		Assert.assertNotNull("Interface Type not found", matchGenericType);
		// operation
		ITypeHierarchy parameterizedTypeHierarchy = JdtUtils.resolveTypeHierarchy(parameterizedType,
				parameterizedType.getJavaProject(), false, new NullProgressMonitor());
		CompilationUnit compilationUnit = JdtUtils.parse(parameterizedType, null);
		List<IType> resolvedTypeParameters = JdtUtils.resolveTypeArguments(parameterizedType, compilationUnit,
				matchGenericType, parameterizedTypeHierarchy, new NullProgressMonitor());
		// verification
		Assert.assertNull("No type parameters expected", resolvedTypeParameters);
	}

	@Test
	public void shouldResolveMultipleConcreteTypeArgumentsOnSourceImplementation() throws CoreException {
		// preconditions
		IType parameterizedType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.extra.AnotherDummyProvider",
				javaProject, new NullProgressMonitor());
		Assert.assertNotNull("Parameterized Type not found", parameterizedType);

		// MessageBodyReader
		IType matchGenericType = JdtUtils.resolveType("javax.ws.rs.ext.MessageBodyReader", javaProject,
				new NullProgressMonitor());
		Assert.assertNotNull("Interface Type not found", matchGenericType);
		CompilationUnit compilationUnit = JdtUtils.parse(parameterizedType, null);
		ITypeHierarchy parameterizedTypeHierarchy = JdtUtils.resolveTypeHierarchy(parameterizedType,
				parameterizedType.getJavaProject(), false, new NullProgressMonitor());

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
		// preconditions
		IType parameterizedType = JdtUtils.resolveType("org.jboss.resteasy.plugins.providers.jaxb.CollectionProvider",
				javaProject, new NullProgressMonitor());
		IType unrelatedType = JdtUtils.resolveType("javax.ws.rs.ext.ExceptionMapper", javaProject,
				new NullProgressMonitor());
		Assert.assertNotNull("Parameterized Type not found", parameterizedType);
		// operation
		IType matchSuperInterfaceType = JdtUtils.resolveType("javax.ws.rs.ext.MessageBodyReader", javaProject,
				new NullProgressMonitor());
		// verification
		Assert.assertNotNull("Interface Type not found", matchSuperInterfaceType);
		// operation
		ITypeHierarchy unrelatedTypeHierarchy = JdtUtils.resolveTypeHierarchy(unrelatedType,
				unrelatedType.getJavaProject(), false, new NullProgressMonitor());
		CompilationUnit compilationUnit = JdtUtils.parse(parameterizedType, null);
		final List<IType> typeArgs = JdtUtils.resolveTypeArguments(parameterizedType, compilationUnit,
				matchSuperInterfaceType, unrelatedTypeHierarchy, new NullProgressMonitor());
		// verification
		Assert.assertNull(typeArgs);
	}

	@Test
	public void shouldResolveTopLevelTypeFromSourceType() throws JavaModelException, CoreException {
		// preconditions
		IType resourceType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource",
				javaProject, progressMonitor);
		Assert.assertNotNull("ResourceType not found", resourceType);
		// operation
		final IType topLevelType = JdtUtils.resolveTopLevelType(resourceType.getCompilationUnit());
		// verification
		Assert.assertNotNull("Type not found", topLevelType);
	}

	@Test
	public void shouldNotResolveTopLevelTypeOnBinaryType() throws JavaModelException, CoreException {
		// preconditions
		IType resourceType = JdtUtils.resolveType("org.jboss.resteasy.plugins.providers.jaxb.CollectionProvider",
				javaProject, progressMonitor);
		Assert.assertNotNull("ResourceType not found", resourceType);
		// operation
		final IType topLevelType = JdtUtils.resolveTopLevelType(resourceType.getCompilationUnit());
		// verification
		Assert.assertNull("Type not found", topLevelType);
	}

	@Test
	public void shouldGetTopLevelTypeOKNoneInSourceType() throws JavaModelException, CoreException {
		// preconditions
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject, "Empty.txt",
				"org.jboss.tools.ws.jaxrs.sample", "PersistenceExceptionMapper.java", bundle);
		Assert.assertNotNull("Resource not found", compilationUnit);
		// operation
		final IType topLevelType = JdtUtils.resolveTopLevelType(compilationUnit);
		// verification
		Assert.assertNull("Type not expected", topLevelType);
	}

	@Test
	public void shouldResolveTopLevelTypeOnSourceWithMultipleTypes() throws JavaModelException, CoreException {
		// preconditions
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject, "Multi.txt",
				"org.jboss.tools.ws.jaxrs.sample", "PersistenceExceptionMapper.java", bundle);
		Assert.assertNotNull("Resource not found", compilationUnit);
		// operation
		final IType topLevelType = JdtUtils.resolveTopLevelType(compilationUnit);
		// verification
		Assert.assertNotNull("Type not found", topLevelType);
	}

	@Test
	public void shouldReturnTrueOnTopLevelTypeDetection() throws JavaModelException, CoreException {
		// preconditions

		IType resourceType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource",
				javaProject, progressMonitor);
		Assert.assertNotNull("ResourceType not found", resourceType);
		// operation
		final boolean isTopLevelType = JdtUtils.isTopLevelType(resourceType);
		// verification
		Assert.assertTrue("Wrong result", isTopLevelType);
	}

	@Test
	public void shouldGetCompiltationUnitFromType() throws CoreException {
		// preconditions
		IResource resource = project
				.findMember("src/main/java/org/jboss/tools/ws/jaxrs/sample/services/BookResource.java");
		Assert.assertNotNull("Resource not found", resource);
		// operation
		final ICompilationUnit compilationUnit = JdtUtils.getCompilationUnit(resource);
		// verification
		Assert.assertNotNull("CompilationUnit not found", compilationUnit);
	}

	@Test
	public void shouldGetCompiltationUnitFromProject() {
		// preconditions
		IResource resource = project.findMember("src/main/resources/log4j.xml");
		Assert.assertNotNull("Resource not found", resource);
		// operation
		final ICompilationUnit compilationUnit = JdtUtils.getCompilationUnit(resource);
		// verification
		Assert.assertNull("CompilationUnit not expected", compilationUnit);
	}

	@Test
	public void shoudNotParseNullMember() throws CoreException {
		// preconditions
		// operation
		final CompilationUnit compilationUnit = JdtUtils.parse((IMember) null, progressMonitor);
		// verifications
		Assert.assertNull(compilationUnit);
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
		assertThat(javaAnnotation.getFullyQualifiedName(), equalTo(PATH.qualifiedName));
		assertThat(javaAnnotation.getJavaAnnotationElements().size(), equalTo(1));
		assertThat(javaAnnotation.getJavaAnnotationElements().get("value").get(0), equalTo("/customers"));
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
			assertThat(entry.getKey(), equalTo(entry.getValue().getFullyQualifiedName()));
			assertThat(entry.getValue().getJavaAnnotation(), notNullValue());
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
		assertThat(javaAnnotation.getFullyQualifiedName(), equalTo(PATH.qualifiedName));
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
		Annotation annotation = resolveAnnotation(type, HTTP_METHOD.qualifiedName);
		// verifications
		assertThat(annotation, nullValue());
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
		assertThat(javaAnnotation.getFullyQualifiedName(), equalTo(HTTP_METHOD.qualifiedName));
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
		assertThat(javaAnnotation.getFullyQualifiedName(), equalTo(HTTP_METHOD.qualifiedName));
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
		assertThat(annotation.getFullyQualifiedName(), equalTo(HTTP_METHOD.qualifiedName));
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
		Annotation annotation = resolveAnnotation(type, PATH.qualifiedName);
		// verifications
		assertThat(annotation, nullValue());
	}

	@Test
	public void shouldResolveJavaMethodSignatures() throws CoreException {
		// preconditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// operation
		final List<JavaMethodSignature> methodSignatures = JdtUtils.resolveMethodSignatures(type,
				JdtUtils.parse(type, progressMonitor));
		// verification
		Assert.assertEquals(7, methodSignatures.size());
		for (JavaMethodSignature methodSignature : methodSignatures) {
			for (Entry<String, JavaMethodParameter> methodParameterEntry : methodSignature.getMethodParameters()
					.entrySet()) {
				for (Entry<String, Annotation> annotationEntry : methodParameterEntry.getValue().getAnnotations()
						.entrySet()) {
					assertNotNull(
							"JavaAnnotation for " + methodParameterEntry.getKey() + "." + annotationEntry.getKey()
									+ " is null", annotationEntry.getValue().getJavaAnnotation());
				}
			}
		}
	}

	@Test
	public void shouldResolveJavaMethodSignaturesWithNullAnnotationValue() throws CoreException {
		// pre-condition
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IMethod method = WorkbenchUtils.getMethod(type, "getCustomer");
		method = WorkbenchUtils.replaceFirstOccurrenceOfCode(method, "@PathParam(\"id\") Integer id",
				"@PathParam() Integer id", false);
		// operation
		final JavaMethodSignature methodSignature = JdtUtils.resolveMethodSignature(method,
				JdtUtils.parse(type, progressMonitor));
		// verification
		assertNotNull(methodSignature);
		assertEquals(2, methodSignature.getMethodParameters().size());
		for (Entry<String, JavaMethodParameter> methodParameterEntry : methodSignature.getMethodParameters().entrySet()) {
			for (Entry<String, Annotation> annotationEntry : methodParameterEntry.getValue().getAnnotations()
					.entrySet()) {
				assertNotNull("JavaAnnotation for " + methodParameterEntry.getKey() + "." + annotationEntry.getKey()
						+ " is null", annotationEntry.getValue().getJavaAnnotation());
			}
		}
		assertNull(methodSignature.getMethodParameters().get("id").getAnnotation(PATH_PARAM.qualifiedName)
				.getValue("value"));
	}

	@Test
	public void shouldResolveJavaMethodSignaturesForParameterizedType() throws CoreException {
		// preconditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.ParameterizedResource");
		// operation
		final List<JavaMethodSignature> methodSignatures = JdtUtils.resolveMethodSignatures(type,
				JdtUtils.parse(type, progressMonitor));
		// verification
		Assert.assertEquals(1, methodSignatures.size());
	}

	@Test
	public void shouldResolveJavaMethodSignature() throws CoreException {
		// preconditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod method = getMethod(type, "getCustomers");
		// operation
		final JavaMethodSignature methodSignature = JdtUtils.resolveMethodSignature(method,
				JdtUtils.parse(type, progressMonitor));
		// verification
		assertThat(methodSignature, notNullValue());
		assertThat(methodSignature.getJavaMethod(), notNullValue());
		assertThat(methodSignature.getMethodParameters().size(), equalTo(3));
		for (Entry<String, JavaMethodParameter> parameterEntry : methodSignature.getMethodParameters().entrySet()) {
			JavaMethodParameter parameter = parameterEntry.getValue();
			assertThat(parameter.getAnnotations().size(), isOneOf(1, 2));
			for (Annotation annotation : parameter.getAnnotations().values()) {
				assertThat(annotation.getJavaAnnotation(), notNullValue());
			}
		}
		assertThat(methodSignature.getReturnedType().getFullyQualifiedName(), endsWith(".List"));
	}

	@Test
	public void shouldConfirmSuperType() throws CoreException {
		// preconditions
		final IType bookType = getType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final IType objectType = getType(Object.class.getName());
		// operation
		final boolean typeOrSuperType = JdtUtils.isTypeOrSuperType(objectType, bookType);
		// verification
		assertThat(typeOrSuperType, is(true));
	}

	@Test
	public void shouldConfirmSuperTypeWhenSameType() throws CoreException {
		// preconditions
		final IType subType = getType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final IType superType = getType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		// operation
		final boolean typeOrSuperType = JdtUtils.isTypeOrSuperType(superType, subType);
		// verification
		assertThat(typeOrSuperType, is(true));
	}

	@Test
	public void shouldNotConfirmSuperType() throws CoreException {
		// preconditions
		final IType bookType = getType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final IType objectType = getType(RESPONSE.qualifiedName);
		// operation
		final boolean typeOrSuperType = JdtUtils.isTypeOrSuperType(objectType, bookType);
		// verification
		assertThat(typeOrSuperType, is(false));
	}

	@Test
	public void shouldRetrieveTypeAnnotationFromNameLocation() throws CoreException {
		// preconditions
		final IType customerType = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final Annotation annotation = resolveAnnotation(customerType, PATH.qualifiedName);
		final int offset = annotation.getJavaAnnotation().getNameRange().getOffset();
		// operation
		final Annotation foundAnnotation = JdtUtils.resolveAnnotationAt(offset, customerType.getCompilationUnit());
		// verification
		assertThat(foundAnnotation, notNullValue());
		assertThat(foundAnnotation.getJavaAnnotation(), notNullValue());
	}

	@Test
	public void shouldRetrieveTypeAnnotationFromMemberPairLocation() throws CoreException {
		// preconditions
		final IType customerType = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final Annotation annotation = resolveAnnotation(customerType, PATH.qualifiedName);
		final int offset = annotation.getJavaAnnotation().getSourceRange().getOffset() + PATH.qualifiedName.length()
				+ 3;
		// operation
		final Annotation foundAnnotation = JdtUtils.resolveAnnotationAt(offset, customerType.getCompilationUnit());
		// verification
		assertThat(foundAnnotation, notNullValue());
		assertThat(foundAnnotation.getJavaAnnotation(), notNullValue());
	}

	@Test
	public void shouldRetrieveAnnotationTypeAnnotationFromNameLocation() throws CoreException {
		// preconditions
		final IType customerType = getType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final Annotation annotation = resolveAnnotation(customerType, HTTP_METHOD.qualifiedName);
		final int offset = annotation.getJavaAnnotation().getNameRange().getOffset();
		// operation
		final Annotation foundAnnotation = JdtUtils.resolveAnnotationAt(offset, customerType.getCompilationUnit());
		// verification
		assertThat(foundAnnotation, notNullValue());
		assertThat(foundAnnotation.getJavaAnnotation(), notNullValue());
	}

	@Test
	public void shouldRetrieveAnnotationTypeAnnotationFromMemberPairLocation() throws CoreException {
		// preconditions
		final IType customerType = getType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final Annotation annotation = resolveAnnotation(customerType, HTTP_METHOD.qualifiedName);
		final int offset = annotation.getJavaAnnotation().getSourceRange().getOffset()
				+ HTTP_METHOD.qualifiedName.length() + 3;
		// operation
		final Annotation foundAnnotation = JdtUtils.resolveAnnotationAt(offset, customerType.getCompilationUnit());
		// verification
		assertThat(foundAnnotation, notNullValue());
		assertThat(foundAnnotation.getJavaAnnotation(), notNullValue());
	}

	@Test
	public void shouldRetrieveMethodAnnotationFromNameLocation() throws CoreException {
		// preconditions
		final IType customerType = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod method = getMethod(customerType, "createCustomer");
		final Annotation annotation = resolveAnnotation(method, CONSUMES.qualifiedName);
		final int offset = annotation.getJavaAnnotation().getNameRange().getOffset();
		// operation
		final Annotation foundAnnotation = JdtUtils.resolveAnnotationAt(offset, customerType.getCompilationUnit());
		// verification
		assertThat(foundAnnotation, notNullValue());
		assertThat(foundAnnotation.getJavaAnnotation(), notNullValue());
	}

	@Test
	public void shouldRetrieveMethodAnnotationFromMemberPairLocation() throws CoreException {
		// preconditions
		final IType customerType = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod method = getMethod(customerType, "createCustomer");
		final Annotation annotation = resolveAnnotation(method, CONSUMES.qualifiedName);
		final int offset = annotation.getJavaAnnotation().getSourceRange().getOffset()
				+ CONSUMES.qualifiedName.length() + 3;
		// operation
		final Annotation foundAnnotation = JdtUtils.resolveAnnotationAt(offset, method.getCompilationUnit());
		// verification
		assertThat(foundAnnotation, notNullValue());
		assertThat(foundAnnotation.getJavaAnnotation(), notNullValue());
	}

	@Test
	public void shouldRetrieveLocalVariableAnnotationFromNameLocation() throws CoreException {
		// preconditions
		final IType customerType = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod method = getMethod(customerType, "getCustomer");
		final ILocalVariable localVariable = getLocalVariable(method, "id");
		final int offset = localVariable.getAnnotations()[0].getNameRange().getOffset();
		// operation
		final Annotation foundAnnotation = JdtUtils.resolveAnnotationAt(offset, method.getCompilationUnit());
		// verification
		assertThat(foundAnnotation, notNullValue());
		assertThat(foundAnnotation.getJavaAnnotation(), notNullValue());
	}

	@Test
	public void shouldRetrieveLocalVariableAnnotationFromMemberPairLocation() throws CoreException {
		// preconditions
		final IType customerType = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod method = getMethod(customerType, "getCustomer");
		final ILocalVariable localVariable = getLocalVariable(method, "id");
		final int offset = localVariable.getAnnotations()[0].getNameRange().getOffset()
				+ PATH_PARAM.simpleName.length() + 3;
		// operation
		final Annotation foundAnnotation = JdtUtils.resolveAnnotationAt(offset, customerType.getCompilationUnit());
		// verification
		assertThat(foundAnnotation, notNullValue());
		assertThat(foundAnnotation.getJavaAnnotation(), notNullValue());
	}

	@Test
	public void shouldRetrieveFieldAnnotationFromNameLocation() throws CoreException {
		// preconditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final IField field = getField(type, "foo");
		final Annotation annotation = resolveAnnotation(field, QUERY_PARAM.qualifiedName);
		final int offset = annotation.getJavaAnnotation().getNameRange().getOffset();
		// operation
		final Annotation foundAnnotation = JdtUtils.resolveAnnotationAt(offset, type.getCompilationUnit());
		// verification
		assertThat(foundAnnotation, notNullValue());
		assertThat(foundAnnotation.getJavaAnnotation(), notNullValue());
	}

	@Test
	public void shouldRetrieveFieldAnnotationFromMemberPairLocation() throws CoreException {
		// preconditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final IField field = getField(type, "foo");
		final Annotation annotation = resolveAnnotation(field, QUERY_PARAM.qualifiedName);
		final int offset = annotation.getJavaAnnotation().getSourceRange().getOffset() + CONSUMES.simpleName.length()
				+ 3;
		// operation
		final Annotation foundAnnotation = JdtUtils.resolveAnnotationAt(offset, type.getCompilationUnit());
		// verification
		assertThat(foundAnnotation, notNullValue());
		assertThat(foundAnnotation.getJavaAnnotation(), notNullValue());
	}

	@Test
	public void shouldNotRetrieveAnnotationFromTypeNameLocation() throws CoreException {
		// preconditions
		final IType customerType = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final int offset = customerType.getNameRange().getOffset();
		// operation
		final Annotation foundAnnotation = JdtUtils.resolveAnnotationAt(offset, customerType.getCompilationUnit());
		// verification
		assertThat(foundAnnotation, nullValue());
	}

	@Test
	public void shouldNotRetrieveAnnotationFromMethodNameLocation() throws CoreException {
		// preconditions
		final IType customerType = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod method = getMethod(customerType, "getCustomer");
		final int offset = method.getNameRange().getOffset();
		// operation
		final Annotation foundAnnotation = JdtUtils.resolveAnnotationAt(offset, customerType.getCompilationUnit());
		// verification
		assertThat(foundAnnotation, nullValue());
	}

	@Test
	public void shouldNotRetrieveAnnotationFromMethodBodyLocation() throws CoreException {
		// preconditions
		final IType customerType = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod method = getMethod(customerType, "getCustomer");
		final int offset = method.getSourceRange().getOffset() + method.getSourceRange().getLength() - 2;
		// operation
		final Annotation foundAnnotation = JdtUtils.resolveAnnotationAt(offset, customerType.getCompilationUnit());
		// verification
		assertThat(foundAnnotation, nullValue());
	}

	@Test
	public void shouldNotRetrieveAnnotationFromFieldNameLocation() throws CoreException {
		// preconditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final IField field = getField(type, "foo");
		final int offset = field.getSourceRange().getOffset();
		// operation
		final Annotation foundAnnotation = JdtUtils.resolveAnnotationAt(offset, type.getCompilationUnit());
		// verification
		assertThat(foundAnnotation, nullValue());
	}

	@Test
	public void shouldRetrieveTypeAnnotationMemberValuePairSourceRange() throws CoreException {
		// preconditions
		final IType customerType = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final Annotation annotation = resolveAnnotation(customerType, PATH.qualifiedName);
		// operation
		final ISourceRange range = JdtUtils.resolveMemberPairValueRange(annotation.getJavaAnnotation(),
				annotation.getFullyQualifiedName(), "value");
		// verification
		assertThat(range, notNullValue());
		final ISourceRange annotationRange = annotation.getJavaAnnotation().getSourceRange();
		assertThat(range.getOffset(), greaterThan(annotationRange.getOffset()));
		assertThat(range.getOffset(), lessThan(annotationRange.getOffset() + annotationRange.getLength()));

	}

	@Test
	public void shouldRetrieveTypeSingleMemberAnnotationMemberValuePairSourceRange() throws CoreException {
		// preconditions
		final IType customerType = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final Annotation annotation = resolveAnnotation(customerType, CONSUMES.qualifiedName);
		// operation
		final ISourceRange range = JdtUtils.resolveMemberPairValueRange(annotation.getJavaAnnotation(),
				annotation.getFullyQualifiedName(), "value");
		// verification
		assertThat(range, notNullValue());
		final ISourceRange annotationRange = annotation.getJavaAnnotation().getSourceRange();
		assertThat(range.getOffset(), greaterThan(annotationRange.getOffset()));
		assertThat(range.getOffset(), lessThan(annotationRange.getOffset() + annotationRange.getLength()));

	}

	@Test
	public void shouldRetrieveMethodAnnotationMemberValuePairSourceRange() throws CoreException {
		// preconditions
		final IType customerType = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod method = getMethod(customerType, "getCustomer");
		final Annotation annotation = resolveAnnotation(method, PATH.qualifiedName);
		// operation
		final ISourceRange range = JdtUtils.resolveMemberPairValueRange(annotation.getJavaAnnotation(),
				annotation.getFullyQualifiedName(), "value");
		// verification
		assertThat(range, notNullValue());
		final ISourceRange annotationRange = annotation.getJavaAnnotation().getSourceRange();
		assertThat(range.getOffset(), greaterThan(annotationRange.getOffset()));
		assertThat(range.getOffset(), lessThan(annotationRange.getOffset() + annotationRange.getLength()));
	}

	@Test
	public void shouldRetrieveFieldAnnotationMemberValuePairSourceRange() throws CoreException {
		// preconditions
		final IType type = getType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final IField field = getField(type, "foo");
		final Annotation annotation = resolveAnnotation(field, QUERY_PARAM.qualifiedName);
		// operation
		final ISourceRange range = JdtUtils.resolveMemberPairValueRange(annotation.getJavaAnnotation(),
				annotation.getFullyQualifiedName(), "value");
		// verification
		assertThat(range, notNullValue());
		final ISourceRange annotationRange = annotation.getJavaAnnotation().getSourceRange();
		assertThat(range.getOffset(), greaterThan(annotationRange.getOffset()));
		assertThat(range.getOffset(), lessThan(annotationRange.getOffset() + annotationRange.getLength()));
	}

	@Test
	public void shouldRetrieveLocalVariableAnnotationMemberValuePairSourceRange() throws CoreException {
		// preconditions
		final IType customerType = getType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod method = getMethod(customerType, "getCustomer");
		final ILocalVariable localVariable = getLocalVariable(method, "id");
		final IAnnotation annotation = localVariable.getAnnotations()[0];
		// operation
		final ISourceRange range = JdtUtils.resolveMemberPairValueRange(annotation, PATH_PARAM.qualifiedName,
				"value");
		// verification
		assertThat(range, notNullValue());
		final ISourceRange annotationRange = annotation.getSourceRange();
		assertThat(range.getOffset(), greaterThan(annotationRange.getOffset()));
		assertThat(range.getOffset(), lessThan(annotationRange.getOffset() + annotationRange.getLength()));
	}
	
	@Test
	public void shouldReturnNullWhenRetrievingElementAtLocationOnNullCompilationUnit() throws JavaModelException {
		// preconditions
		// operation
		IJavaElement element = JdtUtils.getElementAt(null, 0);
		// verification
		assertThat(element, nullValue());
	}

}
