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
package org.jboss.tools.ws.jaxrs.ui.quickfix;


import java.lang.annotation.RetentionPolicy;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.text.edits.MultiTextEdit;
import org.jboss.tools.common.refactoring.BaseMarkerResolution;
import org.jboss.tools.common.refactoring.MarkerResolutionUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;

/**
 * @author Xavier Coulon
 *
 */
public class AddRetentionAnnotationMarkerResolution extends BaseMarkerResolution  {
	
	private final IType type;

	public AddRetentionAnnotationMarkerResolution(IType type){
		super(type.getCompilationUnit());
		this.type = type;
		label = NLS.bind(JaxrsQuickFixMessages.ADD_RETENTION_ANNOTATION_MARKER_RESOLUTION_TITLE, type.getElementName());
		init();
	}

	@Override
	protected CompilationUnitChange getChange(ICompilationUnit compilationUnit){
		CompilationUnitChange change = new CompilationUnitChange("", compilationUnit);
		MultiTextEdit edit = new MultiTextEdit();
		change.setEdit(edit);
		try{
			MarkerResolutionUtils.addImport(EnumJaxrsClassname.RETENTION.qualifiedName, compilationUnit, edit);
			MarkerResolutionUtils.addImport(RetentionPolicy.class.getName(), compilationUnit, edit);
			MarkerResolutionUtils.addAnnotation(EnumJaxrsClassname.RETENTION.simpleName, compilationUnit, type, "(RetentionPolicy.RUNTIME)", edit);
		} catch (JavaModelException e) {
			Logger.error("Failed to add @Retention annotation on type " + type.getFullyQualifiedName(), e);
		}
		return change;
	}

	@Override
	public Image getImage() {
		// TODO Auto-generated method stub
		return null;
	}


}
