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

import org.eclipse.core.resources.IStorage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;
import org.jboss.tools.ws.ui.messages.JBossWSUIMessages;

/**
 * Temporary in-memory storage for WS invocation results
 * to pass to the XML editor.
 * @author bfitzpat
 */
public class ResultsXMLStorageInput implements IStorageEditorInput {
	
    private IStorage storage;
    
    public ResultsXMLStorageInput(IStorage storage) {
    	this.storage = storage;
    }
    
    public boolean exists() {
    	return true;
    }
    
    public ImageDescriptor getImageDescriptor() {
    	return null;
    }
    
    public String getName() {
       return storage.getName();
    }
    
    public IPersistableElement getPersistable() {
    	return null;
    }
    
    public IStorage getStorage() {
       return storage;
    }
    
    public String getToolTipText() {
       return JBossWSUIMessages.ResultsXMLStorageInput_WS_Invocation_Results_Prefix + storage.getName();
    }
    
    @SuppressWarnings("rawtypes")
	public Object getAdapter(Class adapter) {
      return null;
    }
}
