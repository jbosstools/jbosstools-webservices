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
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.ui.JBossJaxrsUIPlugin;
import org.jboss.tools.ws.jaxrs.ui.cnf.UriMappingsContentProvider.LoadingStub;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;

/** @author xcoulon */
public class UriMappingsLabelProvider implements IStyledLabelProvider, ILabelProvider {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
	 */
	@Override
	public Image getImage(Object element) {
		if (element instanceof UriPathTemplateCategory) {
 			return JBossJaxrsUIPlugin.getDefault().getImage("restful_web_services.gif", ((UriPathTemplateCategory) element).getProblemLevel());
		} else if (element instanceof UriPathTemplateElement) {
			return JBossJaxrsUIPlugin.getDefault().getImage("url_mapping.gif", ((UriPathTemplateElement) element).getProblemLevel());
		} else if (element instanceof UriPathTemplateMediaTypeMappingElement) {
			switch (((UriPathTemplateMediaTypeMappingElement) element).getType()) {
			case CONSUMES:
				return JBossJaxrsUIPlugin.getDefault().getImage("filter_mapping_in.gif");
			case PRODUCES:
				return JBossJaxrsUIPlugin.getDefault().getImage("filter_mapping_out.gif");
			}
		} else if (element instanceof UriPathTemplateMethodMappingElement) {
			return JBossJaxrsUIPlugin.getDefault().getImage("servlet_mapping.gif");
		} else if (element instanceof LoadingStub) {
			return JBossJaxrsUIPlugin.getDefault().getImage("systemprocess.gif");
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
	@Override
	public void addListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	@Override
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
	@Override
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
	@Override
	public void removeListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public StyledString getStyledText(Object element) {
		if (element instanceof UriPathTemplateCategory) {
			return new StyledString(UriMappingsLabelProviderMessages.JAXRS_WEB_SERVICES);
		}

		if (element instanceof UriPathTemplateElement) {
			IJaxrsEndpoint endpoint = ((UriPathTemplateElement) element).getEndpoint();
			StringBuilder sb = new StringBuilder();
			String httpVerb = endpoint.getHttpMethod().getHttpVerb();
			String uriTemplate = endpoint.getUriPathTemplate();
			sb.append(httpVerb);
			sb.append(" ");
			sb.append(uriTemplate);
			StyledString styledString = new StyledString(sb.toString());
			styledString.setStyle(0, httpVerb.length(), StyledString.QUALIFIER_STYLER);
			Logger.debug("URI Path Template: {}", styledString.getString());
			return styledString;
		}

		if (element instanceof UriPathTemplateMediaTypeMappingElement) {
			UriPathTemplateMediaTypeMappingElement mappingElement = ((UriPathTemplateMediaTypeMappingElement) element);
			StringBuilder sb = new StringBuilder();
			int offset = 0;
			switch (((UriPathTemplateMediaTypeMappingElement) element).getType()) {
			case CONSUMES:
				sb.append(UriMappingsLabelProviderMessages.CONSUMED_MEDIATYPES);
				offset = sb.length();
				break;
			case PRODUCES:
				sb.append(UriMappingsLabelProviderMessages.PRODUCED_MEDIATYPES);
				offset = sb.length();
				break;
			}
			for (Iterator<String> iterator = mappingElement.getMediaTypes().iterator(); iterator.hasNext();) {
				sb.append(iterator.next());
				if (iterator.hasNext()) {
					sb.append(",");
				}
			}
			StyledString styledString = new StyledString(sb.toString());
			styledString.setStyle(0, offset, StyledString.QUALIFIER_STYLER);
			return styledString;
		}
		if (element instanceof UriPathTemplateMethodMappingElement) {
			IJaxrsResourceMethod lastMethod = ((UriPathTemplateMethodMappingElement) element).getResourceMethod();
			StringBuilder sb = new StringBuilder();
			IMethod javaMethod = lastMethod.getJavaElement();
			// TODO : add method parameters from signature
			sb.append(javaMethod.getParent().getElementName()).append(".").append(javaMethod.getElementName())
					.append("(...)");
			return new StyledString(sb.toString());
		}
		if (element instanceof LoadingStub) {
			String message = UriMappingsLabelProviderMessages.LOADING_CONTENT;
			StyledString styledString = new StyledString(message);
			styledString.setStyle(0, message.length(), StyledString.DECORATIONS_STYLER);
			return new StyledString(message);
		}
		return null;
	}

	@Override
	public String getText(Object element) {
		return getStyledText(element).getString();
	}
	
}
