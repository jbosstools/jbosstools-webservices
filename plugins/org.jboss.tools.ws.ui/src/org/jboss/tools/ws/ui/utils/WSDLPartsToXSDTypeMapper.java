/*******************************************************************************
 * Copyright (c) 2002, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.ws.ui.utils;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import javax.wsdl.Part;
import javax.xml.namespace.QName;
import org.eclipse.xsd.XSDComponent;
import org.eclipse.xsd.XSDElementDeclaration;
import org.eclipse.xsd.XSDModelGroupDefinition;
import org.eclipse.xsd.XSDNamedComponent;
import org.eclipse.xsd.XSDSchema;
import org.eclipse.xsd.XSDTypeDefinition;

public class WSDLPartsToXSDTypeMapper
{
  private final char POUND = '#';
  private Vector<Object> xsdSchemaList_;
  private Hashtable<String, XSDNamedComponent> partToXSDCache_;

  public WSDLPartsToXSDTypeMapper() {
    xsdSchemaList_ = new Vector<Object>();
    partToXSDCache_ = new Hashtable<String, XSDNamedComponent>();
  }

  public void addSchemas(Vector<?> schemaList) {
    for (int i=0;i<schemaList.size();i++) {
      Object schema = schemaList.elementAt(i);
      if (schema != null)
        xsdSchemaList_.addElement(schema);
    }
  }

  public XSDNamedComponent getXSDType(Part part, String id) {
    XSDNamedComponent component = getXSDTypeFromCache(id);
    if (component != null)
      return component;
    component = getXSDTypeFromSchema(part);
    if (component != null)
      addToCache(id, component);
    return component;
  }

  public XSDNamedComponent getXSDTypeFromCache(String id) {
    return (XSDNamedComponent)partToXSDCache_.get(id);
  }

  public XSDNamedComponent getXSDTypeFromSchema(Part part) {
    boolean isElementDeclaration = (part.getTypeName() == null);
    QName qName = isElementDeclaration ? part.getElementName() : part.getTypeName();
    return getXSDTypeFromSchema(qName.getNamespaceURI(), qName.getLocalPart(), isElementDeclaration);
  }

  public XSDNamedComponent getXSDTypeFromSchema(String namespaceURI, String localName, boolean isElementDeclaration) {
    for (int i = 0; i < xsdSchemaList_.size(); i++) {
      XSDSchema xsdSchema = (XSDSchema)xsdSchemaList_.elementAt(i);
      Vector<Object> components = new Vector<Object>();
      if (isElementDeclaration)
        components.addAll(xsdSchema.getElementDeclarations());
      else
        components.addAll(xsdSchema.getTypeDefinitions());
      for (Iterator<Object> it = components.iterator(); it.hasNext(); ) {
        XSDNamedComponent component  = (XSDNamedComponent)it.next();
        String compNSURI = component.getTargetNamespace();
        String compLocalname = component.getName();
        if (compNSURI != null && compLocalname != null && compNSURI.equals(namespaceURI) && compLocalname.equals(localName))
          return component;
      }
    }
    return null;
  }

  public XSDNamedComponent resolveXSDNamedComponent(XSDNamedComponent component)
  {
    if (component != null)
    {
      String uri = component.getURI();
      String qname = component.getQName();
      for (int i = 0; i < xsdSchemaList_.size(); i++)
      {
        XSDSchema xsdSchema = (XSDSchema)xsdSchemaList_.elementAt(i);
        if (xsdSchema != null)
        {
          String targetNS = xsdSchema.getTargetNamespace();
          if (targetNS != null && targetNS.equals(trimQName(uri, qname)))
          {
            XSDNamedComponent resolvedComponent = null;
            if (component instanceof XSDTypeDefinition)
              resolvedComponent = xsdSchema.resolveTypeDefinition(qname);
            else if (component instanceof XSDElementDeclaration)
              resolvedComponent = xsdSchema.resolveElementDeclaration(qname);
            else if (component instanceof XSDModelGroupDefinition)
              resolvedComponent = xsdSchema.resolveModelGroupDefinition(qname);
            if (isComponentResolvable(resolvedComponent))
              return resolvedComponent;
          }
        }
      }
    }
    return null;
  }

  private String trimQName(String uri, String qname)
  {
    int index = uri.indexOf(qname);
    if (index != -1)
    {
      String ns = uri.substring(0, index);
      if (ns.charAt(index-1) == POUND)
        return ns.substring(0, index-1);
      else
        return ns;
    }
    else
      return uri;
  }

  private void addToCache(String id, XSDNamedComponent component) {
    partToXSDCache_.put(id,component);
  }

  protected boolean isComponentResolvable(XSDComponent component)
  {
    if (component == null)
      return false;
    XSDSchema schema = component.getSchema();
    if (schema == null)
      return false;
    if (schema.getTargetNamespace() == null)
      return false;
    return true;
  }
}
