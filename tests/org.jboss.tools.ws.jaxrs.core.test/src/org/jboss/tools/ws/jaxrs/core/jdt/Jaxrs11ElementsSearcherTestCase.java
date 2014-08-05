package org.jboss.tools.ws.jaxrs.core.jdt;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils.removeFirstOccurrenceOfCode;
import static org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils.replaceFirstOccurrenceOfCode;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.search.JavaElementsSearcher;
import org.jboss.tools.ws.jaxrs.core.junitrules.TestProjectMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

public class Jaxrs11ElementsSearcherTestCase {

	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule("org.jboss.tools.ws.jaxrs.tests.sampleproject");
	
	@Rule
	public TestProjectMonitor projectMonitor = new TestProjectMonitor("org.jboss.tools.ws.jaxrs.tests.sampleproject");

	private IJavaProject javaProject = null;
	
	@Before
	public void setup() {
		javaProject = projectMonitor.getJavaProject();
	}
	
	@Test
	public void shouldRetrieveAllResourcesInTheProject() throws CoreException {
		// pre-conditions
		// operation
		final Collection<IType> resources = JavaElementsSearcher.findResourceTypes(javaProject, new NullProgressMonitor());
		// verifications
		assertThat(resources.size(), equalTo(7));
	}

	@Test
	public void shouldRetrieveOneResourceInType() throws CoreException {
		// pre-conditions
		IType customerType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		assertThat(customerType, notNullValue());
		// operation
		final Collection<IType> resources = JavaElementsSearcher.findResourceTypes(customerType, new NullProgressMonitor());
		// verifications
		assertThat(resources.size(), equalTo(1));
	}

	@Test
	public void shouldNotRetrieveResourceInOtherType() throws CoreException {
		// pre-conditions
		IType customerType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		assertThat(customerType, notNullValue());
		// operation
		final Collection<IType> resources = JavaElementsSearcher.findResourceTypes(customerType, new NullProgressMonitor());
		// verifications
		assertThat(resources.size(), equalTo(0));
	}

	@Test
	public void shouldRetrieveAllResourceMethodsInProject() throws CoreException {
		// pre-conditions
		// operation
		final Collection<IMethod> resourceMethods = JavaElementsSearcher.findResourceMethods(javaProject,
				new HashSet<String>(), new NullProgressMonitor());
		// verifications
		// just sub resource methods with @Path annotation
		assertThat(resourceMethods.size(), equalTo(19));
	}

	@Test
	public void shouldRetrieveAllResourceMethodsInType() throws CoreException {
		// pre-conditions
		IType customerType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		assertThat(customerType, notNullValue());
		// operation
		final Collection<IMethod> resourceMethods = JavaElementsSearcher.findResourceMethods(customerType,
				new HashSet<String>(), new NullProgressMonitor());
		// verifications
		// just sub resource methods with @Path annotation
		assertThat(resourceMethods.size(), equalTo(4));
	}

	@Test
	public void shouldRetrieveAllHttpMethodsInProject() throws CoreException {
		// pre-conditions
		// operation
		final Collection<IType> httpMethods = JavaElementsSearcher.findHttpMethodTypes(javaProject,
				new NullProgressMonitor());
		// verifications: 2 custom HTTP Methods
		assertThat(httpMethods.size(), equalTo(2));
	}

	@Test
	public void shouldRetrieveOneHttpMethodsInType() throws CoreException {
		// pre-conditions
		IType fooType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		assertThat(fooType, notNullValue());
		// operation
		final Collection<IType> resourceMethods = JavaElementsSearcher.findHttpMethodTypes(fooType,
				new NullProgressMonitor());
		// verifications
		assertThat(resourceMethods.size(), equalTo(1));
	}

	@Test
	public void shouldRetrieveAllProvidersInProject() throws CoreException {
		// pre-conditions
		// operation
		final Set<IType> providerTypes = JavaElementsSearcher.findProviderTypes(javaProject,
				new NullProgressMonitor());
		// verifications
		assertThat(providerTypes.size(), equalTo(5));
	}

	@Test
	public void shouldRetrieveAllProvidersInSourceFolder() throws CoreException {
		// pre-conditions
		final IPackageFragmentRoot sourceFolder = projectMonitor.resolvePackageFragmentRoot("src/main/java");
		// operation
		final Set<IType> providerTypes = JavaElementsSearcher.findProviderTypes(sourceFolder,
				new NullProgressMonitor());
		// verifications
		assertThat(providerTypes.size(), equalTo(2));
	}

	@Test
	public void shouldRetrieveProviderWithoutAnnotation() throws CoreException {
		// pre-conditions
		final IType type = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		removeFirstOccurrenceOfCode(type, "@Provider", false);
		// operation
		final Set<IType> providerTypes = JavaElementsSearcher.findProviderTypes(type,
				new NullProgressMonitor());
		// verifications
		assertThat(providerTypes.size(), equalTo(1));
	}

	@Test
	public void shouldRetrieveProviderWithoutHierarchy() throws CoreException {
		// pre-conditions
		final IType type = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		removeFirstOccurrenceOfCode(type, "implements ExceptionMapper<EntityNotFoundException>", false);
		// operation
		final Set<IType> providerTypes = JavaElementsSearcher.findProviderTypes(type, new NullProgressMonitor());
		// verifications
		assertThat(providerTypes.size(), equalTo(1));
	}
	
	@Test
	public void shouldRetrieveProviderInSourceType() throws CoreException {
		// pre-conditions
		final IType sourceType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		// operation
		final Set<IType> providerTypes = JavaElementsSearcher
				.findProviderTypes(sourceType, new NullProgressMonitor());
		// verifications
		assertThat(providerTypes.size(), equalTo(1));
	}

