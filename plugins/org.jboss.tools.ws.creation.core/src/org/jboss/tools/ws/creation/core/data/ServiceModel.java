package org.jboss.tools.ws.creation.core.data;

import java.util.ArrayList;
import java.util.List;

public class ServiceModel {

private String  webProjectName;
	
	private boolean serverStatus;
	private String  wsdlURI;
	private List<String> portTypes;
	private List<String> serviceName;
	private String customPackage;
	private List<String> bindingFileLocation;
	private String catalog;
	private String serviceClass;
	private boolean isGenWSDL;
	private String target;
	
	
	public String getServiceClass() {
		return serviceClass;
	}

	public void setServiceClass(String serviceClass) {
		this.serviceClass = serviceClass;
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
		this.getPortTypes().add(portType);
	}
	public void setPortTypeList(List<String> portTypeList) {		
		this.portTypes = portTypeList;
	}
	
	public List<String> getServiceNames() {
		return serviceName;
	}
	public void addServiceName(String serviceName) {
		this.getServiceNames().add(serviceName);
	}
	public void setServiceList(List<String> serviceList) {
		this.serviceName = serviceList;
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
		if(bindingFileLocation == null){
			bindingFileLocation = new ArrayList<String>();
		}
		return this.bindingFileLocation;
	}
	public void addBindingFile(String bindingFileLocation){
		this.bindingFileLocation.add(bindingFileLocation);
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
}
