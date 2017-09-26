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
package org.jboss.tools.ws.ui.views;

import java.util.Set;

/**
 * @since 2.0
 */
public class TestEntry implements Cloneable{

	private String url;
	private WSType wsTech;
	private String result;
	private Set<WSProperty> resultHeaders;
	private CustomTestEntry customEntry;
	
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * @since 2.0
	 */
	public void setWsTech(WSType wsTech) {
		this.wsTech = wsTech;
	}

	/**
	 * @since 2.0
	 */
	public WSType getWsTech() {
		return wsTech;
	}
	
	/**
	 * @since 2.0
	 */
	public String getResult() {
		return result;
	}
	
	/**
	 * @since 2.0
	 */
	public void setResult(String result) {
		this.result = result;
	}
	
	/**
	 * @since 2.0
	 */
	public void setResultHeaders(Set<WSProperty> headers) {
		this.resultHeaders = headers;
	}
	
	/**
	 * @since 2.0
	 */
	public Set<WSProperty> getResultHeaders() {
		return resultHeaders;
	}
	
	public CustomTestEntry getCustomEntry() {
		return customEntry;
	}
	
	public void setCustomEntry(CustomTestEntry customEntry) {
		this.customEntry = customEntry;
	}
	
	@Override
	public TestEntry clone() throws CloneNotSupportedException {
		TestEntry newEntry = new TestEntry();
		newEntry.setCustomEntry(this.getCustomEntry().clone());
		newEntry.setResult(this.getResult());
		newEntry.setUrl(this.getUrl());
		newEntry.setResultHeaders(this.getResultHeaders());
		newEntry.setWsTech(this.getWsTech());
		return newEntry;
	}
	
	@Override
	public String toString() {
		return "wsTech: "+wsTech.getType()+" url: "+url; //$NON-NLS-1$ //$NON-NLS-2$
	}
	

}
