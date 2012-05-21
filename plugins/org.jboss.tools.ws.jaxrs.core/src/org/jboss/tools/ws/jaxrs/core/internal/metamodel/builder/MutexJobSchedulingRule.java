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

import org.eclipse.core.runtime.jobs.ISchedulingRule;

/**
 * @author Xavier Coulon
 *
 */
public class MutexJobSchedulingRule implements ISchedulingRule {

	private final static MutexJobSchedulingRule instance = new MutexJobSchedulingRule();
	
	/** 
	 * Private singleton constructor
	 */
	private MutexJobSchedulingRule() {
		super();
	}
	
	public static MutexJobSchedulingRule getInstance() {
		return instance;
	}
	
	@Override
	public boolean contains(ISchedulingRule rule) {
		return rule == this;
	}

	@Override
	public boolean isConflicting(ISchedulingRule rule) {
		return rule == this;
	}

}
