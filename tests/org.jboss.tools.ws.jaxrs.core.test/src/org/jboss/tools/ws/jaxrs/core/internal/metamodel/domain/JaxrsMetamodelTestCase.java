package org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.lang.annotation.Target;
import java.util.List;

import javax.ws.rs.HttpMethod;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.AbstractCommonTestCase;
import org.jboss.tools.ws.jaxrs.core.WorkbenchUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsHttpMethod;
import org.junit.Before;
import org.junit.Test;

public class JaxrsMetamodelTestCase extends AbstractCommonTestCase {

	private JaxrsMetamodel metamodel = null;

	final IProgressMonitor progressMonitor = new NullProgressMonitor();

	private IJaxrsHttpMethod httpMethod;

	@Before
	public void setup() throws CoreException {
		this.metamodel = JaxrsMetamodel.create(javaProject);
		IType javaType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		final Annotation annotation = JdtUtils.resolveAnnotation(javaType, JdtUtils.parse(javaType, progressMonitor),
				HttpMethod.class);
		httpMethod = new JaxrsHttpMethod(javaType, annotation, metamodel);
		metamodel.add(httpMethod);
	}

	@Test
	public void shouldGetAllHttpMethods() {
		assertThat(metamodel.getAllHttpMethods().size(), equalTo(1));
	}

	@Test
	public void shouldGetHttpMethodByType() throws CoreException {
		IType javaType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		assertThat(metamodel.getElement(javaType, IJaxrsHttpMethod.class), equalTo(httpMethod));
	}

	@Test
	public void shouldNotGetHttpMethodByType() throws CoreException {
		IType javaType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				progressMonitor);
		assertThat(metamodel.getElement(javaType), nullValue());
	}

	@Test
	public void shouldGetHttpMethodByAnnotation() throws CoreException {
		IType javaType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		final Annotation annotation = JdtUtils.resolveAnnotation(javaType, JdtUtils.parse(javaType, progressMonitor),
				javax.ws.rs.HttpMethod.class);
		assertThat((IJaxrsHttpMethod) metamodel.getElement(annotation), equalTo(httpMethod));
	}

	@Test
	public void shouldNotGetHttpMethodByAnnotation() throws CoreException {
		IType javaType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		final Annotation annotation = JdtUtils.resolveAnnotation(javaType, JdtUtils.parse(javaType, progressMonitor),
				Target.class);
		assertThat(metamodel.getElement(annotation), nullValue());
	}

	@Test
	public void shouldGetHttpMethodByCompilationUnit() throws CoreException {
		IType javaType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.FOO", javaProject,
				progressMonitor);
		final List<IJaxrsHttpMethod> httpMethods = metamodel.getElements(javaType.getCompilationUnit(),
				IJaxrsHttpMethod.class);
		assertThat(httpMethods.size(), equalTo(1));
		assertThat(httpMethods, contains(httpMethod));
	}

	@Test
	public void shouldNotGetHttpMethodByCompilationUnit() throws CoreException {
		IType javaType = JdtUtils.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource", javaProject,
				progressMonitor);
		final List<IJaxrsHttpMethod> httpMethods = metamodel.getElements(javaType.getCompilationUnit(),
				IJaxrsHttpMethod.class);
		assertThat(httpMethods.size(), equalTo(0));
	}

	@Test
	public void shouldGetHttpMethodByPackageFragmentRoot() throws CoreException {
		IPackageFragmentRoot src = WorkbenchUtils.getPackageFragmentRoot(javaProject, "src/main/java",
				new NullProgressMonitor());
		final List<IJaxrsHttpMethod> httpMethods = metamodel.getElements(src, IJaxrsHttpMethod.class);
		assertThat(httpMethods.size(), equalTo(1));
		assertThat(httpMethods, contains(httpMethod));
	}

	@Test
	public void shouldNotGetHttpMethodByPackageFragmentRoot() throws CoreException {
		IPackageFragmentRoot src = WorkbenchUtils.getPackageFragmentRoot(javaProject, "src/test/java",
				new NullProgressMonitor());
		final List<IJaxrsHttpMethod> httpMethods = metamodel.getElements(src, IJaxrsHttpMethod.class);
		assertThat(httpMethods.size(), equalTo(0));
	}
}
