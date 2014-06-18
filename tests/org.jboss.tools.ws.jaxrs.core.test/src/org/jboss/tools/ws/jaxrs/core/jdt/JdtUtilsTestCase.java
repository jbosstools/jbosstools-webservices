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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.getAnnotation;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.getField;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.getLocalVariable;
import static org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils.replaceFirstOccurrenceOfCode;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.CONSUMES;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.GET;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.HTTP_METHOD;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PATH;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PATH_PARAM;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.QUERY_PARAM;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.RESPONSE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JavaMethodSignature;
import org.jboss.tools.ws.jaxrs.core.internal.utils.TestLogger;
import org.jboss.tools.ws.jaxrs.core.junitrules.TestProjectMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.TestWatcher;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJavaMethodParameter;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJavaMethodSignature;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

public class JdtUtilsTestCase {

	private final IProgressMonitor progressMonitor = new NullProgressMonitor();

	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule("org.jboss.tools.ws.jaxrs.tests.sampleproject");
	
	@Rule
	public TestWatcher testWatcher = new TestWatcher();
	
	@Rule
	public TestProjectMonitor projectMonitor = new TestProjectMonitor("org.jboss.tools.ws.jaxrs.tests.sampleproject");

	private IJavaProject javaProject = null;
	
	@Before
	public void setup() {
		javaProject = projectMonitor.getJavaProject();
	}
	

	@Test
	public void shouldResolveTypeByQNameInSourceCode() throws CoreException {
		// preconditions
		// operation
		final IType type = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		// verification
		Assert.assertNotNull("SourceType not found", type);
	}

	@Test
	public void shouldResolveTypeByQNameInLibrary() throws CoreException {
		// preconditions
		// operation
		final IType type = projectMonitor.resolveType("javax.persistence.PersistenceException");
		// verification
		Assert.assertNotNull("SourceType not found", type);
	}

	@Test
	public void shouldNotResolveTypeByUnknownQName() throws CoreException {
		// preconditions
		// operation
		final IType type = projectMonitor.resolveType("unknown.class");
		// verification
		Assert.assertNull("No SourceType expected", type);
	}

	@Test
	public void shouldResolveTypeByVeryQName() throws CoreException {
		// preconditions
		// operation
		final IType type = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.extra.TestQualifiedException.TestException");
		// verification
		Assert.assertNotNull("SourceType not found", type);
	}

	@Test
	public void shouldAssertTypeIsAbstract() throws CoreException {
		// preconditions
		// operation
		IType type = projectMonitor.resolveType("org.jboss.resteasy.plugins.providers.AbstractEntityProvider");
		// verification
		Assert.assertNotNull("SourceType not found", type);
		Assert.assertTrue("SourceType is abstract", JdtUtils.isAbstractType(type));
	}

	@Test
	public void shouldNotAssertTypeIsAbstract() throws CoreException {
		// preconditions
		// operation
		IType type = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		// verification
		Assert.assertNotNull("SourceType not found", type);
		Assert.assertFalse("SourceType is not abstract", JdtUtils.isAbstractType(type));
	}

	@Test
	public void shouldNotResolveTypeWithNullQName() throws CoreException {
		// preconditions
		// operation
		IType type = projectMonitor.resolveType(null);
		// verification
		Assert.assertNull("No type was expected", type);
	}

	@Test
	public void shouldResolveTypeHierarchyOnClass() throws CoreException {
		// preconditions
		// operation
		IType type = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		// verification
		Assert.assertNotNull("SourceType not found", type);
		Assert.assertNotNull("SourceType hierarchy not found",
				JdtUtils.resolveTypeHierarchy(type, type.getJavaProject(), false, new NullProgressMonitor()));
	}

	@Test
	public void shouldResolveTypeHierarchyOnInterface() throws CoreException {
		// preconditions
		// operation
		IType type = projectMonitor.resolveType("javax.ws.rs.ext.MessageBodyReader");
		// verification
		Assert.assertNotNull("SourceType not found", type);
		Assert.assertNotNull("SourceType hierarchy not found",
				JdtUtils.resolveTypeHierarchy(type, type.getJavaProject(), false, new NullProgressMonitor()));
	}