	@Test
	public void shouldRetrieveProviderInSourceTypeWithMissingProviderAnnotation() throws CoreException {
		// pre-conditions
		final IType sourceType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		replaceFirstOccurrenceOfCode(sourceType, "@Provider", "", false);
		// operation
		final Set<IType> providerTypes = JavaElementsSearcher
				.findProviderTypes(sourceType, new NullProgressMonitor());
		// verifications
		assertThat(providerTypes.size(), equalTo(1));
	}

	@Test
	public void shouldRetrieveProviderInSourceTypeWithMissingTypeHierarchy() throws CoreException {
		// pre-conditions
		final IType sourceType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.providers.EntityNotFoundExceptionMapper");
		replaceFirstOccurrenceOfCode(sourceType, "implements ExceptionMapper<EntityNotFoundException>", "", false);
		// operation
		final Set<IType> providerTypes = JavaElementsSearcher
				.findProviderTypes(sourceType, new NullProgressMonitor());
		// verifications
		assertThat(providerTypes.size(), equalTo(1));
	}

	@Test
	public void shouldNotRetrieveProviderInSourceType() throws CoreException {
		// pre-conditions
		final IType sourceType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		// operation
		final Set<IType> providerTypes = JavaElementsSearcher
				.findProviderTypes(sourceType, new NullProgressMonitor());
		// verifications
		assertThat(providerTypes.size(), equalTo(0));
	}

	@Test
	public void shouldRetrieveAllApplicationsInProject() throws CoreException {
		// pre-conditions
		// operation
		final Collection<IType> applicationTypes = JavaElementsSearcher.findApplicationTypes(javaProject,
				new NullProgressMonitor());
		// verifications
		assertThat(applicationTypes.size(), equalTo(1));
	}

	@Test
	public void shouldRetrieveOneApplicationInType() throws CoreException {
		// pre-conditions
		IType restType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		assertThat(restType, notNullValue());
		// operation
		final Collection<IType> applicationTypes = JavaElementsSearcher.findApplicationTypes(restType,
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
		final Collection<IType> applications = JavaElementsSearcher.findApplicationTypes(type, new NullProgressMonitor());
		// verifications
		assertThat(applications.size(), equalTo(1));
	}

	@Test
	public void shouldRetrieveOneApplicationWithSupertypeOnlyOnScopeProject() throws CoreException {
		// pre-conditions
		final IType type = replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.RestApplication",
				javaProject, "@ApplicationPath(\"/app\")", "", false);
		// operation
		final Collection<IType> applications = JavaElementsSearcher.findApplicationTypes(type.getJavaProject(),
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
		final Collection<IType> applications = JavaElementsSearcher.findApplicationTypes(type, new NullProgressMonitor());
		// verifications
		assertThat(applications.size(), equalTo(1));
	}

	@Test
	public void shouldNotRetrieveApplicationsWithSupertypeOnlyOnScopeLibrary() throws CoreException {
		// pre-conditions
		final IPackageFragmentRoot lib = projectMonitor.resolvePackageFragmentRoot("lib/jaxrs-api-2.0.1.GA.jar");
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.RestApplication", javaProject,
				"@ApplicationPath(\"/app\")", "", false);
		// operation
		final Collection<IType> applications = JavaElementsSearcher.findApplicationTypes(lib, new NullProgressMonitor());
		// verifications
		assertThat(applications.size(), equalTo(0));
	}

	@Test
	public void shouldNotRetrieveApplicationsOnScopeOtherType() throws CoreException {
		// pre-conditions
		IType fooType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		assertThat(fooType, notNullValue());
		// operation
		final Collection<IType> applications = JavaElementsSearcher.findApplicationTypes(fooType, new NullProgressMonitor());
		// verifications
		assertThat(applications.size(), equalTo(0));
	}

	@Test
	public void shouldNotFailWhenJaxrsCoreApplicationTypeIsMissing() throws CoreException, OperationCanceledException,
			InterruptedException {
		// pre-conditions: remove Appllication from project classpath
		projectMonitor.removeClasspathEntry("jaxrs-api-2.0.1.GA.jar");
		// operation
		final Collection<IType> applications = JavaElementsSearcher.findApplicationTypes(javaProject,
				new NullProgressMonitor());
		// verifications
		assertThat(applications.size(), equalTo(1));
	}
	
	@Test
	public void shouldFindRelatedTypesForGivenHttpMethod() throws CoreException {
		// pre-conditions
		final IType fooType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final IType barResourceType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BarResource");
		final IType bazResourceType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BazResource");
		final IType customerResourceType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IType productResourceLocatorType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		assertThat(fooType, notNullValue());
		// operation
		final Collection<IType> relatedTypes = JavaElementsSearcher.findRelatedTypes(fooType, Arrays.asList(customerResourceType, productResourceLocatorType, barResourceType, bazResourceType), new NullProgressMonitor());
		// verifications
		assertThat(relatedTypes.size(), equalTo(1));
		assertThat(relatedTypes.iterator().next(), equalTo(bazResourceType));
	}
	
	@Test
	public void shouldNotFindRelatedTypes() throws CoreException {
		// pre-conditions
		final IType fooType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO");
		final IType barResourceType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.BarResource");
		final IType customerResourceType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final IType productResourceLocatorType = projectMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ProductResourceLocator");
		assertThat(fooType, notNullValue());
		// operation
		final Collection<IType> relatedTypes = JavaElementsSearcher.findRelatedTypes(fooType, Arrays.asList(customerResourceType, productResourceLocatorType, barResourceType), new NullProgressMonitor());
		// verifications
		assertThat(relatedTypes.size(), equalTo(0));
	}
	
}
