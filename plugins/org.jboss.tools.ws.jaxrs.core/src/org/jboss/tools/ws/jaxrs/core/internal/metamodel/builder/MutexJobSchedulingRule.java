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

/**
 * Mutex Scheduling Rule to avoid concurrent JAX-RS Metamodel Build jobs on the same project.
 * 
 * @author Xavier Coulon
 *
 */
public class MutexJobSchedulingRule implements ISchedulingRule {

	private final IProject project;
	
	/** 
	 * Private singleton constructor
	 */
	public MutexJobSchedulingRule(final IProject project) {
		super();
		this.project = project;
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
		return (otherRule instanceof MutexJobSchedulingRule && ((MutexJobSchedulingRule)otherRule).getProject().equals(this.project));
	}


	/**
	 * @return the project
	 */
	public IProject getProject() {
		return project;
	}

}