	@Test
	public void shouldResolveTypeHierarchyOnLibrariesWithSubclasses() throws CoreException {
		// preconditions
		IType type = projectMonitor.resolveType("javax.ws.rs.core.Application");
		Assert.assertNotNull("SourceType not found", type);
		// operation
		final ITypeHierarchy hierarchy = JdtUtils.resolveTypeHierarchy(type, type.getJavaProject(), true,
				new NullProgressMonitor());
		// verifications
		Assert.assertNotNull("SourceType hierarchy not found", hierarchy);
		Assert.assertEquals("SourceType hierarchy incomplete", 1, hierarchy.getSubtypes(type).length);
	}

	@Test
	public void shouldResolveTypeHierarchyOnLibrariesWithNoSubclass() throws CoreException {
		// preconditions
		IType type = projectMonitor.resolveType("javax.ws.rs.core.Application");
		Assert.assertNotNull("SourceType not found", type);
		final IPackageFragmentRoot lib = projectMonitor.resolvePackageFragmentRoot("lib/jaxrs-api-2.0.1.GA.jar");
		Assert.assertNotNull("Lib not found", lib);
		// operation
		final ITypeHierarchy hierarchy = JdtUtils.resolveTypeHierarchy(type, lib, true,
				new NullProgressMonitor());
		// verifications
		Assert.assertNotNull("SourceType hierarchy not found", hierarchy);
		Assert.assertEquals("SourceType hierarchy incomplete", 0, hierarchy.getSubtypes(type).length);
	}

	@Test
	public void shouldNotResolveTypeHierarchyOnRemovedClass() throws CoreException {
		// preconditions
		IType type = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		Assert.assertNotNull("SourceType not found", type);
		// operation
		type.delete(true, new NullProgressMonitor());
		final ITypeHierarchy hierarchy = JdtUtils.resolveTypeHierarchy(type, type.getJavaProject(), false,
				new NullProgressMonitor());
		// verification
		Assert.assertNull("No SourceType hierarchy expected", hierarchy);
	}

	@Test
	public void shouldResolveConcreteTypeArgumentsOnBinaryTypesWithoutSources() throws CoreException,
			OperationCanceledException, InterruptedException {
		// preconditions
		IType parameterizedType = projectMonitor.resolveType("org.jboss.resteasy.plugins.providers.jaxb.CollectionProvider");
		Assert.assertNotNull("Parameterized SourceType not found", parameterizedType);
		IType matchSuperInterfaceType = projectMonitor.resolveType("javax.ws.rs.ext.MessageBodyReader");
		Assert.assertNotNull("Interface SourceType not found", matchSuperInterfaceType);
		ITypeHierarchy parameterizedTypeHierarchy = JdtUtils.resolveTypeHierarchy(parameterizedType,
				parameterizedType.getJavaProject(), false, progressMonitor);
		boolean removedReferencedLibrarySourceAttachment = projectMonitor.removeReferencedLibrarySourceAttachment(
				"resteasy-jaxb-provider-2.0.1.GA.jar");
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
		IType parameterizedType = projectMonitor.resolveType("org.jboss.resteasy.plugins.providers.jaxb.AbstractJAXBProvider");
		Assert.assertNotNull("Parameterized SourceType not found", parameterizedType);
		IType matchGenericType = projectMonitor.resolveType("org.jboss.resteasy.plugins.providers.AbstractEntityProvider");
		Assert.assertNotNull("Interface SourceType not found", matchGenericType);
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
		IType parameterizedType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.extra.AnotherDummyProvider");
		Assert.assertNotNull("Parameterized SourceType not found", parameterizedType);

		// MessageBodyReader
		IType matchGenericType = projectMonitor.resolveType("javax.ws.rs.ext.MessageBodyReader");
		Assert.assertNotNull("Interface SourceType not found", matchGenericType);
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
		matchGenericType = projectMonitor.resolveType("javax.ws.rs.ext.MessageBodyWriter");
		Assert.assertNotNull("Interface SourceType not found", matchGenericType);
		resolvedTypeParameters = JdtUtils.resolveTypeArguments(parameterizedType, compilationUnit, matchGenericType,
				parameterizedTypeHierarchy, new NullProgressMonitor());
		Assert.assertNotNull("No type parameters found", resolvedTypeParameters);
		Assert.assertEquals("Wrong number of type parameters found", 1, resolvedTypeParameters.size());
		Assert.assertEquals("Wrong type parameter found", "java.math.BigDecimal", resolvedTypeParameters.get(0)
				.getFullyQualifiedName());
		// ExceptionMapper
		matchGenericType = projectMonitor.resolveType("javax.ws.rs.ext.ExceptionMapper");
		Assert.assertNotNull("Interface SourceType not found", matchGenericType);
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
		IType parameterizedType = projectMonitor.resolveType("org.jboss.resteasy.plugins.providers.jaxb.CollectionProvider");
		IType unrelatedType = projectMonitor.resolveType("javax.ws.rs.ext.ExceptionMapper");
		Assert.assertNotNull("Parameterized SourceType not found", parameterizedType);
		// operation
		IType matchSuperInterfaceType = projectMonitor.resolveType("javax.ws.rs.ext.MessageBodyReader");
		// verification
		Assert.assertNotNull("Interface SourceType not found", matchSuperInterfaceType);
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
		IType resourceType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		Assert.assertNotNull("ResourceType not found", resourceType);
		// operation
		final IType topLevelType = JdtUtils.resolveTopLevelType(resourceType.getCompilationUnit());
		// verification
		Assert.assertNotNull("SourceType not found", topLevelType);
	}

