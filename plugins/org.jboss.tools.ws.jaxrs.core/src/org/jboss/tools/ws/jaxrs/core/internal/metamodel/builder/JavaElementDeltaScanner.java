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
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder;

import static org.eclipse.jdt.core.IJavaElement.COMPILATION_UNIT;
import static org.eclipse.jdt.core.IJavaElement.JAVA_PROJECT;
import static org.eclipse.jdt.core.IJavaElement.PACKAGE_FRAGMENT_ROOT;
import static org.eclipse.jdt.core.IJavaElementDelta.ADDED;
import static org.eclipse.jdt.core.IJavaElementDelta.CHANGED;
import static org.eclipse.jdt.core.IJavaElementDelta.F_ADDED_TO_CLASSPATH;
import static org.eclipse.jdt.core.IJavaElementDelta.F_AST_AFFECTED;
import static org.eclipse.jdt.core.IJavaElementDelta.F_CONTENT;
import static org.eclipse.jdt.core.IJavaElementDelta.F_FINE_GRAINED;
import static org.eclipse.jdt.core.IJavaElementDelta.F_OPENED;
import static org.eclipse.jdt.core.IJavaElementDelta.F_REMOVED_FROM_CLASSPATH;
import static org.eclipse.jdt.core.IJavaElementDelta.REMOVED;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.IJavaElementDeltaFlag.F_SIGNATURE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JavaMethodSignature;
import org.jboss.tools.ws.jaxrs.core.internal.utils.ConstantUtils;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.jdt.Flags;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJavaMethodSignature;

/**
 * Scans and filters the IJavaElementDelta and IResourceDelta (including their children and annotations) and returns a
 * list of JavaElementChangedEvents that match with the JavaElementChangedEventFilter rules.
 * 
 * @author xcoulon
 * @see @{IJavaElementDelta}
 * @see @{IResourceDelta}
 * @see @{JavaElementChangedEvent}
 * @see @{JavaElementChangedEventFilter}
 */
public class JavaElementDeltaScanner {

	private final JavaElementDeltaFilter javaElementChangedEventFilter = new JavaElementDeltaFilter();

	public List<JavaElementChangedEvent> scanAndFilterEvent(ElementChangedEvent event, IProgressMonitor progressMonitor)
			throws CoreException {
		try {
			progressMonitor.beginTask("Analysing changes", 1);
			Logger.debug("Some java elements changed on a {} event ",
					ConstantUtils.getStaticFieldName(ElementChangedEvent.class, event.getType()));
			return scanDelta(event.getDelta(), event.getType());
		} finally {
			progressMonitor.done();
		}
	}

	/**
	 * Recursively analyse the given Java Element Delta.
	 * 
	 * @param delta
	 * @param eventType
	 * @throws CoreException
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=100267
	 */
	private List<JavaElementChangedEvent> scanDelta(final IJavaElementDelta delta, final int eventType) throws CoreException {
		final List<JavaElementChangedEvent> events = new ArrayList<JavaElementChangedEvent>();
		final IJavaElement element = delta.getElement();
		// skip as the project is closed
		if (element == null) {
			Logger.debug("** skipping this build because the delta element is null **");
			return Collections.emptyList();
		} else if(element.getElementType() == IJavaElement.JAVA_PROJECT && !element.getJavaProject().getProject().isOpen() && delta.getFlags() != F_OPENED) {
			Logger.debug("** skipping this build because the java project is closed. **");
			return Collections.emptyList();
		} else if ((element.getElementType() == IJavaElement.PACKAGE_FRAGMENT_ROOT)) {
			final IPackageFragmentRoot packageFragmentRoot = (IPackageFragmentRoot) element;
			if (!packageFragmentRoot.isExternal()
					&& (packageFragmentRoot.getResource() == null || !packageFragmentRoot.getResource().exists())) {
				return Collections.emptyList();
			}
		} else if (element.getResource() == null || !element.getResource().exists()) {
			return Collections.emptyList();
		}
		final int elementKind = element.getElementType();
		final int deltaKind = retrieveDeltaKind(delta);
		final Flags flags = new Flags(delta.getFlags());
		if(elementKind == JAVA_PROJECT ){
			final JavaElementChangedEvent event = new JavaElementChangedEvent(element, delta.getKind(), eventType, null, new Flags(delta.getFlags()));
			if (javaElementChangedEventFilter.apply(event)) {
				events.add(event);
				// skip anything below
				return events;
			}
		}
		final CompilationUnit compilationUnitAST = getCompilationUnitAST(delta);
		if (elementKind == COMPILATION_UNIT) {
			final ICompilationUnit compilationUnit = (ICompilationUnit) element;
			// compilationUnitAST is null when the given compilation unit'w
			// working copy is being commited (ie, Java Editor is being closed
			// for the given compilation unit, etc.)
			if (compilationUnit.exists() // see https://issues.jboss.org/browse/JBIDE-12760: compilationUnit may not exist
					&& compilationUnit.isWorkingCopy() && compilationUnitAST != null) {
				// assuming possible changes in the method signatures (return type,
				// param types and param annotations). Other changes in methods
				// (renaming, adding/removing params) result in add+remove
				// events on the given method itself.
				if(requiresDiffsComputation(flags)) {
					for(IType type : compilationUnit.getAllTypes()) {
						for(IMethod javaMethod : type.getMethods()) {
							final JavaElementChangedEvent event = new JavaElementChangedEvent(javaMethod, CHANGED, eventType,
									compilationUnitAST, new Flags(F_SIGNATURE));
							if (javaElementChangedEventFilter.apply(event)) {
								events.add(event);
							}
						}
					}
				}
			}
		} 
		// element is part of the compilation unit
		else if(compilationUnitAST != null){
			final JavaElementChangedEvent event = new JavaElementChangedEvent(element, deltaKind, eventType, compilationUnitAST,
					flags);
			if (javaElementChangedEventFilter.apply(event)) {
				events.add(event);
			}
		}
		// continue with children elements, both on annotations and other java
		// elements.
		for (IJavaElementDelta affectedChild : delta.getAffectedChildren()) {
			events.addAll(scanDelta(affectedChild, eventType));
		}
		for (IJavaElementDelta annotation : delta.getAnnotationDeltas()) {
			events.addAll(scanDelta(annotation, eventType));
		}
		return events;
	}

	/**
	 * Returns the {@link CompilationUnit} associated with the
	 * {@link IJavaElement} of the given {@link IJavaElementDelta}.
	 * 
	 * @param delta the given Java Element Delta
	 * @return the associated Compilation Unit AST or null
	 * @throws JavaModelException
	 */
	private CompilationUnit getCompilationUnitAST(final IJavaElementDelta delta) throws JavaModelException {
		CompilationUnit compilationUnitAST = null;
		final IJavaElement element = delta.getElement();
		compilationUnitAST = delta.getCompilationUnitAST();
		if (compilationUnitAST == null) {
			//compilationUnitAST = compilationUnitsRepository.getAST(compilationUnit);
			compilationUnitAST = JdtUtils.parse(element, new NullProgressMonitor());
		}
		return compilationUnitAST;
	}

	/**
	 * @param flags
	 * @return
	 */
	private boolean requiresDiffsComputation(final Flags flags) {
		return flags.hasExactValue(F_CONTENT, F_FINE_GRAINED) 
				|| flags.hasExactValue(F_CONTENT, F_FINE_GRAINED, F_AST_AFFECTED);
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
