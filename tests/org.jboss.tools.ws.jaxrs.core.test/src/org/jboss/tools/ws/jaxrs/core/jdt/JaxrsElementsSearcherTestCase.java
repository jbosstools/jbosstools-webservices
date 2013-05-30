package org.jboss.tools.ws.jaxrs.core.jdt;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.jboss.tools.ws.jaxrs.core.WorkbenchUtils.replaceFirstOccurrenceOfCode;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.AbstractCommonTestCase;
import org.jboss.tools.ws.jaxrs.core.WorkbenchUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsHttpMethod;
import org.junit.Test;

public class JaxrsElementsSearcherTestCase extends AbstractCommonTestCase {

	@Test
	public void shouldRetrieveAllResourcesInTheProject() throws CoreException {
		// pre-conditions
		// operation
		final List<IType> resources = JaxrsElementsSearcher.findResourceTypes(javaProject, new NullProgressMonitor());
		// verifications
		assertThat(resources.size(), equalTo(7));
	}

	@Test
	public void shouldRetrieveOneResourceInType() throws CoreException {
		// pre-conditions
		IType customerType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		assertThat(customerType, notNullValue());
		// operation
		final List<IType> resources = JaxrsElementsSearcher.findResourceTypes(customerType, new NullProgressMonitor());
		// verifications
		assertThat(resources.size(), equalTo(1));
	}

	@Test
	public void shouldNotRetrieveResourceInOtherType() throws CoreException {
		// pre-conditions
		IType customerType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		assertThat(customerType, notNullValue());
		// operation
		final List<IType> resources = JaxrsElementsSearcher.findResourceTypes(customerType, new NullProgressMonitor());
		// verifications
		assertThat(resources.size(), equalTo(0));
	}

	@Test
	public void shouldRetrieveAllResourceMethodsInProject() throws CoreException {
		// pre-conditions
		// operation
		final List<IMethod> resourceMethods = JaxrsElementsSearcher.findResourceMethods(javaProject,
				new ArrayList<IJaxrsHttpMethod>(), new NullProgressMonitor());
		// verifications
		// just sub resource methods with @Path annotation
		assertThat(resourceMethods.size(), equalTo(19));
	}

	@Test
	public void shouldRetrieveAllResourceMethodsInType() throws CoreException {
		// pre-conditions
		IType customerType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		assertThat(customerType, notNullValue());
		// operation
		final List<IMethod> resourceMethods = JaxrsElementsSearcher.findResourceMethods(customerType,
				new ArrayList<IJaxrsHttpMethod>(), new NullProgressMonitor());
		// verifications
		// just sub resource methods with @Path annotation
		assertThat(resourceMethods.size(), equalTo(4));
	}

	@Test
	public void shouldRetrieveAllHttpMethodsInProject() throws CoreException {
		// pre-conditions
		// operation
		final List<IType> httpMethods = JaxrsElementsSearcher.findHttpMethodTypes(javaProject,
				new NullProgressMonitor());
		// verifications
		assertThat(httpMethods.size(), equalTo(1));
	}

	@Test
	public void shouldRetrieveOneHttpMethodsInType() throws CoreException {
		// pre-conditions
		IType fooType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		assertThat(fooType, notNullValue());
		// operation
		final List<IType> resourceMethods = JaxrsElementsSearcher.findHttpMethodTypes(fooType,
				new NullProgressMonitor());
		// verifications
		assertThat(resourceMethods.size(), equalTo(1));
	}

	@Test
	public void shouldRetrieveAllProvidersInProject() throws CoreException {
		// pre-conditions
		// operation
		final List<IType> providerTypes = JaxrsElementsSearcher.findProviderTypes(javaProject,
				new NullProgressMonitor());
		// verifications
		assertThat(providerTypes.size(), equalTo(5));
	}

	@Test
	public void shouldRetrieveAllProvidersInSourceFolder() throws CoreException {
		// pre-conditions
		final IPackageFragmentRoot sourceFolder = getPackageFragmentRoot("src/main/java");
		// operation
		final List<IType> providerTypes = JaxrsElementsSearcher.findProviderTypes(sourceFolder,
				new NullProgressMonitor());
		// verifications
		assertThat(providerTypes.size(), equalTo(2));
	}