	@Test
	public void shouldNotResolveTopLevelTypeOnBinaryType() throws JavaModelException, CoreException {
		// preconditions
		IType resourceType = projectMonitor.resolveType("org.jboss.resteasy.plugins.providers.jaxb.CollectionProvider");
		Assert.assertNotNull("ResourceType not found", resourceType);
		// operation
		final IType topLevelType = JdtUtils.resolveTopLevelType(resourceType.getCompilationUnit());
		// verification
		Assert.assertNull("SourceType not found", topLevelType);
	}

	@Test
	public void shouldGetTopLevelTypeOKNoneInSourceType() throws JavaModelException, CoreException {
		// preconditions
		ICompilationUnit compilationUnit = projectMonitor.createCompilationUnit("Empty.txt",
				"org.jboss.tools.ws.jaxrs.sample", "PersistenceExceptionMapper.java");
		Assert.assertNotNull("Resource not found", compilationUnit);
		// operation
		final IType topLevelType = JdtUtils.resolveTopLevelType(compilationUnit);
		// verification
		Assert.assertNull("SourceType not expected", topLevelType);
	}

	@Test
	public void shouldResolveTopLevelTypeOnSourceWithMultipleTypes() throws JavaModelException, CoreException {
		// preconditions
		ICompilationUnit compilationUnit= projectMonitor.createCompilationUnit("Multi.txt",
				"org.jboss.tools.ws.jaxrs.sample", "PersistenceExceptionMapper.java");
		Assert.assertNotNull("Resource not found", compilationUnit);
		// operation
		final IType topLevelType = JdtUtils.resolveTopLevelType(compilationUnit);
		// verification
		Assert.assertNotNull("SourceType not found", topLevelType);
	}

	@Test
	public void shouldReturnTrueOnTopLevelTypeDetection() throws JavaModelException, CoreException {
		// preconditions
		IType resourceType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		Assert.assertNotNull("ResourceType not found", resourceType);
		// operation
		final boolean isTopLevelType = JdtUtils.isTopLevelType(resourceType);
		// verification
		Assert.assertTrue("Wrong result", isTopLevelType);
	}

	@Test
	public void shouldGetCompiltationUnitFromType() throws CoreException {
		// preconditions
		IResource resource = javaProject.getProject()
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
		IResource resource = javaProject.getProject().findMember("src/main/resources/log4j.xml");
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
		IType type = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		Assert.assertNotNull("SourceType not found", type);
		// operation
		Annotation javaAnnotation = JdtUtils.resolveAnnotation(type, JdtUtils.parse(type, progressMonitor), "Path");
		// verifications
		assertThat(javaAnnotation.getJavaAnnotation(), notNullValue());
		assertThat(javaAnnotation.getFullyQualifiedName(), equalTo(PATH));
		assertThat(javaAnnotation.getJavaAnnotationElements().size(), equalTo(1));
		assertThat(javaAnnotation.getJavaAnnotationElements().get("value").get(0), equalTo("/customers"));
	}

	@Test
	public void shoudResolveAllSourceTypeAnnotations() throws CoreException {
		// pre-conditions
		final IType type = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		Assert.assertNotNull("SourceType not found", type);
		// operation
		final Map<String, Annotation> javaAnnotations = JdtUtils.resolveAllAnnotations(type,
				JdtUtils.parse(type, progressMonitor));
		// verifications
		assertThat(javaAnnotations.size(), equalTo(5));
		for (Entry<String, Annotation> entry : javaAnnotations.entrySet()) {
			assertThat(entry.getKey(), equalTo(entry.getValue().getFullyQualifiedName()));
			assertThat(entry.getValue().getJavaAnnotation(), notNullValue());
		}
	}

