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
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IRegion;
import org.eclipse.wst.validation.internal.core.ValidationException;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.eclipse.wst.validation.internal.provisional.core.IValidationContext;
import org.jboss.tools.common.validation.ContextValidationHelper;
import org.jboss.tools.common.validation.EditorValidationContext;
import org.jboss.tools.common.validation.IAsYouTypeValidator;
import org.jboss.tools.common.validation.IProjectValidationContext;
import org.jboss.tools.common.validation.IValidatingProjectTree;
import org.jboss.tools.common.validation.IValidator;
import org.jboss.tools.common.validation.TempMarkerManager;
import org.jboss.tools.common.validation.ValidatorManager;
import org.jboss.tools.common.validation.internal.SimpleValidatingProjectTree;
import org.jboss.tools.ws.jaxrs.core.configuration.ProjectNatureUtils;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsMetamodelBuilder;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBaseElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsApplication;
import org.jboss.tools.ws.jaxrs.core.metamodel.JaxrsMetamodelLocator;
import org.jboss.tools.ws.jaxrs.core.preferences.JaxrsPreferences;

@SuppressWarnings("restriction")
public class JaxrsMetamodelValidator extends TempMarkerManager implements IValidator, IAsYouTypeValidator {

	/** The JAX-RS Validator ID. */
	public static final String ID = "org.jboss.tools.ws.jaxrs.JaxrsMetamodelValidator"; //$NON-NLS-1$

	/** The custom 'JAX-RS Problem' marker type. */
	public static final String JAXRS_PROBLEM_TYPE = "org.jboss.tools.ws.jaxrs.metamodelMarker";

	private static final String BUNDLE_NAME = JaxrsMetamodelValidator.class.getPackage().getName() + ".messages";

	/*
	 * (non-Javadoc)
	 * @see org.jboss.tools.jst.web.kb.validation.IValidator#isEnabled(org.eclipse.core.resources.IProject)
	 */
	public boolean isEnabled(IProject project) {
		return JaxrsPreferences.isValidationEnabled(project);
	}

	/*
	 * (non-Javadoc)
	 * @see org.jboss.tools.jst.web.kb.validation.IValidator#shouldValidate(org.eclipse .core.resources.IProject)
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
	 * @see org.jboss.tools.jst.web.kb.validation.IValidator#validate(java.util.Set,
	 * org.eclipse.core.resources.IProject, org.jboss.tools.jst.web.kb.internal.validation.ContextValidationHelper,
	 * org.jboss.tools.jst.web.kb.validation.IProjectValidationContext,
	 * org.jboss.tools.jst.web.kb.internal.validation.ValidatorManager,
	 * org.eclipse.wst.validation.internal.provisional.core.IReporter)
	 */
	public IStatus validate(Set<IFile> changedFiles, IProject project, ContextValidationHelper validationHelper,
			IProjectValidationContext context, ValidatorManager manager, IReporter reporter) throws ValidationException {
		Logger.debug("*** Validating project {} after files {} changed... ***", project.getName(), changedFiles.toString());
		init(project, validationHelper, context, manager, reporter);
		setAsYouTypeValidation(false);
		try {
			if (!changedFiles.isEmpty()) {
				for (IFile changedFile : changedFiles) {
					try {
						final JaxrsMetamodel jaxrsMetamodel = JaxrsMetamodelLocator.get(changedFile.getProject());
						validateJaxrsApplicationDeclarations(jaxrsMetamodel);
						validate(reporter, changedFile, jaxrsMetamodel);
					} catch (CoreException e) {
						Logger.error("Failed to validate changed file " + changedFile.getName() + " in project "
								+ changedFile.getProject(), e);
					}
				}
			}	
			// trigger a full validation instead
			else {
				validateAll(project, validationHelper, context, manager, reporter);
			}
		} finally {
			Logger.debug("Validation done.");
		}
		return Status.OK_STATUS;
	}

	/**
	 * @param reporter
	 * @param file
	 * @throws CoreException
	 */
	private void validate(final IReporter reporter, final IFile file, final JaxrsMetamodel jaxrsMetamodel) {
		if (reporter.isCancelled() || !file.isAccessible()) {
			return;
		}
		displaySubtask(JaxrsValidationMessages.VALIDATING_RESOURCE,
				new String[] { file.getProject().getName(), file.getName() });
		try {
			if (jaxrsMetamodel != null) {
				List<JaxrsBaseElement> elements = jaxrsMetamodel.getElements(JdtUtils.getCompilationUnit(file));
				for (JaxrsBaseElement element : elements) {
					validate(element);
				}
			}
		} catch (CoreException e) {
			Logger.error("Failed to validate the resource change", e);
		}
	}

