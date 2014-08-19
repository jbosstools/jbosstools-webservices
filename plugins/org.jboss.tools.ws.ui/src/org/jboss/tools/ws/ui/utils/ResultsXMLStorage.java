/******************************************************************************* 
 * Copyright (c) 2010 - 2014 Red Hat, Inc. and others.  
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.ui.utils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/**
 * Temporary in-memory storage for WS invocation results
 * to pass to the XML editor.
 * @author bfitzpat
 *
 */
public class ResultsXMLStorage implements IStorage {
	  private String string;
	  
	  public ResultsXMLStorage(String input) {
	    this.string = input;
	  }
	 
	  public InputStream getContents() throws CoreException {
	    return new ByteArrayInputStream(string.getBytes());
	  }
	 
	  public IPath getFullPath() {
	    return null;
	  }
	 
	  @SuppressWarnings("rawtypes")
	public Object getAdapter(Class adapter) {
	    return null;
	  }
	 
	  public String getName() {
	    int len = Math.min(5, string.length());
	    return string.substring(0, len).concat("..."); //$NON-NLS-1$
	  }
	 
	  public boolean isReadOnly() {
	    return true;
	  }
}