	@Test
	public void shoudResolveSourceTypeAnnotationFromElement() throws CoreException {
		// pre-conditions
		IType type = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		Assert.assertNotNull("SourceType not found", type);
		IAnnotation annotation = type.getAnnotation("Path");
		Assert.assertNotNull("Annotation not found", annotation);
		// operation
		Annotation javaAnnotation = JdtUtils.resolveAnnotation(annotation, JdtUtils.parse(type, progressMonitor));
		// verifications
		assertThat(javaAnnotation.getJavaAnnotation(), equalTo(annotation));
		assertThat(javaAnnotation.getFullyQualifiedName(), equalTo(PATH));
		assertThat(javaAnnotation.getJavaAnnotationElements().size(), equalTo(1));
		assertThat(javaAnnotation.getJavaAnnotationElements().get("value").get(0), equalTo("/customers"));
	}

	@Test
	public void shoudNotResolveUnknownSourceTypeAnnotationFromClassName() throws CoreException {
		// pre-conditions
		IType type = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		Assert.assertNotNull("SourceType not found", type);
		// operation
		Annotation annotation = getAnnotation(type, HTTP_METHOD);
		// verifications
		assertThat(annotation, nullValue());
	}

	@Test
	public void shoudResolveBinaryTypeAnnotationFromClassName() throws CoreException {
		// pre-conditions
		IType type = projectMonitor.resolveType(GET);
		Assert.assertNotNull("SourceType not found", type);
		// operation
		Annotation javaAnnotation = JdtUtils.resolveAnnotation(type, JdtUtils.parse(type, progressMonitor),
				HTTP_METHOD);
		// verifications
		assertThat(javaAnnotation.getJavaAnnotation(), notNullValue());
		assertThat(javaAnnotation.getFullyQualifiedName(), equalTo(HTTP_METHOD));
		assertThat(javaAnnotation.getJavaAnnotationElements().size(), equalTo(1));
		assertThat(javaAnnotation.getJavaAnnotationElements().get("value").get(0), equalTo("GET"));
	}

	@Test
	public void shoudResolveAllBinaryTypeAnnotations() throws CoreException {
		// pre-conditions
		final IType type = projectMonitor.resolveType(GET);
		Assert.assertNotNull("SourceType not found", type);
		// operation
		final Map<String, Annotation> javaAnnotations = JdtUtils.resolveAllAnnotations(type,
				JdtUtils.parse(type, progressMonitor));
		// verifications
		assertThat(javaAnnotations.size(), equalTo(3));
		Annotation javaAnnotation = javaAnnotations.get(HTTP_METHOD);
		assertThat(javaAnnotation, notNullValue());
		assertThat(javaAnnotation.getJavaAnnotation(), notNullValue());
		assertThat(javaAnnotation.getFullyQualifiedName(), equalTo(HTTP_METHOD));
		assertThat(javaAnnotation.getJavaAnnotationElements().size(), equalTo(1));
		assertThat(javaAnnotation.getJavaAnnotationElements().get("value").get(0), equalTo("GET"));
	}
	
	@Test
	public void shoudResolveBinaryTypeAnnotationFromElement() throws CoreException {
		// pre-conditions
		IType type = projectMonitor.resolveType(GET);
		Assert.assertNotNull("SourceType not found", type);
		IAnnotation javaAnnotation = type.getAnnotation(HTTP_METHOD);
		Assert.assertTrue("Annotation not found", javaAnnotation.exists());
		// operation
		Annotation annotation = JdtUtils.resolveAnnotation(javaAnnotation, JdtUtils.parse(type, progressMonitor));
		// verifications
		assertThat(annotation.getJavaAnnotation(), equalTo(javaAnnotation));
		assertThat(annotation.getFullyQualifiedName(), equalTo(HTTP_METHOD));
		assertThat(annotation.getJavaAnnotationElements().size(), equalTo(1));
		assertThat(annotation.getJavaAnnotationElements().get("value").get(0), equalTo("GET"));
	}