	@Test
	public void shouldRetrieveProviderWithoutAnnotation() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		WorkbenchUtils.removeFirstOccurrenceOfCode(type, "@Provider", false);
		// operation
		final List<IType> providerTypes = JaxrsElementsSearcher.findProviderTypes(type,
				new NullProgressMonitor());
		// verifications
		assertThat(providerTypes.size(), equalTo(1));
	}

	@Test
	public void shouldRetrieveProviderWithoutHierarchy() throws CoreException {
		// pre-conditions
		final IType type = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		WorkbenchUtils.removeFirstOccurrenceOfCode(type, "implements ExceptionMapper<EntityNotFoundException>", false);
		// operation
		final List<IType> providerTypes = JaxrsElementsSearcher.findProviderTypes(type, new NullProgressMonitor());
		// verifications
		assertThat(providerTypes.size(), equalTo(1));
	}
	
	@Test
	public void shouldRetrieveProviderInSourceType() throws CoreException {
		// pre-conditions
		final IType sourceType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		// operation
		final List<IType> providerTypes = JaxrsElementsSearcher
				.findProviderTypes(sourceType, new NullProgressMonitor());
		// verifications
		assertThat(providerTypes.size(), equalTo(1));
	}

	@Test
	public void shouldRetrieveProviderInSourceTypeWithMissingProviderAnnotation() throws CoreException {
		// pre-conditions
		final IType sourceType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		replaceFirstOccurrenceOfCode(sourceType, "@Provider", "", false);
		// operation
		final List<IType> providerTypes = JaxrsElementsSearcher
				.findProviderTypes(sourceType, new NullProgressMonitor());
		// verifications
		assertThat(providerTypes.size(), equalTo(1));
	}

	@Test
	public void shouldRetrieveProviderInSourceTypeWithMissingTypeHierarchy() throws CoreException {
		// pre-conditions
		final IType sourceType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		replaceFirstOccurrenceOfCode(sourceType, "implements ExceptionMapper<EntityNotFoundException>", "", false);
		// operation
		final List<IType> providerTypes = JaxrsElementsSearcher
				.findProviderTypes(sourceType, new NullProgressMonitor());
		// verifications
		assertThat(providerTypes.size(), equalTo(1));
	}

	@Test
	public void shouldNotRetrieveProviderInSourceType() throws CoreException {
		// pre-conditions
		final IType sourceType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		// operation
		final List<IType> providerTypes = JaxrsElementsSearcher
				.findProviderTypes(sourceType, new NullProgressMonitor());
		// verifications
		assertThat(providerTypes.size(), equalTo(0));
	}

	@Test
	public void shouldRetrieveAllApplicationsInProject() throws CoreException {
		// pre-conditions
		// operation
		final List<IType> applicationTypes = JaxrsElementsSearcher.findApplicationTypes(javaProject,
				new NullProgressMonitor());
		// verifications
		assertThat(applicationTypes.size(), equalTo(1));
	}

	@Test
	public void shouldRetrieveOneApplicationInType() throws CoreException {
		// pre-conditions
		IType restType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		assertThat(restType, notNullValue());
		// operation
		final List<IType> applicationTypes = JaxrsElementsSearcher.findApplicationTypes(restType,
				new NullProgressMonitor());
		// verifications
		assertThat(applicationTypes.size(), equalTo(1));
	}

	@Test
	public void shouldRetrieveOneApplicationWithSupertypeOnlyOnScopeType() throws CoreException {
		// pre-conditions
		final IType type = replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.RestApplication",
				javaProject, "@ApplicationPath(\"/app\")", "", false);
		// operation
		final List<IType> applications = JaxrsElementsSearcher.findApplicationTypes(type, new NullProgressMonitor());
		// verifications
		assertThat(applications.size(), equalTo(1));
	}

	@Test
	public void shouldRetrieveOneApplicationWithSupertypeOnlyOnScopeProject() throws CoreException {
		// pre-conditions
		final IType type = replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.RestApplication",
				javaProject, "@ApplicationPath(\"/app\")", "", false);
		// operation
		final List<IType> applications = JaxrsElementsSearcher.findApplicationTypes(type.getJavaProject(),
				new NullProgressMonitor());
		// verifications
		assertThat(applications.size(), equalTo(1));
	}

	@Test
	public void shouldRetrieveOneApplicationWithAnnotationOnly() throws CoreException {
		// pre-conditions
		final IType type = replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.RestApplication",
				javaProject, "@ApplicationPath(\"/app\")", "", false);
		// operation
		final List<IType> applications = JaxrsElementsSearcher.findApplicationTypes(type, new NullProgressMonitor());
		// verifications
		assertThat(applications.size(), equalTo(1));
	}

	@Test
	public void shouldNotRetrieveApplicationsWithSupertypeOnlyOnScopeLibrary() throws CoreException {
		// pre-conditions
		final IPackageFragmentRoot lib = WorkbenchUtils.getPackageFragmentRoot(javaProject,
				"lib/jaxrs-api-2.0.1.GA.jar", new NullProgressMonitor());
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.RestApplication", javaProject,
				"@ApplicationPath(\"/app\")", "", false);
		// operation
		final List<IType> applications = JaxrsElementsSearcher.findApplicationTypes(lib, new NullProgressMonitor());
		// verifications
		assertThat(applications.size(), equalTo(0));
	}

	@Test
	public void shouldNotRetrieveApplicationsOnScopeOtherType() throws CoreException {
		// pre-conditions
		IType fooType = resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		assertThat(fooType, notNullValue());
		// operation
		final List<IType> applications = JaxrsElementsSearcher.findApplicationTypes(fooType, new NullProgressMonitor());
		// verifications
		assertThat(applications.size(), equalTo(0));
	}

	@Test
	public void shouldNotFailWhenJaxrsCoreApplicationTypeIsMissing() throws CoreException, OperationCanceledException,
			InterruptedException {
		// pre-conditions: remove Appllication from project classpath
		WorkbenchUtils.removeClasspathEntry(javaProject, "jaxrs-api-2.0.1.GA.jar", null);
		// operation
		final List<IType> applications = JaxrsElementsSearcher.findApplicationTypes(javaProject,
				new NullProgressMonitor());
		// verifications
		assertThat(applications.size(), equalTo(1));
	}
}
