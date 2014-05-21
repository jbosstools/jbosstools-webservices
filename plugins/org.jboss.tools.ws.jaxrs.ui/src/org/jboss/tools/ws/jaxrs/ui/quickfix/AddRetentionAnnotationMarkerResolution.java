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


import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.osgi.util.NLS;
import org.eclipse.text.edits.MultiTextEdit;
import org.jboss.tools.common.refactoring.MarkerResolutionUtils;
import org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;

/**
 * @author Xavier Coulon
 *
 */
public class AddRetentionAnnotationMarkerResolution extends AbstractAnnotationMarkerResolution  {
	
	/**
	 * Constructor.
	 * @param type the type on which the {@code @java.lang.annotation.Retention} annotation should be added 
	 * @param annotationValue the new annotation value(s) to set
	 */
	public AddRetentionAnnotationMarkerResolution(final IType type, final String annotationValues){
		super(type, JaxrsClassnames.RETENTION,  annotationValues, AbstractAnnotationMarkerResolution.ADD, NLS.bind(JaxrsQuickFixMessages.ADD_RETENTION_ANNOTATION_MARKER_RESOLUTION_TITLE, type.getElementName()));
	}
	
	/**
	 * Adds the import declaration for the {@code java.lang.annotation.RetentionPolicy} class
	 */
	@Override
	protected CompilationUnitChange getChange(ICompilationUnit compilationUnit) {
		final CompilationUnitChange change = super.getChange(compilationUnit);
		final MultiTextEdit edit = new MultiTextEdit();
		change.addEdit(edit);
		try{
			MarkerResolutionUtils.addImport("java.lang.annotation.RetentionPolicy", compilationUnit);
		} catch (JavaModelException e) {
			Logger.error("Failed to add import for 'java.lang.annotation.RetentionPolicy' on '" + compilationUnit.getElementName() +"'", e);
		}
		return change;
	}

	@Override
	String[] getImports() {
		return new String[]{"java.lang.annotation.Retention", "java.lang.annotation.RetentionPolicy"};
	}


}
