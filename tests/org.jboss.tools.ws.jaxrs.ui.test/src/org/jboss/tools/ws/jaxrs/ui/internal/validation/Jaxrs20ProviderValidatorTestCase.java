/**
 * 
 */
package org.jboss.tools.ws.jaxrs.ui.internal.validation;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.jboss.tools.ws.jaxrs.core.junitrules.ResourcesUtils.replaceFirstOccurrenceOfCode;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.deleteJaxrsMarkers;
import static org.jboss.tools.ws.jaxrs.ui.internal.validation.ValidationUtils.findJaxrsMarkers;
import static org.junit.Assert.assertThat;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.wst.validation.ReporterHelper;
import org.eclipse.wst.validation.internal.core.ValidationException;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.jboss.tools.common.validation.ContextValidationHelper;
import org.jboss.tools.common.validation.IProjectValidationContext;
import org.jboss.tools.common.validation.ValidatorManager;
import org.jboss.tools.common.validation.internal.ProjectValidationContext;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.AbstractJaxrsBaseElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsJavaApplication;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsNameBinding;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsProvider;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.junitrules.JaxrsMetamodelMonitor;
import org.jboss.tools.ws.jaxrs.core.junitrules.TestWatcher;
import org.jboss.tools.ws.jaxrs.core.junitrules.WorkspaceSetupRule;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.ui.JBossJaxrsUIPlugin;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.ui.preferences.JaxrsPreferences;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author xcoulon
 *
 */
@SuppressWarnings("restriction")
public class Jaxrs20ProviderValidatorTestCase {
	private final IReporter reporter = new ReporterHelper(new NullProgressMonitor());
	private final ContextValidationHelper validationHelper = new ContextValidationHelper();
	private final IProjectValidationContext context = new ProjectValidationContext();
	private final ValidatorManager validatorManager = new ValidatorManager();

	protected void removeAllElementsExcept(final IJaxrsElement... elementsToKeep) throws CoreException {
		final Set<String> resourcesToKeep = new HashSet<String>();
		for (IJaxrsElement element : elementsToKeep) {
			if (element != null) {
				resourcesToKeep.add(element.getIdentifier());
			}
		}
		final List<IJaxrsElement> allElements = metamodel.getAllElements();
		for (Iterator<IJaxrsElement> iterator = allElements.iterator(); iterator.hasNext();) {
			AbstractJaxrsBaseElement element = (AbstractJaxrsBaseElement) iterator.next();
			if (element.getResource() == null || !resourcesToKeep.contains(element.getIdentifier())) {
				element.remove();
			}
		}
	}

	@ClassRule
	public static WorkspaceSetupRule workspaceSetupRule = new WorkspaceSetupRule(
			"org.jboss.tools.ws.jaxrs.tests.sampleproject2");
	@Rule
	public JaxrsMetamodelMonitor metamodelMonitor = new JaxrsMetamodelMonitor(
			"org.jboss.tools.ws.jaxrs.tests.sampleproject2", true);
	private JaxrsMetamodel metamodel = null;
	private IProject project = null;
	
	@Rule
	public TestWatcher watcher = new TestWatcher();

	@Before
	public void setup() throws CoreException {
		metamodel = metamodelMonitor.getMetamodel();
		project = metamodel.getProject();
	}

	@After
	public void resetProblemLevelPreferences() {
		final IEclipsePreferences defaultPreferences = ((IScopeContext) DefaultScope.INSTANCE)
				.getNode(JBossJaxrsUIPlugin.PLUGIN_ID);
		defaultPreferences.put(JaxrsPreferences.PROVIDER_MISSING_IMPLEMENTATION, JaxrsPreferences.ERROR);
	}

