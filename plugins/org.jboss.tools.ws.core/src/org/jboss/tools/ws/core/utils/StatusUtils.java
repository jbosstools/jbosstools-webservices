/******************************************************************************* 
 * Copyright (c) 2008 - 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.ws.core.utils;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;

public class StatusUtils
{
  public static IStatus errorStatus( String errorMessage )
  {
    return new Status( IStatus.ERROR, "id", 0, errorMessage, null ); //$NON-NLS-1$
  }
  
  public static IStatus errorStatus( Throwable exc )
  {
    String message = exc.getMessage();
    
    return new Status( IStatus.ERROR, "id", 0, message == null ? "" : message, exc ); //$NON-NLS-1$ //$NON-NLS-2$
  }
  
  public static IStatus errorStatus( String message, Throwable exc )
  {
    return new Status( IStatus.ERROR, "id", 0, message, exc ); //$NON-NLS-1$
  }
  
  public static MultiStatus multiStatus( String message, IStatus[] children, Throwable exc )
  {
    return new MultiStatus( "id", 0, children, message, exc );   //$NON-NLS-1$
  }
  
  public static MultiStatus multiStatus( String message, IStatus[] children )
  {
    return new MultiStatus( "id", 0, children, message, null );   //$NON-NLS-1$
  }
  
  public static IStatus warningStatus( String warningMessage )
  {
    return new Status( IStatus.WARNING, "id", 0, warningMessage, null ); //$NON-NLS-1$
  }
  
  public static IStatus warningStatus( String warningMessage, Throwable exc )
  {
    return new Status( IStatus.WARNING, "id", 0, warningMessage, exc ); //$NON-NLS-1$
  }
  
  public static IStatus infoStatus( String infoMessage )
  {
    return new Status( IStatus.INFO, "id", 0, infoMessage, null ); //$NON-NLS-1$
  }
}