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

package org.jboss.tools.ws.jaxrs.core.internal.builder;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.ext.Provider;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.WorkbenchUtils;
import org.jboss.tools.ws.jaxrs.core.builder.AbstractMetamodelBuilderTestCase;
import org.jboss.tools.ws.jaxrs.core.metamodel.HTTPMethod;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author xcoulon
 * 
 */
// @RunListeners(DirtiesProjectCodeListener.class)
public class HttpMethodChangesTestCase extends AbstractMetamodelBuilderTestCase {

	@Test
	public void shouldIncreaseWhenAddingHTTPMethodFromFile() throws CoreException {
		// pre-conditions
		Assert.assertEquals(6, metamodel.getHttpMethods().size());
		// operation
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject, "FooHTTPMethod.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "FOO.java", bundle);
		// post-conditions : 1 more request method designator
		Assert.assertEquals(7, metamodel.getHttpMethods().size());
		Assert.assertNotNull("HTTPMethod not retrieved", metamodel.getHttpMethods().getByVerb("FOO"));
		Assert.assertNotNull("HTTPMethod not retrieved",
				metamodel.getHttpMethods().getByType(compilationUnit.getTypes()[0]));
	}

	@Test
	public void shouldNotIncreaseWhenAddingEmptyCompilationUnit() throws CoreException {
		// pre-conditions
		Assert.assertEquals(6, metamodel.getHttpMethods().size());
		// operation
		WorkbenchUtils.createCompilationUnit(javaProject, "EmptyCompilationUnit.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "FOO.java", bundle);

		// post-conditions : nothing changed
		Assert.assertEquals(6, metamodel.getHttpMethods().size());
	}

	@Test
	public void shouldNotIncreaseWhenAddingMemberInExistingCompilationUnit() throws CoreException {
		// pre-conditions
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject, "FooHTTPMethod.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "FOO.java", bundle);
		Assert.assertEquals(7, metamodel.getHttpMethods().size());
		// operation
		WorkbenchUtils.appendCompilationUnitType(compilationUnit, "FooBarHTTPMethodMember.txt", bundle);

		// post-conditions : nothing changed
		Assert.assertEquals(7, metamodel.getHttpMethods().size());
	}

	@Test
	public void shouldFollowMovingFile() throws CoreException {
		// pre-conditions
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject, "FooHTTPMethod.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "FOO.java", bundle);
		Assert.assertEquals(7, metamodel.getHttpMethods().size());
		// operation
		WorkbenchUtils.move(compilationUnit, "org.jboss.tools.ws.jaxrs.sample", bundle);
		// post-conditions
		Assert.assertEquals(7, metamodel.getHttpMethods().size());
		HTTPMethod httpMethod = metamodel.getHttpMethods().getByVerb("FOO");
		Assert.assertNotNull("HTTPMethod not found", httpMethod);
		Assert.assertTrue("Associated JavaType does not exist: " + httpMethod.getJavaElement(), httpMethod
				.getJavaElement().exists());
		Assert.assertEquals("Wrong qualified name", "org.jboss.tools.ws.jaxrs.sample.FOO", httpMethod
				.getJavaElement().getFullyQualifiedName());
	}

	@Test
	public void shouldDecreaseWhenRemovingFile() throws CoreException {
		// pre-conditions
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject, "FooHTTPMethod.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "FOO.java", bundle);
		Assert.assertEquals(7, metamodel.getHttpMethods().size());
		// operation
		WorkbenchUtils.delete(compilationUnit);
		// post-conditions
		Assert.assertEquals(6, metamodel.getHttpMethods().size());
	}

	@Test
	public void shouldDecreaseWhenRemovingPrimaryMember() throws CoreException {
		// pre-conditions
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject, "FooHTTPMethod.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "FOO.java", bundle);
		Assert.assertEquals(7, metamodel.getHttpMethods().size());
		// operation
		WorkbenchUtils.delete(compilationUnit.getType("FOO"));
		// post-conditions
		Assert.assertEquals(6, metamodel.getHttpMethods().size());
	}

	@Test
	public void shouldNotDecreaseWhenRemovingAnotherMember() throws CoreException {
		// pre-conditions
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject, "FooHTTPMethod.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "FOO.java", bundle);
		WorkbenchUtils.appendCompilationUnitType(compilationUnit, "FooBarHTTPMethodMember.txt", bundle);
		Assert.assertEquals(7, metamodel.getHttpMethods().size());
		// operation
		compilationUnit.getType("FOOBAR").delete(true, new NullProgressMonitor());
		// post-conditions
		Assert.assertEquals(7, metamodel.getHttpMethods().size());
	}

	@Test
	public void shouldFollowWhenRenamingMember() throws CoreException {
		// pre-conditions
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject, "FooHTTPMethod.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "FOO.java", bundle);
		Assert.assertEquals(7, metamodel.getHttpMethods().size());
		// operation
		WorkbenchUtils.rename(compilationUnit, "BAR.java");

		// post-conditions
		Assert.assertEquals(7, metamodel.getHttpMethods().size());
		HTTPMethod httpMethod = metamodel.getHttpMethods().getByVerb("FOO");
		Assert.assertNotNull("HTTPMethod not found", httpMethod);
		Assert.assertTrue("No associated JavaType", httpMethod.getJavaElement().exists());
		Assert.assertEquals("Wrong qualified name", "org.jboss.tools.ws.jaxrs.sample.services.BAR", httpMethod
				.getJavaElement().getFullyQualifiedName());
	}

	@Test
	public void shouldNotIncreaseWhenAddingHttpMethodAnnotationWithoutImport() throws CoreException {
		// pre-conditions
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject, "PseudoFooHTTPMethod.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "FOO.java", bundle);
		Assert.assertEquals(6, metamodel.getHttpMethods().size());
		// operation
		IType fooType = compilationUnit.getType("FOO");
		WorkbenchUtils.addTypeAnnotation(fooType, "@HttpMethod(\"FOO\")");

		// post-conditions : missing import : @HttpMethod annotation considered
		// in current package, thus does not match the expected
		// HTTP_METHOD annotation -> unchanged number of request method
		// designators
		Assert.assertEquals(6, metamodel.getHttpMethods().size());
		HTTPMethod fooElement = metamodel.getHttpMethods().getByVerb("FOO");
		Assert.assertNull("HTTPMethod not retrieved", fooElement);
	}

	@Test
	public void shouldIncreaseWhenAddingHttpMethodAnnotationThenImport() throws CoreException {
		// pre-conditions
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject, "PseudoFooHTTPMethod.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "FOO.java", bundle);
		Assert.assertEquals(6, metamodel.getHttpMethods().size());
		// operation
		IType fooType = compilationUnit.getType("FOO");
		WorkbenchUtils.addTypeAnnotation(fooType, "@HttpMethod(\"FOO\")");
		Assert.assertNull("Element not expected", metamodel.getHttpMethods().getByVerb("FOO"));
		WorkbenchUtils.addImport(compilationUnit, HttpMethod.class.getName());
		// post-conditions
		Assert.assertEquals(7, metamodel.getHttpMethods().size());
		HTTPMethod fooElement = metamodel.getHttpMethods().getByVerb("FOO");
		Assert.assertNotNull("HTTPMethod not retrieved", fooElement);
		Assert.assertFalse("Errors HTTPMethod not expected", fooElement.hasErrors());
	}

	@Test
	public void shouldIncreaseWhenAddingHttpMethodImportThenAnnotation() throws CoreException {
		// pre-conditions
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject, "PseudoFooHTTPMethod.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "FOO.java", bundle);
		Assert.assertEquals(6, metamodel.getHttpMethods().size());
		// operation
		WorkbenchUtils.addImport(compilationUnit, HttpMethod.class.getName());
		IType fooType = compilationUnit.getType("FOO");
		WorkbenchUtils.addTypeAnnotation(fooType, "@HttpMethod(\"FOO\")");

		// post-conditions
		Assert.assertEquals(7, metamodel.getHttpMethods().size());
		Assert.assertNotNull("HTTPMethod not retrieved", metamodel.getHttpMethods().getByVerb("FOO"));
	}

	@Test
	public void shouldReportErrorWhenModifyingAnnotationImportDeclaration() throws CoreException {
		// pre-conditions
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject, "FooHTTPMethod.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "FOO.java", bundle);
		Assert.assertEquals(7, metamodel.getHttpMethods().size());
		// operation
		WorkbenchUtils.modifyImport(compilationUnit, HttpMethod.class.getName(), Provider.class.getName());

		// post-conditions : 1 HTTPMethod less (import is missing)
		Assert.assertEquals(7, metamodel.getHttpMethods().size());
		Assert.assertTrue("HTTPMethod not marked with errors", metamodel.getHttpMethods().getByVerb("FOO").hasErrors());
	}

	@Test
	public void shouldIncreaseWhenAddingAnnotationImportDeclaration() throws CoreException {
		// pre-conditions
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject, "PseudoFooHTTPMethod.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "FOO.java", bundle);
		IType fooType = compilationUnit.getType("FOO");
		WorkbenchUtils.addTypeAnnotation(fooType, "@HttpMethod(\"FOO\")");
		WorkbenchUtils.addImport(compilationUnit, Provider.class.getName());
		//
		Assert.assertEquals(6, metamodel.getHttpMethods().size());
		// operation
		WorkbenchUtils.modifyImport(compilationUnit, Provider.class.getName(), HttpMethod.class.getName());

		// post-conditions : 1 HTTPMethod more (import is valid)
		Assert.assertEquals(7, metamodel.getHttpMethods().size());
		Assert.assertNotNull("HTTPMethod not retrieved", metamodel.getHttpMethods().getByVerb("FOO"));
	}

	@Test
	public void shouldReportErrorWhenRemovingAnnotationImportDeclaration() throws CoreException {
		// pre-conditions
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject, "FooHTTPMethod.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "FOO.java", bundle);
		Assert.assertEquals(7, metamodel.getHttpMethods().size());
		// operation
		WorkbenchUtils.removeImport(compilationUnit, HttpMethod.class.getName());

		// post-conditions : 1 HTTPMethod less
		Assert.assertEquals(7, metamodel.getHttpMethods().size());
		Assert.assertTrue("HTTPMethod should be marked with errors", metamodel.getHttpMethods().getByVerb("FOO")
				.hasErrors());
	}

	@Test
	public void shouldFollowWhenModifyingHttpMethodAnnotation() throws CoreException {
		// pre-conditions
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject, "FooHTTPMethod.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "FOO.java", bundle);
		Assert.assertEquals(7, metamodel.getHttpMethods().size());
		// operation
		IType fooType = compilationUnit.getType("FOO");
		WorkbenchUtils.replaceFirstOccurrenceOfCode(fooType, "@HttpMethod(\"FOO\")", "@HttpMethod(\"Foo\")");
		// post-conditions
		Assert.assertEquals(7, metamodel.getHttpMethods().size());
		Assert.assertNotNull("HTTPMethod not retrieved", metamodel.getHttpMethods().getByVerb("Foo"));
	}

	@Test
	public void shouldDecreaseWhenRemovingHttpMethodAnnotation() throws CoreException {
		// pre-conditions
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject, "FooHTTPMethod.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "FOO.java", bundle);
		Assert.assertEquals(7, metamodel.getHttpMethods().size());
		// operation
		IType fooType = compilationUnit.getType("FOO");
		WorkbenchUtils.removeFirstOccurrenceOfCode(fooType, "@HttpMethod(\"FOO\")");

		// post-conditions
		Assert.assertEquals(6, metamodel.getHttpMethods().size());
	}

	@Test
	public void shouldNotCreateWhenSyntaxErrorOnHttpMethodAnnotation() throws CoreException {
		// pre-conditions
		Assert.assertEquals(6, metamodel.getHttpMethods().size());
		// operation
		WorkbenchUtils.createCompilationUnit(javaProject, "FooHTTPMethodWithCompilationError.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "FOO.java", bundle);
		// post-conditions
		Assert.assertEquals(6, metamodel.getHttpMethods().size());
	}
}
