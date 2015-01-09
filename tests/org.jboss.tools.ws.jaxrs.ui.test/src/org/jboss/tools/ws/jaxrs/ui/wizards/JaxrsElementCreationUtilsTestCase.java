/**
 * 
 */
package org.jboss.tools.ws.jaxrs.ui.wizards;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author xcoulon
 *
 */
public class JaxrsElementCreationUtilsTestCase {
	
	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule(
			"org.jboss.tools.ws.jaxrs.tests.sampleproject");

	@Rule
	public JaxrsMetamodelMonitor metamodelMonitor = new JaxrsMetamodelMonitor(
			"org.jboss.tools.ws.jaxrs.tests.sampleproject", false);

	private JaxrsMetamodel metamodel = null;

	private IJavaProject javaProject = null;

	@Before
	public void setup() throws CoreException {
		metamodel = metamodelMonitor.getMetamodel();
		javaProject = metamodel.getJavaProject();
	}
	
	@Test
	public void shouldFindTopLevelPackage() throws CoreException {
		// given
		final IResource sourceFolder = javaProject.getProject().findMember(new Path("src").append("main").append("java"));
		final IPackageFragmentRoot packageFragmentRoot = javaProject.getPackageFragmentRoot(sourceFolder);
		// when
		final IPackageFragment projectTopLevelPackage = JaxrsElementCreationUtils.getProjectTopLevelPackage(packageFragmentRoot);
		// then
		assertThat(projectTopLevelPackage, notNullValue());
		assertThat(projectTopLevelPackage.getElementName(), equalTo("org.jboss.tools.ws.jaxrs.sample"));
	}
	
}
