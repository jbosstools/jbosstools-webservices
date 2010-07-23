/*******************************************************************************
 * Copyright (c) 2010 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.ws.ui.test.utils;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import javax.wsdl.Definition;
import javax.wsdl.WSDLException;

import org.jboss.tools.ws.ui.utils.TesterWSDLUtils;
import org.junit.Assert;
import org.junit.Test;

public class TesterWSDLUtilsTest {

	@Test
	public void testGetNSServiceNameAndMessageNameArray() {
		Definition def = readWSDL("/jbide6558/x.wsdl");
		String[] r = TesterWSDLUtils.getNSServiceNameAndMessageNameArray(def, "HelloWorldService", "HelloWorldPort", "HelloWorldBinding", "sayHello");
		Assert.assertArrayEquals(new String[] {"http://webservices.samples.jboss.org/", "HelloWorldService", "HelloWorldPort"}, r);

		def = readWSDL("/jbide6593/original.wsdl");
		r = TesterWSDLUtils.getNSServiceNameAndMessageNameArray(def, "gsearch_rss", "gsearch_rssSoap", "gsearch_rssSoap", "GetSearchResults");
		Assert.assertArrayEquals(new String[] {"http://www.ecubicle.net/webservices", "gsearch_rss", "gsearch_rssSoap"}, r);

		r = TesterWSDLUtils.getNSServiceNameAndMessageNameArray(def, "EchoService", "EchoPort", "EchoPortBinding", "echo");
		Assert.assertArrayEquals(new String[] {"http://webservices.www.ecubicle.net/", "EchoService", "EchoPort"}, r);
	}

	@Test
	public void testGetEndpointURL() {
		Definition def = readWSDL("/jbide6558/x.wsdl");
		String r = TesterWSDLUtils.getEndpointURL(def, "HelloWorldService", "HelloWorldPort", "HelloWorldBinding", "sayHello");
		Assert.assertEquals("http://localhost:8080/ws/HelloWorld", r);

		def = readWSDL("/jbide6593/original.wsdl");
		r = TesterWSDLUtils.getEndpointURL(def, "gsearch_rss", "gsearch_rssSoap", "gsearch_rssSoap", "GetSearchResults");
		Assert.assertEquals("http://www.ecubicle.net/gsearch_rss.asmx", r);

		r = TesterWSDLUtils.getEndpointURL(def, "EchoService", "EchoPort", "EchoPortBinding", "echo");
		Assert.assertEquals("http://localhost:8080/webws/EchoPortType", r);
	}

	@Test
	public void testGetActionURL() {
		Definition def = readWSDL("/jbide6558/x.wsdl");
		String r = TesterWSDLUtils.getActionURL(def, "HelloWorldService", "HelloWorldPort", "HelloWorldBinding", "sayHello");
		Assert.assertEquals("", r);

		def = readWSDL("/jbide6593/original.wsdl");
		r = TesterWSDLUtils.getActionURL(def, "gsearch_rss", "gsearch_rssSoap", "gsearch_rssSoap", "GetSearchResults");
		Assert.assertEquals("http://www.ecubicle.net/webservices/GetSearchResults", r);

		r = TesterWSDLUtils.getActionURL(def, "EchoService", "EchoPort", "EchoPortBinding", "echo");
		Assert.assertEquals("", r);
	}

	@Test
	public void testJBIDE6497() {
		String s1 = getSampleMessage("/jbide6497/original.wsdl", "gsearch_rss", "gsearch_rssSoap", "gsearch_rssSoap", "GetSearchResults");
		Assert.assertTrue("was: '" + s1, s1.contains("<GetSearchResults xmlns = \"http://www.ecubicle.net/webservices\">"));
		Assert.assertTrue("was: '" + s1, s1.contains("<gQuery>?</gQuery>"));

		s1 = getSampleMessage("/jbide6593/original.wsdl", "EchoService", "EchoPort", "EchoPortBinding", "echo");
		Assert.assertTrue("was: '" + s1, s1.contains("<p><age>?</age>"));
		Assert.assertTrue("was: '" + s1, s1.contains("<male>?</male>"));
		Assert.assertTrue("was: '" + s1, s1.contains("<tax>?</tax>"));
		Assert.assertTrue("was: '" + s1, s1.contains("<echo xmlns = \"http://test.jboss.org/ns\">"));
	}

	@Test
	public void testJBIDE6558() {
		String s1 = getSampleMessage("/jbide6558/x.wsdl", "HelloWorldService", "HelloWorldPort", "HelloWorldBinding", "sayHello");
		Assert.assertTrue(s1.contains("xmlns = \"http://webservices.samples.jboss.org/\""));
		Assert.assertTrue(s1.contains("<arg0>?</arg0>"));
		String s2 = getSampleMessage("/jbide6558/y.wsdl", "HelloWorldService", "HelloWorldPort", "HelloWorldBinding", "sayHello");
		Assert.assertEquals(s1, s2);
	}

	@Test
	public void testJBIDE6593() {
		String s1 = getSampleMessage("/jbide6497/original.wsdl", "gsearch_rss", "gsearch_rssSoap", "gsearch_rssSoap", "GetSearchResults");
		Assert.assertTrue(s1.contains("<GetSearchResults xmlns = \"http://www.ecubicle.net/webservices\">"));
		Assert.assertTrue(s1.contains("<gQuery>?</gQuery>"));

		String s2 = getSampleMessage("/jbide6593/original.wsdl", "gsearch_rss", "gsearch_rssSoap", "gsearch_rssSoap", "GetSearchResults");
		Assert.assertEquals(s1, s2);
	}

	@Test
	public void testJBIDE6694() {
		String s1 = getSampleMessage("/jbide6694/ConverterPortType.wsdl", "ConverterPortType", "ConverterPortTypeImplPort", "ConverterPortTypeBinding", "convert");
		Assert.assertTrue(s1.contains("<ChangeUnit xmlns = \"http://test.jboss.org/ns\">"));
		Assert.assertTrue(s1.contains("<value>?</value>"));
		Assert.assertTrue(s1.contains("<fromUnit>?</fromUnit>"));
		Assert.assertTrue(s1.contains("<toUnit>?</toUnit>"));

		String s2 = getSampleMessage("/jbide6694/jbide6694.wsdl", "Converter", "ConverterPort", "ConverterBinding", "convert");
		Assert.assertTrue(s2.contains("<ChangeUnit xmlns = \"http://test.jboss.org/ns\">"));
		Assert.assertTrue(s2.contains("<value>?</value>"));
		Assert.assertTrue(s2.contains("<fromUnit>?</fromUnit>"));
		Assert.assertTrue(s2.contains("<toUnit>?</toUnit>"));
	}

	private String getSampleMessage(String res, String service, String port, String binding, String operation) {
		Definition def = readWSDL(res);
		return TesterWSDLUtils.getSampleSOAPInputMessage(def, service, port, binding, operation);
	}

	private Definition readWSDL(String path) {
		try {
			URL url = TesterWSDLUtilsTest.class.getResource(path).toURI().toURL();
			return TesterWSDLUtils.readWSDLURL(url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} catch (URISyntaxException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} catch (WSDLException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		return null;
	}
}
