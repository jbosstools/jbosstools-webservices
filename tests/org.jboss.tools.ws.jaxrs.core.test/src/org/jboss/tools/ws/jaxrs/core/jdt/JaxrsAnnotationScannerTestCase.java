package org.jboss.tools.ws.jaxrs.core.jdt;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.AbstractCommonTestCase;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsHttpMethod;
import org.junit.Ignore;
import org.junit.Test;

public class JaxrsAnnotationScannerTestCase extends AbstractCommonTestCase {

	@Test
	public void shouldRetrieveAllResourcesInTheProject() throws CoreException {
		// pre-conditions
		// operation
		final List<IType> resources = JaxrsAnnotationsScanner.findResourceTypes(javaProject, new NullProgressMonitor());
		// verifications
		assertThat(resources.size(), equalTo(7));
	}

	@Test
	public void shouldRetrieveOneResourceInType() throws CoreException {
		// pre-conditions
		IType customerType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource",
				javaProject, null);
		assertThat(customerType, notNullValue());
		// operation
		final List<IType> resources = JaxrsAnnotationsScanner
				.findResourceTypes(customerType, new NullProgressMonitor());
		// verifications
		assertThat(resources.size(), equalTo(1));
	}

	@Test
	public void shouldNotRetrieveResourceInOtherType() throws CoreException {
		// pre-conditions
		IType customerType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication",
				javaProject, null);
		assertThat(customerType, notNullValue());
		// operation
		final List<IType> resources = JaxrsAnnotationsScanner
				.findResourceTypes(customerType, new NullProgressMonitor());
		// verifications
		assertThat(resources.size(), equalTo(0));
	}

	@Test
	public void shouldRetrieveAllResourceMethodsInProject() throws CoreException {
		// pre-conditions
		// operation
		final List<IMethod> resourceMethods = JaxrsAnnotationsScanner.findResourceMethods(javaProject,new ArrayList<IJaxrsHttpMethod>(),
				new NullProgressMonitor());
		// verifications
		assertThat(resourceMethods.size(), equalTo(14)); // just sub resource methods with @Path annotation
	}

	@Test
	public void shouldRetrieveAllResourceMethodsInType() throws CoreException {
		// pre-conditions
		IType customerType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource",
				javaProject, null);
		assertThat(customerType, notNullValue());
		// operation
		final List<IMethod> resourceMethods = JaxrsAnnotationsScanner.findResourceMethods(customerType,new ArrayList<IJaxrsHttpMethod>(),
				new NullProgressMonitor());
		// verifications
		assertThat(resourceMethods.size(), equalTo(4)); // just sub resource methods with @Path annotation
	}

	@Test
	public void shouldRetrieveAllHttpMethodsInProject() throws CoreException {
		// pre-conditions
		// operation
		final List<IType> httpMethods = JaxrsAnnotationsScanner.findHttpMethodTypes(javaProject,
				new NullProgressMonitor());
		// verifications
		assertThat(httpMethods.size(), equalTo(1));
	}

	@Test
	public void shouldRetrieveOneHttpMethodsInType() throws CoreException {
		// pre-conditions
		IType fooType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject, null);
		assertThat(fooType, notNullValue());
		// operation
		final List<IType> resourceMethods = JaxrsAnnotationsScanner.findHttpMethodTypes(fooType,
				new NullProgressMonitor());
		// verifications
		assertThat(resourceMethods.size(), equalTo(1));
	}

	@Test
	@Ignore("Providers are not supported yet")
	public void shouldRetrieveProviders() {
	}

	@Test
	public void shouldRetrieveAllApplicationsInProject() throws CoreException {
		// pre-conditions
		// operation
		final List<IType> applications = JaxrsAnnotationsScanner.findApplicationTypes(javaProject,
				new NullProgressMonitor());
		// verifications
		assertThat(applications.size(), equalTo(1));
	}

	@Test
	public void shouldRetrieveOneApplicationsInType() throws CoreException {
		// pre-conditions
		IType restType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication", javaProject, null);
		assertThat(restType, notNullValue());
		// operation
		final List<IType> applications = JaxrsAnnotationsScanner.findApplicationTypes(restType,
				new NullProgressMonitor());
		// verifications
		assertThat(applications.size(), equalTo(1));
	}

	@Test
	public void shouldNotRetrieveApplicationsInOtherType() throws CoreException {
		// pre-conditions
		IType fooType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject, null);
		assertThat(fooType, notNullValue());
		// operation
		final List<IType> applications = JaxrsAnnotationsScanner.findApplicationTypes(fooType,
				new NullProgressMonitor());
		// verifications
		assertThat(applications.size(), equalTo(0));
	}
}
