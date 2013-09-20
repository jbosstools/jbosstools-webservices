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
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation;

import java.util.Collection;
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
import org.jboss.tools.common.validation.internal.SimpleValidatingProjectTree;
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCorePlugin;
import org.jboss.tools.ws.jaxrs.core.configuration.ProjectNatureUtils;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsMetamodelBuilder;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBaseElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsApplication;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsMetamodelLocator;
import org.jboss.tools.ws.jaxrs.core.preferences.JaxrsPreferences;

/**
 * JAX-RS Metamodel Validator. Relies on delegates to validate each category of
 * element.
 * 
 * @author Xavier Coulon
 * 
 */
@SuppressWarnings("restriction")
public class JaxrsMetamodelValidator extends TempMarkerManager implements IValidator, IAsYouTypeValidator, IMarkerManager {

	/** ID of the Preference Page.*/
	private static final String PREFERENCE_PAGE_ID = "org.jboss.tools.ws.jaxrs.ui.preferencePages.JAXRSValidatorPreferencePage";

	/** ID of the Property Page.*/
	private static final String PROPERTY_PAGE_ID = "org.jboss.tools.ws.jaxrs.ui.propertyPages.JaxrsValidatorPreferencePage";

	/** The JAX-RS Validator ID. */
	public static final String ID = "org.jboss.tools.ws.jaxrs.JaxrsMetamodelValidator"; //$NON-NLS-1$

	/** The name of the message bundle.*/
	private static final String BUNDLE_NAME = JaxrsMetamodelValidator.class.getPackage().getName() + ".messages";

	public static final String JAXRS_PROBLEM_TYPE = "jaxrsProblemType";
	
