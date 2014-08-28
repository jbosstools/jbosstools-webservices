package org.jboss.tools.ws.jaxrs.core.jdt;

import static org.hamcrest.Matchers.equalTo;
import static org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils.replaceFirstOccurrenceOfCode;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsParameterAggregator;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.search.JavaElementsSearcher;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.TestBanner;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

public class Jaxrs20ElementsSearcherTestCase {

	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule(
			"org.jboss.tools.ws.jaxrs.tests.sampleproject2");
	@Rule
	public JaxrsMetamodelMonitor metamodelMonitor = new JaxrsMetamodelMonitor(
			"org.jboss.tools.ws.jaxrs.tests.sampleproject2", false);

	@Rule
	public TestBanner testWatcher = new TestBanner();

	private IJavaProject javaProject = null;

	@Before
	public void setup() throws CoreException {
		this.javaProject = metamodelMonitor.getMetamodel().getJavaProject();
	}

	@Test
	public void shouldFindRelatedTypesForGivenResource() throws CoreException {
		// pre-conditions
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.CarResource", javaProject, "final CarParameterAggregator car", "@BeanParam final CarParameterAggregator car", false);
		final JaxrsResource carResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CarResource");
		final JaxrsParameterAggregator carParameterAggregator = metamodelMonitor.createParameterAggregator("org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		// operation
		final Collection<IType> relatedTypes = JavaElementsSearcher.findRelatedTypes(carResource.getJavaElement(), Arrays.asList(carResource.getJavaElement(), carParameterAggregator.getJavaElement()), new NullProgressMonitor());
		// verifications
		assertThat(relatedTypes.size(), equalTo(1));
		assertThat(relatedTypes.iterator().next(), equalTo(carParameterAggregator.getJavaElement()));
	}

	@Test
	public void shouldFindRelatedTypesForGivenParameterAggregator() throws CoreException {
		// pre-conditions
		replaceFirstOccurrenceOfCode("org.jboss.tools.ws.jaxrs.sample.services.CarResource", javaProject, "final CarParameterAggregator car", "@BeanParam final CarParameterAggregator car", false);
		final JaxrsResource carResource = metamodelMonitor.createResource("org.jboss.tools.ws.jaxrs.sample.services.CarResource");
		final JaxrsParameterAggregator carParameterAggregator = metamodelMonitor.createParameterAggregator("org.jboss.tools.ws.jaxrs.sample.services.CarParameterAggregator");
		// operation
		final Collection<IType> relatedTypes = JavaElementsSearcher.findRelatedTypes(carParameterAggregator.getJavaElement(), Arrays.asList(carResource.getJavaElement(), carParameterAggregator.getJavaElement()), new NullProgressMonitor());
		// verifications
		assertThat(relatedTypes.size(), equalTo(1));
		assertThat(relatedTypes.iterator().next(), equalTo(carResource.getJavaElement()));
	}
	
		
}
