package org.jboss.tools.ws.jaxrs.core.internal.utils;

import java.io.FileInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jst.j2ee.web.componentcore.util.WebArtifactEdit;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class WtpUtils {

	public static IFolder getWebInfFolder(IProject project) {
		IVirtualComponent component = ComponentCore.createComponent(project);
		if(component == null) {
			return null;
		}
		IVirtualFolder contentFolder = component.getRootFolder();
		final IFolder underlyingFolder = (IFolder) contentFolder.getFolder(WebArtifactEdit.WEB_INF)
				.getUnderlyingFolder();
		return underlyingFolder;
	}

	/**
	 * Attempts to find the applicationPath configured in the application's web deployment description. The
	 * applicationPath is expected to be configured as below: <code>
	 * 	<servlet-mapping>
	 * 		<servlet-name>com.acme.MyApplication</servlet-name>
	 * 		<url-pattern>/hello/*</url-pattern>
	 * 	</servlet-mapping>
	 * </code> where
	 * <code>com.acme.MyApplication</code> is a subtype of <code>javax.ws.rs.Application</code> and is the given
	 * 'applicationType' parameter of this method. If the webapp does not provide its own subtype of
	 * <code>javax.ws.rs.Application</code>, then the applicationType parameter can be
	 * <code>javax.ws.rs.Application</code> itself.
	 * 
	 * @param javaProject
	 *            the current java project
	 * @param applicationTypeName
	 *            the name of the type/subtype to match in the servlet-mapping
	 * @return the applicationPath or null if it is not configured.
	 * @throws CoreException
	 */
	public static String getApplicationPath(IProject project, String applicationTypeName) throws CoreException {
		IFolder webInfFolder = getWebInfFolder(project);
		if(webInfFolder == null) {
			return null;
		}
		IResource webxmlResource = webInfFolder.findMember("web.xml");
		if (webxmlResource == null || !webxmlResource.exists()) {
			Logger.debug("No deployment descriptor found in project '{}'", project.getName());
			return null;
		}
		/*if (webxmlResource.isSynchronized(IResource.DEPTH_INFINITE)) {
			Logger.debug("Resource is not in sync'");
			webxmlResource.refreshLocal(IResource.DEPTH_ZERO, new NullProgressMonitor());
		}*/
		// cannot use the WebTools WebArtifact Edit APIs because the web.xml configuration does not require a servlet,
		// but just a servlet-mapping element.
		/*
		 * WebArtifactEdit webArtifactEdit = WebArtifactEdit.getWebArtifactEditForRead(javaProject.getProject()); //
		 * webArtifactEdit.getDeploymentDescriptorRoot().eResource().unload(); if (!webArtifactEdit.isValid()) { return
		 * null; } webArtifactEdit.getDeploymentDescriptorRoot(); WebApp webApp = webArtifactEdit.getWebApp();
		 * EList<ServletMapping> servletMappings = webApp.getServletMappings(); for (ServletMapping servletMapping :
		 * servletMappings) { if (servletMapping.getName().equals(applicationTypeName)) { return
		 * servletMapping.getUrlPattern(); } }
		 */
		// using a good old xpath expression to scan the file.
		try {
			DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
			domFactory.setNamespaceAware(false); // never forget this!
			domFactory.setValidating(false);
			DocumentBuilder builder = domFactory.newDocumentBuilder();
			final FileInputStream fileInputStream = new FileInputStream(webxmlResource.getLocation().toFile());
			InputSource inputSource = new InputSource(fileInputStream);
			Document doc = builder.parse(inputSource);

			XPath xpath = XPathFactory.newInstance().newXPath();
			String expression = "//servlet-mapping[servlet-name=\"" + applicationTypeName + "\"]/url-pattern/text()";

			XPathExpression expr = xpath.compile(expression);
			Node urlPattern = (Node) expr.evaluate(doc, XPathConstants.NODE);
			if (urlPattern != null) {
				Logger.debug("Found matching url-pattern: {}", urlPattern);
				return urlPattern.getTextContent();
			}
		} catch (Exception e) {
			Logger.error("Unable to parse file '" + webxmlResource.getProjectRelativePath().toOSString()
					+ "' to find <servlet-mapping> elements", e);
		}

		Logger.debug("No servlet mapping found for {} in {}", applicationTypeName,
				webxmlResource.getProjectRelativePath());
		return null;
	}

	/**
	 * Indicates if the given resource is the web deployment descriptor (or not).
	 * 
	 * @param resource
	 * @return
	 */
	public static boolean isWebDeploymentDescriptor(IResource resource) {
		final IFolder webinfFolder = getWebInfFolder(resource.getProject());
		if(webinfFolder == null) {
			return false;
		}
		final IFile file = webinfFolder.getFile("web.xml");
		if (file == null) {
			return false;
		}
		return resource.getFullPath().equals(file.getFullPath());
	}

	/**
	 * Returns true if the given project has a web deployment descriptor, false otherwise.
	 * 
	 * @param project
	 * @return
	 */
	public static boolean hasWebDeploymentDescriptor(IProject project) {
		final IFolder webinfFolder = getWebInfFolder(project);
		if(webinfFolder == null) {
			return false;
		}
		final IFile file = webinfFolder.getFile("web.xml");
		return (file != null && file.exists());
	}

	public static IResource getWebDeploymentDescriptor(IProject project) {
		final IFolder webinfFolder = getWebInfFolder(project);
		final IFile file = webinfFolder.getFile("web.xml");
		if(file != null && file.exists()) {
			return project.findMember(file.getProjectRelativePath());
		}
		return null;
	}

}
