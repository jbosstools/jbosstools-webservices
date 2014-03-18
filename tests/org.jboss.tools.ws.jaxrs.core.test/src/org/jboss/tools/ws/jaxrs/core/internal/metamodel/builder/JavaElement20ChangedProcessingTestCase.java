package org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder;

import static org.eclipse.jdt.core.IJavaElementDelta.CHANGED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils.replaceAllOccurrencesOfCode;
import static org.junit.Assert.assertThat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsProvider;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsNameBinding;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsProvider;
import org.jboss.tools.ws.jaxrs.core.utils.JdtUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

public class JavaElement20ChangedProcessingTestCase {
	private final static int ANY_EVENT_TYPE = 0;
	private final static int NO_FLAG = 0;
	private static final boolean PRIMARY_COPY = false;
	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule(
			"org.jboss.tools.ws.jaxrs.tests.sampleproject2");
	@Rule
	public JaxrsMetamodelMonitor metamodelMonitor = new JaxrsMetamodelMonitor(
			"org.jboss.tools.ws.jaxrs.tests.sampleproject2", false);
	private JaxrsMetamodel metamodel = null;

	@Before
	public void setup() {
		metamodel = metamodelMonitor.getMetamodel();
		assertThat(metamodel, notNullValue());
	}

	private void processEvent(IMember element, int deltaKind) throws CoreException {
		final JavaElementChangedEvent delta = new JavaElementChangedEvent(element, deltaKind, ANY_EVENT_TYPE, JdtUtils.parse(element,
				new NullProgressMonitor()), NO_FLAG);
		metamodel.processJavaElementChange(delta, new NullProgressMonitor());
	}

	private void processEvent(ICompilationUnit element, int deltaKind) throws CoreException {
		final JavaElementChangedEvent delta = new JavaElementChangedEvent(element, deltaKind, ANY_EVENT_TYPE, JdtUtils.parse(element,
				new NullProgressMonitor()), NO_FLAG);
		metamodel.processJavaElementChange(delta, new NullProgressMonitor());
	}

