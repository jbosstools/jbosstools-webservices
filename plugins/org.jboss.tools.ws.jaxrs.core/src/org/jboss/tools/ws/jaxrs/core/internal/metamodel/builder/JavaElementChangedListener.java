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
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder;

import static org.eclipse.jdt.core.IJavaElement.PACKAGE_FRAGMENT_ROOT;
import static org.eclipse.jdt.core.IJavaElementDelta.ADDED;
import static org.eclipse.jdt.core.IJavaElementDelta.F_ADDED_TO_CLASSPATH;
import static org.eclipse.jdt.core.IJavaElementDelta.F_REMOVED_FROM_CLASSPATH;
import static org.eclipse.jdt.core.IJavaElementDelta.REMOVED;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.configuration.ProjectNatureUtils;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;

/**
 * Listens to all change events (Java elements and resources) and triggers a new job for each change.<br>
 * Yet, it avoids trigger new Jobs for high level changes (JavaModel, WorkspaceRoot, etc.)
 * 
 * @author xcoulon
 */
public class JavaElementChangedListener implements IElementChangedListener {

	/**
	 * {@inheritDoc} (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.core.IElementChangedListener#elementChanged(org.eclipse.jdt.core.ElementChangedEvent)
	 */
	@Override
	public void elementChanged(ElementChangedEvent event) {
		try {
			if (isApplicable(event.getDelta())) {
				logDelta(event.getDelta(), event.getType());
				Job job = new JaxrsMetamodelBuildJob(event);
				job.schedule();
			}
		} catch (CoreException e) {
			Logger.error("Failed to process Java Element change", e);
		}
	}

	private boolean isApplicable(IJavaElementDelta delta) throws CoreException {
		IJavaProject javaProject = delta.getElement().getJavaProject();
		if (javaProject != null) {
			IProject project = javaProject.getProject();
			if (ProjectNatureUtils.isProjectNatureInstalled(project, ProjectNatureUtils.JAXRS_NATURE_ID)) {
				return true;
			}
		}
		// carry on with children elements.
		for (IJavaElementDelta affectedChild : delta.getAffectedChildren()) {
			if(isApplicable(affectedChild)) {
				return true;
			}
		}
		return false;
	}

	private void logDelta(final IJavaElementDelta delta, final int eventType) {
		IJavaElement element = delta.getElement();
		// skip as the project is closed
		int deltaKind = retrieveDeltaKind(delta);
		int flags = delta.getFlags();
		CompilationUnit compilationUnitAST = null;
		final JavaElementDelta event = new JavaElementDelta(element, deltaKind, eventType, compilationUnitAST, flags);
		Logger.trace("Event {}", event);
		// carry on with children elements.
		for (IJavaElementDelta affectedChild : delta.getAffectedChildren()) {
			logDelta(affectedChild, eventType);
		}
		for (IJavaElementDelta annotation : delta.getAnnotationDeltas()) {
			logDelta(annotation, eventType);
		}
	}

	/**
	 * Retrieves the appropriate kind of the given delta, with some specific adaptations for some element types.
	 * 
	 * @param delta
	 *            the delta.
	 * @return the delta kind.
	 * @see {@link IJavaElementDelta}
	 */
	private static int retrieveDeltaKind(IJavaElementDelta delta) {
		IJavaElement element = delta.getElement();
		int elementType = element.getElementType();
		int flags = delta.getFlags();
		switch (elementType) {
		case PACKAGE_FRAGMENT_ROOT:
			switch (flags) {
			case F_ADDED_TO_CLASSPATH:
				return ADDED;
			case F_REMOVED_FROM_CLASSPATH:
				return REMOVED;
			}
		default:
			return delta.getKind();
		}
	}

}