	@Test
	public void shoudNotResolveBinaryTypeUnknownAnnotationFromElement() throws CoreException {
		// pre-conditions
		IType type = projectMonitor.resolveType(GET);
		Assert.assertNotNull("SourceType not found", type);
		IAnnotation javaAnnotation = type.getAnnotation(PATH);
		Assert.assertFalse("Annotation not expected", javaAnnotation.exists());
		// operation
		Annotation annotation = getAnnotation(type, PATH);
		// verifications
		assertThat(annotation, nullValue());
	}

	@Test
	public void shouldResolveJavaMethodSignatures() throws CoreException {
		// preconditions
		final IType type = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		// operation
		final Map<String, JavaMethodSignature> methodSignatures = JdtUtils.resolveMethodSignatures(type, 
				JdtUtils.parse(type, progressMonitor));
		// verification
		Assert.assertEquals(8, methodSignatures.size());
		for (IJavaMethodSignature methodSignature : methodSignatures.values()) {
			for (IJavaMethodParameter methodParameter : methodSignature.getMethodParameters()) {
				for (Entry<String, Annotation> annotationEntry : methodParameter.getAnnotations().entrySet()) {
					assertNotNull("JavaAnnotation for " + methodParameter.getName() + "." + annotationEntry.getKey()
							+ " is null", annotationEntry.getValue().getJavaAnnotation());
					assertNotNull(methodSignature.toString());
				}
			}
		}
		// testing equals() and hashcode() methods here.
		for(Iterator<Entry<String, JavaMethodSignature>> iterator1 = methodSignatures.entrySet().iterator(); iterator1.hasNext();) {
			final Entry<String, JavaMethodSignature> entry1 = iterator1.next();
			final String key1 = entry1.getKey();
			final IJavaMethodSignature methodSignature1 = entry1.getValue();
			for(Iterator<Entry<String, JavaMethodSignature>> iterator2 = methodSignatures.entrySet().iterator(); iterator2.hasNext();) {
				final Entry<String, JavaMethodSignature> entry2 = iterator2.next();
				final String key2 = entry2.getKey();
				final IJavaMethodSignature methodSignature2 = entry2.getValue();
				if(key1.equals(key2)) {
					assertThat(methodSignature1.equals(methodSignature2), equalTo(true));
					assertThat(methodSignature1.hashCode() == methodSignature2.hashCode(), equalTo(true));
				} else {
					assertThat(methodSignature1.equals(methodSignature2), equalTo(false));
					assertThat(methodSignature1.hashCode() == methodSignature2.hashCode(), equalTo(false));
				}
			}
			
			
		}
	}

	@Test
	public void shouldResolveJavaMethodSignaturesWithNullAnnotationValue() throws CoreException {
		// pre-condition
		final IType type = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		IMethod method = projectMonitor.resolveMethod(type, "getCustomer");
		method = replaceFirstOccurrenceOfCode(method, "@PathParam(\"id\") Integer id",
				"@PathParam() Integer id", false);
		// operation
		final IJavaMethodSignature methodSignature = JdtUtils.resolveMethodSignature(method,
				JdtUtils.parse(type, progressMonitor));
		// verification
		assertNotNull(methodSignature);
		assertEquals(2, methodSignature.getMethodParameters().size());
		for (IJavaMethodParameter methodParameter : methodSignature.getMethodParameters()) {
			for (Entry<String, Annotation> annotationEntry : methodParameter.getAnnotations().entrySet()) {
				assertNotNull("JavaAnnotation for " + methodParameter.getName() + "." + annotationEntry.getKey()
						+ " is null", annotationEntry.getValue().getJavaAnnotation());
			}
		}
		assertNull(methodSignature.getMethodParameter("id").getAnnotation(PATH_PARAM).getValue("value"));
	}

	@Test
	public void shouldResolveJavaMethodSignaturesForParameterizedType() throws CoreException {
		// preconditions
		final IType type = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ParameterizedResource");
		// operation
		final Map<String, JavaMethodSignature> methodSignatures = JdtUtils.resolveMethodSignatures(type, 
				JdtUtils.parse(type, progressMonitor));
		// verification
		Assert.assertEquals(1, methodSignatures.size());
	}

