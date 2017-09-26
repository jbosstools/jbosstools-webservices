/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.ws.reddeer.swt.condition;

import org.eclipse.reddeer.common.condition.AbstractWaitCondition;
import org.jboss.tools.ws.reddeer.ui.tester.views.WsTesterView;

/**
 * Condition is fulfilled when response body in {@link WsTesterView} is not empty.
 *
 * @author jjankovi
 * @author Radoslav Rabara
 */
public class WsTesterNotEmptyResponseText extends AbstractWaitCondition {

	private WsTesterView wsTesterView = new WsTesterView();

	@Override
	public boolean test() {
		return !(wsTesterView.getResponseBody().isEmpty());
	}

	@Override
	public String description() {
		return "WS Tester View has empty response message";
	}
}