	@Override
	public void validate(org.eclipse.wst.validation.internal.provisional.core.IValidator validatorManager,
			IProject rootProject, Collection<IRegion> dirtyRegions, IValidationContext helper, IReporter reporter,
			EditorValidationContext validationContext, IProjectValidationContext projectContext, IFile file) {
		Logger.debug("*** Validating project {} after file {} changed... ***", file.getProject().getName(), file.getFullPath());
		ContextValidationHelper validationHelper = new ContextValidationHelper();
		validationHelper.setProject(rootProject);
		validationHelper.setValidationContextManager(validationContext);
		init(rootProject, validationHelper, projectContext, validatorManager, reporter);
		setAsYouTypeValidation(false);
		this.document = validationContext.getDocument();
		displaySubtask(JaxrsValidationMessages.VALIDATING_RESOURCE,
				new String[] { file.getProject().getName(), file.getName() });
		try {
			final JaxrsMetamodel jaxrsMetamodel = JaxrsMetamodelLocator.get(file.getProject());
			validateJaxrsApplicationDeclarations(jaxrsMetamodel);
			validate(reporter, file, jaxrsMetamodel);
		} catch (CoreException e) {
			Logger.error(
					"Failed to validate changed file " + file.getName() + " in project "
							+ file.getProject(), e);
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
			final JaxrsMetamodel jaxrsMetamodel = JaxrsMetamodelLocator.get(project);
			// validate that the number of jax-rs applications (java or web.xml) is 1.
			validateJaxrsApplicationDeclarations(jaxrsMetamodel);
			// validate all other elements
			if (jaxrsMetamodel != null) {
				for (JaxrsBaseElement element : jaxrsMetamodel.getAllElements()) {
					validate(element);
				}
			}
		} catch (CoreException e) {
			Logger.error("Failed to validate project '", e);
		} finally {
			Logger.debug("Validation done.");
		}

		return Status.OK_STATUS;
	}

	private void validateJaxrsApplicationDeclarations(JaxrsMetamodel jaxrsMetamodel) throws CoreException {
		if(jaxrsMetamodel == null) {
			return;
		}
		MarkerUtils.clearMarkers(jaxrsMetamodel.getProject());
		final List<IJaxrsApplication> allApplications = jaxrsMetamodel.getAllApplications();
		if(allApplications.isEmpty()) {
			this.addProblem(JaxrsValidationMessages.APPLICATION_NO_OCCURRENCE_FOUND,
					JaxrsPreferences.APPLICATION_NO_OCCURRENCE_FOUND, new String[0],
					0, 0, jaxrsMetamodel.getProject());
			
		} else if(allApplications.size() > 1) {
			this.addProblem(JaxrsValidationMessages.APPLICATION_TOO_MANY_OCCURRENCES,
					JaxrsPreferences.APPLICATION_TOO_MANY_OCCURRENCES, new String[0],
					0, 0, jaxrsMetamodel.getProject());
		}
	}

	/**
	 * Uses the appropriate validator to validate the given JAX-RS element, or does nothing if no validator could be
	 * found.
	 * 
	 * @param element
	 * @throws CoreException
	 */
	private void validate(JaxrsBaseElement element) throws CoreException {
		Logger.debug("Validating element {}", element.getName());
			switch (element.getElementCategory()) {
			case APPLICATION:
				break;
			case HTTP_METHOD:
				new JaxrsHttpMethodValidatorDelegate(this, (JaxrsHttpMethod) element).validate();
			case PROVIDER:
				break;
			case RESOURCE:
				// this validator delegate also deals with ResourceMethods and ResourceFields 
				new JaxrsResourceValidatorDelegate(this, (JaxrsResource) element).validate();
			default:
				// skipping other categories of elements at this validator level. (see above)
				break;
		}
	}

	@Override
	protected String getMessageBundleName() {
		return BUNDLE_NAME;
	}

	@Override
	protected String getPreference(IProject project, String preferenceKey) {
		return JaxrsPreferences.getInstance().getProjectPreference(project, preferenceKey);
	}

	@Override
	protected String getPreferencePageId() {
		return "org.jboss.tools.ws.jaxrs.ui";
	}

	@Override
	public int getMaxNumberOfMarkersPerFile(IProject project) {
		return JaxrsPreferences.getMaxNumberOfProblemMarkersPerFile(project);
	}

	@Override
	public String getMarkerType() {
		return JAXRS_PROBLEM_TYPE;
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
		return new SimpleValidatingProjectTree(project);
	}

}