	@Test
	public void shouldResolveJavaMethodSignature() throws CoreException {
		// preconditions
		final IType type = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod method = projectMonitor.resolveMethod(type, "getCustomers");
		// operation
		final JavaMethodSignature methodSignature = JdtUtils.resolveMethodSignature(method,
				JdtUtils.parse(type, progressMonitor));
		// verification
		assertThat(methodSignature, notNullValue());
		assertThat(methodSignature.getJavaMethod(), notNullValue());
		assertThat(methodSignature.getMethodParameters().size(), equalTo(3));
		for (IJavaMethodParameter parameter : methodSignature.getMethodParameters()) {
			assertThat(parameter.getAnnotations().size(), isOneOf(1, 2));
			for (Annotation annotation : parameter.getAnnotations().values()) {
				assertThat(annotation.getJavaAnnotation(), notNullValue());
			}
		}
		assertThat(methodSignature.getReturnedType().getDisplayableTypeName(), equalTo("List<Customer>"));
	}

	@Test
	public void shouldConfirmSuperType() throws CoreException {
		// preconditions
		final IType bookType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final IType objectType = projectMonitor.resolveType(Object.class.getName());
		// operation
		final boolean typeOrSuperType = JdtUtils.isTypeOrSuperType(objectType, bookType);
		// verification
		assertThat(typeOrSuperType, is(true));
	}

	@Test
	public void shouldConfirmSuperTypeWhenSameType() throws CoreException {
		// preconditions
		final IType subType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final IType superType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		// operation
		final boolean typeOrSuperType = JdtUtils.isTypeOrSuperType(superType, subType);
		// verification
		assertThat(typeOrSuperType, is(true));
	}

	@Test
	public void shouldNotConfirmSuperType() throws CoreException {
		// preconditions
		final IType bookType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		final IType objectType = projectMonitor.resolveType(RESPONSE);
		// operation
		final boolean typeOrSuperType = JdtUtils.isTypeOrSuperType(objectType, bookType);
		// verification
		assertThat(typeOrSuperType, is(false));
	}

	@Test
	public void shouldRetrieveTypeAnnotationFromNameLocation() throws CoreException {
		// preconditions
		final IType customerType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final Annotation annotation = getAnnotation(customerType, PATH);
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
		final IType customerType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final Annotation annotation = getAnnotation(customerType, PATH);
		final int offset = annotation.getJavaAnnotation().getSourceRange().getOffset() + PATH.length()
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
		final IType customerType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final Annotation annotation = getAnnotation(customerType, HTTP_METHOD);
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
		final IType customerType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final Annotation annotation = getAnnotation(customerType, HTTP_METHOD);
		final int offset = annotation.getJavaAnnotation().getSourceRange().getOffset()
				+ HTTP_METHOD.length() + 3;
		// operation
		final Annotation foundAnnotation = JdtUtils.resolveAnnotationAt(offset, customerType.getCompilationUnit());
		// verification
		assertThat(foundAnnotation, notNullValue());
		assertThat(foundAnnotation.getJavaAnnotation(), notNullValue());
	}

	@Test
	public void shouldRetrieveMethodAnnotationFromNameLocation() throws CoreException {
		// preconditions
		final IType customerType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod method = projectMonitor.resolveMethod(customerType, "createCustomer");
		final Annotation annotation = getAnnotation(method, CONSUMES);
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
		final IType customerType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod method = projectMonitor.resolveMethod(customerType, "createCustomer");
		final Annotation annotation = getAnnotation(method, CONSUMES);
		final int offset = annotation.getJavaAnnotation().getSourceRange().getOffset()
				+ CONSUMES.length() + 3;
		// operation
		final Annotation foundAnnotation = JdtUtils.resolveAnnotationAt(offset, method.getCompilationUnit());
		// verification
		assertThat(foundAnnotation, notNullValue());
		assertThat(foundAnnotation.getJavaAnnotation(), notNullValue());
	}

	@Test
	public void shouldRetrieveLocalVariableAnnotationFromNameLocation() throws CoreException {
		// preconditions
		final IType customerType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod method = projectMonitor.resolveMethod(customerType, "getCustomer");
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
		final IType customerType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod method = projectMonitor.resolveMethod(customerType, "getCustomer");
		final ILocalVariable localVariable = getLocalVariable(method, "id");
		final int offset = localVariable.getAnnotations()[0].getNameRange().getOffset() + "PathParam".length() + 3;
		// operation
		final Annotation foundAnnotation = JdtUtils.resolveAnnotationAt(offset, customerType.getCompilationUnit());
		// verification
		assertThat(foundAnnotation, notNullValue());
		assertThat(foundAnnotation.getFullyQualifiedName(), equalTo("javax.ws.rs.PathParam"));
		assertThat(foundAnnotation.getJavaAnnotation(), notNullValue());
	}

