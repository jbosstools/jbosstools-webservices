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

package org.jboss.tools.ws.jaxrs.core.utils;

import java.util.List;
import java.util.Map;

import javax.ws.rs.QueryParam;

import junit.framework.Assert;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jface.text.ITypedRegion;
import org.jboss.tools.ws.jaxrs.core.AbstractCommonTestCase;
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCoreTestsPlugin;
import org.jboss.tools.ws.jaxrs.core.WorkbenchTasks;
import org.jboss.tools.ws.jaxrs.core.WorkbenchUtils;
import org.junit.Test;
import org.osgi.framework.Bundle;

//FIXME : add assertions on binary source attachement, causing errors or failures when missing
public class JdtUtilsTestCase extends AbstractCommonTestCase {

	public static final String PROVIDER = "javax.ws.rs.ext.Provider";

	public static final String PRODUCES = "javax.ws.rs.Produces";

	public static final String CONSUMES = "javax.ws.rs.Consumes";

	public static final String PATH = "javax.ws.rs.Path";

	public static final String HTTP_METHOD = "javax.ws.rs.HttpMethod";

	private Bundle bundle = JBossJaxrsCoreTestsPlugin.getDefault().getBundle();

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
	public void shouldAssertTypeHasAnnotationWithFullyQualifiedNameUsage() throws JavaModelException, CoreException {
		NullProgressMonitor progressMonitor = new NullProgressMonitor();
		IType resourceType = JdtUtils.resolveType(
				"org.jboss.tools.ws.jaxrs.sample.services.PurchaseOrderResource", javaProject, progressMonitor);
		Assert.assertNotNull("ResourceType not found", resourceType);
		CompilationUnit compilationUnit = JdtUtils.parse(resourceType, progressMonitor);
		Assert.assertNotNull("Type Annotation not found",
				JdtUtils.resolveAnnotationBinding(resourceType, compilationUnit, PATH));
	}