	@Test
	public void shouldCreateContainerRequestFilterWhenAddingSuperClassWithoutProviderAnnotation() throws CoreException {
		// pre-condition
		final ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit("CustomJAXRS2Provider.txt",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors", "CustomJAXRS2Provider.java");
		assertThat(compilationUnit, notNullValue());
		// operation
		replaceAllOccurrencesOfCode(compilationUnit, "public class CustomJAXRS2Provider",
				"public class CustomJAXRS2Provider implements ContainerRequestFilter", false);
		processEvent(compilationUnit, CHANGED);
		// verification
		final IJaxrsProvider provider = metamodel.findProvider(compilationUnit.findPrimaryType());
		assertThat(provider, notNullValue());
		assertThat(provider.getElementKind(), equalTo(EnumElementKind.CONTAINER_REQUEST_FILTER));
	}

	@Test
	public void shouldRemoveContainerRequestFilterWhenRemovingSuperClassWithoutProviderAnnotation()
			throws CoreException {
		// pre-condition
		final IType providerType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomRequestFilter");
		replaceAllOccurrencesOfCode(providerType, "@Provider", "", PRIMARY_COPY);
		final IJaxrsProvider provider = metamodelMonitor.createProvider(providerType);
		assertThat(provider, notNullValue());
		// operation
		replaceAllOccurrencesOfCode(providerType, "CustomRequestFilter implements ContainerRequestFilter",
				"CustomRequestFilter", false);
		processEvent(providerType, CHANGED);
		// verification
		assertThat(metamodel.findProvider(providerType), nullValue());
	}

	@Test
	public void shouldNotRemoveContainerRequestFilterWhenRemovingSuperClassWhenProviderAnnotationExists()
			throws CoreException {
		// pre-condition
		final IType providerType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomRequestFilter");
		final IJaxrsProvider provider = metamodelMonitor.createProvider(providerType);
		assertThat(provider, notNullValue());
		// operation
		replaceAllOccurrencesOfCode(providerType, "CustomRequestFilter implements ContainerRequestFilter",
				"CustomRequestFilter", false);
		processEvent(providerType, CHANGED);
		// verification
		assertThat(metamodel.findProvider(providerType), equalTo(provider));
		assertThat(provider.getElementKind(), equalTo(EnumElementKind.UNDEFINED_PROVIDER));
	}

	@Test
	public void shouldCreateContainerResponseFilterWhenAddingSuperClassWithoutProviderAnnotation() throws CoreException {
		// pre-condition
		final ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit("CustomJAXRS2Provider.txt",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors", "CustomJAXRS2Provider.java");
		assertThat(compilationUnit, notNullValue());
		// operation
		replaceAllOccurrencesOfCode(compilationUnit, "public class CustomJAXRS2Provider",
				"public class CustomJAXRS2Provider implements ContainerResponseFilter", false);
		processEvent(compilationUnit, CHANGED);
		// verification
		final IJaxrsProvider provider = metamodel.findProvider(compilationUnit.findPrimaryType());
		assertThat(provider, notNullValue());
		assertThat(provider.getElementKind(), equalTo(EnumElementKind.CONTAINER_RESPONSE_FILTER));
	}

	@Test
	public void shouldRemoveContainerResponseFilterWhenRemovingSuperClassWithoutProviderAnnotation()
			throws CoreException {
		// pre-condition
		final IType providerType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilter");
		replaceAllOccurrencesOfCode(providerType, "@Provider", "", PRIMARY_COPY);
		final JaxrsProvider provider = metamodelMonitor.createProvider(providerType);
		assertThat(provider, notNullValue());
		// operation
		replaceAllOccurrencesOfCode(providerType, "CustomResponseFilter implements ContainerResponseFilter",
				"CustomResponseFilter", false);
		processEvent(providerType, CHANGED);
		// verification
		assertThat(metamodel.findProvider(providerType), nullValue());
		
	}

	@Test
	public void shouldNotRemoveContainerResponseFilterWhenRemovingSuperClassWhenProviderAnnotationExists()
			throws CoreException {
		// pre-condition
		final IType providerType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilter");
		final IJaxrsProvider provider = metamodelMonitor.createProvider(providerType);
		assertThat(provider, notNullValue());
		// operation
		replaceAllOccurrencesOfCode(providerType, "CustomResponseFilter implements ContainerResponseFilter",
				"CustomResponseFilter", false);
		processEvent(providerType, CHANGED);
		// verification
		assertThat(metamodel.findProvider(providerType), equalTo(provider));
		assertThat(provider.getElementKind(), equalTo(EnumElementKind.UNDEFINED_PROVIDER));
	}

	@Test
	public void shouldCreateContainerFilterWhenAddingSuperClassWithoutProviderAnnotation() throws CoreException {
		// pre-condition
		final ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit("CustomJAXRS2Provider.txt",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors", "CustomJAXRS2Provider.java");
		assertThat(compilationUnit, notNullValue());
		// operation
		replaceAllOccurrencesOfCode(compilationUnit, "public class CustomJAXRS2Provider",
				"public class CustomJAXRS2Provider implements ContainerResponseFilter", false);
		processEvent(compilationUnit, CHANGED);
		replaceAllOccurrencesOfCode(compilationUnit,
				"public class CustomJAXRS2Provider implements ContainerResponseFilter",
				"public class CustomJAXRS2Provider implements ContainerRequestFilter, ContainerResponseFilter", false);
		processEvent(compilationUnit, CHANGED);
		// verification
		final IJaxrsProvider provider = metamodel.findProvider(compilationUnit.findPrimaryType());
		assertThat(provider, notNullValue());
		assertThat(provider.getElementKind(), equalTo(EnumElementKind.CONTAINER_FILTER));
	}

	@Test
	public void shouldBecomeContainerResponseFilterWhenRemovingSuperClassWithoutProviderAnnotation()
			throws CoreException {
		// pre-condition
		final ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit(
				"CustomJAXRS2ContainerFilter.txt", "org.jboss.tools.ws.jaxrs.sample.services.interceptors",
				"CustomJAXRS2ContainerFilter.java");
		final IJaxrsProvider provider = metamodelMonitor.createProvider(compilationUnit.findPrimaryType());
		assertThat(provider.getElementKind(), equalTo(EnumElementKind.CONTAINER_FILTER));
		// operation
		replaceAllOccurrencesOfCode(compilationUnit,
				"CustomJAXRS2ContainerFilter implements ContainerRequestFilter, ContainerResponseFilter",
				"CustomJAXRS2ContainerFilter implements ContainerResponseFilter", false);
		processEvent(compilationUnit, CHANGED);
		// verification
		final IJaxrsProvider modifiedProvider = metamodel.findProvider(compilationUnit.findPrimaryType());
		assertThat(modifiedProvider, notNullValue());
		assertThat(modifiedProvider.getElementKind(), equalTo(EnumElementKind.CONTAINER_RESPONSE_FILTER));
	}

	@Test
	public void shouldBecomeContainerRequestFilterWhenRemovingSuperClassWithoutProviderAnnotation()
			throws CoreException {
		// pre-condition
		final ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit(
				"CustomJAXRS2ContainerFilter.txt", "org.jboss.tools.ws.jaxrs.sample.services.interceptors",
				"CustomJAXRS2ContainerFilter.java");
		final IJaxrsProvider provider = metamodelMonitor.createProvider(compilationUnit.findPrimaryType());
		assertThat(provider.getElementKind(), equalTo(EnumElementKind.CONTAINER_FILTER));
		// operation
		replaceAllOccurrencesOfCode(compilationUnit,
				"CustomJAXRS2ContainerFilter implements ContainerRequestFilter, ContainerResponseFilter",
				"CustomJAXRS2ContainerFilter implements ContainerRequestFilter", false);
		processEvent(compilationUnit, CHANGED);
		// verification
		final IJaxrsProvider modifiedProvider = metamodel.findProvider(compilationUnit.findPrimaryType());
		assertThat(modifiedProvider, notNullValue());
		assertThat(modifiedProvider.getElementKind(), equalTo(EnumElementKind.CONTAINER_REQUEST_FILTER));
	}

	@Test
	public void shouldCreateReaderInterceptorWhenAddingSuperClassWithoutProviderAnnotation() throws CoreException {
		// pre-condition
		final ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit("CustomJAXRS2Provider.txt",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors", "CustomJAXRS2Provider.java");
		assertThat(compilationUnit, notNullValue());
		// operation
		replaceAllOccurrencesOfCode(compilationUnit, "public class CustomJAXRS2Provider",
				"public class CustomJAXRS2Provider implements ReaderInterceptor", false);
		processEvent(compilationUnit, CHANGED);
		// verification
		final IJaxrsProvider provider = metamodel.findProvider(compilationUnit.findPrimaryType());
		assertThat(provider, notNullValue());
		assertThat(provider.getElementKind(), equalTo(EnumElementKind.ENTITY_READER_INTERCEPTOR));
	}

	@Test
	public void shouldRemoveReaderInterceptorWhenRemovingSuperClassWithoutProviderAnnotation() throws CoreException {
		// pre-condition
		final IType providerType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomReaderInterceptor");
		replaceAllOccurrencesOfCode(providerType, "@Provider", "", PRIMARY_COPY);
		final IJaxrsProvider provider = metamodelMonitor.createProvider(providerType);
		assertThat(provider.getElementKind(), equalTo(EnumElementKind.ENTITY_READER_INTERCEPTOR));
		// operation
		replaceAllOccurrencesOfCode(providerType, "CustomReaderInterceptor implements ReaderInterceptor",
				"CustomReaderInterceptor", false);
		processEvent(providerType, CHANGED);
		// verification
		assertThat(metamodel.findProvider(providerType), nullValue());
		assertThat(provider.getElementKind(), equalTo(EnumElementKind.UNDEFINED_PROVIDER));
	}

	@Test
	public void shouldNotRemoveReaderInterceptorWhenRemovingSuperClassWhenProviderAnnotationExists()
			throws CoreException {
		// pre-condition
		final IType providerType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomReaderInterceptor");
		final IJaxrsProvider provider = metamodelMonitor.createProvider(providerType);
		assertThat(provider.getElementKind(), equalTo(EnumElementKind.ENTITY_READER_INTERCEPTOR));
		// operation
		replaceAllOccurrencesOfCode(providerType, "CustomReaderInterceptor implements ReaderInterceptor",
				"CustomReaderInterceptor", false);
		processEvent(providerType, CHANGED);
		// verification
		assertThat(metamodel.findProvider(providerType), equalTo(provider));
		assertThat(provider.getElementKind(), equalTo(EnumElementKind.UNDEFINED_PROVIDER));
	}

	@Test
	public void shouldCreateWriterInterceptorWhenAddingSuperClassWithoutProviderAnnotation() throws CoreException {
		// pre-condition
		final ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit("CustomJAXRS2Provider.txt",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors", "CustomJAXRS2Provider.java");
		assertThat(compilationUnit, notNullValue());
		// operation
		replaceAllOccurrencesOfCode(compilationUnit, "public class CustomJAXRS2Provider",
				"public class CustomJAXRS2Provider implements WriterInterceptor", false);
		processEvent(compilationUnit, CHANGED);
		// verification
		final IJaxrsProvider provider = metamodel.findProvider(compilationUnit.findPrimaryType());
		assertThat(provider, notNullValue());
		assertThat(provider.getElementKind(), equalTo(EnumElementKind.ENTITY_WRITER_INTERCEPTOR));
	}

	@Test
	public void shouldRemoveWriterInterceptorWhenRemovingSuperClassWithoutProviderAnnotation() throws CoreException {
		// pre-condition
		final IType providerType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomWriterInterceptor");
		replaceAllOccurrencesOfCode(providerType, "@Provider", "", PRIMARY_COPY);
		final IJaxrsProvider provider = metamodelMonitor.createProvider(providerType);
		assertThat(provider.getElementKind(), equalTo(EnumElementKind.ENTITY_WRITER_INTERCEPTOR));
		// operation
		replaceAllOccurrencesOfCode(providerType, "CustomWriterInterceptor implements WriterInterceptor",
				"CustomWriterInterceptor", false);
		processEvent(providerType, CHANGED);
		// verification
		assertThat(metamodel.findProvider(providerType), nullValue());
	}

	@Test
	public void shouldNotRemoveWriterInterceptorWhenRemovingSuperClassWhenProviderAnnotationExists()
			throws CoreException {
		// pre-condition
		final IType providerType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomWriterInterceptor");
		final IJaxrsProvider provider = metamodelMonitor.createProvider(providerType);
		assertThat(provider, notNullValue());
		// operation
		replaceAllOccurrencesOfCode(providerType, "CustomWriterInterceptor implements WriterInterceptor",
				"CustomWriterInterceptor", false);
		processEvent(providerType, CHANGED);
		// verification
		assertThat(metamodel.findProvider(providerType), equalTo(provider));
		assertThat(provider.getElementKind(), equalTo(EnumElementKind.UNDEFINED_PROVIDER));
	}

	@Test
	public void shouldCreateEntityInterceptorWhenAddingSuperClassWithoutProviderAnnotation() throws CoreException {
		// pre-condition
		final ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit("CustomJAXRS2Provider.txt",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors", "CustomJAXRS2Provider.java");
		assertThat(compilationUnit, notNullValue());
		// operation
		replaceAllOccurrencesOfCode(compilationUnit, "public class CustomJAXRS2Provider",
				"public class CustomJAXRS2Provider implements ReaderInterceptor", false);
		processEvent(compilationUnit, CHANGED);
		replaceAllOccurrencesOfCode(compilationUnit, "public class CustomJAXRS2Provider implements ReaderInterceptor",
				"public class CustomJAXRS2Provider implements ReaderInterceptor, WriterInterceptor", false);
		processEvent(compilationUnit, CHANGED);
		// verification
		final IJaxrsProvider provider = metamodel.findProvider(compilationUnit.findPrimaryType());
		assertThat(provider, notNullValue());
		assertThat(provider.getElementKind(), equalTo(EnumElementKind.ENTITY_INTERCEPTOR));
	}

	@Test
	public void shouldBecomeReaderInterceptorWhenRemovingSuperClassWithoutProviderAnnotation() throws CoreException {
		// pre-condition
		final ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit(
				"CustomJAXRS2EntityInterceptor.txt", "org.jboss.tools.ws.jaxrs.sample.services.interceptors",
				"CustomJAXRS2EntityInterceptor.java");
		final IJaxrsProvider provider = metamodelMonitor.createProvider(compilationUnit.findPrimaryType());
		assertThat(provider, notNullValue());
		assertThat(provider.getElementKind(), equalTo(EnumElementKind.ENTITY_INTERCEPTOR));
		// operation
		replaceAllOccurrencesOfCode(compilationUnit,
				"CustomJAXRS2EntityInterceptor implements ReaderInterceptor, WriterInterceptor",
				"CustomJAXRS2EntityInterceptor implements ReaderInterceptor", false);
		processEvent(compilationUnit, CHANGED);
		// verification
		final IJaxrsProvider modifiedProvider = metamodel.findProvider(compilationUnit.findPrimaryType());
		assertThat(modifiedProvider, notNullValue());
		assertThat(modifiedProvider.getElementKind(), equalTo(EnumElementKind.ENTITY_READER_INTERCEPTOR));
	}

	@Test
	public void shouldBecomeWriterInterceptorWhenRemovingSuperClassWithoutProviderAnnotation() throws CoreException {
		// pre-condition
		final ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit(
				"CustomJAXRS2EntityInterceptor.txt", "org.jboss.tools.ws.jaxrs.sample.services.interceptors",
				"CustomJAXRS2EntityInterceptor.java");
		final IJaxrsProvider provider = metamodelMonitor.createProvider(compilationUnit.findPrimaryType());
		assertThat(provider, notNullValue());
		assertThat(provider.getElementKind(), equalTo(EnumElementKind.ENTITY_INTERCEPTOR));
		// operation
		replaceAllOccurrencesOfCode(compilationUnit,
				"CustomJAXRS2EntityInterceptor implements ReaderInterceptor, WriterInterceptor",
				"CustomJAXRS2EntityInterceptor implements WriterInterceptor", false);
		processEvent(compilationUnit, CHANGED);
		// verification
		final IJaxrsProvider modifiedProvider = metamodel.findProvider(compilationUnit.findPrimaryType());
		assertThat(modifiedProvider, notNullValue());
		assertThat(modifiedProvider.getElementKind(), equalTo(EnumElementKind.ENTITY_WRITER_INTERCEPTOR));
	}

	@Test
	public void shouldCreateNameBindingWhenAddingMetaAnnotation() throws CoreException {
		// pre-condition
		final ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit("CustomJAXRS2NameBinding.txt",
				"org.jboss.tools.ws.jaxrs.sample.services.interceptors", "CustomJAXRS2NameBinding.java");
		assertThat(compilationUnit, notNullValue());
		// operation
		replaceAllOccurrencesOfCode(compilationUnit, "public @interface CustomJAXRS2NameBinding",
				"@NameBinding public @interface CustomJAXRS2NameBinding", false);
		processEvent(compilationUnit, CHANGED);
		// verification
		final IJaxrsNameBinding nameBinding = metamodel.findNameBinding(compilationUnit.findPrimaryType().getFullyQualifiedName());
		assertThat(nameBinding, notNullValue());
	}

	@Test
	public void shouldRemoveNameBindingWhenRemovingMetaAnnotation() throws CoreException {
		// pre-condition
		final IType nameBindingType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		assertThat(nameBindingType, notNullValue());
		// operation
		replaceAllOccurrencesOfCode(nameBindingType, "@NameBinding", "", false);
		processEvent(nameBindingType, CHANGED);
		// verification
		final IJaxrsNameBinding nameBinding = metamodel.findNameBinding(nameBindingType.getFullyQualifiedName());
		assertThat(nameBinding, nullValue());
	}

	@Test
	public void shouldAddNameBindingAnnotationOnProvidersWhenAddingMetaAnnotation() throws CoreException {
		// pre-condition: custom annotation already exists but is not an
		// @NameBinding yet.
		final ICompilationUnit nameBindingCompilationUnit = metamodelMonitor.createCompilationUnit(
				"CustomJAXRS2NameBinding.txt", "org.jboss.tools.ws.jaxrs.sample.services.interceptors",
				"CustomJAXRS2NameBinding.java");
		assertThat(nameBindingCompilationUnit, notNullValue());
		// apply this annotation on 2 providers
		final IType readerInterceptorType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomReaderInterceptor");
		assertThat(readerInterceptorType, notNullValue());
		replaceAllOccurrencesOfCode(readerInterceptorType, "public class CustomReaderInterceptor",
				"@CustomJAXRS2NameBinding public class CustomReaderInterceptor", false);
		final IJaxrsProvider readerInterceptor = metamodelMonitor.createProvider(readerInterceptorType);
		assertThat(readerInterceptor.getNameBindingAnnotations().size(), equalTo(0));
		final IType writerInterceptorType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomWriterInterceptor");
		assertThat(writerInterceptorType, notNullValue());
		replaceAllOccurrencesOfCode(writerInterceptorType, "public class CustomWriterInterceptor",
				"@CustomJAXRS2NameBinding public class CustomWriterInterceptor", false);
		final IJaxrsProvider writerInterceptor = metamodelMonitor.createProvider(writerInterceptorType);
		assertThat(writerInterceptor.getNameBindingAnnotations().size(), equalTo(0));
		// operation: add the @NameBinding annotation
		replaceAllOccurrencesOfCode(nameBindingCompilationUnit, "public @interface CustomJAXRS2NameBinding",
				"@NameBinding @interface class CustomJAXRS2NameBinding", false);
		processEvent(nameBindingCompilationUnit, CHANGED);
		// verification: NameBinding exist in metamodel and on the 2 providers
		final IJaxrsNameBinding nameBinding = metamodel.findNameBinding(nameBindingCompilationUnit.findPrimaryType().getFullyQualifiedName());
		assertThat(nameBinding, notNullValue());
		assertThat(nameBinding.getElementKind(), equalTo(EnumElementKind.NAME_BINDING));
		assertThat(readerInterceptor.getNameBindingAnnotations().size(), equalTo(1));
		assertThat(writerInterceptor.getNameBindingAnnotations().size(), equalTo(1));
	}

	@Test
	public void shouldRemoveNameBindingAnnotationOnProvidersWhenRemovingMetaAnnotation() throws CoreException {
		// pre-condition: NameBinding already exists and is referenced on a provider
		final IType nameBindingType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		metamodelMonitor.createNameBinding(nameBindingType);
		assertThat(nameBindingType, notNullValue());
		final IType customResponseFilterWithBindingType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding");
		final IJaxrsProvider customResponseFilterWithBinding = metamodelMonitor.createProvider(customResponseFilterWithBindingType);
		assertThat(customResponseFilterWithBinding.getNameBindingAnnotations().size(), equalTo(1));
		// operation: remove the @NameBinding annotation
		replaceAllOccurrencesOfCode(nameBindingType, "@NameBinding", "", false);
		processEvent(nameBindingType, CHANGED);
		// verification
		final IJaxrsProvider provider = metamodel.findProvider(nameBindingType);
		assertThat(provider, nullValue());
		assertThat(customResponseFilterWithBinding.getNameBindingAnnotations().size(), equalTo(0));
	}
}