	@Test
	public void shouldRetrieveFieldAnnotationFromNameLocation() throws CoreException {
		// preconditions
		final IType type = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final IField field = getField(type, "_foo");
		final Annotation annotation = getAnnotation(field, QUERY_PARAM);
		final int offset = annotation.getJavaAnnotation().getNameRange().getOffset();
		// operation
		final Annotation foundAnnotation = JdtUtils.resolveAnnotationAt(offset, type.getCompilationUnit());
		// verification
		assertThat(foundAnnotation, notNullValue());
		assertThat(foundAnnotation.getFullyQualifiedName(), equalTo("javax.ws.rs.QueryParam"));
		assertThat(foundAnnotation.getJavaAnnotation(), notNullValue());
	}

	@Test
	public void shouldRetrieveFieldAnnotationFromMemberPairLocation() throws CoreException {
		// preconditions
		final IType type = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final IField field = getField(type, "_foo");
		final Annotation annotation = getAnnotation(field, QUERY_PARAM);
		final int offset = annotation.getJavaAnnotation().getSourceRange().getOffset() + "Consumes".length()
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
		final IType customerType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final int offset = customerType.getNameRange().getOffset();
		// operation
		final Annotation foundAnnotation = JdtUtils.resolveAnnotationAt(offset, customerType.getCompilationUnit());
		// verification
		assertThat(foundAnnotation, nullValue());
	}

	@Test
	public void shouldNotRetrieveAnnotationFromMethodNameLocation() throws CoreException {
		// preconditions
		final IType customerType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod method = projectMonitor.resolveMethod(customerType, "getCustomer");
		final int offset = method.getNameRange().getOffset();
		// operation
		final Annotation foundAnnotation = JdtUtils.resolveAnnotationAt(offset, customerType.getCompilationUnit());
		// verification
		assertThat(foundAnnotation, nullValue());
	}

	@Test
	public void shouldNotRetrieveAnnotationFromMethodBodyLocation() throws CoreException {
		// preconditions
		final IType customerType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod method = projectMonitor.resolveMethod(customerType, "getCustomer");
		final int offset = method.getSourceRange().getOffset() + method.getSourceRange().getLength() - 2;
		// operation
		TestLogger.info("Compilation unit: \n{}", CompilationUnitsRepository.getInstance()
				.getAST(customerType.getCompilationUnit()));
		final Annotation foundAnnotation = JdtUtils.resolveAnnotationAt(offset, customerType.getCompilationUnit());
		// verification
		assertThat(foundAnnotation, nullValue());
	}

	@Test
	public void shouldNotRetrieveAnnotationFromFieldNameLocation() throws CoreException {
		// preconditions
		final IType type = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final IField field = getField(type, "_foo");
		final int offset = field.getSourceRange().getOffset();
		// operation
		final Annotation foundAnnotation = JdtUtils.resolveAnnotationAt(offset, type.getCompilationUnit());
		// verification
		assertThat(foundAnnotation, nullValue());
	}

	@Test
	public void shouldRetrieveTypeAnnotationMemberValuePairSourceRange() throws CoreException {
		// preconditions
		final IType customerType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final Annotation annotation = getAnnotation(customerType, PATH);
		// operation
		final ISourceRange range = JdtUtils.resolveMemberPairValueRange(annotation.getJavaAnnotation(), "value");
		// verification
		assertThat(range, notNullValue());
		final ISourceRange annotationRange = annotation.getJavaAnnotation().getSourceRange();
		assertThat(range.getOffset(), greaterThan(annotationRange.getOffset()));
		assertThat(range.getOffset(), lessThan(annotationRange.getOffset() + annotationRange.getLength()));

	}

	@Test
	public void shouldRetrieveTypeSingleMemberAnnotationMemberValuePairSourceRange() throws CoreException {
		// preconditions
		final IType customerType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final Annotation annotation = getAnnotation(customerType, CONSUMES);
		// operation
		final ISourceRange range = JdtUtils.resolveMemberPairValueRange(annotation.getJavaAnnotation(), "value");
		// verification
		assertThat(range, notNullValue());
		final ISourceRange annotationRange = annotation.getJavaAnnotation().getSourceRange();
		assertThat(range.getOffset(), greaterThan(annotationRange.getOffset()));
		assertThat(range.getOffset(), lessThan(annotationRange.getOffset() + annotationRange.getLength()));

	}

