/**
 * 
 */
package org.jboss.tools.ws.jaxrs.ui.cnf;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;

/**
 * @author xcoulon
 *
 */
public class UriMappingsContentProviderTestCase {

	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule("org.jboss.tools.ws.jaxrs.tests.sampleproject");
	
	@Rule
	public JaxrsMetamodelMonitor metamodelMonitor = new JaxrsMetamodelMonitor("org.jboss.tools.ws.jaxrs.tests.sampleproject", true);
	
	private JaxrsMetamodel metamodel = null;

	private IProject project = null;

	private IJavaProject javaProject = null;

	@Before
	public void setup() throws CoreException {
		this.metamodel = metamodelMonitor.getMetamodel();
		this.project = metamodel.getProject();
		this.javaProject = metamodel.getJavaProject();
	}

	@Test
	// @see https://issues.jboss.org/browse/JBIDE-18690
	public void shouldReturnTrueWhenProjectExists() {
		// given
		final UriMappingsContentProvider uriMappingsContentProvider = new UriMappingsContentProvider();
		final UriPathTemplateCategory uriPathTemplateCategory = new UriPathTemplateCategory(uriMappingsContentProvider, javaProject);
		// when
		boolean hasChildren = uriMappingsContentProvider.hasChildren(uriPathTemplateCategory);
		// then
		Assert.assertThat(hasChildren, equalTo(true));
	}

	@Test
	// @see https://issues.jboss.org/browse/JBIDE-18690
	public void shouldNotFailWhenProjectIsDeleted() throws CoreException {
		// given
		final UriMappingsContentProvider uriMappingsContentProvider = new UriMappingsContentProvider();
		final UriPathTemplateCategory uriPathTemplateCategory = new UriPathTemplateCategory(uriMappingsContentProvider, javaProject);
		// when
		ResourcesUtils.delete(project);
		boolean hasChildren = uriMappingsContentProvider.hasChildren(uriPathTemplateCategory);
		// then
		Assert.assertThat(hasChildren, equalTo(false));
	}
}
