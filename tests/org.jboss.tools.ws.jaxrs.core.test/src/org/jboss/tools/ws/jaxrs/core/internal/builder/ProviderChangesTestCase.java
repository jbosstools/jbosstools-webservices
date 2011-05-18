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

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.WorkbenchUtils;
import org.jboss.tools.ws.jaxrs.core.builder.AbstractMetamodelBuilderTestCase;
import org.jboss.tools.ws.jaxrs.core.metamodel.Provider;
import org.jboss.tools.ws.jaxrs.core.metamodel.Provider.EnumProviderKind;
import org.jboss.tools.ws.jaxrs.core.utils.JdtUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author xcoulon
 * 
 */
public class ProviderChangesTestCase extends AbstractMetamodelBuilderTestCase {

	@Test
	public void shouldNotChangeWhenAddingEmptyCompilationUnit() throws CoreException {
		// pre-conditions
		Assert.assertEquals(5, metamodel.getProviders().size());
		// operation
		WorkbenchUtils.createCompilationUnit(javaProject, "EmptyCompilationUnit.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "FOO.java", bundle);
		// post-conditions : nothing changed
		Assert.assertEquals(6, metamodel.getHttpMethods().size());
		Assert.assertEquals(5, metamodel.getProviders().size());

	}

	@Test
	public void shouldIncreaseWhenAddingProviderFromFile() throws CoreException {
		// pre-conditions
		Assert.assertEquals(5, metamodel.getProviders().size());
		// operation
		WorkbenchUtils.createCompilationUnit(javaProject, "PersistenceExceptionMapper.txt",
				"org.jboss.tools.ws.jaxrs.sample.services.providers", "PersistenceExceptionMapper.java", bundle);
		// post-conditions
		Assert.assertEquals(6, metamodel.getProviders().size());
	}

	@Test
	public void shouldNotChangeWhenAddingMemberInExistingCompilationUnit() throws CoreException {
		// pre-conditions
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject,
				"PersistenceExceptionMapper.txt", "org.jboss.tools.ws.jaxrs.sample.services.providers",
				"PersistenceExceptionMapper.java", bundle);
		Assert.assertEquals(6, metamodel.getProviders().size());
		// operation
		WorkbenchUtils.appendCompilationUnitType(compilationUnit, "EntityExistsExceptionMapperMember.txt", bundle);
		// post-conditions
		Assert.assertEquals(6, metamodel.getProviders().size());
	}

