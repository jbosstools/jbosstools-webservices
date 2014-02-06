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

package org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain;

import static org.eclipse.jdt.core.IJavaElementDelta.ADDED;
import static org.eclipse.jdt.core.IJavaElementDelta.CHANGED;
import static org.eclipse.jdt.core.IJavaElementDelta.REMOVED;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.indexation.LuceneFields.FIELD_COMPILATION_UNIT_IDENTIFIER;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.indexation.LuceneFields.FIELD_JAVA_APPLICATION;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.indexation.LuceneFields.FIELD_JAVA_CLASS_NAME;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.indexation.LuceneFields.FIELD_JAVA_ELEMENT;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.indexation.LuceneFields.FIELD_JAVA_PROJECT_IDENTIFIER;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.indexation.LuceneFields.FIELD_JAXRS_ELEMENT;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.indexation.LuceneFields.FIELD_JAXRS_PROBLEM_TYPE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.indexation.LuceneFields.FIELD_PACKAGE_FRAGMENT_ROOT_IDENTIFIER;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.indexation.LuceneFields.FIELD_PROVIDER_KIND;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.indexation.LuceneFields.FIELD_RESOURCE_PATH;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.indexation.LuceneFields.FIELD_RETURNED_TYPE_NAME;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.indexation.LuceneFields.FIELD_TYPE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.indexation.LuceneFields.FIELD_WEBXML_APPLICATION;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.LockObtainFailedException;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.common.validation.IValidatingProjectSet;
import org.jboss.tools.common.validation.IValidatingProjectTree;
import org.jboss.tools.common.validation.internal.ProjectValidationContext;
import org.jboss.tools.common.validation.internal.SimpleValidatingProjectTree;
import org.jboss.tools.common.validation.internal.ValidatingProjectSet;
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCorePlugin;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JavaElementDelta;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementChangedProcessorDelegate;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.ResourceDelta;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.indexation.JaxrsElementsIndexationDelegate;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.indexation.LuceneDocumentFactory;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation.JaxrsMetamodelValidator;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.internal.utils.WtpUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsApplication;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElementChangedListener;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsEndpointChangedListener;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsProvider;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResource;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsStatus;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsEndpointDelta;

/**
 * Manages all the JAX-RS domain classes of the JAX-RS Metamodel. Not only a
 * POJO, but also provides business services.
 * 
 * @author xcoulon
 */
public class JaxrsMetamodel implements IJaxrsMetamodel {

	/**
	 * The qualified name of the metamodel when stored in the project session
	 * properties.
	 */
	public static final QualifiedName METAMODEL_QUALIFIED_NAME = new QualifiedName(JBossJaxrsCorePlugin.PLUGIN_ID,
			"metamodel");

	/** The enclosing JavaProject. */
	private final IJavaProject javaProject;

	/** Indicates if the element has problems. */
	private int problemLevel;

	/**
	 * Internal store of all the JAX-RS elements of this metamodel (elements are
	 * indexed by the handleIdentifier of their associated java element).
	 */
	private final Map<String, JaxrsBaseElement> elements = new HashMap<String, JaxrsBaseElement>();

	/**
	 * Internal store of all the JAX-RS Endpoints, indexed by their unique
	 * indentified.
	 */
	private final Map<String, JaxrsEndpoint> endpoints = new HashMap<String, JaxrsEndpoint>();

	/** The JAX-RS Elements and Endpoint indexation delegate. */
	private final JaxrsElementsIndexationDelegate indexationService;

	/** The Listeners for JAX-RS Element changes. */
	private final Set<IJaxrsElementChangedListener> elementChangedListeners = new HashSet<IJaxrsElementChangedListener>();

	/** The Listeners for JAX-RS Endpoint changes. */
	private final Set<IJaxrsEndpointChangedListener> endpointChangedListeners = new HashSet<IJaxrsEndpointChangedListener>();

	/** A boolean marker that indicates if the metamodel is being initialized (ie, first/full build).*/
	private boolean initializing=true;

	/** The Project Validation Tree, including the Validation Context. 
	 *  The reason is that a new validation context should not be created at each validation, it should be kept between builds as it contains information about changed (or cleaned ones in this case) resources.
	 * */
	private SimpleValidatingProjectTree validatingProjectTree = null;

	/** The last known build status for this metamodel. */
	private IStatus buildStatus = Status.OK_STATUS;

