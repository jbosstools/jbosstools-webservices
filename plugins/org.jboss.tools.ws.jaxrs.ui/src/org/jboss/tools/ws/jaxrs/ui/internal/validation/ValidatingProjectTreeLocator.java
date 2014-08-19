/******************************************************************************* 
 * Copyright (c) 2012 - 2014 Red Hat, Inc. and others.  
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.jaxrs.ui.internal.validation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.jboss.tools.common.validation.IValidatingProjectSet;
import org.jboss.tools.common.validation.IValidatingProjectTree;
import org.jboss.tools.common.validation.internal.ProjectValidationContext;
import org.jboss.tools.common.validation.internal.SimpleValidatingProjectTree;
import org.jboss.tools.common.validation.internal.ValidatingProjectSet;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsMetamodel;

/**
 * A Singleton utility class that holds the {@link IValidatingProjectTree} for a
 * given {@link IJaxrsMetamodel}'s underlying {@link IProject}.
 * 
 * The reason is that a new validation context should not be created at each
 * validation, it should be kept between builds as it contains information about
 * changed (or cleaned ones in this case) resources.
 * 
 * @author xcoulon
 *
 */
public class ValidatingProjectTreeLocator {
	/** The singleton instance. */
	private static ValidatingProjectTreeLocator instance = new ValidatingProjectTreeLocator();
	/**
	 * Map of {@link IValidatingProjectTree} indexed by one of their associated
	 * project name.
	 */
	private final Map<String, IValidatingProjectTree> validatingProjectTrees = new HashMap<String, IValidatingProjectTree>();

	/**
	 * The private singleton constructor
	 */
	private ValidatingProjectTreeLocator() {
		// TODO Auto-generated constructor stub
	}

	public static ValidatingProjectTreeLocator getInstance() {
		return instance;
	}

	public IValidatingProjectTree getValidatingProjects(final IProject project) {
		if (validatingProjectTrees.get(project.getName()) == null) {
			final Set<IProject> projects = new HashSet<IProject>();
			projects.add(project);
			final IValidatingProjectSet projectSet = new ValidatingProjectSet(project, projects,
					new ProjectValidationContext());
			validatingProjectTrees.put(project.getName(), new SimpleValidatingProjectTree(projectSet));
		}
		return validatingProjectTrees.get(project.getName());
	}
}