	/** 
	 * Constructor.
	 */
	public JaxrsMetamodelValidator() {
		super.setProblemType(JaxrsValidationConstants.JAXRS_PROBLEM_TYPE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jboss.tools.jst.web.kb.validation.IValidator#isEnabled(org.eclipse
	 * .core.resources.IProject)
	 */
	public boolean isEnabled(IProject project) {
		return JaxrsPreferences.isValidationEnabled(project);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jboss.tools.jst.web.kb.validation.IValidator#shouldValidate(org.eclipse
	 * .core.resources.IProject)
	 */
	public boolean shouldValidate(IProject project) {
		try {
			return project.isAccessible() && project.hasNature(ProjectNatureUtils.JAXRS_NATURE_ID)
					&& isEnabled(project);
		} catch (CoreException e) {
			Logger.error("Failed to check if JAX-RS validation is required for project '" + project.getName() + "'", e);
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jboss.tools.jst.web.kb.validation.IValidator#validate(java.util.Set,
	 * org.eclipse.core.resources.IProject,
	 * org.jboss.tools.jst.web.kb.internal.validation.ContextValidationHelper,
	 * org.jboss.tools.jst.web.kb.validation.IProjectValidationContext,
	 * org.jboss.tools.jst.web.kb.internal.validation.ValidatorManager,
	 * org.eclipse.wst.validation.internal.provisional.core.IReporter)
	 */
	public IStatus validate(Set<IFile> changedFiles, IProject project, ContextValidationHelper validationHelper,
			IProjectValidationContext context, ValidatorManager manager, IReporter reporter) throws ValidationException {
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
					final Set<IResource> allResources = completeValidationSet(metamodel,
							changedFiles.toArray(new IFile[changedFiles.size()]));
					for (IResource changedResource : allResources) {
						validate(changedResource, metamodel, reporter);
					}
					// validate at the metamodel level for cross-elements validation
					validate(metamodel);
					final int currentProblemLevel = metamodel.getProblemLevel();
					if(currentProblemLevel != previousProblemLevel) {
						Logger.debug("Informing metamodel that problem level changed from {} to {}", previousProblemLevel,
								currentProblemLevel);
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
			Logger.debug("Validation done.");
		}
		return Status.OK_STATUS;
	}

	/**
	 * Completes the list of resources to validate by adding those that might be impacted by the given changedResources.
	 * 
	 * @param metamodel the JAX-RS Metamodel
	 * @param changedResources the resources that initially changed
	 * @return all resources that should be validated
	 */
	private Set<IResource> completeValidationSet(final JaxrsMetamodel metamodel, final IFile... changedResources) {
		final Set<IResource> resources = new HashSet<IResource>();
		for (IResource changedResource : changedResources) {
			resources.add(changedResource);
			if (changedResource.exists() && metamodel.getApplication(changedResource) != null) {
				Logger.debug("Adding all applications and project in the set of files to validate...");
				for (IJaxrsApplication application : metamodel.getAllApplications()) {
					resources.add(application.getResource());
				}
				resources.add(metamodel.getProject());
			} else {
				// look for existing markers with problem type = APPLICATION_TOO_MANY_OCCURRENCES, since
				// the removal of an application may fix that problem on other applications
				final List<IResource> duplicateAppResources = metamodel.findResourcesWithProblemOfType(JaxrsPreferences.APPLICATION_TOO_MANY_OCCURRENCES);
				if(!changedResource.exists()) {
					resources.addAll(duplicateAppResources);
				} else if(duplicateAppResources.contains(changedResource)) {
					resources.addAll(duplicateAppResources);
				}
			}
		}
		return resources;
	}

	/**
	 * Validate the given {@link IResource} in the given {@link JaxrsMetamodel} and reports  
	 * @param changedResource
	 * @param reporter
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
				final Collection<IJaxrsElement> elements = metamodel.getElements(changedResource);
				// if no (more) JAX-RS element matches the resource to validate, then make 
				// sure no JAX-RS Problem marker remains on that resource
				if(elements.isEmpty()) {
					deleteJaxrsMarkers(metamodel, changedResource);
				} else {
					for (IJaxrsElement element : elements) {
						validate(element);
					}
				}
			}
		} catch (CoreException e) {
			Logger.error("Failed to validate the resource change", e);
		}
	}

	@Override
	public void validate(final org.eclipse.wst.validation.internal.provisional.core.IValidator validatorManager,
			final IProject rootProject, final Collection<IRegion> dirtyRegions, final IValidationContext helper, final IReporter reporter,
			final EditorValidationContext validationContext, final IProjectValidationContext projectContext, final IFile changedFile) {
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
			final JaxrsMetamodel jaxrsMetamodel = JaxrsMetamodelLocator.get(changedFile.getProject());
			final Set<IResource> allResources = completeValidationSet(jaxrsMetamodel, changedFile);
			for (IResource changedResource : allResources) {
				validate(changedResource, jaxrsMetamodel, reporter);
				//FIXME: notify UI
			}
		} catch (CoreException e) {
			Logger.error(
					"Failed to validate changed file " + changedFile.getName() + " in project "
							+ changedFile.getProject(), e);
		} finally {
			Logger.debug("Validation done.");
		}
	}

	@Override
	public IStatus validateAll(IProject project, ContextValidationHelper validationHelper,
			IProjectValidationContext validationContext, ValidatorManager manager, IReporter reporter)
			throws ValidationException {
		Logger.debug("*** Validating all files in project {} ***", project.getName());
		init(project, validationHelper, validationContext, manager, reporter);
		setAsYouTypeValidation(false);
		displaySubtask(JaxrsValidationMessages.VALIDATING_PROJECT, new String[] { project.getName() });
		try {
			final JaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(project);
			if (metamodel != null) {
				final int previousProblemLevel = metamodel.getProblemLevel();
				for (IJaxrsElement element : metamodel.getAllElements()) {
					validate(element);
				}
				validate(metamodel);
				final int currentProblemLevel = metamodel.getProblemLevel();
				if(currentProblemLevel != previousProblemLevel) {
					Logger.debug("Informing metamodel that problem level changed from {} to {}", previousProblemLevel,
							currentProblemLevel);
					metamodel.notifyMetamodelProblemLevelChanged();
				}
			}
		} catch (CoreException e) {
			Logger.error("Failed to validate project '", e);
		} finally {
			Logger.debug("Validation done.");
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
		if(!element.isBinary()) {
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
			if(application.isJavaApplication()) {
				return new JaxrsJavaApplicationValidatorDelegate(this);
			} else {
				return new JaxrsWebxmlApplicationValidatorDelegate(this);
			}
		case HTTP_METHOD:
			return new JaxrsHttpMethodValidatorDelegate(this);
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

	@Override
	public IValidatingProjectTree getValidatingProjects(IProject project) {
		try {
			JaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(project);
			if(metamodel!=null) {
				return metamodel.getValidatingProjectTree();
			}
		} catch (CoreException e) {
			Logger.error(e.getMessage(), e);
		}
		return new SimpleValidatingProjectTree(project);
	}

	@Override
	public void registerPreferenceInfo() {
		PreferenceInfoManager.register(getProblemType(), new JaxrsPreferenceInfo());
	}

	public static void deleteJaxrsMarkers(final IJaxrsElement element) throws CoreException {
		if (element == null) {
			return;
		}
		deleteJaxrsMarkers((JaxrsMetamodel)element.getMetamodel(), element.getResource());
	}

	public static void deleteJaxrsMarkers(final JaxrsMetamodel metamodel, final IResource resource) throws CoreException {
		if (resource == null) {
			return;
		}
		Logger.debug("Clearing JAX-RS markers for resource " + resource.getName());
		resource.deleteMarkers(JaxrsValidationConstants.JAXRS_PROBLEM_TYPE, true, IResource.DEPTH_ONE);
		metamodel.unregisterMarkers(resource);
	}
	
	/* (non-Javadoc)
	 * @see org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation.IMarkerManager#addProblem(java.lang.String, java.lang.String, java.lang.String[], org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel)
	 */
	@Override
	public IMarker addMarker(final JaxrsMetamodel metamodel, final String message, final String[] messageArguments, final String preferenceKey) throws CoreException {
		final IProject project = metamodel.getProject();
		Logger.debug("Reporting problem '{}' on project '{}'", message, project.getName());
		final IMarker marker = addProblem(message, preferenceKey, messageArguments, 0, 0, project);
		marker.setAttribute(JAXRS_PROBLEM_TYPE, preferenceKey);
		metamodel.registerMarker(marker);
		return marker;
	}
	
	/* (non-Javadoc)
	 * @see org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation.IMarkerManager#addProblem(java.lang.String, java.lang.String, java.lang.String[], org.eclipse.jdt.core.ISourceRange, org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBaseElement)
	 */
	@Override
	public IMarker addMarker(final JaxrsBaseElement element, final ISourceRange range, final String message, final String[] messageArguments, final String preferenceKey) throws CoreException {
		final IResource resource = element.getResource();
		Logger.debug("Reporting problem '{}' on resource '{}'", message, resource.getFullPath().toString());
		final IMarker marker = addProblem(message, preferenceKey, messageArguments, range.getLength(), range.getOffset(), resource);
		marker.setAttribute(JAXRS_PROBLEM_TYPE, preferenceKey);
		element.registerMarker(marker);
		return marker;
	}

	/* (non-Javadoc)
	 * @see org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation.IMarkerManager#addProblem(java.lang.String, java.lang.String, java.lang.String[], org.eclipse.jdt.core.ISourceRange, org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBaseElement, int)
	 */
	@Override
	public IMarker addMarker(final JaxrsBaseElement element, final ISourceRange range, final String message, final String[] messageArguments, final String preferenceKey, final int quickFixId) throws CoreException {
		final IResource resource = element.getResource();
		Logger.debug("Reporting problem '{}' on resource '{}'", message, resource.getFullPath().toString());
		final IMarker marker = addProblem(message, preferenceKey, messageArguments, range.getLength(), range.getOffset(), resource, quickFixId);
		marker.setAttribute(JAXRS_PROBLEM_TYPE, preferenceKey);
		element.registerMarker(marker);
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
			return JBossJaxrsCorePlugin.PLUGIN_ID;
		}

	}

}
