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
import org.eclipse.swt.graphics.Image;
import org.eclipse.text.edits.MultiTextEdit;
import org.jboss.tools.common.refactoring.BaseMarkerResolution;
import org.jboss.tools.common.refactoring.MarkerResolutionUtils;
import org.jboss.tools.ws.jaxrs.ui.JBossJaxrsUIPlugin;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;

/**
 * @author Xavier Coulon
 *
 */
public abstract class AbstractAnnotationMarkerResolution extends BaseMarkerResolution  {
	
	/** the SourceType to annotate. */
	private final IType type;

	/** The annotation name to use when inserting/updating the given annotation */
	private final String annotationName;
	
	/** The annotation value(s) to use when inserting/updating the given annotation */
	private final String annotationValue;
	
	/** The kind of edit (ADD/UPDATE). */
	private final int editMode;
	
	/** Mode to ADD the annotation. */
	public static final int ADD = 0;

	/** Mode to UPDATE the annotation. */
	public static final int UPDATE = 1;
	
	/**
	 * Constructor
	 * @param type the type on which the annotation value should be added or updated
	 * @param annotationName the name of annotation to add or update
	 * @param annotationValue the annotation value to set
	 * @param editMode the edit mode (ADD/UPDATE)
	 * @param quickFixLabel the label of the quickfix to display
	 */
	public AbstractAnnotationMarkerResolution(final IType type, final String annotationName, final String annotationValue, final int editMode, final String label){
		super(type.getCompilationUnit());
		this.type = type;
		this.annotationName = annotationName;
		this.annotationValue = '(' + annotationValue + ')';
		this.editMode = editMode;
		this.label = label;
		init();
	}

	@Override
	protected CompilationUnitChange getChange(ICompilationUnit compilationUnit){
		final CompilationUnitChange change = new CompilationUnitChange("", compilationUnit);
		final MultiTextEdit edit = new MultiTextEdit();
		change.setEdit(edit);
		try{
			MarkerResolutionUtils.addImport(annotationName, compilationUnit, edit);
			if(editMode == ADD) {
				MarkerResolutionUtils.addAnnotation(annotationName, compilationUnit, type, annotationValue , edit);
			} else {
				MarkerResolutionUtils.updateAnnotation(annotationName, compilationUnit, type, annotationValue, edit);
			}
		} catch (JavaModelException e) {
			Logger.error("Failed to add '@" + annotationName + "' annotation on type '" + type.getFullyQualifiedName() + "'", e);
		}
		return change;
	}

	@Override
	public Image getImage() {
		return JBossJaxrsUIPlugin.getDefault().getImage("annotation_obj.gif");
	}
	
	abstract String[] getImports();


}