	/** A Read/Write Lock to avoid concurrent access to the elements while changes are being processed. */
	private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);

	/**
	 * Full constructor.
	 * 
	 * @param javaProject
	 *            the enclosing java project
	 * @throws CoreException
	 *             in case of underlying exception
	 * @throws IOException
	 * @throws LockObtainFailedException
	 * @throws CorruptIndexException
	 */
	private JaxrsMetamodel(final IJavaProject javaProject) throws CoreException {
		this.javaProject = javaProject;
		indexationService = new JaxrsElementsIndexationDelegate(this);
		addBuiltinHttpMethods();
	}

	/**
	 * Sets the last known build {@link IStatus} for this JAX-RS Metamodel.
	 * This allows external classes to know if the last build completed successfully or not.
	 * @param buildStatus the last known build status
	 */
	public void setBuildStatus(final IStatus buildStatus) {
		this.buildStatus = buildStatus;
	}
	
	/**
	 * Returns the last known build {@link IStatus} for this JAX-RS Metamodel.
	 * This allows external classes to know if the last build completed successfully or not.
	 */
	public IStatus getBuildStatus() {
		return this.buildStatus;
	}


	/**
	 * Initializes and returns the {@link IValidatingProjectTree} associated with the undelying {@link IProject} for this {@link JaxrsMetamodel}
	 * @return the Validating Project Tree 
	 */
	public IValidatingProjectTree getValidatingProjectTree() {
		if(this.validatingProjectTree == null) {
			final Set<IProject> projects = new HashSet<IProject>();
			projects.add(javaProject.getProject());
			final IValidatingProjectSet projectSet = new ValidatingProjectSet(javaProject.getProject(), projects, new ProjectValidationContext());
			this.validatingProjectTree = new SimpleValidatingProjectTree(projectSet);
		}
		return this.validatingProjectTree;
	}

	@Override
	public boolean isInitializing() {
		return this.initializing;
	}
	

	/**
	 * Resets the problem level for this given element.
	 */
	public void resetProblemLevel() {
		indexationService.unindexMarkers(getProject());
		this.problemLevel = 0;
	}

	/**
	 * Registers a marker (from the underlying {@link IResource}) and potentially raises the
	 * problem level on the metamodel itself. If this metamodel already has an higher problem
	 * level, that later value is kept.
	 * Also, the marker is indexed on the underlying resource and the custom JAX-RS Problem type.
	 * 
	 * @param marker: the marker that has been added to the underlying project.
	 *            
	 * @throws CoreException
	 * 
	 * @see {@link JaxrsMetamodelValidator#JAXRS_PROBLEM_MARKER_ID}
	 */
	public void registerMarker(final IMarker marker) {
		indexationService.indexMarker(marker);
		this.problemLevel = Math.max(this.problemLevel, marker.getAttribute(IMarker.SEVERITY, 0));
	}
	
	/**
	 * Removes (deletes and unindexes) the markers for the given {@link IResource}.
	 * @param resource a JAX-RS Element's underlying resource.
	 * @throws CoreException 
	 */
	public void removeMarkers(final IResource resource) throws CoreException {
		Logger.debug("Clearing JAX-RS markers for resource " + resource.getName());
		indexationService.unindexMarkers(resource);
		resource.deleteMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID, true, IResource.DEPTH_ONE);
	}
	
	/**
	 * Removes (deletes and unindexes) the given {@link IMarker}.
	 * @param marker the lonely marker to unindex and delete
	 * @throws CoreException 
	 */
	public void removeMarker(final IMarker marker) throws CoreException {
		indexationService.unindexMarker(marker);
		marker.delete();
	}


	/**
	 * @return <code>Math.max</code> between the internal problem level and all its endpoints problem level.
	 * @see IMarker for the severity level (value "0" meaning
	 *      "no problem, dude")
	 */
	public final int getProblemLevel() {
		int globalLevel = problemLevel;
		for(Entry<String, JaxrsBaseElement> entry : this.elements.entrySet()) {
			globalLevel = Math.max(globalLevel, entry.getValue().getProblemLevel());
		}
		return globalLevel;
	}

	/**
	 * Preload the HttpMethods collection with 6 items from the specification:
	 * <ul>
	 * <li>@GET</li>
	 * <li>@POST</li>
	 * <li>@PUT</li>
	 * <li>@DELETE</li>
	 * <li>@OPTIONS</li>
	 * <li>@HEAD</li>
	 * </ul>
	 * 
	 * @throws CoreException
	 */
	private void addBuiltinHttpMethods() throws CoreException {
		JaxrsBuiltinHttpMethod.from("javax.ws.rs.GET", "GET").buildIn(this);
		JaxrsBuiltinHttpMethod.from("javax.ws.rs.POST", "POST").buildIn(this);
		JaxrsBuiltinHttpMethod.from("javax.ws.rs.PUT", "PUT").buildIn(this);
		JaxrsBuiltinHttpMethod.from("javax.ws.rs.DELETE", "DELETE").buildIn(this);
		JaxrsBuiltinHttpMethod.from("javax.ws.rs.OPTIONS", "OPTIONS").buildIn(this);
		JaxrsBuiltinHttpMethod.from("javax.ws.rs.HEAD", "HEAD").buildIn(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jboss.tools.ws.jaxrs.core.internal.metamodel.IMetamodel#getJavaProject
	 * ()
	 */
	public IJavaProject getJavaProject() {
		return javaProject;
	}

	/**
	 * Accessor to the metamodel from the given project's session properties.
	 * 
	 * @param javaProject
	 *            the java project
	 * @return the metamodel or null if none was found or if the given javaProject was null or closed
	 * @throws CoreException
	 *             in case of underlying exception
	 */
	public static JaxrsMetamodel create(final IJavaProject javaProject) throws CoreException {
		Logger.debug("*** Returning a new Metamodel for project {} ***", javaProject.getElementName());
		final JaxrsMetamodel metamodel = new JaxrsMetamodel(javaProject);
		Logger.debug("JAX-RS Metamodel created for project {}", javaProject.getElementName());
		javaProject.getProject().setSessionProperty(METAMODEL_QUALIFIED_NAME, metamodel);
		return metamodel;
	}

	/**
	 * @throws CoreException
	 *             in case of underlying exception
	 * @throws IOException
	 * @throws CorruptIndexException
	 */
	public final void remove() {
		try {
			readWriteLock.writeLock().lock();
			javaProject.getProject().setSessionProperty(METAMODEL_QUALIFIED_NAME, null);
			indexationService.dispose();
			final IProject project = getProject();
			if(project.exists() && project.isOpen()) {
				project.deleteMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID, true, IResource.DEPTH_INFINITE);
				Logger.debug("JAX-RS Problem markers removed.");
			}
		} catch (Exception e) {
			Logger.error("Failed to remove JAX-RS Metamodel for project " + javaProject.getElementName(), e);
		} finally {
			Logger.debug("JAX-RS Metamodel removed for project " + javaProject.getElementName());
			readWriteLock.writeLock().unlock();
		}
	}

	@Override
	public IProject getProject() {
		if (javaProject == null) {
			return null;
		}
		return javaProject.getProject();
	}

	// ********************************************************************************
	// Processing JavaElementDelta (after ElementChangedEvent)
	// ********************************************************************************

	/**
	 * Registers the given listener for further notifications when JAX-RS
	 * Elements changed in this metamodel.
	 * 
	 * @param listener
	 */
	public void addListener(final IJaxrsElementChangedListener listener) {
		this.elementChangedListeners.add(listener);
	}

	/**
	 * Unregisters the given listener for further notifications when JAX-RS
	 * Elements changed in this metamodel.
	 * 
	 * @param listener
	 */
	public void removeListener(final IJaxrsElementChangedListener listener) {
		this.elementChangedListeners.remove(listener);
	}

	/**
	 * Registers the given listener for further notifications when JAX-RS
	 * Endpoints changed in this metamodel.
	 * 
	 * @param listener
	 */
	@Override
	public void addListener(final IJaxrsEndpointChangedListener listener) {
		if(!endpointChangedListeners.contains(listener)) { 
			Logger.debug("*** Registering EndpointChangedListener for project {} ***", javaProject.getElementName());
			this.endpointChangedListeners.add(listener);
		}
	}

	/**
	 * Unregisters the given listener for further notifications when JAX-RS
	 * Endpoints changed in this metamodel.
	 * 
	 * @param listener
	 */
	@Override
	public void removeListener(final IJaxrsEndpointChangedListener listener) {
		this.endpointChangedListeners.remove(listener);
	}

	/**
	 * Notify that a JAX-RS Element changed
	 * 
	 * @param delta
	 *            the delta including the element that changed, the kind of
	 *            change and the assciated flags or
	 *            {@link JaxrsElementDelta#F_NONE} if no change occurred
	 * 
	 * @see {@link JaxrsElementDelta}
	 */
	private void notifyListeners(final JaxrsElementDelta delta) {
		Logger.trace("Notify elementChangedListeners after {}", delta);
		for (IJaxrsElementChangedListener listener : elementChangedListeners) {
			listener.notifyElementChanged(delta);
		}
	}

	/**
	 * Notify that a JAX-RS Endpoint was added/changed/removed
	 * 
	 * @param endpoint
	 *            the endpoint that was added/changed/removed
	 * @param deltaKind
	 *            the kind of change
	 * @param flags
	 *            some optional flags (use {@link JaxrsElementDelta#F_NONE} if
	 *            no change occurred)
	 */
	private void notifyListeners(final IJaxrsEndpoint endpoint, final int deltaKind) {
		if (endpoint != null && !endpointChangedListeners.isEmpty()) {
			JaxrsEndpointDelta delta = new JaxrsEndpointDelta(endpoint, deltaKind);
			Logger.trace("Notify project '{}' elementChangedListeners after {}", javaProject.getElementName(), delta);
			for (IJaxrsEndpointChangedListener listener : endpointChangedListeners) {
				listener.notifyEndpointChanged(delta);
			}
		} else if(endpointChangedListeners.isEmpty()) {
			Logger.debug("*** No Listener for project '{}' to notify after endpoint changes: {} (change={}) ***", javaProject.getElementName(), endpoint, deltaKind);
		}
	}

	// ********************************************************************************
	// Processing JavaElementDelta (after ElementChangedEvent)
	// ********************************************************************************

	/**
	 * Process a single Java Element change
	 * 
	 * @param delta
	 * @param progressMonitor
	 * @throws CoreException
	 */
	public void processJavaElementChange(final JavaElementDelta delta, final IProgressMonitor progressMonitor)
			throws CoreException {
		try {
			Logger.debug("Processing {}", delta);
			readWriteLock.writeLock().lock();
			final IJavaElement element = delta.getElement();
			final CompilationUnit ast = delta.getCompilationUnitAST();
			final int deltaKind = delta.getKind();
			if (element.getElementType() == IJavaElement.ANNOTATION) {
				processJavaAnnotationChange((IAnnotation) element, deltaKind, ast, progressMonitor);
			} else {
				processJavaElementChange(element, deltaKind, ast, progressMonitor);
			}
		} finally {
			progressMonitor.done();
			readWriteLock.writeLock().unlock();
			setBuildStatus(Status.OK_STATUS);
			Logger.debug("Done processing Java changes.");
		}
	}

	/**
	 * Process {@link IJavaElement} change.
	 * 
	 * @param element
	 * @param deltaKind
	 * @param ast
	 * @param progressMonitor
	 * @throws JavaModelException
	 * @throws CoreException
	 */
	private void processJavaElementChange(final IJavaElement element, final int deltaKind, final CompilationUnit ast,
			final IProgressMonitor progressMonitor) throws JavaModelException, CoreException {
		if (deltaKind == ADDED) {
			JaxrsElementFactory.createElements(element, ast, this, progressMonitor);
		} else {
			final List<IJaxrsElement> jaxrsElements = searchJaxrsElements(element);
			if (deltaKind == CHANGED) {
				if (jaxrsElements.isEmpty()) {
					JaxrsElementFactory.createElements(element, ast, this, progressMonitor);
				} else {
					for (Iterator<IJaxrsElement> iterator = jaxrsElements.iterator(); iterator.hasNext();) {
						JaxrsJavaElement<?> jaxrsElement = (JaxrsJavaElement<?>) iterator.next();
						jaxrsElement.update(element, ast);
					}
				}
			} else {
				for (Iterator<IJaxrsElement> iterator = jaxrsElements.iterator(); iterator.hasNext();) {
					JaxrsJavaElement<?> jaxrsElement = (JaxrsJavaElement<?>) iterator.next();
					jaxrsElement.remove();
				}
			}
		}
	}

	/**
	 * Process Annotation change.
	 * 
	 * @param element
	 * @param deltaKind
	 * @param ast
	 * @param progressMonitor
	 * @param metamodel
	 * @throws JavaModelException
	 * @throws CoreException
	 */
	private void processJavaAnnotationChange(final IAnnotation javaAnnotation, final int deltaKind,
			final CompilationUnit ast, final IProgressMonitor progressMonitor) throws JavaModelException, CoreException {

		// if the java parent element for the given annotation already matches
		// some JAX-RS element in this metamodel, then just update the JAX-RS
		// element
		final JaxrsJavaElement<?> matchingElement = (JaxrsJavaElement<?>) findElement(javaAnnotation.getParent());
		if (matchingElement != null) {
			final Annotation annotation = JdtUtils.resolveAnnotation(javaAnnotation, ast);
			switch (deltaKind) {
			case ADDED:
				matchingElement.addAnnotation(annotation);
				break;
			case CHANGED:
				matchingElement.updateAnnotation(annotation);
				break;
			case REMOVED:
				matchingElement.removeAnnotation(javaAnnotation);
				break;
			}
		} else {
			JaxrsElementFactory.createElements(javaAnnotation, ast, this, progressMonitor);
		}
	}

	// ********************************************************************************
	// Processing ResourceDelta (after ResourceChangedEvent)
	// ********************************************************************************
	/**
	 * Process the entire project since there was no metamodel yet for it.
	 * 
	 * @param project
	 *            the project
	 * @param progressMonitor
	 *            the progress monitor
	 * @throws CoreException
	 */
	public void processProject(final IProgressMonitor progressMonitor) throws CoreException {
		try {
			readWriteLock.writeLock().lock();
			this.initializing = true;
			progressMonitor.beginTask("Processing project '" + getProject().getName() + "'...", 1);
			// start with a fresh new metamodel
			this.elements.clear();
			this.endpoints.clear();
			this.indexationService.clear();
			addBuiltinHttpMethods();
			Logger.debug("Processing project '" + getProject().getName() + "'...");
			processResourceChange(new ResourceDelta(getProject(), ADDED, 0), progressMonitor);
			if (WtpUtils.hasWebDeploymentDescriptor(getProject())) {
				processWebDeploymentDescriptorChange(
						new ResourceDelta(WtpUtils.getWebDeploymentDescriptor(getProject()), ADDED, 0));
			}
			progressMonitor.worked(1);
		} catch (CoreException e) {
			Logger.error("Failed while processing resource results", e);
		} finally {
			progressMonitor.done();
			readWriteLock.writeLock().unlock();
			setBuildStatus(Status.OK_STATUS);
			Logger.debug("Done processing resource results.");
			this.initializing = false;
		}
	}

	/**
	 * Process the project resource that changed.
	 * 
	 * @param affectedResources
	 *            the affected resources, all in the same project
	 * @param progressMonitor
	 *            the progress monitor
	 */
	public void processAffectedResources(final List<ResourceDelta> affectedResources,
			final IProgressMonitor progressMonitor) {
		try {
			readWriteLock.writeLock().lock();
			progressMonitor.beginTask("Processing Resource " + affectedResources.size() + " change(s)...",
					affectedResources.size());
			Logger.debug("Processing {} Resource change(s)...", affectedResources.size());
			for (ResourceDelta event : affectedResources) {
				processResourceChange(event, progressMonitor);
				progressMonitor.worked(1);
			}
		} catch (CoreException e) {
			Logger.error("Failed while processing Resource results", e);
		} finally {
			progressMonitor.done();
			readWriteLock.writeLock().unlock();
			setBuildStatus(Status.OK_STATUS);
			Logger.debug("Done processing Resource results.");
		}
	}

	/**
	 * Process any resource change.
	 * 
	 * @param event
	 * @param progressMonitor
	 * @return
	 * @throws CoreException
	 */
	private void processResourceChange(final ResourceDelta event, final IProgressMonitor progressMonitor)
			throws CoreException {
		Logger.debug("Processing {}", event);
		final IResource resource = event.getResource();
		if (resource == null) {
			return;
		}
		final IJavaElement javaElement = JavaCore.create(resource);
		// ignore changes on binary files (added/removed/changed jars to improve
		// builder performances)
		if (javaElement != null && !JdtUtils.isArchive(javaElement)) {
			processJavaElement(javaElement, event.getDeltaKind(), progressMonitor);
		} else if (WtpUtils.isWebDeploymentDescriptor(resource)) {
			processWebDeploymentDescriptorChange(new ResourceDelta(resource, event.getDeltaKind(), 0));
		}

	}

	/**
	 * Process the givne {@link IJavaElement} to see if it can be a JAX-RS element
	 * @param javaElement
	 * @param deltaKind
	 * @param progressMonitor
	 * @throws CoreException
	 * @throws JavaModelException
	 */
	private void processJavaElement(final IJavaElement javaElement, final int deltaKind,
			final IProgressMonitor progressMonitor) throws CoreException, JavaModelException {
		final List<JaxrsJavaElement<?>> matchingElements = findElements(javaElement);
		switch (deltaKind) {
		case ADDED:
			JaxrsElementFactory.createElements(javaElement, JdtUtils.parse(javaElement, progressMonitor), this,
					progressMonitor);
			break;
		case CHANGED:
			final CompilationUnit ast = JdtUtils.parse(javaElement, progressMonitor);
			if (matchingElements.isEmpty()) {
				JaxrsElementFactory.createElements(javaElement, ast, this, progressMonitor);
			} else {
				for (JaxrsJavaElement<?> element : matchingElements) {
					// only working on JAX-RS elements bound to IType, not
					// IFields nor IMethods
					// those elements will internally update their own
					// children elements based on IMethods and IFields
					if (element.getJavaElement().getElementType() == IJavaElement.TYPE) {
						element.update(javaElement, ast);
					}
				}
			}
			break;
		case REMOVED:
			for (JaxrsJavaElement<?> element : matchingElements) {
				element.remove();
			}
			break;
		}
	}

	/**
	 * Process change in the 'web.xml' deployment descriptor
	 * @param delta
	 * @throws CoreException
	 */
	private void processWebDeploymentDescriptorChange(final ResourceDelta delta)
			throws CoreException {
		final IResource webxmlResource = delta.getResource();
		final JaxrsWebxmlApplication webxmlElement = (JaxrsWebxmlApplication) findElement(webxmlResource);
		switch (delta.getDeltaKind()) {
		case ADDED:
			JaxrsWebxmlApplication.from(webxmlResource).inMetamodel(this).build();
			break;
		case CHANGED:
			if (webxmlElement != null) {
				webxmlElement.update(webxmlResource);
			} else {
				JaxrsWebxmlApplication.from(webxmlResource).inMetamodel(this).build();
			}
			break;
		case REMOVED:
			if (webxmlElement != null) {
				webxmlElement.remove();
			}
			break;
		}
	}

	// ********************************************************************************
	// JAX-RS Element Addition, Update and Removal
	// ********************************************************************************

	/**
	 * Adds the given {@link JaxrsBaseElement} element into the JAX-RS Metamodel, including its
	 * indexation if the underlying {@link IJavaElement} is not already part of the metamodel (avoiding duplicate elements)
	 * 
	 * @param element
	 *            the element to add
	 * @throws CoreException
	 */
	public void add(final JaxrsBaseElement element) throws CoreException {
		try {
			readWriteLock.writeLock().lock();
			if (element == null || findElementByIdentifier(element) != null) {
				return;
			}
			this.elements.put(element.getIdentifier(), element);
			indexationService.indexElement(element);
			notifyListeners(new JaxrsElementDelta(element, ADDED));
			processElementChange(new JaxrsElementDelta(element, ADDED));
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	/**
	 * Cascade effect on JAX-RS Endpoints
	 * 
	 * @param element
	 *            the element that was added/changed/removed
	 * @param deltaKind
	 *            the kind of delta (ADDED / CHANGD / REMOVED)
	 * @throws CoreException
	 */
	public void processElementChange(final JaxrsElementDelta delta) throws CoreException {
		try {
			readWriteLock.writeLock().lock();
			JaxrsElementChangedProcessorDelegate.processEvent(delta);
		} finally {
			readWriteLock.writeLock().unlock();
		}

	}

	/**
	 * Updates the given JAX-RS Element in the Metamodel index.
	 * 
	 * @param element
	 *            the element to update
	 * @throws CoreException
	 * @see {@link JaxrsElementDelta} for flag values
	 */
	public void update(final JaxrsElementDelta delta) throws CoreException {
		if (delta.isRelevant()) {
			try {
				readWriteLock.writeLock().lock();
				indexationService.reindexElement(delta.getElement());
				notifyListeners(delta);
				processElementChange(delta);
			} finally {
				readWriteLock.writeLock().unlock();
			}
		}
	}
	
	/**
	 * Notifies all registered listeners that the problem level of the given {@link IJaxrsElement} changed
	 * @param element the JAX-RS element whose problem level changed
	 */
	public void notifyElementProblemLevelChanged(final IJaxrsElement element) {
		final List<JaxrsEndpoint> affectedEndpoints = findEndpoints(element);
		for (JaxrsEndpoint affectedEndpoint : affectedEndpoints) {
			for (IJaxrsEndpointChangedListener listener : endpointChangedListeners) {
				listener.notifyEndpointProblemLevelChanged(affectedEndpoint);
			}
		}
	}

	/**
	 * Notifies all registered listeners that the problem level of this {@link JaxrsMetamodel} changed
	 */
	public void notifyMetamodelProblemLevelChanged() {
		for (IJaxrsEndpointChangedListener listener : endpointChangedListeners) {
			listener.notifyMetamodelProblemLevelChanged(this);
		}
	}

	/**
	 * Updates the given JAX-RS Endpoint in the Metamodel index.
	 * 
	 * @param element
	 *            the element to update
	 */
	public void update(final JaxrsEndpoint endpoint) {
		// skip null endpoints
		if (endpoint == null) {
			return;
		}
		try {
			readWriteLock.writeLock().lock();
			indexationService.reindexElement(endpoint);
			notifyListeners(endpoint, CHANGED);
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	/**
	 * Removes the given element from the JAX-RS Metamodel, including from the
	 * index.
	 * 
	 * @param elements
	 *            the element to remove
	 * @throws CoreException
	 */
	// FIXME: make protected instead of public
	protected void remove(final IJaxrsElement element) throws CoreException {
		if (element == null) {
			return;
		}
		try {
			readWriteLock.writeLock().lock();
			processElementChange(new JaxrsElementDelta(element, REMOVED));
			// actual removal and unindexing should be done at the end
			elements.remove(element.getIdentifier());
			indexationService.unindexElement(element);
			notifyListeners(new JaxrsElementDelta(element, REMOVED));
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	/**
	 * Removes the given endpoint from the JAX-RS Metamodel, including from the
	 * index.
	 * 
	 * @param elements
	 *            the element to remove
	 * @throws CoreException
	 */
	protected void remove(final JaxrsEndpoint endpoint) {
		if (endpoint == null) {
			return;
		}
		try {
			readWriteLock.writeLock().lock();
			endpoints.remove(endpoint.getIdentifier());
			indexationService.unindexEndpoint(endpoint);
			notifyListeners(endpoint, REMOVED);
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	// ********************************************************************************
	// JAX-RS Element retrieval
	// ********************************************************************************

	/**
	 * Search for the JAX-RS java-based elements matching the given
	 * {@link IJavaElement} in the metamodel.
	 * 
	 * @param element
	 * @param ast
	 * @return the matching JAX-RS Elements or an empty set if no JAX-RS
	 *         element matched in the metamodel.
	 * @throws JavaModelException
	 */
	private List<IJaxrsElement> searchJaxrsElements(final IJavaElement element) throws JavaModelException {
		if (element == null) {
			return Collections.emptyList();
		}
		try {
			readWriteLock.readLock().lock();
			final List<IJaxrsElement> result = new ArrayList<IJaxrsElement>();
			final Term javaElementTerm = new Term(FIELD_JAVA_ELEMENT, Boolean.TRUE.toString());
			switch (element.getElementType()) {
			case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				final Term packageFragmentRootIdentifier = new Term(FIELD_PACKAGE_FRAGMENT_ROOT_IDENTIFIER,
						element.getHandleIdentifier());
				result.addAll(searchJaxrsElements(javaElementTerm, packageFragmentRootIdentifier));
				break;
			case IJavaElement.COMPILATION_UNIT:
				final Term compilationUnitTerm = new Term(FIELD_COMPILATION_UNIT_IDENTIFIER, element.getHandleIdentifier());
				result.addAll(searchJaxrsElements(javaElementTerm, compilationUnitTerm));
				break;
			case IJavaElement.TYPE:
			case IJavaElement.FIELD:
			case IJavaElement.METHOD:
				final IJaxrsElement foundElement = this.elements.get(element.getHandleIdentifier());
				if (foundElement != null) {
					result.add(foundElement);
				}
				break;
			}
			return result;
		} finally {
			readWriteLock.readLock().unlock();
		}

	}

	/**
	 * Retrieves the JAX-RS Elements whose identifier matches the given temrs.
	 * 
	 * @param expectedElementType the expected type of elements to retrieve. Because generics are not flexible :-( 
	 * @param terms
	 *            the search terms
	 * @return the JAX-RS Elements or empty list if none was found.
	 */
	private <T extends IJaxrsElement> List<T> searchJaxrsElements(final Term... terms) {
		try {
			readWriteLock.readLock().lock();
			return indexationService.searchElements(terms);
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	/**
	 * Retrieves the JAX-RS Endpoints whose identifier matches the given terms.
	 * 
	 * @param terms
	 *            the search terms
	 * @return the JAX-RS Element or null if none was found.
	 */
	private List<JaxrsEndpoint> searchJaxrsEndpoints(Term... terms) {
		Logger.debugIndexing("Searching for Endpoints with using: {}", Arrays.asList(terms));
		return indexationService.searchEndpoints(terms);
	}

	@SuppressWarnings("unchecked")
	private <T extends IJaxrsStatus> T searchJaxrsElement(Term... terms) {
		final String matchingIdentifier = indexationService.searchElement(terms);
		final T element = (T) this.elements.get(matchingIdentifier);
		if (element == null) {
			Logger.trace("No element matching terms", (Object[]) terms);
		}
		return element;
	}

	/**
	 * Returns an unmodifiable set of all the elements in the Metamodel.
	 * 
	 * @return
	 */
	public Collection<JaxrsBaseElement> getAllElements() {
		try {
			readWriteLock.readLock().lock();
			return elements.values();
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	/**
	 * Returns the {@link IJaxrsElement} for the given identifier.
	 * 
	 * @param identifier the element identifier
	 * @return the matching element or {@code null} if none matched.
	 */
	public IJaxrsElement getElement(final String identifier) {
		try {
			readWriteLock.readLock().lock();
			return this.elements.get(identifier);
		} finally {
			readWriteLock.readLock().unlock();
		}

	}

	
	/**
	 * Searches and returns all JAX-RS elements those underlying resource is the
	 * given resource
	 * 
	 * @param resource
	 *            the resource
	 * @return the matching JAX-RS elements
	 */
	public List<IJaxrsElement> getElements(final IResource resource) {
		if (resource == null) {
			return Collections.emptyList();
		}
		try {
			readWriteLock.readLock().lock();
			Term resourcePathTerm = new Term(FIELD_RESOURCE_PATH, resource.getFullPath().toPortableString());
			return searchJaxrsElements(resourcePathTerm);
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	/**
	 * Searches and returns a single JAX-RS element those underlying resource is
	 * the given resource
	 * 
	 * @param resource
	 *            the resource
	 * @return the matching JAX-RS element
	 */
	private JaxrsBaseElement findElement(IResource resource) {
		if (resource == null) {
			return null;
		}
		final Term resourcePathTerm = new Term(FIELD_RESOURCE_PATH, resource.getFullPath().toPortableString());
		return searchJaxrsElement(resourcePathTerm);
	}

	/**
	 * Searches and returns the JAX-RS Element matching the given
	 * Identifier, or null if no element with the same identifier already exists in the Metamodel
	 * 
	 * @param element
	 *            the element (as returned by {@link IJaxrsElement#getIdentifier()})
	 * @return the JAX-RS Element matching the given identifier or <code>null</code>.
	 */
	private IJaxrsStatus findElementByIdentifier(final IJaxrsElement element) {
		return searchJaxrsElement(LuceneDocumentFactory.getIdentifierTerm(element));
	}
	
	/**
	 * Searches and returns all JAX-RS Java-based Elements matching the given
	 * {@link IJavaElement}, which can be {@link Annotation} {@link IProject},
	 * {@link IPackageFragmentRoot}, {@link ICompilationUnit} or an
	 * {@link IMember}
	 * 
	 * @param expectedElementsType
	 *            the expected type of the elements to find.
	 * @param element
	 *            the java element
	 * 
	 * @return the JAX-RS Elements matching the given Java Element or empty list
	 *         if none matches.
	 */
	public <T extends IJaxrsElement> List<T> findElements(final IJavaElement javaElement) {
		if (javaElement == null) {
			return Collections.emptyList();
		}
		try {
			readWriteLock.readLock().lock();
			final String identifier = javaElement.getHandleIdentifier();
			switch (javaElement.getElementType()) {
			case IJavaElement.JAVA_PROJECT:
				return searchJaxrsElements(new Term(FIELD_JAVA_PROJECT_IDENTIFIER, identifier));
			case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				return searchJaxrsElements(new Term(FIELD_PACKAGE_FRAGMENT_ROOT_IDENTIFIER, identifier));
			case IJavaElement.COMPILATION_UNIT:
				return searchJaxrsElements(new Term(FIELD_COMPILATION_UNIT_IDENTIFIER, identifier));
			default:
				return searchJaxrsElements(LuceneDocumentFactory.getIdentifierTerm(javaElement));
			}
		} finally {
			readWriteLock.readLock().unlock();
		}
	}
	
	/**
	 * Finds and returns a {@link List} of {@link IResource}s that have the given problemType as an {@link IMarker} attached
	 * to them, or empty list if none was found.
	 * @param problemType the problem type to look for
	 * @return the resources having the given problem type.
	 * @throws CoreException 
	 * @throws NumberFormatException 
	 */
	public List<IResource> findResourcesWithProblemOfType(final String problemType) {
		if(problemType == null) {
			return Collections.emptyList();
		}
		try {
			readWriteLock.readLock().lock();
			final Term categoryTerm = new Term(FIELD_TYPE, IMarker.class.getSimpleName());
			final Term probleTypeTerm = new Term(FIELD_JAXRS_PROBLEM_TYPE, problemType);
			return indexationService.searchResources(categoryTerm, probleTypeTerm);
		} finally {
			readWriteLock.readLock().unlock();
		}
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public IJaxrsElement findElement(final IJavaElement javaElement) {
		if (javaElement == null) {
			return null;
		}
		try {
			readWriteLock.readLock().lock();
			return searchJaxrsElement(LuceneDocumentFactory.getIdentifierTerm(javaElement));
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	/**
	 * returns true if this metamodel already contains the given element.
	 * 
	 * @param element
	 * @return
	 */
	public boolean containsElement(JaxrsBaseElement element) {
		try {
			if(element == null) {
				return false;
			}
			readWriteLock.readLock().lock();
			return this.elements.containsKey(element.getIdentifier());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	/**
	 * @return all the JAX-RS Application in the JAX-RS Metamodel The result is a
	 *         separate unmodifiable list
	 */
	public final List<IJaxrsApplication> getAllApplications() {
		try {
			readWriteLock.readLock().lock();
			Term categoryTerm = new Term(FIELD_TYPE, EnumElementCategory.APPLICATION.toString());
			return searchJaxrsElements(categoryTerm);
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	/**
	 * <p>
	 * Returns the application that is used to compute the Endpoint's URI Path
	 * Templates, or null if no application was specified in the code. An
	 * invalid application may be returned, though (ie, a Type annotated with
	 * {@link javax.ws.rs.ApplicationPath} but not extending the
	 * {@link javax.ws.rs.Application} type). If multiple applications have been
	 * defined, the pure web.xml one is returned.
	 * </p>
	 * 
	 * @return the application or null if none exist yet.
	 */
	public final IJaxrsApplication getApplication() {
		try {
			readWriteLock.readLock().lock();
			// try to return pure web.xml first
			final JaxrsWebxmlApplication webxmlApplication = findWebxmlApplication();
			if (webxmlApplication != null && webxmlApplication.exists()) {
				return webxmlApplication;
			}
			// otherwise, return first existing java-based application
			final List<JaxrsJavaApplication> javaApplications = getJavaApplications();
			for (JaxrsJavaApplication application : javaApplications) {
				if (application.exists()) {
					return application;
				}
			}
			return null;
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	/**
	 * Returns the Application (Java or Webxml) those underlying resource
	 * matches the given resource, or null if not found
	 * 
	 * @param resource
	 * @return the associated application or null
	 */
	public final IJaxrsApplication getApplication(final IResource resource) {
		try {
			if (resource == null) {
				return null;
			}
			readWriteLock.readLock().lock();
			final Term resourceTerm = new Term(FIELD_RESOURCE_PATH, resource.getFullPath().toPortableString());
			final Term categoryTerm = new Term(FIELD_TYPE, EnumElementCategory.APPLICATION.toString());
			return searchJaxrsElement(resourceTerm, categoryTerm);
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	/**
	 * @return the web.xml based application and all the java-based application
	 *         overrides, or an empty collection if none exist in the metamodel.
	 */
	public final List<JaxrsJavaApplication> getJavaApplications() {
		try {
			readWriteLock.readLock().lock();
			final Term categoryTerm = new Term(FIELD_TYPE, EnumElementCategory.APPLICATION.toString());
			final Term kindTerm = new Term(FIELD_JAVA_APPLICATION, Boolean.TRUE.toString());
			return searchJaxrsElements(categoryTerm, kindTerm);
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	/**
	 * @return the java application that matches the given classname, or null if
	 *         none was found.
	 */
	public final JaxrsJavaApplication findJavaApplicationByTypeName(final String typeName) {
		if(typeName == null) {
			return null;
		}
		try {
			readWriteLock.readLock().lock();
			final Term classNameTerm = new Term(FIELD_JAVA_CLASS_NAME, typeName);
			final Term categoryTerm = new Term(FIELD_TYPE, EnumElementCategory.APPLICATION.toString());
			final Term kindTerm = new Term(FIELD_JAVA_APPLICATION, Boolean.TRUE.toString());
			final String matchingIdentifier = indexationService.searchElement(classNameTerm, categoryTerm, kindTerm);
			return (JaxrsJavaApplication) elements.get(matchingIdentifier);
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	/**
	 * @return the web.xml based application and all the java-based application
	 *         overrides, or an empty collection if none exist in the metamodel.
	 */
	public final List<JaxrsWebxmlApplication> findWebxmlApplications() {
		try {
			readWriteLock.readLock().lock();
			final Term categoryTerm = new Term(FIELD_TYPE, EnumElementCategory.APPLICATION.toString());
			final Term kindTerm = new Term(FIELD_WEBXML_APPLICATION, Boolean.TRUE.toString());
			return searchJaxrsElements(categoryTerm, kindTerm);
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	/**
	 * @return the <strong>pure JEE</strong> web.xml based application. There
	 *         can be only one (at most), defined with the
	 *         <code>javax.ws.rs.core.Application</code> class name. Other
	 *         web.xml application declarations are just to override the
	 *         java-based application <code>@ApplicationPath</code> value and
	 *         <strong>will not be returned</strong> by this method.
	 */
	public final JaxrsWebxmlApplication findWebxmlApplication() {
		try {
			readWriteLock.readLock().lock();
			final Term categoryTerm = new Term(FIELD_TYPE, EnumElementCategory.APPLICATION.toString());
			final Term kindTerm = new Term(FIELD_WEBXML_APPLICATION, Boolean.TRUE.toString());
			final String elementIdentifier = indexationService.searchElement(categoryTerm, kindTerm);
			return (JaxrsWebxmlApplication) elements.get(elementIdentifier);
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	/**
	 * @return the webxml application that mat)ches the given classname, or null
	 *         if none was found.
	 */
	public final JaxrsWebxmlApplication findWebxmlApplicationByClassName(final String className) {
		if(className == null) {
			return null;
		}
		try {
			readWriteLock.readLock().lock();
			final Term classNameTerm = new Term(FIELD_JAVA_CLASS_NAME, className);
			final Term categoryTerm = new Term(FIELD_TYPE, EnumElementCategory.APPLICATION.toString());
			final Term kindTerm = new Term(FIELD_WEBXML_APPLICATION, Boolean.TRUE.toString());
			final String matchingIdentifier = indexationService.searchElement(classNameTerm, categoryTerm, kindTerm);
			return (JaxrsWebxmlApplication) elements.get(matchingIdentifier);
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	/**
	 * Returns all the JAX-RS HTTP Methods in the Metamodel.
	 * 
	 * @return the JAX-RS HTTP Methods
	 */
	public final List<IJaxrsHttpMethod> findAllHttpMethods() {
		try {
			readWriteLock.readLock().lock();
			final Term categoryTerm = new Term(FIELD_TYPE, EnumElementCategory.HTTP_METHOD.toString());
			return searchJaxrsElements(categoryTerm);
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	/**
	 * Search and return the JAX-RS HTTP Method for the given fully qualified
	 * java name
	 * 
	 * @param typeName
	 *            the fully qualified java name
	 * @return the matching JAX-RS HTTP Method or null if none was found
	 * @throws CoreException
	 */
	public JaxrsHttpMethod findHttpMethodByTypeName(final String typeName) throws CoreException {
		if (typeName == null) {
			return null;
		}
		try {
			readWriteLock.readLock().lock();
			final Term categoryTerm = new Term(FIELD_TYPE, EnumElementCategory.HTTP_METHOD.toString());
			final Term typeTerm = new Term(FIELD_JAVA_CLASS_NAME, typeName);
			return searchJaxrsElement(categoryTerm, typeTerm);
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	/**
	 * Search for a JAX-RS Provider matching the given JavaType.
	 * 
	 * @param providerType
	 *            : the underlying Java Type for this Provider
	 * @return the JAX-RS Provider or null if not found
	 */
	public IJaxrsProvider findProvider(final IType providerType) {
		if (providerType == null) {
			return null;
		}
		try {
			readWriteLock.readLock().lock();
			final Term projectTerm = new Term(FIELD_JAVA_PROJECT_IDENTIFIER, getJavaProject().getHandleIdentifier());
			final Term categoryTerm = new Term(FIELD_TYPE, EnumElementCategory.PROVIDER.toString());
			final Term typeTerm = new Term(FIELD_JAVA_CLASS_NAME, providerType.getFullyQualifiedName());
			return searchJaxrsElement(projectTerm, categoryTerm, typeTerm);
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	/**
	 * Search for a JAX-RS Resource matching the given JavaType.
	 * 
	 * @param resourceType
	 *            : the underlying Java Type for this JAX-RS Resource
	 * @return the JAX-RS Resource or null if not found
	 */
	public JaxrsResource findResource(IType resourceType) {
		if (resourceType == null) {
			return null;
		}
		try {
			readWriteLock.readLock().lock();
			final Term projectTerm = new Term(FIELD_JAVA_PROJECT_IDENTIFIER, getJavaProject().getHandleIdentifier());
			final Term categoryTerm = new Term(FIELD_TYPE, EnumElementCategory.RESOURCE.toString());
			final Term typeTerm = new Term(FIELD_JAVA_CLASS_NAME, resourceType.getFullyQualifiedName());
			return searchJaxrsElement(projectTerm, categoryTerm, typeTerm);
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	/**
	 * Search for a JAX-RS Resource Methods matching the given Returned Type.
	 * 
	 * @param returnedType
	 *            : the returned type of the JAX-RS Resource Methods
	 * @return the JAX-RS Resource or empty list if not found
	 */
	public List<IJaxrsResourceMethod> findResourceMethodsByReturnedType(final IType returnedType) {
		if (returnedType == null) {
			return Collections.emptyList();
		}
		try {
			readWriteLock.readLock().lock();
			final Term projectTerm = new Term(FIELD_JAVA_PROJECT_IDENTIFIER, getJavaProject().getHandleIdentifier());
			final Term categoryTerm = new Term(FIELD_TYPE, EnumElementCategory.RESOURCE_METHOD.toString());
			final Term typeTerm = new Term(FIELD_RETURNED_TYPE_NAME, returnedType.getFullyQualifiedName());
			return searchJaxrsElements(projectTerm, categoryTerm, typeTerm);
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	/**
	 * Search for all JAX-RS Providers that are associated with the given Type
	 * Name for the given Kind.
	 * <p>
	 * For example, looking for an {@link EnumElementKind#EXCEPTION_MAPPER} that
	 * should catch exceptions of type
	 * <code>javax.persistence.EntityNotFoundException</code>.
	 * </p>
	 * 
	 * @param providerKind
	 *            : the kind of provider
	 * @param providedClassName
	 *            : the associated classname of the provider
	 * @return the JAX-RS Providers or empty list if no match
	 */
	public List<JaxrsProvider> findProviders(final EnumElementKind providerKind, final String providedClassName) {
		if (providerKind == null || providedClassName == null) {
			return Collections.emptyList();
		}
		try {
			readWriteLock.readLock().lock();
			final Term projectTerm = new Term(FIELD_JAVA_PROJECT_IDENTIFIER, getJavaProject().getHandleIdentifier());
			final Term categoryTerm = new Term(FIELD_TYPE, EnumElementCategory.PROVIDER.toString());
			final Term providerKindTerm = new Term(FIELD_PROVIDER_KIND + providerKind.toString(), providedClassName);
			return searchJaxrsElements(projectTerm, categoryTerm, providerKindTerm);
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	/**
	 * Searches and retrives all the {@link JaxrsEndpoint}s in the metamodel
	 * that use the given {@link JaxrsResourceMethod}
	 * 
	 * @param resourceMethod
	 *            the resource method used by the searched Endpoints
	 * @return a collection containing zero or more matches, or
	 *         empty list if the input was {@code null}.
	 */
	public List<JaxrsEndpoint> findEndpoints(final IJaxrsElement element) {
		if (element == null) {
			return Collections.emptyList();
		}
		try {
			readWriteLock.readLock().lock();
			final Term projectTerm = new Term(FIELD_JAVA_PROJECT_IDENTIFIER, getJavaProject().getHandleIdentifier());
			final Term categoryTerm = new Term(FIELD_TYPE, EnumElementCategory.ENDPOINT.toString());
			final Term jaxrsElementTerm = new Term(FIELD_JAXRS_ELEMENT, element.getIdentifier());
			return searchJaxrsEndpoints(projectTerm, categoryTerm, jaxrsElementTerm);
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	/**
	 * Returns all the JAX-RS Resources in the Metamodel.
	 * 
	 * @return the JAX-RS Resources
	 */
	public final List<IJaxrsResource> getAllResources() {
		try {
			readWriteLock.readLock().lock();
			final Term categoryTerm = new Term(FIELD_TYPE, EnumElementCategory.RESOURCE.toString());
			return searchJaxrsElements(categoryTerm);
		} finally {
			readWriteLock.readLock().unlock();
		}
	}

	public boolean add(JaxrsEndpoint endpoint) {
		try {
			readWriteLock.writeLock().lock();
			// skip
			if (endpoint == null || this.endpoints.containsValue(endpoint)) {
				return false;
			}
			this.endpoints.put(endpoint.getIdentifier(), endpoint);
			indexationService.indexElement(endpoint);
			notifyListeners(endpoint, ADDED);
			return true;
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@Override
	public List<IJaxrsEndpoint> getAllEndpoints() {
		try {
			readWriteLock.readLock().lock();
			return new ArrayList<IJaxrsEndpoint>(this.endpoints.values());
		} finally {
			readWriteLock.readLock().unlock();
		}
	}
	
	/**
	 * Returns the endpoint identified by the given identifier.
	 * 
	 * @param identifier
	 *            the endpoint identifier
	 * @return the endpoint or {@code null} if none exists.
	 */
	public JaxrsEndpoint getEndpoint(String identifier) {
		try {
			readWriteLock.readLock().lock();
			return this.endpoints.get(identifier);
		} finally {
			readWriteLock.readLock().unlock();
		}
	}




	/**
	 * Removes all {@link IJaxrsEndpoint}s from this metamodel.
	 * 
	 * @param resourceMethod
	 */
	public void removeEndpoints(final IJaxrsElement removedElement) {
		try {
			readWriteLock.writeLock().lock();
			final List<JaxrsEndpoint> elementEndpoints = findEndpoints(removedElement);
			for (JaxrsEndpoint endpoint : elementEndpoints) {
				endpoint.remove();
			}
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((javaProject == null) ? 0 : javaProject.getHandleIdentifier().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return "JAX-RS Metamodel for project '" + this.javaProject.getElementName() + "'";
	}

	/**
	 * Returns a displayable (in logs) status of this metamodel.
	 * @return
	 */
	public String getStatus() {
		return new StringBuilder("JAX-RS Metamodel for project '").append(getProject().getName()).append("' now has ")
				.append(getAllApplications().size()).append(" Applications, ").append(findAllHttpMethods().size())
				.append(" HttpMethods, ").append(getAllResources().size()).append(" Resources and ")
				.append(getAllEndpoints().size()).append(" Endpoints.").toString();
	}


}
