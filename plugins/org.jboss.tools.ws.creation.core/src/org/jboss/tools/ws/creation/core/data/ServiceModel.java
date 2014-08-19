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
package org.jboss.tools.ws.creation.core.data;

import java.util.ArrayList;
import java.util.List;

import javax.wsdl.Definition;
import javax.wsdl.Service;

import org.eclipse.jdt.core.IJavaProject;

public class ServiceModel {

private String  webProjectName;
	
	private boolean serverStatus;
	private String  wsdlURI;
	private String customPackage;
	private List<String> bindingFiles;
	private String catalog;
	private List<String> serviceClasses;
	private boolean isGenWSDL;
	private String target;
	private int wsScenario;
	private boolean extension = true;
	private String serviceName;
	private String customClassName;
	private String applicationClassName;
	private String applicationName;
	private String javaSourceFolder;
	private Definition wsdlDefinition;
	private IJavaProject project;
	private Service service;
	private String addOptions;
	private List<String> srcList;

	public int getWsScenario() {
		return wsScenario;
	}

	public void setWsScenario(int wsScenario) {
		this.wsScenario = wsScenario;
	}

	private boolean isGenImplementation = true;

	private boolean UpdateWebxml = true;
	

	public List<String> getServiceClasses(){
		if(serviceClasses == null){
			serviceClasses = new ArrayList<String>();
		}
		return this.serviceClasses;
	}
	
	public void addServiceClasses(String serviceCls){
		this.serviceClasses = getServiceClasses();
		if(!serviceClasses.contains(serviceCls)){
			serviceClasses.add(serviceCls);
		}
	}
	
	public String getCustomPackage() {
		return customPackage;
	}
	public void setCustomPackage(String packageText) {
		this.customPackage = packageText;
	}
	
	public String getWsdlURI() {
		return wsdlURI;
	}
	public void setWsdlURI(String wsdlURI) {
		this.wsdlURI = wsdlURI;
	}
	
	public boolean getServerStatus() {
		return serverStatus;
	}
	public void setServerStatus(boolean b) {
		this.serverStatus = b;
	}
	
	public String getWebProjectName() {
		return webProjectName;
	}
	public void setWebProjectName(String webProjectName) {
		this.webProjectName = webProjectName;
	}
	
	public List<String> getBindingFiles(){
		if(bindingFiles == null){
			bindingFiles = new ArrayList<String>();
		}
		return this.bindingFiles;
	}
	public void addBindingFile(String bindingFileLocation){
		this.bindingFiles = this.getBindingFiles();
		if(!this.bindingFiles.contains(bindingFileLocation)){
			this.bindingFiles.add(bindingFileLocation);
		}
	}

	public boolean isGenWSDL() {
		return isGenWSDL;
	}

	public void setGenWSDL(boolean isGenWSDL) {
		this.isGenWSDL = isGenWSDL;
	}
	
	public String getCatalog(){
		return this.catalog;
	}
	public void setCatalog(String catalog){
		this.catalog = catalog;
	}
	
	public String getTarget(){
		return this.target;
	}
	public void setTarget(String target){
		this.target = target;
	}
	
	public void setGenerateImplementatoin(boolean isGenImpl){
		this.isGenImplementation = isGenImpl;
	}
	
	public boolean isGenImplementation(){
		return this.isGenImplementation;
	}
	
	public boolean isUpdateWebxml(){
		return this.UpdateWebxml;
	}
	
	public void setUpdateWebxml(boolean updateWebxml){
		this.UpdateWebxml = updateWebxml;
	}
	
	public boolean enableSOAP12(){
		return extension;
	}
	
	public void setEnableSOAP12(boolean enable){
		this.extension = enable;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setCustomClassName(String className) {
		this.customClassName = className;
	}

	public String getCustomClassName() {
		return customClassName;
	}

	public String getApplicationClassName() {
		return applicationClassName;
	}

	public void setApplicationClassName(String applicationClassName) {
		this.applicationClassName = applicationClassName;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}
	
	public String getJavaSourceFolder() {
		return javaSourceFolder;
	}

	public void setJavaSourceFolder(String javaSourceFolder) {
		this.javaSourceFolder = javaSourceFolder;
	}
	

	public Definition getWsdlDefinition() {
		return wsdlDefinition;
	}

	public void setWsdlDefinition(Definition wsdlDefinition) {
		this.wsdlDefinition = wsdlDefinition;
	} 
	
	public IJavaProject getJavaProject() {
		return project;
	}

	public void setJavaProject(IJavaProject project) {
		this.project = project;
	}
	
	public Service getService() {
		return service;
	}

	public void setService(Service service) {
		this.service = service;
	}
	
	public String getAddOptions() {
		return addOptions;
	}

	public void setAddOptions(String addOptions) {
		this.addOptions = addOptions;
	}

	public List<String> getSrcList() {
		return srcList;
	}

	public void setSrcList(List<String> srcList) {
		this.srcList = srcList;
	}

}
