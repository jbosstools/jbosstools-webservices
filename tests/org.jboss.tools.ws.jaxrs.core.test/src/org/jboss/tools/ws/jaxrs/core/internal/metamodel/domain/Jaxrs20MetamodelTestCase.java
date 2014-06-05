/**
 * 
 */
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.internal.utils.TestLogger;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.TestWatcher;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsNameBinding;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author xcoulon
 *
 */
public class Jaxrs20MetamodelTestCase {
	
	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule(
			"org.jboss.tools.ws.jaxrs.tests.sampleproject2");
	@Rule
	public JaxrsMetamodelMonitor metamodelMonitor = new JaxrsMetamodelMonitor(
			"org.jboss.tools.ws.jaxrs.tests.sampleproject2", true);
	@Rule
	public TestWatcher watcher = new TestWatcher();
	private JaxrsMetamodel metamodel = null;

	@Before
	public void setup() throws CoreException {
		metamodel = metamodelMonitor.getMetamodel();
	}
	
	@Test
	public void shouldRetrieveAllProviders() {
		// operation
		final List<IJaxrsProvider> allProviders = metamodel.findAllProviders();
		//verification
		for(IJaxrsProvider provider : allProviders) {
			TestLogger.debug(provider.toString());
		}
		assertThat(allProviders.size(), equalTo(5));
	}

	@Test
	public void shouldRetrieveAllNameBindings() {
		// operation
		final List<IJaxrsNameBinding> allNameBindings = metamodel.findAllNameBindings();
		//verification
		for(IJaxrsNameBinding nameBinding : allNameBindings) {
			TestLogger.debug(nameBinding.toString());
		}
		assertThat(allNameBindings.size(), equalTo(2));
	}

	@Test
	public void shouldRetrieveAllElements() {
		// operation
		final List<IJaxrsElement> allElements = metamodel.findAllElements();
		//verification
		for(IJaxrsElement element : allElements) {
			TestLogger.debug(element.toString());
		}
		assertThat(allElements.size(), equalTo(36));
	}
	
	@Test
	public void shouldRetrieveParamConverterProvider() throws CoreException {
		final IType paramConverterProviderType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CarParamConverterProvider");
		final JaxrsParamConverterProvider paramConverterProvider = (JaxrsParamConverterProvider) metamodel.findElement(paramConverterProviderType);
		Assert.assertNotNull("ParamConverterProvider not found", paramConverterProvider);
	}
	
}
