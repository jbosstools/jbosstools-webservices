/******************************************************************************* 
 * Copyright (c) 2012 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jdt.core.IJavaProject;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;

/**
 * Mutex Scheduling Rule to avoid concurrent JAX-RS Metamodel Build jobs on the same project.
 * 
 * @author Xavier Coulon
 *
 */
public class MutexJobSchedulingRule implements ISchedulingRule {

	/** The java project being built. */
	private final IJavaProject javaProject;
	
	/** 
	 * Private singleton constructor
	 */
	public MutexJobSchedulingRule(final IJavaProject javaProject) {
		super();
		this.javaProject = javaProject;
	}
	
	@Override
	public boolean contains(final ISchedulingRule rule) {
		return rule == this;
	}

	/**
	 * Returns true if the given {@link ISchedulingRule} is a {@link MutexJobSchedulingRule} and applies on the same {@link IProject} 
	 */
	@Override
	public boolean isConflicting(final ISchedulingRule otherRule) {
		final boolean conflict = otherRule instanceof MutexJobSchedulingRule && ((MutexJobSchedulingRule)otherRule).getProject().equals(this.javaProject);
		if(conflict) {
			Logger.trace("Rule conflict between {} and {}", this, otherRule);
		}
		return conflict;
	}


	/**
	 * @return the project
	 */
	public IJavaProject getProject() {
		return javaProject;
	}

}
