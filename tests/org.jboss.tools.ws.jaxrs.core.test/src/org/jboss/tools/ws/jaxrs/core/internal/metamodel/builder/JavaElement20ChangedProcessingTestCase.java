package org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder;

import static org.eclipse.jdt.core.IJavaElementDelta.ADDED;
import static org.eclipse.jdt.core.IJavaElementDelta.CHANGED;
import static org.eclipse.jdt.core.IJavaElementDelta.REMOVED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils.replaceAllOccurrencesOfCode;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsElementFactory;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsProvider;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.junitrules.JavaElementsUtils;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJavaMethodParameter;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsNameBinding;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsParameterAggregator;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsProvider;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

public class JavaElement20ChangedProcessingTestCase {
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

	private List<IJaxrsElement> createElement(final IType type) throws CoreException, JavaModelException {
		return JaxrsElementFactory.createElements(type, JdtUtils.parse(type, null), metamodel, null);
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
		metamodelMonitor.processEvent(compilationUnit, CHANGED);
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
		metamodelMonitor.processEvent(providerType, CHANGED);
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
		metamodelMonitor.processEvent(providerType, CHANGED);
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
		metamodelMonitor.processEvent(compilationUnit, CHANGED);
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
		metamodelMonitor.processEvent(providerType, CHANGED);
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
		metamodelMonitor.processEvent(providerType, CHANGED);
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
		metamodelMonitor.processEvent(compilationUnit, CHANGED);
		replaceAllOccurrencesOfCode(compilationUnit,
				"public class CustomJAXRS2Provider implements ContainerResponseFilter",
				"public class CustomJAXRS2Provider implements ContainerRequestFilter, ContainerResponseFilter", false);
		metamodelMonitor.processEvent(compilationUnit, CHANGED);
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
		metamodelMonitor.processEvent(compilationUnit, CHANGED);
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
		metamodelMonitor.processEvent(compilationUnit, CHANGED);
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
		metamodelMonitor.processEvent(compilationUnit, CHANGED);
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
		metamodelMonitor.processEvent(providerType, CHANGED);
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
		metamodelMonitor.processEvent(providerType, CHANGED);
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
		metamodelMonitor.processEvent(compilationUnit, CHANGED);
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
		metamodelMonitor.processEvent(providerType, CHANGED);
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
		metamodelMonitor.processEvent(providerType, CHANGED);
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
		metamodelMonitor.processEvent(compilationUnit, CHANGED);
		replaceAllOccurrencesOfCode(compilationUnit, "public class CustomJAXRS2Provider implements ReaderInterceptor",
				"public class CustomJAXRS2Provider implements ReaderInterceptor, WriterInterceptor", false);
		metamodelMonitor.processEvent(compilationUnit, CHANGED);
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
		metamodelMonitor.processEvent(compilationUnit, CHANGED);
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
		metamodelMonitor.processEvent(compilationUnit, CHANGED);
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
		metamodelMonitor.processEvent(compilationUnit, CHANGED);
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
		metamodelMonitor.processEvent(nameBindingType, CHANGED);
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
		metamodelMonitor.processEvent(nameBindingCompilationUnit, CHANGED);
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
		metamodelMonitor.processEvent(nameBindingType, CHANGED);
		// verification
		final IJaxrsProvider provider = metamodel.findProvider(nameBindingType);
		assertThat(provider, nullValue());
		assertThat(customResponseFilterWithBinding.getNameBindingAnnotations().size(), equalTo(0));
	}
	