	@Test
	public void shouldFollowWhenMovingFile() throws CoreException {
		// pre-conditions
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject,
				"PersistenceExceptionMapper.txt", "org.jboss.tools.ws.jaxrs.sample.services.providers",
				"PersistenceExceptionMapper.java", bundle);
		Assert.assertEquals(6, metamodel.getProviders().size());
		// operation
		WorkbenchUtils.move(compilationUnit, "org.jboss.tools.ws.jaxrs.sample", bundle);
		// post-conditions
		Assert.assertEquals(6, metamodel.getProviders().size());
		Provider provider = metamodel.getProviders().getFor("javax.persistence.PersistenceException");
		Assert.assertNotNull("PersistenceExceptionMapper not found", provider);
		Assert.assertEquals("PersistenceExceptionMapper", provider.getSimpleName());
		Assert.assertTrue("Wrong associated JavaType", provider.getJavaElement().exists());
		Assert.assertEquals("Wrong qualified name", "org.jboss.tools.ws.jaxrs.sample.PersistenceExceptionMapper",
				provider.getJavaElement().getFullyQualifiedName());
	}

	@Test
	public void shouldDecreaseWhenRemovingFile() throws CoreException {
		// pre-conditions
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject,
				"PersistenceExceptionMapper.txt", "org.jboss.tools.ws.jaxrs.sample.services.providers",
				"PersistenceExceptionMapper.java", bundle);
		Assert.assertEquals(6, metamodel.getProviders().size());
		// operation
		WorkbenchUtils.delete(compilationUnit);
		// post-conditions
		Assert.assertEquals(5, metamodel.getProviders().size());
	}

	@Test
	public void shouldDecreaseWhenRemovingPrimaryMember() throws CoreException {
		// pre-conditions
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject,
				"PersistenceExceptionMapper.txt", "org.jboss.tools.ws.jaxrs.sample.services.providers",
				"PersistenceExceptionMapper.java", bundle);
		Assert.assertEquals(6, metamodel.getProviders().size());
		// operation
		WorkbenchUtils.delete(compilationUnit.getType("PersistenceExceptionMapper"));
		// post-conditions
		Assert.assertEquals(5, metamodel.getProviders().size());
	}

	@Test
	public void shouldNotChangeWhenRemovingAnotherMember() throws CoreException {
		// pre-conditions
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject,
				"PersistenceExceptionMapper.txt", "org.jboss.tools.ws.jaxrs.sample.services.providers",
				"PersistenceExceptionMapper.java", bundle);
		WorkbenchUtils.appendCompilationUnitType(compilationUnit, "FooBarExceptionMapperMember.txt", bundle);
		Assert.assertEquals(6, metamodel.getProviders().size());
		// operation
		compilationUnit.getType("FooBarExceptionMapper").delete(true, new NullProgressMonitor());
		// post-conditions
		Assert.assertEquals(6, metamodel.getProviders().size());
	}

	@Test
	public void shouldFollowWhenRenamingMember() throws CoreException {
		// pre-conditions
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject,
				"PersistenceExceptionMapper.txt", "org.jboss.tools.ws.jaxrs.sample.services.providers",
				"PersistenceExceptionMapper.java", bundle);
		Assert.assertEquals(6, metamodel.getProviders().size());
		// operation
		WorkbenchUtils.rename(compilationUnit, "AnotherPersistenceExceptionMapper.java");
		// post-conditions
		Assert.assertEquals(6, metamodel.getProviders().size());
		Provider provider = metamodel.getProviders().getFor("javax.persistence.PersistenceException");
		Assert.assertNotNull("ExceptionMapper not found", provider);
		Assert.assertEquals("AnotherPersistenceExceptionMapper", provider.getSimpleName());
	}

	@Test
	public void shouldFollowWhenMovingParamererizedType() throws CoreException {
		// pre-conditions
		Assert.assertEquals(5, metamodel.getProviders().size());
		IType exceptionType = JdtUtils.resolveType(
				"org.jboss.tools.ws.jaxrs.sample.extra.TestQualifiedException.TestException", javaProject, null);
		Provider provider = metamodel.getProviders().getFor(exceptionType);
		Assert.assertNotNull("Provider not found", provider);
		IType actualExceptionType = provider.getProvidedKinds().get(EnumProviderKind.EXCEPTION_MAPPER);
		Assert.assertEquals("Wrong java type", exceptionType, actualExceptionType);
		// operation
		IPackageFragment pkg = WorkbenchUtils.createPackage(javaProject, "org.jboss.tools.ws.jaxrs.sample");
		exceptionType.getCompilationUnit().move(pkg, null, null, false, null);
		WorkbenchUtils.replaceFirstOccurrenceOfCode(provider.getJavaElement(),
				"import org.jboss.tools.ws.jaxrs.sample.extra.TestQualifiedException.TestException;",
				"import org.jboss.tools.ws.jaxrs.sample.TestQualifiedException.TestException; "
						+ "import org.jboss.tools.ws.jaxrs.sample.TestQualifiedException;");
		// post-conditions
		actualExceptionType = provider.getProvidedKinds().get(EnumProviderKind.EXCEPTION_MAPPER);
		exceptionType = JdtUtils.resolveType(
				"org.jboss.tools.ws.jaxrs.sample.TestQualifiedException.TestException", javaProject, null);
		Assert.assertEquals("Wrong java type", exceptionType.getFullyQualifiedName(),
				actualExceptionType.getFullyQualifiedName());
		Assert.assertEquals("Wrong pkg name",
				"org.jboss.tools.ws.jaxrs.sample.TestQualifiedException$TestException",
				actualExceptionType.getFullyQualifiedName());
	}

	@Test
	public void shouldIncreaseWhenAddingProviderAnnotation() throws CoreException {
		// pre-conditions
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject,
				"PseudoPersistenceExceptionMapper.txt", "org.jboss.tools.ws.jaxrs.sample.services.providers",
				"PersistenceExceptionMapper.java", bundle);
		Assert.assertEquals(5, metamodel.getProviders().size());
		// operation
		IType persistenceExceptionMapperType = compilationUnit.getType("PersistenceExceptionMapper");
		WorkbenchUtils.addTypeAnnotation(persistenceExceptionMapperType, "@Provider");
		// post-conditions
		Assert.assertEquals(6, metamodel.getProviders().size());
	}

	@Test
	public void shouldDecreaseWhenRemovingProviderAnnotation() throws CoreException {
		// pre-conditions
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject,
				"PersistenceExceptionMapper.txt", "org.jboss.tools.ws.jaxrs.sample.services.providers",
				"PersistenceExceptionMapper.java", bundle);
		Assert.assertEquals(6, metamodel.getProviders().size());
		// operation
		WorkbenchUtils.removeFirstOccurrenceOfCode(compilationUnit.findPrimaryType(), "@Provider");
		// post-conditions
		Assert.assertEquals(5, metamodel.getProviders().size());
	}

	@Test
	public void shouldReportErrorWhenRemovingParameterizedTypeImport() throws CoreException {
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject,
				"PersistenceExceptionMapper.txt", "org.jboss.tools.ws.jaxrs.sample.services.providers",
				"PersistenceExceptionMapper.java", bundle);
		Assert.assertEquals(6, metamodel.getProviders().size());
		// operation
		WorkbenchUtils.removeImport(compilationUnit, "javax.persistence.PersistenceException");
		// post-conditions
		Assert.assertEquals(6, metamodel.getProviders().size());
		Provider provider = metamodel.getProviders().getByType(compilationUnit.findPrimaryType());
		Assert.assertNotNull("Wrong provider", provider);
		Assert.assertTrue("Provider not marked with errors", provider.hasErrors());
		Assert.assertEquals("Wrong number of provided kinds", 1, provider.getProvidedKinds().size());
		Assert.assertNotNull("No provider expected",
				metamodel.getProviders().getFor("javax.persistence.PersistenceException"));
	}

	@Test
	public void shouldIncreaseWhenAddingParameterizedTypeThenImport() throws CoreException, InterruptedException {
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject,
				"PersistenceExceptionMapperWithMissingImport.txt",
				"org.jboss.tools.ws.jaxrs.sample.services.providers", "PersistenceExceptionMapper.java", bundle);
		Assert.assertEquals(5, metamodel.getProviders().size());
		Provider provider = metamodel.getProviders().getByType(compilationUnit.findPrimaryType());
		Assert.assertNull("Provider not expected", provider);
		// operation
		WorkbenchUtils.addImport(compilationUnit, "javax.persistence.PersistenceException");
		// post-conditions
		Assert.assertEquals(6, metamodel.getProviders().size());
		provider = metamodel.getProviders().getByType(compilationUnit.findPrimaryType());
		Assert.assertNotNull("Wrong provider", provider);
		Assert.assertFalse("Error(s) still reported", provider.hasErrors());
		Assert.assertEquals("Wrong number of provided kinds", 1, provider.getProvidedKinds().size());
		Assert.assertEquals(1, metamodel.getProviders().getFor("javax.persistence.PersistenceException")
				.getProvidedKinds().size());
	}

	@Test
	public void shouldNotDecreaseWhenRemovingParameterizedType() throws CoreException {
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject,
				"PersistenceExceptionMapper.txt", "org.jboss.tools.ws.jaxrs.sample.services.providers",
				"PersistenceExceptionMapper.java", bundle);
		Assert.assertEquals(6, metamodel.getProviders().size());
		// operation
		WorkbenchUtils.replaceAllOccurrencesOfCode(compilationUnit, "<PersistenceException>", "<>");
		// post-conditions
		Assert.assertEquals(6, metamodel.getProviders().size());
		Provider provider = metamodel.getProviders().getByType(compilationUnit.findPrimaryType());
		Assert.assertNotNull("Wrong provider", provider);
		Assert.assertTrue("No error marker found", provider.hasErrors());
	}

	@Test
	public void shouldFollowWhenReplacingParameterizedTypeWithExistingType() throws CoreException {
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject,
				"PersistenceExceptionMapper.txt", "org.jboss.tools.ws.jaxrs.sample.services.providers",
				"PersistenceExceptionMapper.java", bundle);
		Assert.assertEquals(6, metamodel.getProviders().size());
		// operation
		WorkbenchUtils
				.replaceAllOccurrencesOfCode(compilationUnit, "<PersistenceException>", "<EntityExistsException>");
		WorkbenchUtils
				.replaceAllOccurrencesOfCode(compilationUnit, "(PersistenceException ", "(EntityExistsException ");
		// post-conditions
		Assert.assertEquals(6, metamodel.getProviders().size());
		Provider provider = metamodel.getProviders().getFor("javax.persistence.EntityExistsException");
		Assert.assertNotNull("PersistenceExceptionMapper not found", provider);
	}

	@Test
	public void shouldReportErrorWhenReplacingParameterizedTypeWithInvalidType() throws CoreException {
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject,
				"PersistenceExceptionMapper.txt", "org.jboss.tools.ws.jaxrs.sample.services.providers",
				"PersistenceExceptionMapper.java", bundle);
		Assert.assertEquals(6, metamodel.getProviders().size());
		// operation
		WorkbenchUtils.replaceAllOccurrencesOfCode(compilationUnit, "<PersistenceException>", "<FooException>");
		// post-conditions
		Assert.assertEquals(6, metamodel.getProviders().size());
		Provider provider = metamodel.getProviders().getByType(compilationUnit.findPrimaryType());
		Assert.assertNotNull("Wrong provider", provider);
		Assert.assertEquals("Wrong number of provided kinds", 1, provider.getProvidedKinds().size());
		Assert.assertTrue("No error reported", provider.hasErrors());
		Assert.assertNotNull("No provider found",
				metamodel.getProviders().getFor("javax.persistence.PersistenceException"));

	}

	@Test
	public void shouldChangeWhenAddingProducesAnnotation() throws CoreException {
		Assert.assertEquals(5, metamodel.getProviders().size());
		IType providerType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.extra.AnotherDummyProvider",
				javaProject, new NullProgressMonitor());
		Provider provider = metamodel.getProviders().getByType(providerType);
		Assert.assertNull("No mediatype expected", provider.getMediaTypeCapabilities(EnumProviderKind.PRODUCER));
		// operation
		WorkbenchUtils.addImport(providerType, "javax.ws.rs.Produces");
		WorkbenchUtils.addTypeAnnotation(providerType, "@Produces(MediaType.APPLICATION_JSON)");
		// post-conditions
		Assert.assertEquals(5, metamodel.getProviders().size());
		Assert.assertEquals("Wrong mediatype", "application/json",
				provider.getMediaTypeCapabilities(EnumProviderKind.PRODUCER).get(0));
	}

	@Test
	public void shouldChangeWhenChangingProducesAnnotation() throws CoreException {
		Assert.assertEquals(5, metamodel.getProviders().size());
		IType providerType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.extra.DummyProvider",
				javaProject, new NullProgressMonitor());
		// operation
		WorkbenchUtils.replaceFirstOccurrenceOfCode(providerType, "@Produces(MediaType.APPLICATION_XML)",
				"@Produces(MediaType.APPLICATION_JSON)");
		// post-conditions
		Assert.assertEquals(5, metamodel.getProviders().size());
		Provider provider = metamodel.getProviders().getByType(providerType);
		Assert.assertEquals("Wrong mediatype", "application/json",
				provider.getMediaTypeCapabilities(EnumProviderKind.PRODUCER).get(0));
		Assert.assertEquals("Wrong mediatype", "application/xml",
				provider.getMediaTypeCapabilities(EnumProviderKind.CONSUMER).get(0));
	}

	@Test
	public void shouldChangeWhenRemovingProducesAnnotation() throws CoreException {
		Assert.assertEquals(5, metamodel.getProviders().size());
		IType providerType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.extra.DummyProvider",
				javaProject, new NullProgressMonitor());
		// operation
		WorkbenchUtils.removeFirstOccurrenceOfCode(providerType, "@Produces(MediaType.APPLICATION_XML)");
		// post-conditions
		Assert.assertEquals(5, metamodel.getProviders().size());
		Provider provider = metamodel.getProviders().getByType(providerType);
		Assert.assertNull("No mediatype expected", provider.getMediaTypeCapabilities(EnumProviderKind.PRODUCER));
	}

	@Test
	public void shouldChangeWhenAddingConsumesAnnotation() throws CoreException {
		Assert.assertEquals(5, metamodel.getProviders().size());
		IType providerType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.extra.AnotherDummyProvider",
				javaProject, new NullProgressMonitor());
		Provider provider = metamodel.getProviders().getByType(providerType);
		Assert.assertNull("No mediatype expected", provider.getMediaTypeCapabilities(EnumProviderKind.PRODUCER));
		// operation
		WorkbenchUtils.addImport(providerType, "javax.ws.rs.Consumes");
		WorkbenchUtils.addTypeAnnotation(providerType, "@Consumes(MediaType.APPLICATION_JSON)");
		// post-conditions
		Assert.assertEquals(5, metamodel.getProviders().size());
		Assert.assertEquals("Wrong mediatype", "application/json",
				provider.getMediaTypeCapabilities(EnumProviderKind.CONSUMER).get(0));
	}

	@Test
	public void shouldChangeWhenChangingConsumesAnnotation() throws CoreException {
		Assert.assertEquals(5, metamodel.getProviders().size());
		IType providerType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.extra.DummyProvider",
				javaProject, new NullProgressMonitor());
		// operation
		WorkbenchUtils.replaceFirstOccurrenceOfCode(providerType, "@Consumes(MediaType.APPLICATION_XML)",
				"@Consumes(MediaType.APPLICATION_JSON)");
		// post-conditions
		Assert.assertEquals(5, metamodel.getProviders().size());
		Provider provider = metamodel.getProviders().getByType(providerType);
		Assert.assertEquals("Wrong mediatype", "application/json",
				provider.getMediaTypeCapabilities(EnumProviderKind.CONSUMER).get(0));
		Assert.assertEquals("Wrong mediatype", "application/xml",
				provider.getMediaTypeCapabilities(EnumProviderKind.PRODUCER).get(0));
	}

	@Test
	public void shouldChangeWhenRemovingConsumesAnnotation() throws CoreException {
		Assert.assertEquals(5, metamodel.getProviders().size());
		IType providerType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.extra.DummyProvider",
				javaProject, new NullProgressMonitor());
		// operation
		WorkbenchUtils.removeFirstOccurrenceOfCode(providerType, "@Consumes(MediaType.APPLICATION_XML)");
		// post-conditions
		Assert.assertEquals(5, metamodel.getProviders().size());
		Provider provider = metamodel.getProviders().getByType(providerType);
		Assert.assertNull("No mediatype expected", provider.getMediaTypeCapabilities(EnumProviderKind.CONSUMER));
	}

	@Test
	public void shouldNotChangeWhenAddingConsumesAnnotationOnProducer() throws CoreException {
		// pre-conditions
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject,
				"SimpleMessageBodyWriter.txt", "org.jboss.tools.ws.jaxrs.sample.services.providers",
				"SimpleMessageBodyWriter.java", bundle);
		Assert.assertEquals(6, metamodel.getProviders().size());
		// operation
		IType providerType = JdtUtils.resolveTopLevelType(compilationUnit);
		WorkbenchUtils.addTypeAnnotation(providerType, "@Consumes(MediaType.APPLICATION_JSON)");
		// post-conditions
		Assert.assertEquals(6, metamodel.getProviders().size());
		Provider provider = metamodel.getProviders().getByType(providerType);
		Assert.assertNull("No mediatype expected", provider.getMediaTypeCapabilities(EnumProviderKind.PRODUCER));
		Assert.assertNull("No mediatype expected here: provider is not a MessageBodyReader !",
				provider.getMediaTypeCapabilities(EnumProviderKind.CONSUMER));
	}

	@Test
	public void shouldNotChangeWhenAddingProducesAnnotationOnConsumer() throws CoreException {
		// pre-conditions
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject,
				"SimpleMessageBodyReader.txt", "org.jboss.tools.ws.jaxrs.sample.services.providers",
				"SimpleMessageBodyReader.java", bundle);
		Assert.assertEquals(6, metamodel.getProviders().size());
		// operation
		IType providerType = JdtUtils.resolveTopLevelType(compilationUnit);
		WorkbenchUtils.addTypeAnnotation(providerType, "@Produces(MediaType.APPLICATION_JSON)");
		// post-conditions
		Assert.assertEquals(6, metamodel.getProviders().size());
		Provider provider = metamodel.getProviders().getByType(providerType);
		Assert.assertNull("No mediatype expected", provider.getMediaTypeCapabilities(EnumProviderKind.CONSUMER));
		Assert.assertNull("No mediatype expected here: provider is not a MessageBodyWriter !",
				provider.getMediaTypeCapabilities(EnumProviderKind.PRODUCER));
	}

	@Test
	public void shouldNotChangeWhenRemovingIOExceptionImportDeclaration() throws CoreException {
		// pre-conditions
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject,
				"SimpleMessageBodyReader.txt", "org.jboss.tools.ws.jaxrs.sample.services.providers",
				"SimpleMessageBodyReader.java", bundle);
		Assert.assertEquals(6, metamodel.getProviders().size());
		// operation
		IType providerType = JdtUtils.resolveTopLevelType(compilationUnit);
		WorkbenchUtils.removeFirstOccurrenceOfCode(providerType, "import java.io.IOException;");
		// post-conditions
		Assert.assertEquals(6, metamodel.getProviders().size());
		Provider provider = metamodel.getProviders().getByType(providerType);
		Assert.assertNull("No mediatype expected", provider.getMediaTypeCapabilities(EnumProviderKind.CONSUMER));
		Assert.assertNull("No mediatype expected here: provider is not a MessageBodyWriter !",
				provider.getMediaTypeCapabilities(EnumProviderKind.PRODUCER));
	}

	@Test
	public void shouldReportErrorWhenSettingInvalidMediaTypeOnProducesAnnotation() throws CoreException {
		Assert.assertEquals(5, metamodel.getProviders().size());
		IType providerType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.extra.DummyProvider",
				javaProject, new NullProgressMonitor());
		// operation
		WorkbenchUtils.replaceFirstOccurrenceOfCode(providerType, "@Produces(MediaType.APPLICATION_XML)",
				"@Produces(FOO)");
		// post-conditions
		Assert.assertEquals(5, metamodel.getProviders().size());
		Provider provider = metamodel.getProviders().getByType(providerType);
		List<String> mediaTypes = provider.getMediaTypeCapabilities(EnumProviderKind.PRODUCER);
		Assert.assertTrue("No error reported", provider.hasErrors());
		Assert.assertEquals("Wrong number of mediatypes", 1, mediaTypes.size());
		Assert.assertEquals("Wrong mediatype", "application/xml", mediaTypes.get(0));

	}

	@Test
	public void shouldReportErrorWhenSettingInvalidMediaTypeOnConsumesAnnotation() throws CoreException {
		Assert.assertEquals(5, metamodel.getProviders().size());
		IType providerType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.extra.DummyProvider",
				javaProject, new NullProgressMonitor());
		// operation
		WorkbenchUtils.replaceFirstOccurrenceOfCode(providerType, "@Consumes(MediaType.APPLICATION_XML)",
				"@Consumes(FOO)");
		// post-conditions
		Assert.assertEquals(5, metamodel.getProviders().size());
		Provider provider = metamodel.getProviders().getByType(providerType);
		Assert.assertTrue("No error reported", provider.hasErrors());
		Assert.assertEquals("Wrong number of Mediatypes", 1,
				provider.getMediaTypeCapabilities(EnumProviderKind.CONSUMER).size());
		Assert.assertEquals("Wrong mediatype", "application/xml",
				provider.getMediaTypeCapabilities(EnumProviderKind.CONSUMER).get(0));
	}

	@Test
	public void shouldReportErrorWhenRemovingProviderAnnotationImport() throws CoreException {
		// pre-conditions
		Assert.assertEquals(5, metamodel.getProviders().size());
		IType providerType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.extra.DummyProvider",
				javaProject, new NullProgressMonitor());
		// operation
		WorkbenchUtils.removeImport(providerType.getCompilationUnit(), javax.ws.rs.ext.Provider.class.getName());
		// post-conditions : 1 HTTPMethod less
		Assert.assertEquals(5, metamodel.getProviders().size());
		Provider provider = metamodel.getProviders().getByType(providerType);
		Assert.assertNotNull("Provider should be retrieved", provider);
		Assert.assertTrue("No error reported", provider.hasErrors());
	}

	@Test
	public void shouldChangeWhenMakingMultipleChangesAtSameTime() throws CoreException {
		// pre-condition
		ICompilationUnit compilationUnit = WorkbenchUtils.createCompilationUnit(javaProject,
				"PseudoPersistenceExceptionMapper.txt", "org.jboss.tools.ws.jaxrs.sample.services.providers",
				"PersistenceExceptionMapper.java", bundle);
		Assert.assertEquals(5, metamodel.getProviders().size());
		// operation
		String[] oldContents = new String[] { "import javax.persistence.PersistenceException;",
				"public class PersistenceExceptionMapper implements ExceptionMapper<PersistenceException>",
				"public Response toResponse(PersistenceException exception)" };
		String[] newContents = new String[] {
				"import javax.persistence.EntityExistsException;",
				"@Provider public class PersistenceExceptionMapper implements ExceptionMapper<EntityExistsException>",
				"public Response toResponse(EntityExistsException exception)" };

		WorkbenchUtils.replaceFirstOccurrenceOfCode(compilationUnit, oldContents, newContents);
		// post-condition
		Assert.assertEquals(6, metamodel.getProviders().size());
		Assert.assertNotNull("Provider not found",
				metamodel.getProviders().getFor("javax.persistence.EntityExistsException"));
		Assert.assertNull("Provider not expected",
				metamodel.getProviders().getFor("javax.persistence.PersistenceException"));
	}
}
