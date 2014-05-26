/******************************************************************************* 
 * Copyright (c) 2008 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.jaxrs.ui.internal.validation;

import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jface.text.IRegion;
import org.eclipse.wst.validation.internal.core.ValidationException;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.eclipse.wst.validation.internal.provisional.core.IValidationContext;
import org.jboss.tools.common.validation.ContextValidationHelper;
import org.jboss.tools.common.validation.EditorValidationContext;
import org.jboss.tools.common.validation.IAsYouTypeValidator;
import org.jboss.tools.common.validation.IPreferenceInfo;
import org.jboss.tools.common.validation.IProjectValidationContext;
import org.jboss.tools.common.validation.IValidatingProjectTree;
import org.jboss.tools.common.validation.IValidator;
import org.jboss.tools.common.validation.PreferenceInfoManager;
import org.jboss.tools.common.validation.TempMarkerManager;
import org.jboss.tools.common.validation.ValidatorManager;
import org.jboss.tools.ws.jaxrs.core.configuration.ProjectNatureUtils;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsMetamodelBuilder;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.AbstractJaxrsBaseElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsShadowElementsCache;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsApplication;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsProvider;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResource;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsMetamodelLocator;
import org.jboss.tools.ws.jaxrs.ui.JBossJaxrsUIPlugin;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.ui.preferences.JaxrsPreferences;

/**
 * JAX-RS Metamodel Validator. Relies on delegates to validate each category of
 * element.
 * 
 * @author Xavier Coulon
 * 
 */
