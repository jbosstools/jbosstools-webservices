package org.jboss.tools.ws.creation.core.data;

import java.util.ArrayList;
import java.util.List;

public class ServiceModel {

private String  webProjectName;
	
	private boolean serverStatus;
	private String  wsdlURI;
	private List<String> portTypes;
	private List<String> serviceNames;
	private String customPackage;
	private List<String> bindingFiles;
	private String catalog;
	private List<String> serviceClasses;
	private boolean isGenWSDL;
	private String target;
	private int wsScenario;
	private boolean extension;
	private String serviceName;
	private String customClassName;

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
	
	public List<String> getPortTypes() {
		if(portTypes == null){
			portTypes = new ArrayList<String>();
		}
		return portTypes;
	}
	public void addPortTypes(String portType) {		
		this.portTypes = getPortTypes();
		if(!this.portTypes.contains(portType)){
			this.portTypes.add(portType);
		}
	}
	public void setPortTypeList(List<String> portTypeList) {		
		this.portTypes = portTypeList;
	}
	
	public List<String> getServiceNames() {
		if(serviceNames == null){
			serviceNames = new ArrayList<String>();
		}
		return serviceNames;
	}
	public void addServiceName(String serviceName) {
		this.serviceNames = getServiceClasses();
		if(!serviceName.contains(serviceName)){
			this.serviceNames.add(serviceName);
		}
	}
	public void setServiceList(List<String> serviceList) {
		this.serviceNames = serviceList;
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
}