	@Test
	public void shouldNotReportProblemOnContainerRequestFilter() throws CoreException, ValidationException {
		// preconditions
		final IType providerType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomRequestFilter");
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(providerType);
		removeAllElementsExcept(provider);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation: no need to report further problems, JDT Validation
		// already reports compilation errors
		final IMarker[] providerMarkers = findJaxrsMarkers(provider);
		for (IMarker marker : providerMarkers) {
			Logger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(providerMarkers.length, equalTo(0));
	}

	@Test
	public void shouldNotReportProblemOnContainerResponseFilter() throws CoreException, ValidationException {
		// preconditions
		final IType providerType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilter");
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(providerType);
		removeAllElementsExcept(provider);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation: no need to report further problems, JDT Validation
		// already reports compilation errors
		final IMarker[] markers = findJaxrsMarkers(provider);
		for (IMarker marker : markers) {
			Logger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(0));
	}

	@Test
	public void shouldNotReportProblemOnReaderInterceptor() throws CoreException, ValidationException {
		// preconditions
		final IType providerType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomReaderInterceptor");
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(providerType);
		removeAllElementsExcept(provider);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation: no need to report further problems, JDT Validation
		// already reports compilation errors
		final IMarker[] markers = findJaxrsMarkers(provider);
		for (IMarker marker : markers) {
			Logger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(0));
	}

	@Test
	public void shouldNotReportProblemOnWriterInterceptor() throws CoreException, ValidationException {
		// preconditions
		final IType providerType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomWriterInterceptor");
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(providerType);
		removeAllElementsExcept(provider);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validateAll(project, validationHelper, context, validatorManager, reporter);
		// validation: no need to report further problems, JDT Validation
		// already reports compilation errors
		final IMarker[] markers = findJaxrsMarkers(provider);
		for (IMarker marker : markers) {
			Logger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(0));
	}

	@Test
	public void shouldReportWarningWhenPreMatchingAnnotationUsedOnAnotherProvider() throws CoreException,
			ValidationException {
		// preconditions
		final IType providerType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomWriterInterceptor");
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(providerType);
		replaceFirstOccurrenceOfCode(providerType, "@Provider", "@Provider @PreMatching", false);
		removeAllElementsExcept(provider);
		deleteJaxrsMarkers(project);
		metamodelMonitor.resetElementChangesNotifications();
		// operation
		new JaxrsMetamodelValidator().validate(ValidationUtils.toSet(provider.getResource()), project, validationHelper, context, validatorManager, reporter);
		// validation: no need to report further problems, JDT Validation
		// already reports compilation errors
		final IMarker[] markers = findJaxrsMarkers(provider);
		for (IMarker marker : markers) {
			Logger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(1));
		assertThat(markers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""),
				equalTo(JaxrsPreferences.PROVIDER_INVALID_PRE_MATCHING_ANNOTATION_USAGE));
		assertThat(markers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("'{0}'")));
	}

	@Test
	public void shouldNotReportProblemWhenApplicationHasCustomNameBindingAnnotation() throws CoreException,
			ValidationException {
		// pre-condition: remove custom Name Binding meta-annotation on
		// CustomInterceptorBinding annotation
		final IType providerType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding");
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(providerType);
		final IType nameBindingType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		final JaxrsNameBinding nameBinding = (JaxrsNameBinding) metamodel.findElement(nameBindingType);
		final IType applicationType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		replaceFirstOccurrenceOfCode(applicationType, "public class", "@CustomInterceptorBinding public class", false);
		final JaxrsJavaApplication application = (JaxrsJavaApplication) metamodel.findElement(applicationType);
		removeAllElementsExcept(provider, nameBinding, application);
		// operation: validate the resource
		new JaxrsMetamodelValidator().validate(ValidationUtils.toSet(applicationType.getResource(), provider.getResource()), project, validationHelper, context, validatorManager, reporter);
		// verification: no error on RestApplication: there's a Filter/Interceptor
		// with such a Name Binding
		final IMarker[] applicationMarkers = findJaxrsMarkers(application);
		for (IMarker marker : applicationMarkers) {
			Logger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(applicationMarkers.length, equalTo(0));
		// verification: no error on Provider: there's an Application
		// with such a Name Binding
		final IMarker[] providerMarkers = findJaxrsMarkers(provider);
		for (IMarker marker : providerMarkers) {
			Logger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(providerMarkers.length, equalTo(0));

	}

	@Test
	public void shouldNotReportProblemWhenResourceMethodHasCustomNameBindingAnnotation() throws CoreException,
			ValidationException {
		// pre-condition
		final IType providerType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding");
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(providerType);
		final IType nameBindingType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		final JaxrsNameBinding nameBinding = (JaxrsNameBinding) metamodel.findElement(nameBindingType);
		final IType customerResourceType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResource customerResource = (JaxrsResource) metamodel.findElement(customerResourceType);
		final IMethod getCustomerMethod = metamodelMonitor.resolveMethod(customerResourceType, "getCustomer");
		final JaxrsResourceMethod customerResourceMethod = customerResource.getMethods().get(
				getCustomerMethod.getHandleIdentifier());
		removeAllElementsExcept(provider, nameBinding, customerResource, customerResourceMethod);
		// operation: validate the resource
		new JaxrsMetamodelValidator().validate(ValidationUtils.toSet(provider.getResource()), project, validationHelper, context, validatorManager, reporter);
		// verification
		final IMarker[] providerMarkers = findJaxrsMarkers(provider);
		for (IMarker marker : providerMarkers) {
			Logger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(providerMarkers.length, equalTo(0));
		final IMarker[] resourceMarkers = findJaxrsMarkers(customerResource);
		for (IMarker marker : resourceMarkers) {
			Logger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(resourceMarkers.length, equalTo(0));
	}

	@Test
	public void shouldNotReportProblemWhenResourceHasCustomNameBindingAnnotation() throws CoreException,
			ValidationException {
		// pre-condition
		final IType providerType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding");
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(providerType);
		final IType nameBindingType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		final JaxrsNameBinding nameBinding = (JaxrsNameBinding) metamodel.findElement(nameBindingType);
		final IType gameResourceType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		final JaxrsResource gameResource = (JaxrsResource) metamodel.findElement(gameResourceType);
		removeAllElementsExcept(provider, nameBinding, gameResource);
		// operation: validate the resource
		new JaxrsMetamodelValidator().validate(ValidationUtils.toSet(provider.getResource()), project, validationHelper, context, validatorManager, reporter);
		// verification
		final IMarker[] providerMarkers = findJaxrsMarkers(provider);
		for (IMarker marker : providerMarkers) {
			Logger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(providerMarkers.length, equalTo(0));
		final IMarker[] resourceMarkers = findJaxrsMarkers(gameResource);
		for (IMarker marker : resourceMarkers) {
			Logger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(resourceMarkers.length, equalTo(0));
	}

	@Test
	public void shouldNotReportProblemWhenMissingNameBindingAnnotationOnMetaAnnotation() throws CoreException,
			ValidationException {
		// pre-condition: remove @NameBinding annotation on
		// CustomInterceptorBinding meta-annotation
		final IType nameBindingType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		replaceFirstOccurrenceOfCode(nameBindingType, "@NameBinding", "", false);
		final IType providerType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding");
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(providerType);
		final JaxrsNameBinding nameBinding = (JaxrsNameBinding) metamodel.findElement(nameBindingType);
		final IType gameResourceType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		final JaxrsResource gameResource = (JaxrsResource) metamodel.findElement(gameResourceType);
		removeAllElementsExcept(provider, nameBinding, gameResource);
		// operation: validate the resource
		new JaxrsMetamodelValidator().validate(ValidationUtils.toSet(nameBindingType.getResource()), project, validationHelper, context, validatorManager, reporter);
		// verification: there's no problem, the Interceptor became a Global
		// Interceptor ;-)
		final IMarker[] markers = findJaxrsMarkers(provider);
		for (IMarker marker : markers) {
			Logger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(0));
		final IMarker[] resourceMarkers = findJaxrsMarkers(gameResource);
		for (IMarker marker : resourceMarkers) {
			Logger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(resourceMarkers.length, equalTo(0));
	}

	@Test
	public void shouldReportProblemOnApplicationWhenNoProviderHasNameBindingAnnotation() throws CoreException,
			ValidationException {
		// pre-condition: remove custom Name Binding meta-annotation on
		// CustomInterceptorBinding annotation
		final IType providerType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding");
		replaceFirstOccurrenceOfCode(providerType, "@CustomInterceptorBinding", "", false);
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(providerType);
		final IType nameBindingType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		final JaxrsNameBinding nameBinding = (JaxrsNameBinding) metamodel.findElement(nameBindingType);
		final IType applicationType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.RestApplication");
		replaceFirstOccurrenceOfCode(applicationType, "public class", "@CustomInterceptorBinding public class", false);
		final JaxrsJavaApplication application = (JaxrsJavaApplication) metamodel.findElement(applicationType);
		removeAllElementsExcept(provider, nameBinding, application);
		// operation: validate the *provider* that just changed
		new JaxrsMetamodelValidator().validate(ValidationUtils.toSet(application.getResource(), provider.getResource()), project, validationHelper, context, validatorManager, reporter);
		// verification: error on *RestApplication*: no Filter/Interceptor
		// with such a Name Binding
		final IMarker[] markers = findJaxrsMarkers(application);
		for (IMarker marker : markers) {
			Logger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(1));
		assertThat(markers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""),
				equalTo(JaxrsPreferences.PROVIDER_MISSING_BINDING));
		assertThat(markers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("'{0}'")));
	}

	@Test
	public void shouldReportProblemOnResourceMethodWhenNoProviderHasNameBindingAnnotation() throws CoreException,
			ValidationException {
		// pre-condition: remove custom Name Binding meta-annotation on
		// CustomInterceptorBinding annotation
		final IType providerType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding");
		replaceFirstOccurrenceOfCode(providerType, "@CustomInterceptorBinding", "", false);
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(providerType);
		final IType nameBindingType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		final JaxrsNameBinding nameBinding = (JaxrsNameBinding) metamodel.findElement(nameBindingType);
		final IType customerResourceType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		final JaxrsResource customerResource = (JaxrsResource) metamodel.findElement(customerResourceType);
		final IMethod getCustomerMethod = metamodelMonitor.resolveMethod(customerResourceType, "getCustomer");
		final JaxrsResourceMethod customerResourceMethod = customerResource.getMethods().get(
				getCustomerMethod.getHandleIdentifier());
		removeAllElementsExcept(provider, nameBinding, customerResource, customerResourceMethod);
		// operation: validate the *provider* that just changed
		new JaxrsMetamodelValidator().validate(ValidationUtils.toSet(provider.getResource()), project, validationHelper, context, validatorManager, reporter);
		// verification: error on *Customer Resource Method*: no Filter/Interceptor
		// with such a Name Binding
		final IMarker[] markers = findJaxrsMarkers(customerResourceMethod);
		for (IMarker marker : markers) {
			Logger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(1));
		assertThat(markers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""),
				equalTo(JaxrsPreferences.PROVIDER_MISSING_BINDING));
		assertThat(markers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("'{0}'")));
	}

	@Test
	public void shouldReportProblemOnResourceWhenNoProviderHasNameBindingAnnotation() throws CoreException,
			ValidationException {
		// pre-condition: remove custom Name Binding meta-annotation on
		// CustomInterceptorBinding annotation
		final IType providerType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding");
		replaceFirstOccurrenceOfCode(providerType, "@CustomInterceptorBinding", "", false);
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(providerType);
		final IType nameBindingType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		final JaxrsNameBinding nameBinding = (JaxrsNameBinding) metamodel.findElement(nameBindingType);
		final IType gameResourceType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		final JaxrsResource gameResource = (JaxrsResource) metamodel.findElement(gameResourceType);
		removeAllElementsExcept(provider, nameBinding, gameResource);
		// operation: validate the *provider* that just changed
		new JaxrsMetamodelValidator().validate(ValidationUtils.toSet(provider.getResource(), gameResource.getResource()), project, validationHelper, context, validatorManager, reporter);
		// verification: error on *Game Resource*: no Filter/Interceptor
		// with such a Name Binding
		final IMarker[] markers = findJaxrsMarkers(gameResource);
		for (IMarker marker : markers) {
			Logger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(1));
		assertThat(markers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""),
				equalTo(JaxrsPreferences.PROVIDER_MISSING_BINDING));
		assertThat(markers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("'{0}'")));
	}

	@Test
	public void shouldReportProblemWhenNoElementHasNameBindingAnnotation() throws CoreException, ValidationException {
		// pre-condition: remove custom NameBinding annotation on
		// CustomerResource method
		final IType providerType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding");
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(providerType);
		final IType nameBindingType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		final JaxrsNameBinding nameBinding = (JaxrsNameBinding) metamodel.findElement(nameBindingType);
		final IType customerResourceType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.CustomerResource");
		replaceFirstOccurrenceOfCode(customerResourceType, "@CustomInterceptorBinding", "", false);
		final JaxrsResource customerResource = (JaxrsResource) metamodel.findElement(customerResourceType);
		removeAllElementsExcept(provider, nameBinding, customerResource);
		// operation: validate the *customer resource* that just changed
		new JaxrsMetamodelValidator().validate(ValidationUtils.toSet(customerResource.getResource(), provider.getResource()), project, validationHelper, context, validatorManager, reporter);
		// verification: error on *Provider*: no other element
		// with such a Name Binding
		final IMarker[] markers = findJaxrsMarkers(provider);
		for (IMarker marker : markers) {
			Logger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(markers.length, equalTo(1));
		assertThat(markers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""),
				equalTo(JaxrsPreferences.PROVIDER_UNUSED_BINDING));
		assertThat(markers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("'{0}'")));
	}

	@Test
	public void shouldReportProblemWhenNoProviderHasAllNameBindingAnnotations() throws CoreException,
			ValidationException {
		// pre-condition: add another @NameBinding annotation on the Resource
		final IType providerType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomResponseFilterWithBinding");
		final JaxrsProvider provider = (JaxrsProvider) metamodel.findElement(providerType);
		final IType customNameBindingType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.CustomInterceptorBinding");
		final JaxrsNameBinding customNameBinding = (JaxrsNameBinding) metamodel.findElement(customNameBindingType);
		final IType anotherNameBindingType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.interceptors.AnotherCustomInterceptorBinding");
		final JaxrsNameBinding anotherNameBinding = (JaxrsNameBinding) metamodel.findElement(anotherNameBindingType);
		final IType gameResourceType = metamodelMonitor
				.resolveType("org.jboss.tools.ws.jaxrs.sample.services.GameResource");
		replaceFirstOccurrenceOfCode(gameResourceType, "@CustomInterceptorBinding",
				"@CustomInterceptorBinding @AnotherCustomInterceptorBinding", false);
		final JaxrsResource gameResource = (JaxrsResource) metamodel.findElement(gameResourceType);
		removeAllElementsExcept(provider, customNameBinding, anotherNameBinding, gameResource);
		// operation: validate the *Game Resource* that just changed
		new JaxrsMetamodelValidator().validate(ValidationUtils.toSet(gameResource.getResource(), provider.getResource()), project, validationHelper, context, validatorManager, reporter);
		// verification: error on Game Resource: no Filter/Interceptor
		// with the *two* NameBindings
		final IMarker[] gameResourceMarkers = findJaxrsMarkers(gameResource);
		for (IMarker marker : gameResourceMarkers) {
			Logger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(gameResourceMarkers.length, equalTo(1));
		assertThat(gameResourceMarkers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""),
				equalTo(JaxrsPreferences.PROVIDER_MISSING_BINDING));
		assertThat(gameResourceMarkers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("'{0}'")));
		// verification: side effect: error on Provider because no element is
		// bound to this Name
		final IMarker[] providerMarkers = findJaxrsMarkers(provider);
		for (IMarker marker : providerMarkers) {
			Logger.debug("problem at line {}: {}", marker.getAttribute(IMarker.LINE_NUMBER),
					marker.getAttribute(IMarker.MESSAGE));
		}
		assertThat(providerMarkers.length, equalTo(1));
		assertThat(providerMarkers[0].getAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, ""),
				equalTo(JaxrsPreferences.PROVIDER_UNUSED_BINDING));
		assertThat(providerMarkers[0].getAttribute(IMarker.MESSAGE, ""), not(containsString("'{0}'")));
	}
	
	
}