@SuppressWarnings("restriction")
public class JaxrsMetamodelValidator extends TempMarkerManager implements IValidator, IAsYouTypeValidator,
		IMarkerManager {
	/** ID of the Preference Page. */
	private static final String PREFERENCE_PAGE_ID = "org.jboss.tools.ws.jaxrs.ui.preferencePages.JaxrsValidatorPreferencePage";
	/** ID of the Property Page. */
	private static final String PROPERTY_PAGE_ID = "org.jboss.tools.ws.jaxrs.ui.propertyPages.JaxrsValidatorPreferencePage";
	/** The JAX-RS Validator ID. */
	public static final String ID = "org.jboss.tools.ws.jaxrs.JaxrsMetamodelValidator"; //$NON-NLS-1$
	/** The name of the message bundle. */
	private static final String BUNDLE_NAME = JaxrsMetamodelValidator.class.getPackage().getName() + ".messages";
	/** The custom 'JAX-RS Problem' problem marker id. */
	public static final String JAXRS_PROBLEM_MARKER_ID = "org.jboss.tools.ws.jaxrs.metamodelMarker"; 
	/** The type of JAX-RS problem. */
	public static final String JAXRS_PROBLEM_TYPE = "problemType";

	/**
	 * Constructor.
	 */
	public JaxrsMetamodelValidator() {
		super.setProblemType(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID);
	}

	/**
	 * {@inheritDoc} 
	 */
	public boolean isEnabled(final IProject project) {
		return JaxrsPreferences.isValidationEnabled(project);
	}

	/**
	 * {@inheritDoc} 
	 */
	public boolean shouldValidate(final IProject project) {
		try {
			return project.isAccessible() && project.hasNature(ProjectNatureUtils.JAXRS_NATURE_ID)
					&& isEnabled(project);
		} catch (CoreException e) {
			Logger.error("Failed to check if JAX-RS validation is required for project '" + project.getName() + "'", e);
		}
		Logger.debug("*** Skipping JAX-RS validation for project {}", project.getName());
		return false;
	}

	/**
	 * {@inheritDoc} 
	 */
	public IStatus validate(final Set<IFile> changedFiles, final IProject project, final ContextValidationHelper validationHelper,
			final IProjectValidationContext context, final ValidatorManager manager, final IReporter reporter) throws ValidationException {
		final long startTime = System.currentTimeMillis();
		init(project, validationHelper, context, manager, reporter);
		setAsYouTypeValidation(false);
		try {
			if (!changedFiles.isEmpty()) {
				Logger.debug("*** Validating project {} after files {} changed... ***", project.getName(),
						changedFiles.toString());
				final JaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(project);
				// prevent failure in case validation would be called at
				// workbench startup, even before metamodel is built.
				if (metamodel != null) {
					final int previousProblemLevel = metamodel.getProblemLevel();
					// validate each JAX-RS element individually
					final List<IResource> allResources = completeValidationSet(metamodel,
							changedFiles.toArray(new IFile[changedFiles.size()]));
					for (IResource changedResource : allResources) {
						validate(changedResource, metamodel, reporter);
					}
					// validate at the metamodel level for cross-elements
					// validation
					//validate(metamodel);
					final int currentProblemLevel = metamodel.getProblemLevel();
					if (currentProblemLevel != previousProblemLevel) {
						Logger.debug("Informing metamodel that problem level changed from {} to {}",
								previousProblemLevel, currentProblemLevel);
						metamodel.notifyMetamodelProblemLevelChanged();
					}
				}
			} else {
				Logger.debug("*** Validating full project {} because no file changed... ***", project.getName());
				validateAll(project, validationHelper, context, manager, reporter);
			}
		} catch (CoreException e) {
			Logger.error("Failed to validate changed files " + changedFiles + " in project " + project, e);
		} finally {
			final long endTime = System.currentTimeMillis();
			Logger.debug("Validation done in {} ms.", (endTime - startTime));
		}
		return Status.OK_STATUS;
	}

	/**
	 * Completes the list of resources to validate by adding those that might be impacted by the given changedResources.
	 * 
	 * @param metamodel
	 *            the JAX-RS Metamodel
	 * @param changedResources
	 *            the resources that initially changed
	 * @return all resources that should be validated, sorted in such a manner that if the resources contain one or more
	 *         {@link IProject}s, those later ones will be at the end of the result list.
	 * @throws CoreException
	 */
	private List<IResource> completeValidationSet(final JaxrsMetamodel metamodel, final IFile... changedResources)
			throws CoreException {
		final Set<IResource> resources = new HashSet<IResource>();
		// add all given changed resources
		resources.addAll(Arrays.asList(changedResources));
		final Set<EnumElementKind> elementKindChanges = analyzeChangeResources(metamodel, changedResources);
		boolean applicationsChanged = elementKindChanges.contains(APPLICATION_JAVA) || elementKindChanges.contains(APPLICATION_WEBXML);
		boolean resourcesChanged = elementKindChanges.contains(ROOT_RESOURCE) || elementKindChanges.contains(SUBRESOURCE) || elementKindChanges.contains(SUBRESOURCE_LOCATOR) || elementKindChanges.contains(UNDEFINED_RESOURCE);
		boolean filtersChanged = elementKindChanges.contains(CONTAINER_FILTER) || elementKindChanges.contains(CONTAINER_REQUEST_FILTER) || elementKindChanges.contains(CONTAINER_RESPONSE_FILTER) || elementKindChanges.contains(UNDEFINED_PROVIDER);
		boolean interceptorsChanged = elementKindChanges.contains(ENTITY_INTERCEPTOR) || elementKindChanges.contains(ENTITY_READER_INTERCEPTOR) || elementKindChanges.contains(ENTITY_WRITER_INTERCEPTOR);
		boolean nameBindingChanged = elementKindChanges.contains(NAME_BINDING);
		
		// if there was an Application, add all other Applications and the
		// project (to check for
		// duplicate/overrides)
		if (applicationsChanged) {
			resources.addAll(getApplicationUnderlyingResources(metamodel));
			resources.add(metamodel.getProject());
		}
		// if there are Applications, Resources (incl. ResourceMethods), then
		// also include Filters and Interceptors (to check for NameBindings).
		if (applicationsChanged || resourcesChanged) {
			resources.addAll(getFiltersAndInterceptorsUnderlyingResources(metamodel));
		}
		// if there are Filters or Interceptors, add all Applications, Resources
		// (incl. ResourceMethods) (to check for NameBindings).
		if (filtersChanged || interceptorsChanged) {
			resources.addAll(getApplicationUnderlyingResources(metamodel));
			resources.addAll(getResourceUnderlyingResources(metamodel));
		}
		// if there was a NameBinding, add all Applications, Resources (incl.
		// ResourceMethods), Filters and Interceptors (to check for
		// NameBindings).
		if (nameBindingChanged) {
			resources.addAll(getApplicationUnderlyingResources(metamodel));
			resources.addAll(getFiltersAndInterceptorsUnderlyingResources(metamodel));
			resources.addAll(getResourceUnderlyingResources(metamodel));
		}
		// put the result in a list that will be sorted
		final ArrayList<IResource> result = new ArrayList<IResource>(resources);
		Collections.sort(result, new Comparator<IResource>() {
			@Override
			public int compare(final IResource a, final IResource b) {
				return a.getType() - b.getType();
			}
		});
		return result;
	}

	/**
	 * Analyzes the changed resources in comparison with their known corresponding {@link EnumElementKind} in the {@link JaxrsShadowElementsCache}.
	 * @param changedResources the resource that changed
	 * @return the set of element types that changed.
	 */
	private Set<EnumElementKind> analyzeChangeResources(final JaxrsMetamodel metamodel, final IFile[] changedResources) {
		final Set<EnumElementKind> elementKindChanges = new HashSet<EnumElementKind>();
		for(IFile changedResource : changedResources) {
			// retrieve the previous known element kind associated with this resource
			final EnumElementKind previousElementKind = metamodel.getShadowElementKind(changedResource);
			// now, see what the metamodel has for this resource
			final IJaxrsElement currentElement = metamodel.findElement(changedResource);
			// now, let's add the data we have: both the old and the new, in case of changes (addition, deletion and even change...)
			if(previousElementKind != null) {
				elementKindChanges.add(previousElementKind);
			}
			if(currentElement != null) {
				elementKindChanges.add(currentElement.getElementKind());
			}
			// let's update the cache for the resource, from the current element
			if(currentElement == null) {
				metamodel.removeShadowedElement(changedResource);
			} else {
				metamodel.addShadowedElement(currentElement);
			}
		}
		return elementKindChanges;
	}

	/**
	 * @return all underlying {@link IResource}s of the {@link IJaxrsProvider}s
	 *         in the given {@link JaxrsMetamodel}, if they are
	 *         {@code Container Filters} or {@code Interceptors}.
	 * @param metamodel
	 *            the metamodel to use
	 */
	private Set<IResource> getFiltersAndInterceptorsUnderlyingResources(final JaxrsMetamodel metamodel) {
		final Set<IResource> providerResources = new HashSet<IResource>();
		final List<IJaxrsProvider> providers = metamodel.findAllProviders();
		for (IJaxrsProvider provider : providers) {
			switch (provider.getElementKind()) {
			case CONTAINER_REQUEST_FILTER:
			case CONTAINER_RESPONSE_FILTER:
			case CONTAINER_FILTER:
			case ENTITY_READER_INTERCEPTOR:
			case ENTITY_WRITER_INTERCEPTOR:
			case ENTITY_INTERCEPTOR:
				providerResources.add(provider.getResource());
				break;
			default:
				break;
			}
		}
		return providerResources;
	}

	/**
	 * @return all underlying {@link IResource}s of the
	 *         {@link IJaxrsApplication}s in the given {@link JaxrsMetamodel}.
	 * @param metamodel
	 *            the metamodel to use
	 */
	private Set<IResource> getApplicationUnderlyingResources(final JaxrsMetamodel metamodel) {
		Logger.debug("Adding all JAX-RS Applications and project in the set of files to validate...");
		final Set<IResource> applicationResources = new HashSet<IResource>();
		final List<IJaxrsApplication> applications = metamodel.findAllApplications();
		for (IJaxrsApplication application : applications) {
			applicationResources.add(application.getResource());
		}
		return applicationResources;
	}

	/**
	 * @return all underlying {@link IResource}s of the {@link IJaxrsResource}s
	 *         in the given {@link JaxrsMetamodel}.
	 * @param metamodel
	 *            the metamodel to use
	 */
	private Set<IResource> getResourceUnderlyingResources(final JaxrsMetamodel metamodel) {
		Logger.debug("Adding all JAX-RS Resources (and children) and project in the set of files to validate...");
		final Set<IResource> resourceResources = new HashSet<IResource>();
		final List<IJaxrsResource> resources = metamodel.findAllResources();
		for (IJaxrsResource resource : resources) {
			resourceResources.add(resource.getResource());
		}
		return resourceResources;
	}

	/**
	 * Validate the given {@link IResource} in the given {@link JaxrsMetamodel}
	 * and reports
	 * 
	 * @param changedResource
	 *            the changed resource
	 * @param metamodel
	 *            the metamodel
	 * @param reporter
	 *            the problem reportoe
	 * @throws CoreException
	 */
	private void validate(final IResource changedResource, final JaxrsMetamodel metamodel, final IReporter reporter) {
		if (reporter.isCancelled() || !changedResource.isAccessible()) {
			return;
		}
		displaySubtask(JaxrsValidationMessages.VALIDATING_RESOURCE, new String[] {
				changedResource.getProject().getName(), changedResource.getName() });
		try {
			if (metamodel != null) {
				if(changedResource.getType() == IResource.PROJECT) {
					validate(metamodel);
				} else {
					final List<IJaxrsElement> elements = metamodel.findElements(changedResource);
					// if no (more) JAX-RS element matches the resource to validate,
					// then make
					// sure no JAX-RS Problem marker remains on that resource
					if (elements.isEmpty()) {
						removeMarkers(metamodel, changedResource);
					} else {
						for (IJaxrsElement element : elements) {
							validate(element);
						}
					}
				}
			}
		} catch (CoreException e) {
			Logger.error("Failed to validate the resource change", e);
		}
	}

	@Override
	public void validate(final org.eclipse.wst.validation.internal.provisional.core.IValidator validatorManager,
			final IProject rootProject, final Collection<IRegion> dirtyRegions, final IValidationContext helper,
			final IReporter reporter, final EditorValidationContext validationContext,
			final IProjectValidationContext projectContext, final IFile changedFile) {
		final long startTime = System.currentTimeMillis();
		Logger.debug("*** Validating project {} after file {} changed... ***", changedFile.getProject().getName(),
				changedFile.getFullPath());
		final ContextValidationHelper validationHelper = new ContextValidationHelper();
		validationHelper.setProject(rootProject);
		validationHelper.setValidationContextManager(validationContext);
		init(rootProject, validationHelper, projectContext, validatorManager, reporter);
		setAsYouTypeValidation(false);
		this.document = validationContext.getDocument();
		displaySubtask(JaxrsValidationMessages.VALIDATING_RESOURCE, new String[] { changedFile.getProject().getName(),
				changedFile.getName() });
		try {
			final JaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(changedFile.getProject());
			if (metamodel != null) {
				final List<IResource> allResources = completeValidationSet(metamodel, changedFile);
				for (IResource changedResource : allResources) {
					validate(changedResource, metamodel, reporter);
				}
				// validate at the metamodel level for cross-elements validation
				validate(metamodel);
			}
		} catch (CoreException e) {
			Logger.error(
					"Failed to validate changed file " + changedFile.getName() + " in project "
							+ changedFile.getProject(), e);
		} finally {
			final long endTime = System.currentTimeMillis();
			Logger.debug("Validation done in {} ms.", (endTime - startTime));
		}
	}

	@Override
	public IStatus validateAll(IProject project, ContextValidationHelper validationHelper,
			IProjectValidationContext validationContext, ValidatorManager manager, IReporter reporter)
			throws ValidationException {
		final long startTime = System.currentTimeMillis();
		Logger.debug("*** Validating all files in project {} ***", project.getName());
		init(project, validationHelper, validationContext, manager, reporter);
		setAsYouTypeValidation(false);
		displaySubtask(JaxrsValidationMessages.VALIDATING_PROJECT, new String[] { project.getName() });
		try {
			final JaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(project);
			if (metamodel != null) {
				// immediately index the JAX-RS elements for this metamodel
				//metamodel.getShadowElements().reset();
				final int previousProblemLevel = metamodel.getProblemLevel();
				final List<IJaxrsElement> allElements = metamodel.getAllElements();
				for (IJaxrsElement element : allElements) {
					validate(element);
				}
				validate(metamodel);
				final int currentProblemLevel = metamodel.getProblemLevel();
				if (currentProblemLevel != previousProblemLevel) {
					Logger.debug("Informing metamodel that problem level changed from {} to {}", previousProblemLevel,
							currentProblemLevel);
					metamodel.notifyMetamodelProblemLevelChanged();
				}
			}
		} catch (CoreException e) {
			Logger.error("Failed to validate project '", e);
		} finally {
			final long endTime = System.currentTimeMillis();
			Logger.debug("Validation done in {} ms.", (endTime - startTime));
		}
		return Status.OK_STATUS;
	}

	/**
	 * Uses the appropriate validator to validate the given JAX-RS element, or
	 * does nothing if no validator could be found.
	 * 
	 * @param element
	 * @throws CoreException
	 */
	private void validate(IJaxrsElement element) throws CoreException {
		// skip validation on binary JAX-RS elements (if metamodel contains any)
		if (!element.isBinary()) {
			@SuppressWarnings("unchecked")
			final IJaxrsElementValidator<IJaxrsElement> validator = (IJaxrsElementValidator<IJaxrsElement>) getValidator(element);
			if (validator != null) {
				validator.validate(element);
			}
		}
	}

	private IJaxrsElementValidator<? extends IJaxrsElement> getValidator(final IJaxrsElement element) {
		switch (element.getElementKind().getCategory()) {
		case APPLICATION:
			final IJaxrsApplication application = (IJaxrsApplication) element;
			if (application.isJavaApplication()) {
				return new JaxrsJavaApplicationValidatorDelegate(this);
			} else {
				return new JaxrsWebxmlApplicationValidatorDelegate(this);
			}
		case HTTP_METHOD:
			return new JaxrsHttpMethodValidatorDelegate(this);
		case NAME_BINDING:
			return new JaxrsNameBindingValidatorDelegate(this);
		case PARAM_CONVERTER_PROVIDER:
			return new JaxrsParamConverterProviderValidatorDelegate(this);
		case PROVIDER:
			return new JaxrsProviderValidatorDelegate(this);
		case RESOURCE:
			// this validator delegate also deals with ResourceMethods and
			// ResourceFields
			return new JaxrsResourceValidatorDelegate(this);
		default:
			// skipping other categories of elements at this validator level.
			// (see above)
			return null;
		}
	}

	/**
	 * Uses the appropriate validator to validate the given JAX-RS element, or
	 * does nothing if no validator could be found.
	 * 
	 * @param element
	 * @throws CoreException
	 */
	private void validate(JaxrsMetamodel metamodel) throws CoreException {
		new JaxrsMetamodelValidatorDelegate(this).validate(metamodel);
	}

	@Override
	protected String getMessageBundleName() {
		return BUNDLE_NAME;
	}

	@Override
	protected String getPreference(IProject project, String preferenceKey) {
		return JaxrsPreferences.getInstance().getProjectPreference(project, preferenceKey);
	}

	public int getMaxNumberOfMarkersPerFile(IProject project) {
		return JaxrsPreferences.getMaxNumberOfProblemMarkersPerFile(project);
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getBuilderId() {
		return JaxrsMetamodelBuilder.BUILDER_ID;
	}

	/**
	 * Initializes and returns the {@link IValidatingProjectTree} associated
	 * with the underlying {@link IProject} for this {@link JaxrsMetamodel}
	 * 
	 * @return the Validating Project Tree
	 */
	@Override
	public IValidatingProjectTree getValidatingProjects(IProject project) {
		return ValidatingProjectTreeLocator.getInstance().getValidatingProjects(project);
	}

	@Override
	public void registerPreferenceInfo() {
		PreferenceInfoManager.register(getProblemType(), new JaxrsPreferenceInfo());
	}

	/**
	 * Removes JAX-RS {@link IMarker}s on the {@link IJaxrsElement}s assciated
	 * with the given {@link IResource} inthe given {@link JaxrsMetamodel}.
	 * 
	 * @param metamodel
	 *            the JAX-RS Metamodel
	 * @param resource
	 *            the JAX-RS Elements' underlying resource
	 * @throws CoreException
	 */
	public static void removeMarkers(final JaxrsMetamodel metamodel, final IResource resource) throws CoreException {
		if (resource == null) {
			return;
		}
		resource.deleteMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID, true, IResource.DEPTH_ONE);
		// metamodel.removeMarkers(resource);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation.IMarkerManager
	 * #addProblem(java.lang.String, java.lang.String, java.lang.String[],
	 * org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel)
	 */
	@Override
	public IMarker addMarker(final JaxrsMetamodel metamodel, final String message, final String[] messageArguments,
			final String preferenceKey) throws CoreException {
		final IProject project = metamodel.getProject();
		Logger.debug("Reporting problem '{}' on project '{}'", message, project.getName());
		final IMarker marker = addProblem(message, preferenceKey, messageArguments, 0, 0, project);
		if (marker != null) {
			marker.setAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, preferenceKey);
			metamodel.registerMarker(marker);
		}
		return marker;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation.IMarkerManager
	 * #addProblem(java.lang.String, java.lang.String, java.lang.String[],
	 * org.eclipse.jdt.core.ISourceRange,
	 * org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBaseElement)
	 */
	@Override
	public IMarker addMarker(final AbstractJaxrsBaseElement element, final ISourceRange range, final String message,
			final String[] messageArguments, final String preferenceKey) throws CoreException {
		final IResource resource = element.getResource();
		Logger.debug("Reporting problem '{}' on resource '{}'", message, resource.getFullPath().toString());
		final IMarker marker = addProblem(message, preferenceKey, messageArguments, range.getLength(),
				range.getOffset(), resource);
		if (marker != null) {
			marker.setAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, preferenceKey);
			element.registerMarker(marker);
		}
		return marker;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation.IMarkerManager
	 * #addProblem(java.lang.String, java.lang.String, java.lang.String[],
	 * org.eclipse.jdt.core.ISourceRange,
	 * org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBaseElement,
	 * int)
	 */
	@Override
	public IMarker addMarker(final AbstractJaxrsBaseElement element, final ISourceRange range, final String message,
			final String[] messageArguments, final String preferenceKey, final int quickFixId) throws CoreException {
		// (range == null) occurs when there is no value at all for the annotation
		if(range == null) {
			return null;
		}
		final IResource resource = element.getResource();
		Logger.debug("Reporting problem '{}' on resource '{}'", message, resource.getFullPath().toString());
		final IMarker marker = addProblem(message, preferenceKey, messageArguments, range.getLength(),
				range.getOffset(), resource, quickFixId);
		if (marker != null) {
			marker.setAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, preferenceKey);
			element.registerMarker(marker);
		}
		return marker;
	}

	class JaxrsPreferenceInfo implements IPreferenceInfo {
		@Override
		public String getPreferencePageId() {
			return PREFERENCE_PAGE_ID;
		}

		@Override
		public String getPropertyPageId() {
			return PROPERTY_PAGE_ID;
		}

		@Override
		public String getPluginId() {
			return JBossJaxrsUIPlugin.PLUGIN_ID;
		}
	}

	@Override
	public boolean shouldValidateAsYouType(IProject project) {
		return shouldValidate(project);
	}
}
