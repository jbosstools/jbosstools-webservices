package org.jboos.tools.ws.creation.core.data;

public class ServiceModel {

private String  webProjectName;
	
	private boolean serverStatus;
	private String  wsdlURI;
	private String portName;
	private String serviceName;
	private String customPackage;
	private String bindingFileLocation;
	private String serviceClass;
	
	
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
	public String getPortName() {
		return portName;
	}
	public void setPortName(String portName) {
		this.portName = portName;
	}
	public String getServiceName() {
		return serviceName;
	}
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
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
	
	public String getBindingFileLocation(){
		return this.bindingFileLocation;
	}
	public void setBindingFileLcation(String bindingFileLocation){
		this.bindingFileLocation = bindingFileLocation;
	}
}
