/******************************************************************************* 
 * Copyright (c) 2008 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.ws.jaxrs.ui.cnf;

import java.util.Iterator;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.jboss.tools.ws.jaxrs.core.metamodel.ResolvedUriMapping;
import org.jboss.tools.ws.jaxrs.core.metamodel.ResourceMethod;
import org.jboss.tools.ws.jaxrs.ui.JBossJaxrsUIPlugin;

/**
 * @author xcoulon
 * 
 */
public class UriMappingsLabelProvider implements IStyledLabelProvider, ILabelProvider {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		if (element instanceof UriPathTemplateCategory) {
			return JBossJaxrsUIPlugin.getDefault().createImage("wsdl_file_obj.gif");
		}

		if (element instanceof UriPathTemplateElement) {
			if(((UriPathTemplateElement)element).hasErrors()) {
				return JBossJaxrsUIPlugin.getDefault().createImage("url_mapping_error.gif");
			}
			return JBossJaxrsUIPlugin.getDefault().createImage("url_mapping.gif");
		}

		if (element instanceof UriPathTemplateMediaTypeMappingElement) {
			switch (((UriPathTemplateMediaTypeMappingElement) element).getMediaType()) {
			case CONSUMES:
				return JBossJaxrsUIPlugin.getDefault().createImage("filter_mapping_in.gif");
			case PROVIDES:
				return JBossJaxrsUIPlugin.getDefault().createImage("filter_mapping_out.gif");
			}
		}

		if (element instanceof UriPathTemplateMethodMappingElement) {
			return JBossJaxrsUIPlugin.getDefault().createImage("servlet_mapping.gif");
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.
	 * jface.viewers.ILabelProviderListener)
	 */
	public void addListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	public void dispose() {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang
	 * .Object, java.lang.String)
	 */
	public boolean isLabelProperty(Object element, String property) {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse
	 * .jface.viewers.ILabelProviderListener)
	 */
	public void removeListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public StyledString getStyledText(Object element) {
		if (element instanceof UriPathTemplateCategory) {
			return new StyledString("RESTful Web Services");
		}

		if (element instanceof UriPathTemplateElement) {
			ResolvedUriMapping uriMapping = ((UriPathTemplateElement) element).getResolvedUriMapping();
			StringBuilder sb = new StringBuilder();
			String httpVerb = uriMapping.getHTTPMethod().getHttpVerb();
			String uriTemplate = uriMapping.getFullUriPathTemplate();
			sb.append(httpVerb);
			sb.append(" ");
			sb.append(uriTemplate);
			StyledString styledString = new StyledString(sb.toString());
			styledString.setStyle(0, httpVerb.length(), StyledString.QUALIFIER_STYLER);
			return styledString;
		}

		if (element instanceof UriPathTemplateMediaTypeMappingElement) {
			UriPathTemplateMediaTypeMappingElement mappingElement = ((UriPathTemplateMediaTypeMappingElement) element);
			StringBuilder sb = new StringBuilder();
			for (Iterator<String> iterator = mappingElement.getMediaTypes().iterator(); iterator.hasNext();) {
				sb.append(iterator.next());
				if (iterator.hasNext()) {
					sb.append(",");
				}
			}
			return new StyledString(sb.toString());
		}
		if (element instanceof UriPathTemplateMethodMappingElement) {
			ResourceMethod lastMethod = ((UriPathTemplateMethodMappingElement) element).getLastMethod();
			StringBuilder sb = new StringBuilder();
			IMethod javaMethod = lastMethod.getJavaElement();
			// TODO : add method parameters from signature
			sb.append(javaMethod.getParent().getElementName()).append(".").append(javaMethod.getElementName())
					.append("(...)");
			return new StyledString(sb.toString());
		}
		return null;
	}

	@Override
	public String getText(Object element) {
		return getStyledText(element).getString();
	}

}