	@Test
	public void shouldRetrieveMethodAnnotationMemberValuePairSourceRange() throws CoreException {
		// preconditions
		final IType customerType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod method = projectMonitor.resolveMethod(customerType, "getCustomer");
		final Annotation annotation = getAnnotation(method, PATH);
		// operation
		final ISourceRange range = JdtUtils.resolveMemberPairValueRange(annotation.getJavaAnnotation(), "value");
		// verification
		assertThat(range, notNullValue());
		final ISourceRange annotationRange = annotation.getJavaAnnotation().getSourceRange();
		assertThat(range.getOffset(), greaterThan(annotationRange.getOffset()));
		assertThat(range.getOffset(), lessThan(annotationRange.getOffset() + annotationRange.getLength()));
	}

	@Test
	public void shouldRetrieveFieldAnnotationMemberValuePairSourceRange() throws CoreException {
		// preconditions
		final IType type = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final IField field = getField(type, "_foo");
		final Annotation annotation = getAnnotation(field, QUERY_PARAM);
		// operation
		final ISourceRange range = JdtUtils.resolveMemberPairValueRange(annotation.getJavaAnnotation(), "value");
		// verification
		assertThat(range, notNullValue());
		final ISourceRange annotationRange = annotation.getJavaAnnotation().getSourceRange();
		assertThat(range.getOffset(), greaterThan(annotationRange.getOffset()));
		assertThat(range.getOffset(), lessThan(annotationRange.getOffset() + annotationRange.getLength()));
	}

	@Test
	public void shouldRetrieveLocalVariableAnnotationMemberValuePairSourceRange() throws CoreException {
		// preconditions
		final IType customerType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod method = projectMonitor.resolveMethod(customerType, "getCustomer");
		final ILocalVariable localVariable = getLocalVariable(method, "id");
		final IAnnotation annotation = localVariable.getAnnotations()[0];
		// operation
		final ISourceRange range = JdtUtils.resolveMemberPairValueRange(annotation, "value");
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
	
	
	@Test
	public void shouldRetrieveMethodAtLocation() throws CoreException {
		// preconditions
		final IType customerType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IMethod method = projectMonitor.resolveMethod(customerType, "getCustomer");
		// operation
		final IJavaElement element = JdtUtils.getElementAt(customerType.getCompilationUnit(), method.getSourceRange().getOffset(), IJavaElement.METHOD);
		// verification
		assertThat(element, notNullValue());
		assertThat((IMethod)element, equalTo(method));
		
	}
	
	@Test
	public void shouldResolveJavaFieldType() throws CoreException {
		// preconditions
		final IType type = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final IField field = type.getField("_foo");
		// operation
		final SourceType fieldType = JdtUtils.resolveFieldType(field, JdtUtils.parse(type, progressMonitor));
		// verification
		assertThat(fieldType.getDisplayableTypeName(), equalTo(String.class.getSimpleName()));
	}

	@Test
	public void shouldResolveJavaFieldPrimitiveType() throws CoreException {
		// preconditions
		final IType type = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		replaceFirstOccurrenceOfCode(type, "String _foo", "int _foo", false);
		final IField field = type.getField("_foo");
		// operation
		final SourceType fieldType = JdtUtils.resolveFieldType(field, JdtUtils.parse(type, progressMonitor));
		// verification
		assertThat(fieldType.getDisplayableTypeName(), equalTo("int"));
	}

	@Test
	public void shouldNotResolveJavaFieldType() throws CoreException {
		// preconditions
		final IType type = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		final IField field = type.getField("unknown");
		// operation
		final SourceType fieldType = JdtUtils.resolveFieldType(field, JdtUtils.parse(type, progressMonitor));
		// verification
		assertThat(fieldType, nullValue());
	}
	
	@Test
	public void shouldFindSubtypesInProjectOnly() throws CoreException {
		// preconditions
		final IType objectType = projectMonitor.resolveType("java.lang.Object");
		final IType bookType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BookResource");
		// operation
		final List<IType> subtypes = JdtUtils.findSubtypes(objectType);
		// verification
		assertThat(subtypes.size(), greaterThan(1));
		assertThat(subtypes.contains(bookType), is(true));
	}


}
