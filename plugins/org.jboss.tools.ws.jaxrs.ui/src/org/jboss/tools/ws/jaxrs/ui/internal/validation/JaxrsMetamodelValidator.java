/******************************************************************************* 
 * Copyright (c) 2008 - 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.jaxrs.ui.internal.validation;

import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind.APPLICATION_JAVA;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind.APPLICATION_WEBXML;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind.CONTAINER_FILTER;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind.CONTAINER_REQUEST_FILTER;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind.CONTAINER_RESPONSE_FILTER;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind.ENTITY_INTERCEPTOR;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind.ENTITY_READER_INTERCEPTOR;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind.ENTITY_WRITER_INTERCEPTOR;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind.NAME_BINDING;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind.ROOT_RESOURCE;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind.SUBRESOURCE;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind.SUBRESOURCE_LOCATOR;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind.UNDEFINED_PROVIDER;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind.UNDEFINED_RESOURCE;
import static org.jboss.tools.ws.jaxrs.core.validation.IJaxrsValidation.JAXRS_PROBLEM_MARKER_ID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.IRegion;
import org.eclipse.wst.validation.internal.core.ValidationException;
import org.eclipse.wst.validation.internal.provisional.core.IMessage;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.eclipse.wst.validation.internal.provisional.core.IValidationContext;
import org.jboss.tools.common.validation.ContextValidationHelper;
import org.jboss.tools.common.validation.EditorValidationContext;
import org.jboss.tools.common.validation.IJavaElementValidator;
import org.jboss.tools.common.validation.IPreferenceInfo;
import org.jboss.tools.common.validation.IProjectValidationContext;
import org.jboss.tools.common.validation.IStringValidator;
import org.jboss.tools.common.validation.ITypedReporter;
import org.jboss.tools.common.validation.IValidatingProjectTree;
import org.jboss.tools.common.validation.IValidator;
import org.jboss.tools.common.validation.PreferenceInfoManager;
import org.jboss.tools.common.validation.TempMarkerManager;
import org.jboss.tools.common.validation.ValidatorManager;
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCorePlugin;
import org.jboss.tools.ws.jaxrs.core.configuration.ProjectNatureUtils;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsMetamodelBuilder;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBaseElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsJavaElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsShadowElementsCache;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.search.JavaElementsSearcher;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsApplication;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsEndpoint;
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
public class JaxrsMetamodelValidator extends TempMarkerManager implements IValidator, IJavaElementValidator, IStringValidator,
		IMarkerManager {
	/** ID of the Preference Page. */
	private static final String PREFERENCE_PAGE_ID = "org.jboss.tools.ws.jaxrs.ui.preferencePages.JaxrsValidatorPreferencePage";
	/** ID of the Property Page. */
	private static final String PROPERTY_PAGE_ID = "org.jboss.tools.ws.jaxrs.ui.propertyPages.JaxrsValidatorPreferencePage";
	/** The JAX-RS Validator ID. */
	public static final String ID = "org.jboss.tools.ws.jaxrs.JaxrsMetamodelValidator"; //$NON-NLS-1$
	/** The name of the message bundle. */
	private static final String BUNDLE_NAME = JaxrsMetamodelValidator.class.getPackage().getName() + ".messages";
	/** The type of JAX-RS problem. */
	public static final String JAXRS_PROBLEM_TYPE = "problemType";
	
	/**
	 * Constructor.
	 */
	public JaxrsMetamodelValidator() {
		super.setProblemType(JAXRS_PROBLEM_MARKER_ID);
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
		init(project, validationHelper, context, manager, reporter, false);
		// switch to full validation when '.project' file was altered
		final IResource dotProject = project.findMember(".project");
		final IResource facetSettings = project.findMember(new Path(".settings").append("org.eclipse.wst.common.project.facet.core.xml"));
		if (changedFiles.size() == 1 && (changedFiles.contains(dotProject) || changedFiles.contains(facetSettings))) {
			validateAll(project, validationHelper, context, manager, reporter);
		} else if (!changedFiles.isEmpty()) {
			try {
				Logger.debug("*** Validating project {} after files {} changed... ***", project.getName(),
						changedFiles.toString());
				displaySubtask(JaxrsValidationMessages.VALIDATING_RESOURCE, new String[] { project.getName() });
				final JaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(project);
				// prevent failure in case validation would be called at
				// workbench startup, even before metamodel is built.
				if (metamodel != null) {
					// validate each JAX-RS element individually
					final List<IResource> allResources = completeValidationSet(metamodel,
							changedFiles.toArray(new IFile[changedFiles.size()]));
					final List<IJaxrsElement> elementsToValidate = new ArrayList<IJaxrsElement>();
					boolean includeMetamodel = allResources.contains(project);
					for (IResource changedResource : allResources) {
						elementsToValidate.addAll(metamodel.findElements(changedResource));
					}
					validate(elementsToValidate, metamodel, includeMetamodel);
				}
			} catch (CoreException e) {
				Logger.error("Failed to validate changed files " + changedFiles + " in project " + project, e);
			} finally {
				final long endTime = System.currentTimeMillis();
				Logger.debug("Validation of {} files done in {} ms.", changedFiles.size(), (endTime - startTime));
			}
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
		final IProject project = metamodel.getProject();
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
			resources.add(project);
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
		// check if the given changedFile is *referenced* in JAX-RS elements of the metamodel (for cross-type validation)
		final List<IType> knownTypes = metamodel.getAllJavaElements(IJavaElement.TYPE);
		for(IFile changedResource : changedResources) {
			final ICompilationUnit changedCompilationUnit = JdtUtils.getCompilationUnit(changedResource);
			if(changedCompilationUnit != null) {
				final Collection<IType> foundRelatedTypes = JavaElementsSearcher.findRelatedTypes(changedCompilationUnit.findPrimaryType(), knownTypes, new NullProgressMonitor());
				for(IType relatedType : foundRelatedTypes) {
					resources.add(relatedType.getResource());
				}
			}
		}
		// if the given changedFile matches a ParamConverterProvider, add all JAX-RS resources (naive approach).
		if(elementKindChanges.contains(EnumElementKind.PARAM_CONVERTER_PROVIDER)) {
			resources.addAll(getResourceUnderlyingResources(metamodel));
		}
		// check if there are JAX-RS element changes in the given resources 
		if(!elementKindChanges.isEmpty()) {
			resources.add(project);
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
			final Set<EnumElementKind> previousElementKinds = metamodel.getShadowElementKinds(changedResource);
			// now, see what the metamodel has for this resource
			final IJaxrsElement currentElement = metamodel.findElement(changedResource);
			// now, let's add the data we have: both the old and the new, in case of changes (addition, deletion and even change...)
			if(previousElementKinds != null) {
				elementKindChanges.addAll(previousElementKinds);
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
		final Collection<IJaxrsProvider> providers = metamodel.findAllProviders();
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
		final Collection<IJaxrsApplication> applications = metamodel.findAllApplications();
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
		final Collection<IJaxrsResource> resources = metamodel.findAllResources();
		for (IJaxrsResource resource : resources) {
			resourceResources.add(resource.getResource());
		}
		return resourceResources;
	}

	/** 
	 * 
	 * @param element the {@link IJaxrsElement}
	 * @return the associated {@link CompilationUnit} if the given {@code element} is based on an {@link IJavaElement}, {@code null} otherwise.
	 * @throws JavaModelException 
	 * 
	 */
	private static CompilationUnit getAST(final IJaxrsElement element) throws JavaModelException {
		if(element instanceof JaxrsJavaElement<?>) {
			final IMember javaElement = ((JaxrsJavaElement<?>)element).getJavaElement();
			// built-in HTTP methods have no underlying Java Element.
			if(javaElement != null) {
				return JdtUtils.parse(javaElement.getCompilationUnit(), new NullProgressMonitor());
			}
		}
		return null;
	}

	/**
	 * As-you-type validation, called when before the user saved the modified file. The given dirtyRegions indicate where the changes occurred.
	 * As opposed to other validation methods in this class, there is no cross-resource validation performed here, ie, only local changes are taken into account.
	 * 
	 * @param validatorManager the validation manager
	 * @param rootProject the root project
	 * @param dirtyRegions the dirty regions
	 * @param helper the validation context
	 * @param reporter the validation reporter
	 * @param validationContext the validation context
	 * @param projectContext the project context
	 * @param changedFile the file that changed
	 */
	@Override
	public void validate(final org.eclipse.wst.validation.internal.provisional.core.IValidator validatorManager,
			final IProject rootProject, final Collection<IRegion> dirtyRegions, final IValidationContext helper,
			final IReporter reporter, final EditorValidationContext validationContext,
			final IProjectValidationContext projectContext, final IFile changedFile) {
		final long startTime = System.currentTimeMillis();
		Logger.debug("*** Validating project {} after file {} changed... ***", changedFile.getProject().getName(),
				changedFile.getFullPath());
		try {
			final ContextValidationHelper validationHelper = new ContextValidationHelper();
			validationHelper.setProject(rootProject);
			validationHelper.setValidationContextManager(validationContext);
			init(rootProject, validationHelper, projectContext, validatorManager, reporter);
			setAsYouTypeValidation(true);
			if(reporter instanceof ITypedReporter) {
				((ITypedReporter)reporter).addTypeForFile(getProblemType());
			}
			this.document = validationContext.getDocument();
			final JaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(changedFile.getProject());
			final ICompilationUnit compilationUnit = JdtUtils.getCompilationUnit(changedFile);
			if (metamodel != null && compilationUnit != null) {
				final CompilationUnit ast = JdtUtils.parse(compilationUnit, new NullProgressMonitor());
				final Set<JaxrsJavaElement<?>> changedWorkingCopies = new HashSet<JaxrsJavaElement<?>>();
				final Set<IJaxrsElement> changedElements = metamodel.findElements(changedFile);
				for(IJaxrsElement changedElement : changedElements) {
					if(changedElement instanceof JaxrsJavaElement<?>) {
						final JaxrsJavaElement<?> changedJavaElement = (JaxrsJavaElement<?>) changedElement;
						final JaxrsJavaElement<?> workingCopyElement =  (JaxrsJavaElement<?>) changedJavaElement.getWorkingCopy();
						workingCopyElement.update(changedJavaElement.getJavaElement(), ast);
						changedWorkingCopies.add(workingCopyElement);
					}
				}
				for(IJaxrsElement workingCopyElement : changedWorkingCopies) {
					Logger.debug("Removing message before validating {}", workingCopyElement.getName());
					reporter.removeAllMessages(validatorManager, workingCopyElement.getResource());
				}
				validate(changedWorkingCopies, metamodel, false);
			}
		} catch (CoreException e) {
			Logger.error(
					"Failed to validate changed file " + changedFile.getName() + " in project "
							+ changedFile.getProject(), e);
		} finally {
			final long endTime = System.currentTimeMillis();
			Logger.debug("As-you-type validation done in {} ms.", (endTime - startTime));
		}
	}

	@Override
	public IStatus validateAll(IProject project, ContextValidationHelper validationHelper,
			IProjectValidationContext validationContext, ValidatorManager manager, IReporter reporter)
			throws ValidationException {
		final long startTime = System.currentTimeMillis();
		Logger.debug("*** Validating all files in project {} ***", project.getName());
		init(project, validationHelper, validationContext, manager, reporter, false);
		try {
			final JaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(project);
			if (metamodel != null) {
				displaySubtask(JaxrsValidationMessages.VALIDATING_PROJECT, new String[] { project.getName() });
				final List<IJaxrsElement> allElements = metamodel.getAllElements();
				validate(allElements, metamodel, true);
			}
		} catch (CoreException e) {
			Logger.error("Failed to validate project '", e);
		} finally {
			final long endTime = System.currentTimeMillis();
			Logger.debug("Full validation of project '{}' done in {} ms.", project.getName(), (endTime - startTime));
		}
		return Status.OK_STATUS;
	}

	/**
	 * Uses the appropriate validator to validate the given JAX-RS element, or
	 * does nothing if no validator could be found.
	 * 
	 * @param element the element to validate
	 * @param ast the associated AST
	 * @throws CoreException
	 */
	private void validate(final Collection<? extends IJaxrsElement> elements, final JaxrsMetamodel metamodel, final boolean validateMetamodel) throws CoreException {
		final List<IJaxrsElement> elementsToValidate = new ArrayList<IJaxrsElement>();
		// record the problem severity level at the endpoints level *before* the validation begins
		final int previousMetamodelProblemSeverity = metamodel.getProblemSeverity();
		final List<IJaxrsEndpoint> endpoints = metamodel.getAllEndpoints();
		final Map<String, Integer> endpointProblemSeverities = new HashMap<String, Integer>();
		for(IJaxrsEndpoint endpoint : endpoints) {
			endpointProblemSeverities.put(endpoint.getIdentifier(), endpoint.getProblemLevel());
		}
		for(IJaxrsElement element : elements) {
			// skip validation on binary JAX-RS elements (if metamodel contains any)
			if (element.isBinary()) {
				continue;
			}
			if (element instanceof JaxrsJavaElement<?> && !((JaxrsJavaElement<?>) element).isBasedOnJavaType()) {
				continue;
			}
			elementsToValidate.add(element);
			removeMarkers(element);
		}
		// perform the validation
		for(IJaxrsElement element : elementsToValidate) {
			@SuppressWarnings("unchecked")
			final IJaxrsElementValidator<IJaxrsElement> validator = (IJaxrsElementValidator<IJaxrsElement>) getValidator(element);
			if (validator != null) {
				validator.validate(element, getAST(element));
			}
		}
		if(validateMetamodel) {
			validate(metamodel);
		}
		// check if problem level changed on endpoints, notify the UI if changes occurred
		for(IJaxrsEndpoint endpoint : endpoints) {
			final int previousProblemLevel = endpointProblemSeverities.get(endpoint.getIdentifier());
			final int currentProblemLevel = endpoint.getProblemLevel();
			if(currentProblemLevel != previousProblemLevel) {
				JBossJaxrsCorePlugin.notifyEndpointProblemLevelChanged(endpoint);
			}
		}
		// check if problem level changed at the metamodel level, too
		final int currentMetamodelProblemSeverity = metamodel.getProblemSeverity();
		if(currentMetamodelProblemSeverity != previousMetamodelProblemSeverity) {
			JBossJaxrsCorePlugin.notifyMetamodelProblemLevelChanged(metamodel);
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
			// ResourceFields when validating a whole resource
			return new JaxrsResourceValidatorDelegate(this); 
		case RESOURCE_FIELD:
			return new JaxrsResourceFieldValidatorDelegate(this);
		case RESOURCE_PROPERTY:
			return new JaxrsResourcePropertyValidatorDelegate(this);
		case RESOURCE_METHOD:
			return new JaxrsResourceMethodValidatorDelegate(this);
		case PARAMETER_AGGREGATOR:
			return new JaxrsParameterAggregatorValidatorDelegate(this);
		case PARAMETER_AGGREGATOR_FIELD:
			return new JaxrsParameterAggregatorFieldValidatorDelegate(this);
		case PARAMETER_AGGREGATOR_PROPERTY:
			return new JaxrsParameterAggregatorPropertyValidatorDelegate(this);
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
	 * Removes JAX-RS {@link IMarker}s on the underlying {@link IProject} associated
	 * with the given {@link JaxrsMetamodel}.
	 * 
	 * @param metamodel
	 *            the JAX-RS Metamodel
	 * @param resource
	 *            the JAX-RS Elements' underlying resource
	 * @throws CoreException
	 */
	public static void removeJaxrsMarkers(final IProject project) throws CoreException {
		if (project == null || !project.isOpen()) {
			return;
		}
		project.deleteMarkers(JAXRS_PROBLEM_MARKER_ID, true, IResource.DEPTH_ONE);
	}
	
	/**
	 * Removes JAX-RS {@link IMarker}s on the underlying {@link IProject} associated
	 * with the given {@link JaxrsMetamodel}.
	 * 
	 * @param metamodel
	 *            the JAX-RS Metamodel
	 * @param resource
	 *            the JAX-RS Elements' underlying resource
	 * @throws CoreException
	 */
	public static void removeAllJaxrsMarkers(final IProject project) throws CoreException {
		if (project == null || !project.isOpen()) {
			return;
		}
		project.deleteMarkers(JAXRS_PROBLEM_MARKER_ID, true, IResource.DEPTH_INFINITE);
	}
	
	/**
	 * Removes JAX-RS {@link IMarker}s on the underlying {@link IResource} of the given {@link IJaxrsElement}
	 * 
	 * @param element
	 *            the JAX-RS Element to clean
	 * @throws CoreException
	 */
	public static void removeMarkers(final IJaxrsElement element) throws CoreException {
		if (element == null || element.getResource() == null) {
			return;
		}
		element.getResource().deleteMarkers(JAXRS_PROBLEM_MARKER_ID, true, IResource.DEPTH_ONE);
		((JaxrsBaseElement) element).resetProblemLevel();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IMarker addMarker(final JaxrsBaseElement element, final ISourceRange range, final String message,
			final String[] messageArguments, final String preferenceKey) throws CoreException {
		addProblem(element, range, message, messageArguments, preferenceKey);
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IMarker addMarker(final JaxrsBaseElement element, final ISourceRange range, final String message,
			final String[] messageArguments, final String preferenceKey, final int quickFixId) throws CoreException {
		addProblem(element, range, message, messageArguments, preferenceKey);
		return null;
	}

	/**
	 * @param element
	 * @param range
	 * @param message
	 * @param messageArguments
	 * @param preferenceKey
	 * @param resource
	 * @throws CoreException
	 */
	private void addProblem(final JaxrsBaseElement element, final ISourceRange range, final String message,
			final String[] messageArguments, final String preferenceKey) throws CoreException {
		// (range == null) occurs when there is no value at all for the annotation
		if(element == null || range == null) {
			return;
		}
		final IResource resource = element.getResource();
		if(asYouTypeValidation) {
			Logger.debug("Reporting message '{}' on resource '{}'", message, resource.getFullPath().toString());
			final IMessage validationMessage = addMessage(resource, range.getOffset(), range.getLength(), preferenceKey, message, messageArguments);
			if (validationMessage != null) {
				element.setProblemSeverity(validationMessage.getSeverity());
			}
		} else {
			Logger.debug("Reporting marker '{}' on resource '{}'", message, resource.getFullPath().toString());
			final IMarker marker = addError(message, preferenceKey, messageArguments, range.getLength(), range.getOffset(), resource);
			if (marker != null) {
				marker.setAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, preferenceKey);
				element.setProblemSeverity(marker.getAttribute(IMarker.SEVERITY, 0));
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IMarker addMarker(final JaxrsMetamodel metamodel, final String message, final String[] messageArguments,
			final String preferenceKey) throws CoreException {
		final IProject project = metamodel.getProject();
		if(asYouTypeValidation) {
			Logger.debug("Reporting message '{}' on resource '{}'", message, project.getFullPath().toString());
			final IMessage validationMessage = addMessage(project, 0, 0, preferenceKey, message, messageArguments);
			if (validationMessage != null) {
				metamodel.setProblemSeverity(validationMessage.getSeverity());
			}
		} else {
			Logger.debug("Reporting marker '{}' on resource '{}'", message, project.getFullPath().toString());
			final IMarker marker = addError(message, preferenceKey, messageArguments, 0, 0, project);
			if (marker != null) {
				marker.setAttribute(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, preferenceKey);
				metamodel.setProblemSeverity(marker.getAttribute(IMarker.SEVERITY, 0));
			}
		}
		return null;
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
	public boolean shouldValidateAsYouType(final IProject project) {
		return shouldValidate(project);
	}

}