	@Test
	public void shouldAssertTypeHasAnnotationWithSimpleNameUsage() throws JavaModelException, CoreException {
		NullProgressMonitor progressMonitor = new NullProgressMonitor();
		IType resourceType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource",
				javaProject, progressMonitor);
		Assert.assertNotNull("ResourceType not found", resourceType);
		CompilationUnit compilationUnit = JdtUtils.parse(resourceType, progressMonitor);
		Assert.assertNotNull("Type Annotation not found",
				JdtUtils.resolveAnnotationBinding(resourceType, compilationUnit, PATH));
	}

	@Test
	public void shouldAssertMethodHasAnnotationWithSimpleName() throws JavaModelException, CoreException {
		NullProgressMonitor progressMonitor = new NullProgressMonitor();
		IType resourceType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource",
				javaProject, progressMonitor);
		Assert.assertNotNull("ResourceType not found", resourceType);
		CompilationUnit compilationUnit = JdtUtils.parse(resourceType, progressMonitor);
		for (IMethod method : resourceType.getMethods()) {
			if (method.getElementName().equals("createCustomer")) {
				Assert.assertNotNull("Method Annotation not found",
						JdtUtils.resolveAnnotationBinding(method, compilationUnit, "javax.ws.rs.POST"));
			}
		}
	}

	@Test
	public void shouldNotAssertTypeHasInvalidAnnotation() throws JavaModelException, CoreException {
		NullProgressMonitor progressMonitor = new NullProgressMonitor();
		IType resourceType = JdtUtils.resolveType(
				"org.jboss.tools.ws.jaxrs.sample.services.PurchaseOrderResource", javaProject, progressMonitor);
		Assert.assertNotNull("ResourceType not found", resourceType);
		CompilationUnit compilationUnit = JdtUtils.parse(resourceType, progressMonitor);
		Assert.assertNull("Type Annotation not expected",
				JdtUtils.resolveAnnotationBinding(resourceType, compilationUnit, "@Path"));

	}

	@Test
	public void shouldNotAssertTypeHasUnusedAnnotation() throws JavaModelException, CoreException {
		NullProgressMonitor progressMonitor = new NullProgressMonitor();
		IType resourceType = JdtUtils.resolveType(
				"org.jboss.tools.ws.jaxrs.sample.services.PurchaseOrderResource", javaProject, progressMonitor);
		Assert.assertNotNull("ResourceType not found", resourceType);
		CompilationUnit compilationUnit = JdtUtils.parse(resourceType, progressMonitor);
		Assert.assertNull("Type Annotation not expected",
				JdtUtils.resolveAnnotationBinding(resourceType, compilationUnit, HTTP_METHOD));

	}

	@Test
	public void shouldResolveTypeAnnotationAttributeValue() throws CoreException {
		NullProgressMonitor progressMonitor = new NullProgressMonitor();
		IType resourceType = JdtUtils.resolveType(
				"org.jboss.tools.ws.jaxrs.sample.services.PurchaseOrderResource", javaProject, progressMonitor);
		Assert.assertNotNull("ResourceType not found", resourceType);
		CompilationUnit compilationUnit = JdtUtils.parse(resourceType, progressMonitor);
		IAnnotationBinding annotationBinding = JdtUtils.resolveAnnotationBinding(resourceType, compilationUnit, PATH);
		Assert.assertNotNull("Type Annotation not found", annotationBinding);
		Object value = JdtUtils.resolveAnnotationAttributeValue(annotationBinding, "value");
		Assert.assertEquals("Wrong result", "/orders", value);
	}

	@Test
	public void shouldNotResolveTypeAnnotationAttributeValueWhenAttributeUnknown() throws CoreException {
		NullProgressMonitor progressMonitor = new NullProgressMonitor();
		IType resourceType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource",
				javaProject, progressMonitor);
		Assert.assertNotNull("ResourceType not found", resourceType);
		CompilationUnit compilationUnit = JdtUtils.parse(resourceType, progressMonitor);
		IAnnotationBinding annotationBinding = JdtUtils.resolveAnnotationBinding(resourceType, compilationUnit, PATH);
		Assert.assertNotNull("Type Annotation not found", annotationBinding);
		Object value = JdtUtils.resolveAnnotationAttributeValue(annotationBinding, "unknown");
		Assert.assertNull("Wrong result", value);
	}

	@Test
	public void shouldNotResolveTypeAnnotationOnbinaryType() throws CoreException {
		NullProgressMonitor progressMonitor = new NullProgressMonitor();
		IType type = JdtUtils.resolveType("org.jboss.resteasy.plugins.providers.jaxb.CollectionProvider", javaProject,
				progressMonitor);
		Assert.assertNotNull("Type not found", type);
		CompilationUnit compilationUnit = JdtUtils.parse(type, progressMonitor);
		IAnnotationBinding annotationBinding = JdtUtils.resolveAnnotationBinding(type, compilationUnit, PRODUCES);
		Assert.assertNull("Type Annotation not expected", annotationBinding);
	}

	@Test
	public void shouldResolveTypeAnnotationAttributeValueAsSingleString() throws CoreException {
		NullProgressMonitor progressMonitor = new NullProgressMonitor();
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.PurchaseOrderResource",
				javaProject, progressMonitor);
		Assert.assertNotNull("Type not found", type);
		CompilationUnit compilationUnit = JdtUtils.parse(type, progressMonitor);
		IAnnotationBinding annotationBinding = JdtUtils.resolveAnnotationBinding(type, compilationUnit, PATH);
		Assert.assertNotNull("Type Annotation not found", annotationBinding);
		Object value = JdtUtils.resolveAnnotationAttributeValue(annotationBinding, "value");
		Assert.assertNotNull("Values not found", value);
		Assert.assertTrue("Wrong result type", value instanceof String);
		Assert.assertEquals("Wrong result value", "/orders", value);
	}

	@Test
	public void shouldResolveTypeAnnotationAttributeValueAsSingleStringQualifiedValue() throws CoreException {
		NullProgressMonitor progressMonitor = new NullProgressMonitor();
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource",
				javaProject, progressMonitor);
		Assert.assertNotNull("Type not found", type);
		CompilationUnit compilationUnit = JdtUtils.parse(type, progressMonitor);
		IAnnotationBinding annotationBinding = JdtUtils.resolveAnnotationBinding(type, compilationUnit, PATH);
		Assert.assertNotNull("Type Annotation not found", annotationBinding);
		Object value = JdtUtils.resolveAnnotationAttributeValue(annotationBinding, "value");
		Assert.assertNotNull("Values not found", value);
		Assert.assertTrue("Wrong result type", value instanceof String);
		Assert.assertEquals("Wrong result value", "/customers", value);
	}

	@Test
	public void shouldResolveTypeAnnotationAttributeValueAsSingleQualifiedMediaTypeValue() throws CoreException {
		NullProgressMonitor progressMonitor = new NullProgressMonitor();
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource",
				javaProject, progressMonitor);
		Assert.assertNotNull("Type not found", type);
		CompilationUnit compilationUnit = JdtUtils.parse(type, progressMonitor);
		IAnnotationBinding annotationBinding = JdtUtils.resolveAnnotationBinding(type, compilationUnit, CONSUMES);
		Assert.assertNotNull("Type Annotation not found", annotationBinding);
		Object value = JdtUtils.resolveAnnotationAttributeValue(annotationBinding, "value");
		Assert.assertNotNull("Values not found", value);
		Assert.assertTrue("Wrong result type: " + value.getClass(), value instanceof Object[]);
		Assert.assertEquals("Wrong result: " + value, 1, ((Object[])value).length);
		Assert.assertTrue("Wrong result type: " + value, ((Object[])value)[0] instanceof String);
		Assert.assertEquals("Wrong result value", "application/xml", ((Object[])value)[0]);
	}

	@Test
	public void shouldResolveTypeAnnotationAttributeValueAsSingleSimpleMediaTypeValue() throws CoreException {
		NullProgressMonitor progressMonitor = new NullProgressMonitor();
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.GameResource", javaProject,
				progressMonitor);
		Assert.assertNotNull("Type not found", type);
		// warning : ensure the annotation syntax includes brackets to make an
		// array : @Consumes({ APPLICATION_XML })
		CompilationUnit compilationUnit = JdtUtils.parse(type, progressMonitor);
		IAnnotationBinding annotationBinding = JdtUtils.resolveAnnotationBinding(type, compilationUnit, CONSUMES);
		Assert.assertNotNull("Type Annotation not found", annotationBinding);
		Object values = JdtUtils.resolveAnnotationAttributeValue(annotationBinding, "value");
		Assert.assertNotNull("Values not found", values);
		Assert.assertTrue("Wrong result type: " + values.getClass(), values instanceof Object[]);
		Assert.assertEquals("Wrong result value", "application/xml", ((Object[]) values)[0]);
	}

	@Test
	public void shouldNotResolveTypeAnnotationAttributeValueOnMissingAnnotation() throws CoreException {
		NullProgressMonitor progressMonitor = new NullProgressMonitor();
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BookResource", javaProject,
				progressMonitor);
		Assert.assertNotNull("Type not found", type);
		CompilationUnit compilationUnit = JdtUtils.parse(type, progressMonitor);
		IAnnotationBinding annotationBinding = JdtUtils.resolveAnnotationBinding(type, compilationUnit, CONSUMES);
		Assert.assertNull("Type Annotation not expected", annotationBinding);
	}

	@Test
	public void shouldResolveConcreteTypeArgumentsOnBinaryTypesWithoutSources() throws CoreException,
			OperationCanceledException, InterruptedException {
		NullProgressMonitor progressMonitor = new NullProgressMonitor();
		IType parameterizedType = JdtUtils.resolveType("org.jboss.resteasy.plugins.providers.jaxb.CollectionProvider",
				javaProject, progressMonitor);
		Assert.assertNotNull("Parameterized Type not found", parameterizedType);
		IType matchSuperInterfaceType = JdtUtils.resolveType("javax.ws.rs.ext.MessageBodyReader", javaProject,
				progressMonitor);
		Assert.assertNotNull("Interface Type not found", matchSuperInterfaceType);
		ITypeHierarchy parameterizedTypeHierarchy = JdtUtils.resolveTypeHierarchy(parameterizedType, false,
				progressMonitor);
		boolean removedReferencedLibrarySourceAttachment = WorkbenchTasks.removeReferencedLibrarySourceAttachment(
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
		IType parameterizedType = JdtUtils.resolveType(
				"org.jboss.tools.ws.jaxrs.sample.extra.AnotherDummyProvider", javaProject,
				new NullProgressMonitor());
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
	public void shouldResolveFullyQualifiedNameFromSimpleName() throws JavaModelException, CoreException {
		NullProgressMonitor progressMonitor = new NullProgressMonitor();
		IType resourceType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource",
				javaProject, progressMonitor);
		Assert.assertNotNull("ResourceType not found", resourceType);
		CompilationUnit compilationUnit = JdtUtils.parse(resourceType, progressMonitor);
		IAnnotationBinding annotationBinding = JdtUtils.resolveAnnotationBinding(resourceType, compilationUnit, PATH);
		Assert.assertNotNull("Type Annotation not found", annotationBinding);
		Assert.assertEquals("Type Annotation not found", PATH,
				JdtUtils.resolveAnnotationFullyQualifiedName(annotationBinding));
	}

	@Test
	public void shouldResolveFullyQualifiedNameFromQualifiedName() throws JavaModelException, CoreException {
		NullProgressMonitor progressMonitor = new NullProgressMonitor();
		IType resourceType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource",
				javaProject, progressMonitor);
		Assert.assertNotNull("ResourceType not found", resourceType);
		CompilationUnit compilationUnit = JdtUtils.parse(resourceType, progressMonitor);
		IAnnotationBinding annotationBinding = JdtUtils.resolveAnnotationBinding(resourceType, compilationUnit, CONSUMES);
		Assert.assertNotNull("Type Annotation not found", annotationBinding);
		Assert.assertEquals("Type Annotation not found", CONSUMES,
				JdtUtils.resolveAnnotationFullyQualifiedName(annotationBinding));
	}

	@Test
	public void shouldResolveTopLevelTypeFromSourceType() throws JavaModelException, CoreException {
		NullProgressMonitor progressMonitor = new NullProgressMonitor();
		IType resourceType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource",
				javaProject, progressMonitor);
		Assert.assertNotNull("ResourceType not found", resourceType);
		Assert.assertNotNull("Type not found", JdtUtils.resolveTopLevelType(resourceType.getCompilationUnit()));
	}

	@Test
	public void shouldNotResolveTopLevelTypeOnBinaryType() throws JavaModelException, CoreException {
		NullProgressMonitor progressMonitor = new NullProgressMonitor();
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
		NullProgressMonitor progressMonitor = new NullProgressMonitor();
		IType resourceType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource",
				javaProject, progressMonitor);
		Assert.assertNotNull("ResourceType not found", resourceType);
		Assert.assertTrue("Wrong result", JdtUtils.isTopLevelType(resourceType));
	}
	
	@Test
	public void shouldResolveMethodBinding() throws CoreException {
		NullProgressMonitor progressMonitor = new NullProgressMonitor();
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BookResource", javaProject,
				progressMonitor);
		Assert.assertNotNull("Type not found", type);
		CompilationUnit compilationUnit = JdtUtils.parse(type, null);
		for(IMethod method : type.getMethods()) {
			if(method.getElementName().equals("getProduct")) {
				IMethodBinding methodBinding = JdtUtils.resolveMethodBinding(method, compilationUnit);
				Assert.assertNotNull("Binding not found", methodBinding);
			}
		}
	}
	
	@Test
	public void shouldGetCompiltationUnitFromType() throws CoreException {
		IResource resource = project.findMember("src/main/java/org/jboss/tools/ws/jaxrs/sample/services/BookResource.java");
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
	public void shouldResolveMethodAnnotationBinding() throws CoreException {
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator", javaProject,
				new NullProgressMonitor());
		Assert.assertNotNull("Type not found", type);
		CompilationUnit compilationUnit = JdtUtils.parse(type, null);
		for(IMethod method : type.getMethods()) {
			if(method.getElementName().equals("getProductResourceLocator")) {
				IAnnotationBinding binding = JdtUtils.resolveAnnotationBinding(method, compilationUnit, javax.ws.rs.Path.class);
				Assert.assertNotNull("Binding not found", binding);
			}
		}
	}
	
	@Test
	public void shouldResolveMethodZeroQueryParam() throws CoreException {
		NullProgressMonitor progressMonitor = new NullProgressMonitor();
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BookResource", javaProject,
				progressMonitor);
		Assert.assertNotNull("Type not found", type);
		CompilationUnit compilationUnit = JdtUtils.parse(type, null);

		for(IMethod method : type.getMethods()) {
			if(method.getElementName().equals("getProduct")) {
				Map<IAnnotationBinding, ITypedRegion> resolvedQueryParams = JdtUtils.resolveMethodParamBindings(method, compilationUnit, QueryParam.class);
				Assert.assertEquals("Wrong number of params", 0, resolvedQueryParams.size());
			}
		}
	}

	@Test
	public void shouldResolveMethodZeroQueryParamOnNoAnnotatedParamMethod() throws CoreException {
		NullProgressMonitor progressMonitor = new NullProgressMonitor();
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				progressMonitor);
		Assert.assertNotNull("Type not found", type);
		CompilationUnit compilationUnit = JdtUtils.parse(type, null);

		for(IMethod method : type.getMethods()) {
			if(method.getElementName().equals("createCustomer")) {
				Map<IAnnotationBinding, ITypedRegion> resolvedQueryParams = JdtUtils.resolveMethodParamBindings(method, compilationUnit, QueryParam.class);
				Assert.assertEquals("Wrong number of params", 0, resolvedQueryParams.size());
			}
		}
	}
	
	@Test
	public void shouldResolveMethodZeroQueryParamOnNoQueryParamAnnotatedParamMethod() throws CoreException {
		NullProgressMonitor progressMonitor = new NullProgressMonitor();
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				progressMonitor);
		Assert.assertNotNull("Type not found", type);
		CompilationUnit compilationUnit = JdtUtils.parse(type, null);

		for(IMethod method : type.getMethods()) {
			if(method.getElementName().equals("createCustomer")) {
				Map<IAnnotationBinding, ITypedRegion> resolvedQueryParams = JdtUtils.resolveMethodParamBindings(method, compilationUnit, QueryParam.class);
				Assert.assertEquals("Wrong number of params", 0, resolvedQueryParams.size());
			}
		}
	}
	
	@Test
	public void shouldResolveMethodTwoQueryParam() throws CoreException {
		NullProgressMonitor progressMonitor = new NullProgressMonitor();
		IType type = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				progressMonitor);
		Assert.assertNotNull("Type not found", type);
		CompilationUnit compilationUnit = JdtUtils.parse(type, null);

		for(IMethod method : type.getMethods()) {
			if(method.getElementName().equals("getCustomers")) {
				Map<IAnnotationBinding, ITypedRegion> resolvedQueryParams = JdtUtils.resolveMethodParamBindings(method, compilationUnit, QueryParam.class);
				Assert.assertEquals("Wrong number of params", 2, resolvedQueryParams.size());
			}
		}
	}	
	
	
}
