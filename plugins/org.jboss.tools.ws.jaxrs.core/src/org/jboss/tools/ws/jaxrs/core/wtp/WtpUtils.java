/******************************************************************************* 
 * Copyright (c) 2011 - 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.jaxrs.core.wtp;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jst.j2ee.web.componentcore.util.WebArtifactEdit;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class WtpUtils {

	public static IFolder getWebInfFolder(IProject project) {
		final IVirtualComponent component = ComponentCore.createComponent(project);
		if (component == null) {
			return null;
		}
		final IVirtualFolder contentFolder = component.getRootFolder();
		return (IFolder) contentFolder.getFolder(WebArtifactEdit.WEB_INF)
				.getUnderlyingFolder();
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
	 * @return the applicationPath or {@code null} if it is not configured.
	 * @throws CoreException
	 */
	public static String getApplicationPath(IResource webxmlResource, String applicationTypeName) throws CoreException {
		final long startTime = System.currentTimeMillis();
		try {
			if (webxmlResource == null) {
				return null;
			}
			final String expression = "//servlet-mapping[servlet-name=\"" + applicationTypeName
					+ "\"]/url-pattern/text()";
			final Node urlPattern = evaluateXPathExpression(webxmlResource, expression);
			if (urlPattern != null) {
				Logger.debug("Found matching url-pattern: {} for class {}", urlPattern.getTextContent(),
						applicationTypeName);
				return urlPattern.getTextContent();
			}
			Logger.debug("No servlet mapping found for class '{}' in file '{}'", applicationTypeName,
					webxmlResource.getProjectRelativePath());
			return null;
		} catch (Exception e) {
			Logger.error("Unable to parse file '" + webxmlResource.getProjectRelativePath().toOSString()
					+ "' to find <servlet-mapping> elements", e);
			return null;
		} finally {
			Logger.tracePerf("Found application path for {} in web.xml in {}ms", applicationTypeName,
					(System.currentTimeMillis() - startTime));
		}
	}

	/**
	 * Attempts to find the <strong>node range</strong> for the applicationPath configured in the application's web
	 * deployment description. The applicationPath is expected to be configured as below: <code>
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
	 * @return the source range or null if it is not configured.
	 * @throws CoreException
	 */
	public static ISourceRange getApplicationPathLocation(final IResource webxmlResource,
			final String applicationTypeName) throws CoreException {
		if (webxmlResource == null) {
			return null;
		}
		if (!webxmlResource.exists()) {
			Logger.debug("No deployment descriptor '{}' does not exists", webxmlResource.getLocation());
			return null;
		}
		try {
			final String expression = "//servlet-mapping[servlet-name=\"" + applicationTypeName + "\"]";
			final Node servletMappingNode = evaluateXPathExpression(webxmlResource, expression);
			if (servletMappingNode != null) {
				StringWriter writer = new StringWriter();
				Transformer transformer = TransformerFactory.newInstance().newTransformer();
				transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
				transformer.transform(new DOMSource(servletMappingNode), new StreamResult(writer));
				String servletMappingXml = writer.toString();
				Logger.debug("Found matching servlet-mapping: {}", servletMappingXml);
				final InputStream contents = ((IFile) webxmlResource).getContents();
				int offset = findLocation(contents, servletMappingXml);
				if (offset != -1) {
					int length = servletMappingXml.length();
					return new SourceRange(offset - length + 1, length);
				}
				return new SourceRange(0, 0);
			}
		} catch (Exception e) {
			Logger.error("Unable to parse file '" + webxmlResource.getProjectRelativePath().toOSString()
					+ "' to find <servlet-mapping> elements", e);
		}

		Logger.debug("No servlet mapping found for class '{}' in file '{}'", applicationTypeName,
				webxmlResource.getProjectRelativePath());
		return null;
	}

	/**
	 * Return the searchString location in the input source
	 * 
	 * @param reader
	 * @param searchString
	 * @return the matching location or -1 if not found
	 * @throws IOException
	 */
	private static int findLocation(InputStream stream, String searchString) throws IOException {
		char[] buffer = new char[1024];
		int location = -1;
		int numCharsRead;
		int count = 0;
		Reader reader = null;
		try {
			reader = new InputStreamReader(stream);
			// reading the stream
			while ((numCharsRead = reader.read(buffer)) > 0) {
				// reading the buffer
				for (int c = 0; c < numCharsRead; c++) {
					location++;
					// character matching -> keep counting
					if (buffer[c] == searchString.charAt(count)) {
						count++;
					}
					// character mismatch -> reset counter
					else {
						count = 0;
					}
					// whole match -> \o/
					if (count == searchString.length()) {
						return location;
					}
				}
			}
			return -1;
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
	}

	/**
	 * Evaluate the (XPath) expression on the given resource.
	 * 
	 * @param webxmlResource
	 * @param applicationTypeName
	 * @return the xpath expression evalutation or null if the given
	 *         webxmlresource is null or does not exist, or if an error
	 *         occurred.
	 */
	private static Node evaluateXPathExpression(final IResource webxmlResource, final String expression) {
		if(webxmlResource == null || !webxmlResource.exists()) {
			return null;
		}
		FileInputStream fileInputStream = null;
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(false); // never forget this!
			dbf.setValidating(false);
			dbf.setFeature("http://xml.org/sax/features/namespaces", false);
			dbf.setFeature("http://xml.org/sax/features/validation", false);
			dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
			dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			DocumentBuilder builder = dbf.newDocumentBuilder();
			fileInputStream = new FileInputStream(webxmlResource.getLocation().toFile());
			InputSource inputSource = new InputSource(fileInputStream);
			Document doc = builder.parse(inputSource);
			XPath xpath = XPathFactory.newInstance().newXPath();
			XPathExpression expr = xpath.compile(expression);
			Node servletMapping = (Node) expr.evaluate(doc, XPathConstants.NODE);
			return servletMapping;
		} catch (Exception e) {
			Logger.error("Error while analyzing web deployment descriptor", e);
		} finally {
			if (fileInputStream != null) {
				try {
					fileInputStream.close();
				} catch (IOException e) {
				}
			}
		}
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
		if (webinfFolder == null) {
			return false;
		}
		final IFile file = webinfFolder.getFile("web.xml");
		if (file == null) {
			return false;
		}
		return resource.getFullPath().equals(file.getFullPath());
	}

	/**
	 * Returns {@code true} if the given project has a web deployment descriptor, {@code false} otherwise.
	 * 
	 * @param project
	 * @return
	 */
	public static boolean hasWebDeploymentDescriptor(IProject project) {
		final long startTime = System.currentTimeMillis();
		try {
			final IFolder webinfFolder = getWebInfFolder(project);
			if (webinfFolder == null) {
				return false;
			}
			final IFile file = webinfFolder.getFile("web.xml");
			return (file != null && file.exists());
		} finally {
			Logger.tracePerf("Looked-up Web Deployment Description in {}ms", (System.currentTimeMillis() - startTime));
		}
	}

	/**
	 * @param project the project in which to look for the web.xml file
	 * @return the underlying {@link IFile} for the {@code web.xml}, or null if none exists.
	 * @throws CoreException 
	 */
	public static IFile getWebDeploymentDescriptor(IProject project) throws CoreException {
		final IFolder webinfFolder = getWebInfFolder(project);
		final IFile file = webinfFolder.getFile("web.xml");
		if (file != null && file.exists()) {
			return (IFile) project.findMember(file.getProjectRelativePath());
		}
		return null;
	}

}