	@Test
	public void shouldCreateParameterAggregatorWhenTypeWithFieldAdded() throws CoreException {
		metamodelMonitor.createCompilationUnit("ParameterAggregator.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"ParameterAggregator.java");
		final IType parameterAggregatorType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ParameterAggregator");
		replaceAllOccurrencesOfCode(parameterAggregatorType, "// PLACEHOLDER",
				"@PathParam(\"id1\") private String id1;", false);
		assertThat(parameterAggregatorType, notNullValue());
		// operation
		metamodelMonitor.processEvent(parameterAggregatorType, ADDED);
		// verification
		final IJaxrsParameterAggregator aggregator = metamodel.findParameterAggregator(parameterAggregatorType.getFullyQualifiedName());
		assertThat(aggregator, notNullValue());
		assertThat(aggregator.getElementKind(), equalTo(EnumElementKind.PARAMETER_AGGREGATOR));
	}

	@Test
	public void shouldCreateParameterAggregatorWhenTypeWithMethodAdded() throws CoreException {
		metamodelMonitor.createCompilationUnit("ParameterAggregator.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"ParameterAggregator.java");
		final IType parameterAggregatorType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ParameterAggregator");
		replaceAllOccurrencesOfCode(parameterAggregatorType, "// PLACEHOLDER",
				"@PathParam(\"id2\") public void setId2(String id2) {}", false);
		assertThat(parameterAggregatorType, notNullValue());
		// operation
		metamodelMonitor.processEvent(parameterAggregatorType, ADDED);
		// verification
		final IJaxrsParameterAggregator aggregator = metamodel.findParameterAggregator(parameterAggregatorType.getFullyQualifiedName());
		assertThat(aggregator, notNullValue());
		assertThat(aggregator.getElementKind(), equalTo(EnumElementKind.PARAMETER_AGGREGATOR));
	}

	@Test
	public void shouldNotCreateParameterAggregatorWhenOtherJaxrsTypeAdded() throws CoreException {
		final IType carType = metamodelMonitor.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CarResource");
		assertThat(carType, notNullValue());
		// operation
		metamodelMonitor.processEvent(carType, ADDED);
		// verification
		final IJaxrsParameterAggregator aggregator = metamodel.findParameterAggregator(carType.getFullyQualifiedName());
		assertThat(aggregator, nullValue());
	}

	@Test
	public void shouldNotCreateParameterAggregatorWhenEmptyTypeAdded() throws CoreException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("ParameterAggregator.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"ParameterAggregator.java");
		final IType parameterAggregatorType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ParameterAggregator");
		assertThat(parameterAggregatorType, notNullValue());
		// operation: type added
		metamodelMonitor.processEvent(parameterAggregatorType, ADDED);
		// verification
		final IJaxrsParameterAggregator aggregator = metamodel.findParameterAggregator(parameterAggregatorType.getFullyQualifiedName());
		assertThat(aggregator, nullValue());
	}

	@Test
	public void shouldCreateParameterAggregatorWhenAnnotationAddedOnField() throws CoreException  {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("ParameterAggregator.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"ParameterAggregator.java");
		final IType parameterAggregatorType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ParameterAggregator");
		assertThat(parameterAggregatorType, notNullValue());
		replaceAllOccurrencesOfCode(parameterAggregatorType, "// PLACEHOLDER",
				"private String id1;", false);
		assertThat(createElement(parameterAggregatorType).isEmpty(), equalTo(true));
		// operation: annotation added on field
		replaceAllOccurrencesOfCode(parameterAggregatorType, "private String id1;",
				"@PathParam(\"id1\") private String id1;", false);
		final IField field = JavaElementsUtils.getField(parameterAggregatorType, "id1");
		metamodelMonitor.processEvent(field.getAnnotations()[0], ADDED);
		// verification
		final IJaxrsParameterAggregator aggregator = metamodel.findParameterAggregator(parameterAggregatorType.getFullyQualifiedName());
		assertThat(aggregator, notNullValue());
		assertThat(aggregator.getElementKind(), equalTo(EnumElementKind.PARAMETER_AGGREGATOR));
		assertThat(aggregator.getAllFields().size(), equalTo(1));
	}
	
	@Test
	public void shouldCreateParameterAggregatorWhenAnnotatedFieldAdded() throws CoreException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("ParameterAggregator.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"ParameterAggregator.java");
		final IType parameterAggregatorType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ParameterAggregator");
		assertThat(parameterAggregatorType, notNullValue());
		assertThat(createElement(parameterAggregatorType).isEmpty(), equalTo(true));
		// operation: annotated field added 
		replaceAllOccurrencesOfCode(parameterAggregatorType, "// PLACEHOLDER",
				"@PathParam(\"id1\") private String id1;", false);
		final IField field = JavaElementsUtils.getField(parameterAggregatorType, "id1");
		metamodelMonitor.processEvent(field, ADDED);
		// verification
		final IJaxrsParameterAggregator aggregator = metamodel.findParameterAggregator(parameterAggregatorType
				.getFullyQualifiedName());
		assertThat(aggregator, notNullValue());
		assertThat(aggregator.getElementKind(), equalTo(EnumElementKind.PARAMETER_AGGREGATOR));
		assertThat(aggregator.getAllFields().size(), equalTo(1));
	}
	
	@Test
	public void shouldCreateParameterAggregatorWhenAnnotationAddedOnMethod() throws CoreException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("ParameterAggregator.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"ParameterAggregator.java");
		final IType parameterAggregatorType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ParameterAggregator");
		assertThat(parameterAggregatorType, notNullValue());
		replaceAllOccurrencesOfCode(parameterAggregatorType, "// PLACEHOLDER", "public void setId2(String id2) {}",
				false);
		assertThat(createElement(parameterAggregatorType).isEmpty(), equalTo(true));
		// operation: annotation added on method
		replaceAllOccurrencesOfCode(parameterAggregatorType, "public void setId2(String id2)",
				"@PathParam(\"id2\") public void setId2(String id2)", false);
		final IMethod method = JavaElementsUtils.getMethod(parameterAggregatorType, "setId2");
		metamodelMonitor.processEvent(method.getAnnotations()[0], ADDED);
		// verification
		final IJaxrsParameterAggregator aggregator = metamodel.findParameterAggregator(parameterAggregatorType.getFullyQualifiedName());
		assertThat(aggregator, notNullValue());
		assertThat(aggregator.getElementKind(), equalTo(EnumElementKind.PARAMETER_AGGREGATOR));
		assertThat(aggregator.getAllProperties().size(), equalTo(1));
	}
	
	@Test
	public void shouldCreateParameterAggregatorWhenAnnotatedMethodAdded() throws CoreException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("ParameterAggregator.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"ParameterAggregator.java");
		final IType parameterAggregatorType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ParameterAggregator");
		assertThat(parameterAggregatorType, notNullValue());
		assertThat(createElement(parameterAggregatorType).isEmpty(), equalTo(true));
		// operation: method added
		replaceAllOccurrencesOfCode(parameterAggregatorType, "// PLACEHOLDER",
				"@PathParam(\"id2\") public void setId2(String id2) {}", false);
		final IMethod method = JavaElementsUtils.getMethod(parameterAggregatorType, "setId2");
		metamodelMonitor.processEvent(method, ADDED);
		// verification
		final IJaxrsParameterAggregator aggregator = metamodel.findParameterAggregator(parameterAggregatorType.getFullyQualifiedName());
		assertThat(aggregator, notNullValue());
		assertThat(aggregator.getElementKind(), equalTo(EnumElementKind.PARAMETER_AGGREGATOR));
		assertThat(aggregator.getAllProperties().size(), equalTo(1));
	}
	
	@Test
	public void shouldAddElementInParameterAggregatorWhenAnnotationAddedOnField() throws CoreException  {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("ParameterAggregator.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"ParameterAggregator.java");
		final IType parameterAggregatorType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ParameterAggregator");
		replaceAllOccurrencesOfCode(parameterAggregatorType, "// PLACEHOLDER",
				"// PLACEHOLDER \n @PathParam(\"id1\") private String id1; \n private String id2;", false);
		assertThat(createElement(parameterAggregatorType).isEmpty(), equalTo(false));
		final IJaxrsParameterAggregator aggregator = metamodel.findParameterAggregator(parameterAggregatorType.getFullyQualifiedName());
		assertThat(aggregator, notNullValue());
		// operation: annotation added on field
		replaceAllOccurrencesOfCode(parameterAggregatorType,
				"private String id2;", "@PathParam(\"id2\") private String id2;", false);
		final IField field = JavaElementsUtils.getField(parameterAggregatorType, "id2");
		metamodelMonitor.processEvent(field.getAnnotations()[0], ADDED);
		// verification
		assertThat(aggregator, notNullValue());
		assertThat(aggregator.getElementKind(), equalTo(EnumElementKind.PARAMETER_AGGREGATOR));
		assertThat(aggregator.getAllFields().size(), equalTo(2));
	}

	@Test
	public void shouldAddElementInParameterAggregatorWhenAnnotatedFieldAdded() throws CoreException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("ParameterAggregator.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"ParameterAggregator.java");
		final IType parameterAggregatorType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ParameterAggregator");
		assertThat(parameterAggregatorType, notNullValue());
		replaceAllOccurrencesOfCode(parameterAggregatorType, "// PLACEHOLDER",
				"@PathParam(\"id1\") private String id1;", false);
		assertThat(createElement(parameterAggregatorType).isEmpty(), equalTo(false));
		final IJaxrsParameterAggregator aggregator = metamodel.findParameterAggregator(parameterAggregatorType.getFullyQualifiedName());
		assertThat(aggregator, notNullValue());
		// operation: annotated field added
		replaceAllOccurrencesOfCode(parameterAggregatorType, "@PathParam(\"id1\") private String id1;",
				"@PathParam(\"id1\") private String id1; \n @PathParam(\"id2\") private String id2;", false);
		final IField field = JavaElementsUtils.getField(parameterAggregatorType, "id2");
		metamodelMonitor.processEvent(field, ADDED);
		// verification
		assertThat(aggregator.getElementKind(), equalTo(EnumElementKind.PARAMETER_AGGREGATOR));
		assertThat(aggregator.getAllFields().size(), equalTo(2));
	}
	
	@Test
	public void shouldAddElementInParameterAggregatorWhenAnnotationAddedOnMethod() throws CoreException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("ParameterAggregator.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"ParameterAggregator.java");
		final IType parameterAggregatorType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ParameterAggregator");
		assertThat(parameterAggregatorType, notNullValue());
		replaceAllOccurrencesOfCode(parameterAggregatorType, "// PLACEHOLDER",
				"// PLACEHOLDER \n @PathParam(\"id2\") public void setId2(String id2) {} \n public void setId3(String id3) {}", false);
		assertThat(createElement(parameterAggregatorType).isEmpty(), equalTo(false));
		final IJaxrsParameterAggregator aggregator = metamodel.findParameterAggregator(parameterAggregatorType
				.getFullyQualifiedName());
		assertThat(aggregator, notNullValue());
		// operation: annotation added on method
		replaceAllOccurrencesOfCode(parameterAggregatorType, "public void setId3(String id3)",
				"@PathParam(\"id3\") public void setId3(String id3)", false);
		final IMethod method = JavaElementsUtils.getMethod(parameterAggregatorType, "setId3");
		metamodelMonitor.processEvent(method.getAnnotations()[0], ADDED);
		// verification
		assertThat(aggregator.getElementKind(), equalTo(EnumElementKind.PARAMETER_AGGREGATOR));
		assertThat(aggregator.getAllProperties().size(), equalTo(2));
	}
	
	@Test
	public void shouldAddElementInParameterAggregatorWhenAnnotatedMethodAdded() throws CoreException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("ParameterAggregator.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"ParameterAggregator.java");
		final IType parameterAggregatorType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ParameterAggregator");
		assertThat(parameterAggregatorType, notNullValue());
		replaceAllOccurrencesOfCode(parameterAggregatorType, "// PLACEHOLDER",
				"// PLACEHOLDER \n @PathParam(\"id2\") public void setId2(String id2) {}", false);
		assertThat(createElement(parameterAggregatorType).isEmpty(), equalTo(false));
		final IJaxrsParameterAggregator aggregator = metamodel.findParameterAggregator(parameterAggregatorType.getFullyQualifiedName());
		assertThat(aggregator, notNullValue());
		// operation: annotated method added
		replaceAllOccurrencesOfCode(parameterAggregatorType, "// PLACEHOLDER",
				"// PLACEHOLDER \n @PathParam(\"id3\") public void setId3(String id3) {}", false);
		final IMethod method = JavaElementsUtils.getMethod(parameterAggregatorType, "setId3");
		metamodelMonitor.processEvent(method, ADDED);
		// verification
		assertThat(aggregator.getElementKind(), equalTo(EnumElementKind.PARAMETER_AGGREGATOR));
		assertThat(aggregator.getAllProperties().size(), equalTo(2));
	}
	
	@Test
	public void shouldRemoveElementFromParameterAggregatorWhenAnnotationRemovedFromField() throws CoreException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("ParameterAggregator.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"ParameterAggregator.java");
		final IType parameterAggregatorType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ParameterAggregator");
		replaceAllOccurrencesOfCode(parameterAggregatorType, "// PLACEHOLDER",
				"@PathParam(\"id1\") private String id1; \n @PathParam(\"id2\") private String id2;", false);
		assertThat(createElement(parameterAggregatorType).isEmpty(), equalTo(false));
		final IJaxrsParameterAggregator aggregator = metamodel.findParameterAggregator(parameterAggregatorType
				.getFullyQualifiedName());
		assertThat(aggregator, notNullValue());
		assertThat(aggregator.getAllFields().size(), equalTo(2));
		// operation: annotation removed from field
		final IAnnotation annotation = JavaElementsUtils.getField(parameterAggregatorType, "id2").getAnnotations()[0];
		replaceAllOccurrencesOfCode(parameterAggregatorType, "@PathParam(\"id2\") private String id2;",
				"private String id2;", false);
		metamodelMonitor.processEvent(annotation, REMOVED);
		// verification
		assertThat(aggregator, notNullValue());
		assertThat(aggregator.getElementKind(), equalTo(EnumElementKind.PARAMETER_AGGREGATOR));
		assertThat(aggregator.getAllFields().size(), equalTo(1));
	}
	
	@Test
	public void shouldRemoveElementFromParameterAggregatorWhenAnnotatedFieldRemoved() throws CoreException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("ParameterAggregator.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"ParameterAggregator.java");
		final IType parameterAggregatorType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ParameterAggregator");
		replaceAllOccurrencesOfCode(parameterAggregatorType, "// PLACEHOLDER",
				"@PathParam(\"id1\") private String id1; \n @PathParam(\"id2\") private String id2;", false);
		assertThat(createElement(parameterAggregatorType).isEmpty(), equalTo(false));
		final IJaxrsParameterAggregator aggregator = metamodel.findParameterAggregator(parameterAggregatorType
				.getFullyQualifiedName());
		assertThat(aggregator, notNullValue());
		assertThat(aggregator.getAllFields().size(), equalTo(2));
		// operation: annotated field removed
		final IField field = JavaElementsUtils.getField(parameterAggregatorType, "id2");
		replaceAllOccurrencesOfCode(parameterAggregatorType, "@PathParam(\"id2\") private String id2;",
				"", false);
		metamodelMonitor.processEvent(field, REMOVED);
		// verification
		assertThat(aggregator, notNullValue());
		assertThat(aggregator.getElementKind(), equalTo(EnumElementKind.PARAMETER_AGGREGATOR));
		assertThat(aggregator.getAllFields().size(), equalTo(1));
	}
	
	@Test
	public void shouldRemoveElementFromParameterAggregatorWhenAnnotationRemovedFromMethod() throws CoreException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("ParameterAggregator.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"ParameterAggregator.java");
		final IType parameterAggregatorType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ParameterAggregator");
		assertThat(parameterAggregatorType, notNullValue());
		replaceAllOccurrencesOfCode(parameterAggregatorType, "// PLACEHOLDER",
				"@PathParam(\"id2\") public void setId2(String id2) {} \n @PathParam(\"id3\") public void setId3(String id3) {}", false);
		assertThat(createElement(parameterAggregatorType).isEmpty(), equalTo(false));
		final IJaxrsParameterAggregator aggregator = metamodel.findParameterAggregator(parameterAggregatorType
				.getFullyQualifiedName());
		assertThat(aggregator, notNullValue());
		assertThat(aggregator.getAllProperties().size(), equalTo(2));
		// operation: annotation removed from method
		final IAnnotation annotation = JavaElementsUtils.getMethod(parameterAggregatorType, "setId3").getAnnotations()[0];
		replaceAllOccurrencesOfCode(parameterAggregatorType, "@PathParam(\"id3\") public void setId3(String id3) {}",
				"public void setId3(String id3) {}", false);
		metamodelMonitor.processEvent(annotation, REMOVED);
		// verification
		assertThat(aggregator.getElementKind(), equalTo(EnumElementKind.PARAMETER_AGGREGATOR));
		assertThat(aggregator.getAllProperties().size(), equalTo(1));
	}
	
	@Test
	public void shouldRemoveElementFromParameterAggregatorWhenAnnotatedMethodRemoved() throws CoreException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("ParameterAggregator.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"ParameterAggregator.java");
		final IType parameterAggregatorType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ParameterAggregator");
		assertThat(parameterAggregatorType, notNullValue());
		replaceAllOccurrencesOfCode(parameterAggregatorType, "// PLACEHOLDER",
				"@PathParam(\"id2\") public void setId2(String id2) {} \n @PathParam(\"id3\") public void setId3(String id3) {}", false);
		assertThat(createElement(parameterAggregatorType).isEmpty(), equalTo(false));
		final IJaxrsParameterAggregator aggregator = metamodel.findParameterAggregator(parameterAggregatorType
				.getFullyQualifiedName());
		assertThat(aggregator, notNullValue());
		assertThat(aggregator.getAllProperties().size(), equalTo(2));
		// operation: annotated method removed
		final IMethod method = JavaElementsUtils.getMethod(parameterAggregatorType, "setId3");
		replaceAllOccurrencesOfCode(parameterAggregatorType, "@PathParam(\"id3\") public void setId3(String id3) {}",
				"", false);
		metamodelMonitor.processEvent(method, REMOVED);
		// verification
		assertThat(aggregator.getElementKind(), equalTo(EnumElementKind.PARAMETER_AGGREGATOR));
		assertThat(aggregator.getAllProperties().size(), equalTo(1));
	}
	
	@Test
	public void shouldKeepParameterAggregatorWhenAnnotationRemovedFromLastField() throws CoreException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("ParameterAggregator.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"ParameterAggregator.java");
		final IType parameterAggregatorType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ParameterAggregator");
		replaceAllOccurrencesOfCode(parameterAggregatorType, "// PLACEHOLDER",
				"@PathParam(\"id2\") private String id2;", false);
		assertThat(createElement(parameterAggregatorType).isEmpty(), equalTo(false));
		final IJaxrsParameterAggregator aggregator = metamodel.findParameterAggregator(parameterAggregatorType
				.getFullyQualifiedName());
		assertThat(aggregator, notNullValue());
		assertThat(aggregator.getAllFields().size(), equalTo(1));
		// operation: annotation removed from field
		final IAnnotation annotation = JavaElementsUtils.getField(parameterAggregatorType, "id2").getAnnotations()[0];
		replaceAllOccurrencesOfCode(parameterAggregatorType, "@PathParam(\"id2\") private String id2;",
				"private String id2;", false);
		metamodelMonitor.processEvent(annotation, REMOVED);
		// verification: parameter aggregator still exists, but it is empty
		final IJaxrsParameterAggregator remainingParameterAggregator = metamodel
				.findParameterAggregator(parameterAggregatorType.getFullyQualifiedName());
		assertThat(remainingParameterAggregator, notNullValue());
		assertThat(remainingParameterAggregator.getAllFields().size(), equalTo(0));
		assertThat(remainingParameterAggregator.getAllProperties().size(), equalTo(0));
	}
	
	@Test
	public void shouldKeepParameterAggregatorWhenLastAnnotatedFieldRemoved() throws CoreException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("ParameterAggregator.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"ParameterAggregator.java");
		final IType parameterAggregatorType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ParameterAggregator");
		replaceAllOccurrencesOfCode(parameterAggregatorType, "// PLACEHOLDER",
				"@PathParam(\"id2\") private String id2;", false);
		assertThat(createElement(parameterAggregatorType).isEmpty(), equalTo(false));
		final IJaxrsParameterAggregator aggregator = metamodel.findParameterAggregator(parameterAggregatorType
				.getFullyQualifiedName());
		assertThat(aggregator, notNullValue());
		assertThat(aggregator.getAllFields().size(), equalTo(1));
		// operation: annotated field removed
		final IField field = JavaElementsUtils.getField(parameterAggregatorType, "id2");
		replaceAllOccurrencesOfCode(parameterAggregatorType, "@PathParam(\"id2\") private String id2;", "", false);
		metamodelMonitor.processEvent(field, REMOVED);
		// verification: parameter aggregator still exists, but it is empty
		final IJaxrsParameterAggregator remainingParameterAggregator = metamodel
				.findParameterAggregator(parameterAggregatorType.getFullyQualifiedName());
		assertThat(remainingParameterAggregator, notNullValue());
		assertThat(remainingParameterAggregator.getAllFields().size(), equalTo(0));
		assertThat(remainingParameterAggregator.getAllProperties().size(), equalTo(0));
	}
	
	@Test
	// JAX-RS Parameter aggregator still exists, despite the fact that it is empty (well, it will exists until the next full build...)
	public void shouldKeepParameterAggregatorWhenAnnotationRemovedFromLastMethod() throws CoreException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("ParameterAggregator.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"ParameterAggregator.java");
		final IType parameterAggregatorType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ParameterAggregator");
		assertThat(parameterAggregatorType, notNullValue());
		replaceAllOccurrencesOfCode(
				parameterAggregatorType,
				"// PLACEHOLDER",
				"@PathParam(\"id2\") public void setId2(String id2) {}",
				false);
		assertThat(createElement(parameterAggregatorType).isEmpty(), equalTo(false));
		final IJaxrsParameterAggregator parameterAggregator = metamodel.findParameterAggregator(parameterAggregatorType.getFullyQualifiedName());
		final IJaxrsParameterAggregator aggregator = parameterAggregator;
		assertThat(aggregator, notNullValue());
		assertThat(aggregator.getAllProperties().size(), equalTo(1));
		// operation: annotated method removed
		final IAnnotation annotation = JavaElementsUtils.getMethod(parameterAggregatorType, "setId2").getAnnotations()[0];
		replaceAllOccurrencesOfCode(parameterAggregatorType, "@PathParam(\"id2\") public void setId2(String id2) {}",
				"public void setId2(String id2) {}", false);
		metamodelMonitor.processEvent(annotation, REMOVED);
		// verification: parameter aggregator still exists, but it is empty
		final IJaxrsParameterAggregator remainingParameterAggregator = metamodel.findParameterAggregator(parameterAggregatorType.getFullyQualifiedName());
		assertThat(remainingParameterAggregator, notNullValue());
		assertThat(remainingParameterAggregator.getAllFields().size(), equalTo(0));
		assertThat(remainingParameterAggregator.getAllProperties().size(), equalTo(0));
	}
	
	@Test
	// JAX-RS Parameter aggregator still exists, despite the fact that it is empty (well, it will exists until the next full build...)
	public void shouldKeepParameterAggregatorWhenLastAnnotatedMethodRemoved() throws CoreException {
		// pre-conditions
		metamodelMonitor.createCompilationUnit("ParameterAggregator.txt", "org.jboss.tools.ws.jaxrs.sample.services",
				"ParameterAggregator.java");
		final IType parameterAggregatorType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.ParameterAggregator");
		assertThat(parameterAggregatorType, notNullValue());
		replaceAllOccurrencesOfCode(
				parameterAggregatorType,
				"// PLACEHOLDER",
				"@PathParam(\"id2\") public void setId2(String id2) {}",
				false);
		assertThat(createElement(parameterAggregatorType).isEmpty(), equalTo(false));
		final IJaxrsParameterAggregator aggregator = metamodel.findParameterAggregator(parameterAggregatorType
				.getFullyQualifiedName());
		assertThat(aggregator, notNullValue());
		assertThat(aggregator.getAllProperties().size(), equalTo(1));
		// operation: annotated method removed
		final IMethod method = JavaElementsUtils.getMethod(parameterAggregatorType, "setId2");
		replaceAllOccurrencesOfCode(parameterAggregatorType, "@PathParam(\"id2\") public void setId2(String id2) {}",
				"", false);
		metamodelMonitor.processEvent(method, REMOVED);
		// verification: parameter aggregator still exists, but it is empty
		final IJaxrsParameterAggregator remainingParameterAggregator = metamodel.findParameterAggregator(parameterAggregatorType.getFullyQualifiedName());
		assertThat(remainingParameterAggregator, notNullValue());
		assertThat(remainingParameterAggregator.getAllFields().size(), equalTo(0));
		assertThat(remainingParameterAggregator.getAllProperties().size(), equalTo(0));
	}
	
	//@see https://issues.jboss.org/browse/JBIDE-19734
	@Test
	public void shouldNotFailWhenMethodHasWildcardParameters() throws CoreException {
		final ICompilationUnit compilationUnit = metamodelMonitor.createCompilationUnit("CarResource.txt",
				"org.jboss.tools.ws.jaxrs.sample.services", "CarResource.java");
		assertThat(compilationUnit, notNullValue());
		final JaxrsResource resource = metamodelMonitor
				.createResource("org.jboss.tools.ws.jaxrs.sample.services.CarResource");
		// operation
		replaceAllOccurrencesOfCode(compilationUnit, "Car car", "Class<?> clazz", false);
		metamodelMonitor.processEvent(compilationUnit, CHANGED);
		// verification
		final IJavaMethodParameter methodParameter = metamodelMonitor.resolveResourceMethod(resource, "create1").getJavaMethodParameters().get(0);
		assertTrue(methodParameter.getType().getTypeArguments().isEmpty());
	}
	
}
