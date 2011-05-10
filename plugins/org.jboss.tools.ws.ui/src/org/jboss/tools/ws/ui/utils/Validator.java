/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 * yyyymmdd bug      Email and other contact information
 * -------- -------- -----------------------------------------------------------
 * 20060612   142290 gilberta@ca.ibm.com - Gilbert Andrews
 *******************************************************************************/

package org.jboss.tools.ws.ui.utils;

public class Validator
{
  public static final boolean validateString(String input)
  {
    return ((input != null) && (input.trim().length() > 0));
  }

  public static final boolean validateURL(String input)
  {
	  return (input != null && input.matches("[a-zA-Z\\+\\-\\.]++:.*")); //$NON-NLS-1$
  }
  
  public static final boolean validateInteger(String input)
  {
    try
    {
      Integer.parseInt(input);
      return true;
    }
    catch (NumberFormatException e)
    {
      return false;
    }
  }
}
